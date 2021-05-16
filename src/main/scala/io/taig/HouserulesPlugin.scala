package io.taig

import com.jsuereth.sbtpgp.SbtPgp.autoImport._
import io.github.davidgregory084.TpolecatPlugin
import org.scalafmt.sbt.ScalafmtPlugin
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.ReleasePlugin.autoImport._
import xerial.sbt.Sonatype.GitLabHosting
import xerial.sbt.Sonatype.autoImport._

object HouserulesPlugin extends AutoPlugin {
  object autoImport {
    val githubProject = settingKey[String]("Github project identifier")

    val noPublishSettings: Seq[Def.Setting[_]] = Def.settings(
      publish := {},
      publishLocal := {},
      publishArtifact := false,
      publish / skip := true
    )

    val sonatypePublishSettings: Seq[Def.Setting[_]] = Def.settings(
      credentials ++= {
        (for {
          username <- sys.env.get("SONATYPE_USERNAME")
          password <- sys.env.get("SONATYPE_PASSWORD")
        } yield Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          username,
          password
        )).toList
      },
      homepage := Some(url(s"https://github.com/taig/${githubProject.value.toLowerCase}")),
      licenses := Seq(
        "MIT" -> url(s"https://raw.githubusercontent.com/taig/${githubProject.value.toLowerCase}/master/LICENSE")
      ),
      useGpg := false,
      pgpPassphrase := sys.env
        .get("PGP_PASSWORD")
        .fold(Array.empty[Char])(_.toCharArray)
        .some,
      pgpSecretRing := {
        val secring = file("/tmp/secring.asc")
        sys.env.get("PGP_SECRING").foreach(IO.write(secring, _))
        secring
      },
      pomIncludeRepository := { _ => false },
      Test / publishArtifact := false,
      publishMavenStyle := true,
      publishTo := sonatypePublishToBundle.value,
      sonatypeProjectHosting := Some(GitLabHosting("taig", githubProject.value, "mail@taig.io")),
      sonatypeProfileName := "io.taig"
    )

    val mode = settingKey[Mode]("Execution mode, either 'tolerant' or 'strict'")

    val scalafmtRules = settingKey[Seq[String]]("scalafmt rules")
  }

  import autoImport._

  lazy val IntegrationTest = config("it").extend(Test)

  override def requires: Plugins = ReleasePlugin && ScalafmtPlugin && TpolecatPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = globals

  override def projectSettings: Seq[Def.Setting[_]] =
    compilerPlugins ++ releaseSettings ++ projects

  override def buildSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafmtConfig := {
      val file = (LocalRootProject / baseDirectory).value / ".scalafmt.conf"
      val content =
        s"""# Auto generated scalafmt rules
           |# Use `scalafmtRules` sbt setting to modify
           |${scalafmtRules.value.mkString("\n")}""".stripMargin
      IO.write(file, content)
      file
    },
    scalafmtRules :=
      "assumeStandardLibraryStripMargin = true" ::
        "maxColumn = 120" ::
        "rewrite.rules = [SortImports, SortModifiers]" ::
        "version = 2.7.5" ::
        Nil
  )

  override def projectConfigurations: Seq[Configuration] = Seq(IntegrationTest)

  lazy val compilerPlugins: Seq[Def.Setting[_]] = Def.settings(
    libraryDependencies ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect {
        case (2, minor) =>
          val plugins = compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1") ::
            compilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full) ::
            Nil

          val paradise = compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

          if (minor <= 12) paradise :: plugins else plugins
      }
      .toList
      .flatten
  )

  lazy val globals: Seq[Def.Setting[_]] = Def.settings(
    githubProject := (LocalRootProject / normalizedName).value,
    mode := sys.props.get("mode").flatMap(Mode.parse).getOrElse(Mode.Default),
    organization := "io.taig",
    organizationHomepage := Some(url("https://taig.io/")),
    shellPrompt := { state =>
      val name = Project.extract(state).get(normalizedName)
      s"sbt:$name> "
    }
  )

  lazy val projects: Seq[Def.Setting[_]] = Def.settings(
    Defaults.itSettings,
    inConfig(IntegrationTest)(scalafmtConfigSettings),
    commands += Command.command("publishAndRelease") { state =>
      val validateEnv: String => Unit = key => if (!sys.env.contains(key)) sys.error(s"$$$key is not defined")

      validateEnv("SONATYPE_USERNAME")
      validateEnv("SONATYPE_PASSWORD")

      val snapshot: Boolean = Project.extract(state).get(isSnapshot)

      if (snapshot) "+publishSigned" :: state
      else {
        validateEnv("PGP_SECRING")
        validateEnv("PGP_PASSWORD")
        "+publishSigned" :: "sonatypeBundleRelease" :: state
      }
    },
    libraryDependencies ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect { case (2, _) => "org.typelevel" %% "simulacrum" % "1.0.1" % "provided" }
      .toList,
    scalacOptions ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect { case (2, minor) if minor <= 12 => List("-Xexperimental") }
      .getOrElse(List.empty),
    scalacOptions ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect { case (2, minor) if minor >= 13 => "-Ymacro-annotations" }
      .toList,
    scalacOptions ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect { case (2, minor) if minor >= 12 => "-Ywarn-macros:after" }
      .toList,
    scalacOptions --= {
      if (mode.value == Mode.Tolerant) List("-Xfatal-warnings") else Nil
    },
    scalafmtAll := {
      (Compile / scalafmt)
        .dependsOn(Test / scalafmt)
        .dependsOn(IntegrationTest / scalafmt)
        .dependsOn(Compile / scalafmtSbt)
        .value
    },
    scalafmtCheckAll := {
      (Compile / scalafmtCheck)
        .dependsOn(Test / scalafmtCheck)
        .dependsOn(IntegrationTest / scalafmtCheck)
        .dependsOn(Compile / scalafmtSbtCheck)
        .value
    }
  )

  lazy val releaseSettings: Seq[Def.Setting[_]] = Def.settings(
    releaseCommitMessage := s"Release ${releaseTagName.value}",
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      releaseStepCommandAndRemaining("scalafmtCheckAll"),
      runClean,
      releaseStepCommandAndRemaining("+test"),
      inquireVersions,
      setReleaseVersion,
      ReleaseSteps.updateChangelog,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      ReleaseSteps.commitNextVersion,
      pushChanges
    ),
    releaseTagComment := s"Release ${releaseTagName.value}",
    releaseTagName := version.value
  )
}
