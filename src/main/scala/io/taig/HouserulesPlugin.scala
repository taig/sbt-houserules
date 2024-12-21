package io.taig

import sbt.Keys._
import sbt._

object HouserulesPlugin extends AutoPlugin {
  object autoImport {
    val noPublishSettings: Seq[Def.Setting[_]] = Def.settings(
      publish := {},
      publishLocal := {},
      publishArtifact := false,
      publish / skip := true
    )
  }

  import autoImport._

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings(
    organization := "io.taig",
    organizationHomepage := Some(url("https://taig.io/")),
    shellPrompt := { state =>
      val name = Project.extract(state).get(normalizedName)
      s"sbt:$name> "
    }
  )

  override def buildSettings: Seq[Def.Setting[_]] = Def.settings()

  override def projectSettings: Seq[Def.Setting[_]] = Def.settings()
}
