package io.taig

import org.typelevel.sbt.tpolecat._
import org.typelevel.sbt.tpolecat.TpolecatPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import sbt.Keys._
import sbt._
import scala.collection.immutable.ListMap

object HouserulesPlugin extends AutoPlugin {
  object autoImport {
    val githubProject = settingKey[String]("Github project identifier")

    val noPublishSettings: Seq[Def.Setting[_]] = Def.settings(
      publish := {},
      publishLocal := {},
      publishArtifact := false,
      publish / skip := true
    )

    val scalafmtRules = settingKey[ListMap[String, String]]("scalafmt rules")
  }

  import autoImport._

  override def requires: Plugins = ScalafmtPlugin && TpolecatPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = globals

  override def projectSettings: Seq[Def.Setting[_]] = projects

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
    scalafmtRules := ListMap(
      "version" -> "3.7.17",
      "maxColumn" -> "120",
      "assumeStandardLibraryStripMargin" -> "true",
      "rewrite.rules" -> "[Imports, SortModifiers]",
      "rewrite.imports.sort" -> "original",
      "runner.dialect" -> (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11)) => "scala211"
        case Some((2, 12)) => "scala212"
        case Some((2, 13)) => "scala213"
        case Some((3, _))  => "scala3"
        case _             => "default"
      }),
      "project.excludePaths" -> """["glob:**/metals.sbt"]"""
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
    scalafmtAll := {
      (Compile / scalafmt)
        .dependsOn(Test / scalafmt)
        .dependsOn(Compile / scalafmtSbt)
        .value
    },
    scalafmtCheckAll := {
      (Compile / scalafmtCheck)
        .dependsOn(Test / scalafmtCheck)
        .dependsOn(Compile / scalafmtSbtCheck)
        .value
    }
  )
}
