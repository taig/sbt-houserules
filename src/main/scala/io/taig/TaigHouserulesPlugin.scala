package io.taig

import java.time.Instant

import com.jsuereth.sbtpgp.SbtPgp.autoImport._
import mdoc.MdocPlugin.autoImport._
import microsites.MicrositesPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.ReleasePlugin.autoImport._
import xerial.sbt.Sonatype.GitLabHosting
import xerial.sbt.Sonatype.autoImport._

object TaigHouserulesPlugin extends AutoPlugin {
  object autoImport {
    val githubProject = settingKey[String]("Github project identifier")

    val noPublishSettings: Seq[Def.Setting[_]] = Def.settings(
      publish := {},
      publishLocal := {},
      publishArtifact := false,
      skip in publish := true
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
      homepage := Some(
        url(s"https://github.com/taig/${githubProject.value.toLowerCase}")
      ),
      licenses := Seq(
        "MIT" -> url(
          s"https://raw.githubusercontent.com/taig/${githubProject.value.toLowerCase}/master/LICENSE"
        )
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
      pomIncludeRepository := { _ =>
        false
      },
      publishArtifact in Test := false,
      publishMavenStyle := true,
      publishTo := sonatypePublishToBundle.value,
      sonatypeProjectHosting := Some(
        GitLabHosting("taig", githubProject.value, "mail@taig.io")
      ),
      sonatypeProfileName := "io.taig"
    )

    val micrositeSettings: Seq[Def.Setting[_]] =
      noPublishSettings ++ Def.settings(
        micrositeAuthor := "Niklas Klein",
        micrositeBaseUrl := s"/${githubProject.value}",
        micrositeCompilingDocsTool := WithMdoc,
        micrositeCssDirectory := mdocIn.value / "stylesheet",
        micrositeGithubOwner := "taig",
        micrositeGithubRepo := githubProject.value,
        micrositeGithubToken := Option(System.getenv("GITHUB_TOKEN")),
        micrositeGitterChannel := false,
        micrositeFooterText := Some(
          s"<p>Built for version ${version.value} at ${Instant.now()}</p>"
        ),
        micrositeImgDirectory := mdocIn.value / "image",
        micrositeJsDirectory := mdocIn.value / "javascript",
        micrositePushSiteWith := GitHub4s,
        micrositeTwitterCreator := "@tttaig",
        micrositeUrl := "http://taig.io"
      )

    val mode =
      settingKey[Mode]("Execution mode, either 'tolerant' or 'strict'")

    val scalafmtGenerateConfig = taskKey[File]("Generate .scalafmt.conf")

    val scalafmtRules = settingKey[Seq[String]]("scalafmt rules")
  }

  import autoImport._

  override def requires: Plugins = ReleasePlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = globals

  override def projectSettings: Seq[Def.Setting[_]] =
    compilerPlugins ++ releaseSettings ++ projects

  lazy val compilerPlugins: Seq[Def.Setting[_]] = Def.settings(
    libraryDependencies ++=
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0") ::
        compilerPlugin(
          "org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary
        ) ::
        Nil,
    libraryDependencies ++=
      CrossVersion
        .partialVersion(scalaVersion.value)
        .collect {
          case (2, minor) if minor <= 12 =>
            compilerPlugin(
              "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
            )
        }
        .toList,
    scalacOptions ++=
      CrossVersion
        .partialVersion(scalaVersion.value)
        .collect {
          case (2, minor) if minor >= 13 => "-Ymacro-annotations"
        }
        .toList
  )

  lazy val globals: Seq[Def.Setting[_]] = Def.settings(
    githubProject := (normalizedName in LocalRootProject).value,
    mode := sys.props
      .get("mode")
      .flatMap(Mode.parse)
      .getOrElse(Mode.Default),
    organization := "io.taig",
    organizationHomepage := Some(url("http://taig.io/")),
    scalafmtGenerateConfig := {
      val file = (baseDirectory in LocalRootProject).value / ".scalafmt.conf"
      val content =
        s"""# Auto generated scalafmt rules
           |# Use `scalafmtRules` sbt setting to modify
           |${scalafmtRules.value.mkString("\n")}""".stripMargin
      IO.write(file, content)
      file
    },
    scalafmtRules :=
      "assumeStandardLibraryStripMargin = true" ::
        "maxColumn = 80" ::
        "rewrite.rules = [sortimports]" ::
        "version=2.0.0" ::
        Nil
  )

  lazy val projects: Seq[Def.Setting[_]] = Def.settings(
    commands += Command.command("publishAndRelease") { state =>
      val validateEnv: String => Unit =
        key => if (sys.env.get(key).isEmpty) sys.error(s"$$$key is not defined")

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
    libraryDependencies ++=
      "com.github.mpilquist" %% "simulacrum" % "0.19.0" % "provided" ::
        Nil,
    scalacOptions ++=
      "-deprecation" ::
        "-feature" ::
        "-language:higherKinds" ::
        "-language:implicitConversions" ::
        "-language:postfixOps" ::
        "-unchecked" ::
        "-Ypatmat-exhaust-depth" :: "40" ::
        "-Ywarn-dead-code" ::
        "-Ywarn-value-discard" ::
        Nil,
    scalacOptions ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect {
        case (2, minor) if minor >= 12 =>
          "-Ywarn-unused:imports" ::
            Nil
      }
      .getOrElse(List.empty),
    scalacOptions ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect {
        case (2, minor) if minor <= 12 =>
          "-Xexperimental" ::
            "-Ypartial-unification" ::
            "-Ywarn-infer-any" ::
            Nil
      }
      .getOrElse(List.empty),
    scalacOptions ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect {
        case (2, minor) if minor <= 11 =>
          "-Ywarn-unused-import" ::
            Nil
      }
      .getOrElse(List.empty),
    scalacOptions ++= {
      if (mode.value == Mode.Strict) List("-Xfatal-warnings") else Nil
    },
    scalacOptions in (Compile, console) --=
      "-Ywarn-unused:imports" ::
        "-Ywarn-unused-import" ::
        Nil,
    scalafmtAll := {
      (scalafmt in Compile)
        .dependsOn(scalafmt in Test)
        .dependsOn(scalafmtSbt in Compile)
        .value
    },
    scalafmtCheckAll := {
      (scalafmtCheck in Compile)
        .dependsOn(scalafmtCheck in Test)
        .dependsOn(scalafmtSbtCheck in Compile)
        .value
    },
    scalafmtConfig := scalafmtGenerateConfig.value
  )

  lazy val releaseSettings: Seq[Def.Setting[_]] = Def.settings(
    releaseCommitMessage := s"Release ${releaseTagName.value}",
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      releaseStepCommandAndRemaining("scalafmtCheckAll"),
      runClean,
      runTest,
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
