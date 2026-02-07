package io.taig

import scalafix.sbt.ScalafixPlugin
import scalafix.sbt.ScalafixPlugin.autoImport._
import sbt.Keys._
import sbt._
import scala.collection.immutable.ListMap

object HouserulesScalafixPlugin extends AutoPlugin {
  object autoImport {
    val scalafixGenerateConfig = taskKey[Unit]("Generate scalafix configuration file")

    val scalafixCheck = taskKey[Unit]("scalafix --check")

    val scalafixCheckAll = taskKey[Unit]("scalafixAll --check")

    val scalafixConfiguration = settingKey[Map[String, String]]("scalafix configration")

    val scalafixConfigurationRules = settingKey[Seq[String]]("scalafix rules")
  }

  import autoImport._

  override def requires: Plugins = ScalafixPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
    scalafixConfiguration := ListMap(
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
      "OrganizeImports.targetDialect" -> "Auto",
      "RemoveUnused.imports" -> "false",
      "RemoveUnused.privates" -> "true",
      "RemoveUnused.locals" -> "true",
      "RemoveUnused.patternvars" -> "true",
      "RemoveUnused.params" -> "true"
    ),
    scalafixConfigurationRules := List(
      "DisableSyntax",
      "LeakingImplicitClassVal",
      "NoAutoTupling",
      "NoValInForComprehension",
      "OrganizeImports",
      "RedundantSyntax",
      "RemoveUnused"
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )

  override def buildSettings: Seq[Def.Setting[_]] = Def.settings()

  override def projectSettings: Seq[Def.Setting[_]] = Def.settings(
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
