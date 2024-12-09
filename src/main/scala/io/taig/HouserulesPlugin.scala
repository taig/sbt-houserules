package io.taig

import org.typelevel.sbt.tpolecat._
import org.typelevel.sbt.tpolecat.TpolecatPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import scalafix.sbt.ScalafixPlugin
import scalafix.sbt.ScalafixPlugin.autoImport._
import sbt.Keys._
import sbt._
import scala.collection.immutable.ListMap
import sbt.complete.DefaultParsers

object HouserulesPlugin extends AutoPlugin {
  object autoImport {
    val noPublishSettings: Seq[Def.Setting[_]] = Def.settings(
      publish := {},
      publishLocal := {},
      publishArtifact := false,
      publish / skip := true
    )

    val scalafixGenerateConfig = taskKey[Unit]("Generate scalafix configuration file")

    val scalafixCheck = taskKey[Unit]("scalafix --check")

    val scalafixCheckAll = taskKey[Unit]("scalafixAll --check")

    val scalafixConfiguration = settingKey[Seq[(String, String)]]("scalafix configration")

    val scalafixConfigurationRules = settingKey[Seq[String]]("scalafix rules")

    val scalafmtGenerateConfig = taskKey[Unit]("Generate scalafmt configuration file")

    val scalafmtConfiguration = settingKey[Seq[(String, String)]]("scalafmt configration")
  }

  import autoImport._

  override def requires: Plugins = ScalafixPlugin && ScalafmtPlugin && TpolecatPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
    organization := "io.taig",
    organizationHomepage := Some(url("https://taig.io/")),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    shellPrompt := { state =>
      val name = Project.extract(state).get(normalizedName)
      s"sbt:$name> "
    }
  ) ++ scalafmtGlobalSettings ++ scalafixGlobalSettings

  override def buildSettings: Seq[Def.Setting[_]] = Def.settings(
    tpolecatDefaultOptionsMode := DevMode
  ) ++ scalafmtBuildSettings ++ scalafixBuildSettings

  override def projectSettings: Seq[Def.Setting[_]] = scalafmtProjectSettings ++ scalafixProjectSettings

  def scalafmtGlobalSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafmtConfiguration := List(
      "version" -> "3.8.3",
      "maxColumn" -> "120",
      "assumeStandardLibraryStripMargin" -> "true",
      "rewrite.rules" -> "[Imports, SortModifiers]",
      "rewrite.imports.sort" -> "original",
      "project.excludePaths" -> """["glob:**/metals.sbt"]"""
    )
  )

  def scalafmtBuildSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafmtConfiguration ++= List(
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

  def scalafmtProjectSettings: Seq[Def.Setting[_]] = Def.settings(
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

  def scalafixGlobalSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafixConfiguration := List(
      "DisableSyntax.noVars" -> "true",
      "DisableSyntax.noThrows" -> "true",
      "DisableSyntax.noNulls" -> "true",
      "DisableSyntax.noReturns" -> "true",
      "DisableSyntax.noWhileLoops" -> "true",
      "DisableSyntax.noAsInstanceOf" -> "true",
      "DisableSyntax.noIsInstanceOf" -> "true",
      "DisableSyntax.noXml" -> "true",
      "OrganizeImports.expandRelative" -> "true",
      "OrganizeImports.removeUnused" -> "true",
      "OrganizeImports.targetDialect" -> "Scala3"
    ),
    scalafixConfigurationRules := List(
      "DisableSyntax",
      "LeakingImplicitClassVal",
      "NoAutoTupling",
      "NoValInForComprehension",
      "OrganizeImports",
      "RedundantSyntax",
      "RemoveUnused"
    )
  )

  def scalafixBuildSettings: Seq[Def.Setting[_]] = Def.settings()

  def scalafixProjectSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafixAll := {
      (Test / scalafix)
        .toTask("")
        .dependsOn((Compile / scalafix).toTask(""))
        .value
    },
    scalafixCheckAll := {
      (Test / scalafixCheck)
        .dependsOn(Compile / scalafixCheck)
        .value
    },
    scalafixGenerateConfig := {
      val file = scalafixConfig.value.getOrElse(sys.error("scalafixConfig is not defined"))

      val content =
        s"""# Auto generated scalafix configuration
           |# Use `scalafixConfiguration` sbt setting to modify
           |${scalafixConfiguration.value.map { case (key, value) => s"$key = $value" }.mkString("\n")}
           |
           |# Use `scalafixConfigurationRules` sbt setting to modify
           |rules = ${scalafixConfigurationRules.value.mkString("[", ", ", "]")}""".stripMargin

      IO.write(file, content)
    },
    scalafixConfig := Some(scalafixConfig.value.getOrElse(sourceDirectory.value / ".scalafix.conf")),
    Seq(Compile, Test).flatMap { configuration =>
      inConfig(configuration)(
        Def.settings(
          scalafix := scalafix.dependsOn(scalafixGenerateConfig).evaluated,
          scalafixCheck := scalafix.toTask(" --check").dependsOn(scalafixGenerateConfig).value
        )
      )
    }
  )
}
