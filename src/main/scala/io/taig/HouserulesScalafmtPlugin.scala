package io.taig

import org.scalafmt.sbt.ScalafmtPlugin
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import sbt.Keys._
import sbt._
import scala.collection.immutable.ListMap

object HouserulesScalafmtPlugin extends AutoPlugin {
  object autoImport {
    val scalafmtGenerateConfig = taskKey[Unit]("Generate scalafmt configuration file")

    val scalafmtConfiguration = settingKey[Map[String, String]]("scalafmt configration")
  }

  import autoImport._

  override def requires: Plugins = ScalafmtPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafmtConfiguration := ListMap(
      "version" -> "3.8.5",
      "maxColumn" -> "120",
      "assumeStandardLibraryStripMargin" -> "true",
      "rewrite.rules" -> "[Imports, SortModifiers]",
      "rewrite.imports.sort" -> "original",
      "project.excludePaths" -> """["glob:**/metals.sbt"]"""
    )
  )

  override def buildSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafmtConfiguration ++= ListMap(
      "runner.dialect" -> (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11)) => "scala211"
        case Some((2, 12)) => "scala212"
        case Some((2, 13)) => "scala213"
        case Some((3, _))  => "scala3"
        case _             => "default"
      })
    ),
    scalafmtPrintDiff := true
  )

  override def projectSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafmtGenerateConfig := {
      val target = scalafmtConfig.value
      val configuration = scalafmtConfiguration.value
      val content =
        s"""# Auto generated scalafmt configuration
           |# Use `scalafmtConfiguration` sbt setting to modify
           |${configuration.map { case (key, value) => s"$key = $value" }.mkString("\n")}""".stripMargin
      IO.write(target, content)
    },
    scalafmtConfig := sourceDirectory.value / ".scalafmt.conf",
    Seq(Compile, Test).flatMap { configuration =>
      inConfig(configuration)(
        Def.settings(
          scalafmt := scalafmt.dependsOn(scalafmtGenerateConfig).value,
          scalafmtCheck := scalafmtCheck.dependsOn(scalafmtGenerateConfig).value,
          scalafmtSbt := scalafmtSbt.dependsOn(scalafmtGenerateConfig).value,
          scalafmtSbtCheck := scalafmtSbtCheck.dependsOn(scalafmtGenerateConfig).value
        )
      )
    },
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
