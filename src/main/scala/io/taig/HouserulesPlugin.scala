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

    val scalafixRules = settingKey[ListMap[String, String]]("scalafix rules")

    val scalafmtRules = settingKey[ListMap[String, String]]("scalafmt rules")
  }

  import autoImport._

  override def requires: Plugins = ScalafixPlugin && ScalafmtPlugin && TpolecatPlugin

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
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    shellPrompt := { state =>
      val name = Project.extract(state).get(normalizedName)
      s"sbt:$name> "
    }
  )

  lazy val projects: Seq[Def.Setting[_]] = Def.settings(
    Seq(Compile, Test).flatMap(scalafixConfigSettings),
    scalafixAll := Def.inputTaskDyn {
      val input: String = DefaultParsers.spaceDelimited("").parsed.toList match {
        case Nil => ""
        case arguments => arguments.mkString(" ", " ", "")
      }

      (Test / scalafix).toTask(input).dependsOn((Compile / scalafix).toTask(input))
    }.evaluated,
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

  def scalafixConfigSettings(configuration: Configuration): Seq[Def.Setting[_]] = inConfig(configuration)(
    Def.settings(
      scalafix := {
        val file = scalafixConfig.value.getOrElse(baseDirectory.value / ".scalafix.conf")

        val content =
          s"""# Auto generated scalafix rules
             |# Use `scalafixRules` sbt setting to modify
             |${scalafixRules.value.map { case (key, value) => s"$key = $value" }.mkString("\n")}""".stripMargin

        IO.write(file, content)

        scalafix.evaluated
      },
      scalafixRules := ListMap(
        "rules" -> "[DisableSyntax, LeakingImplicitClassVal, NoAutoTupling, NoValInForComprehension, OrganizeImports, RedundantSyntax, RemoveUnused]",
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
      )
    )
  )
}
