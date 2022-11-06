package io.taig

import io.github.davidgregory084._
import io.github.davidgregory084.TpolecatPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings
import sbt.Keys._
import sbt._

object HouserulesPlugin extends AutoPlugin {
  object autoImport {
    val githubProject = settingKey[String]("Github project identifier")

    val noPublishSettings: Seq[Def.Setting[_]] = Def.settings(
      publish := {},
      publishLocal := {},
      publishArtifact := false,
      publish / skip := true
    )

    val scalafmtRules = settingKey[Map[String, String]]("scalafmt rules")
  }

  import autoImport._

  lazy val IntegrationTest = config("it").extend(Test)

  override def requires: Plugins = ScalafmtPlugin && TpolecatPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = globals

  override def projectSettings: Seq[Def.Setting[_]] = compilerPlugins ++ projects

  override def buildSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafmtConfig := {
      val file = (LocalRootProject / baseDirectory).value / ".scalafmt.conf"
      val content =
        s"""# Auto generated scalafmt rules
           |# Use `scalafmtRules` sbt setting to modify
           |${scalafmtRules.value.map { case (key, value) => s"$key = $value" }.mkString("\n")}""".stripMargin
      IO.write(file, content)
      file
    },
    scalafmtRules := Map(
      "assumeStandardLibraryStripMargin" -> "true",
      "maxColumn" -> "120",
      "rewrite.rules" -> "[Imports, SortModifiers]",
      "rewrite.imports.sort" -> "original",
      "version" -> "3.6.1",
      "runner.dialect" -> (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11)) => "scala211"
        case Some((2, 12)) => "scala212"
        case Some((2, 13)) => "scala213"
        case Some((3, _))  => "scala3"
        case _             => "default"
      })
    ),
    tpolecatDefaultOptionsMode := {
      sys.props
        .get("mode")
        .map {
          case "ci"      => CiMode
          case "dev"     => DevMode
          case "release" => ReleaseMode
          case mode      => sys.error(s"Unknown mode '$mode'. Must be one of: ci | dev | release")
        }
        .getOrElse(DevMode)
    }
  )

  override def projectConfigurations: Seq[Configuration] = Seq(IntegrationTest)

  lazy val compilerPlugins: Seq[Def.Setting[_]] = Def.settings(
    libraryDependencies ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect { case (2, minor) =>
        val plugins = compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1") ::
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full) ::
          Nil

        val paradise = compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

        if (minor <= 12) paradise :: plugins else plugins
      }
      .toList
      .flatten
  )

  lazy val globals: Seq[Def.Setting[_]] = Def.settings(
    githubProject := (LocalRootProject / normalizedName).value,
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
}
