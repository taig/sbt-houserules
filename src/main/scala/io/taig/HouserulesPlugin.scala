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
    val githubProject = settingKey[String]("Github project identifier")

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

    val scalafmtConfiguration = settingKey[Seq[(String, String)]]("scalafmt configration")
  }

  import autoImport._

  override def requires: Plugins = ScalafixPlugin && ScalafmtPlugin && TpolecatPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
    githubProject := (LocalRootProject / normalizedName).value,
    organization := "io.taig",
    organizationHomepage := Some(url("https://taig.io/")),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    shellPrompt := { state =>
      val name = Project.extract(state).get(normalizedName)
      s"sbt:$name> "
    }
  ) ++ scalafmtPresets ++ scalafixPresets

  override def projectSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafmtConfiguration := (ThisBuild / scalafmtConfiguration).value,
    Seq(Default).flatMap(scalafmtSettings),
    scalafixConfiguration := (ThisBuild / scalafixConfiguration).value,
    scalafixConfigurationRules := (ThisBuild / scalafixConfigurationRules).value,
    Seq(Compile, Test).flatMap(scalafixSettings),
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

  override def buildSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafmtConfiguration := (Global / scalafmtConfiguration).value,
    scalafixConfiguration := (Global / scalafixConfiguration).value,
    scalafixConfigurationRules := (Global / scalafixConfigurationRules).value,
    tpolecatDefaultOptionsMode := DevMode
  )

  def scalafmtPresets: Seq[Def.Setting[_]] = scalafmtConfiguration := List(
    "version" -> "3.8.3",
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
  )

  def scalafmtSettings(configuration: Configuration): Seq[Def.Setting[_]] = inConfig(configuration)(
    Def.settings(
      scalafmtConfig := {
        val file = (LocalRootProject / baseDirectory).value / ".scalafmt.conf"
        val content =
          s"""# Auto generated scalafmt configuration
             |# Use `scalafmtConfiguration` sbt setting to modify
             |${scalafmtConfiguration.value.map { case (key, value) => s"$key = $value" }.mkString("\n")}""".stripMargin
        IO.write(file, content)
        file
      }
    )
  )

  def scalafixPresets: Seq[Def.Setting[_]] = Def.settings(
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

  def scalafixSettings(configuration: Configuration): Seq[Def.Setting[_]] = inConfig(configuration)(
    Def.settings(
      scalafixConfig := Some(scalafixConfig.value.getOrElse(baseDirectory.value / ".scalafix.conf")),
      scalafixGenerateConfig := {
        val file = scalafixConfig.value.getOrElse(baseDirectory.value / ".scalafix.conf")

        val content =
          s"""# Auto generated scalafix configuration
             |# Use `scalafixConfiguration` sbt setting to modify
             |${scalafixConfiguration.value.map { case (key, value) => s"$key = $value" }.mkString("\n")}
             |
             |# Use `scalafixConfiguration` sbt setting to modify
             |rules = ${scalafixConfigurationRules.value.mkString("[", ", ", "]")}""".stripMargin

        IO.write(file, content)
      },
      scalafix := scalafix.dependsOn(scalafixGenerateConfig).evaluated,
      scalafixCheck := scalafix.toTask(" --check").value,
      scalafixConfiguration := (Default / scalafixConfiguration).value,
      scalafixConfigurationRules := (Default / scalafixConfigurationRules).value
    )
  )
}
