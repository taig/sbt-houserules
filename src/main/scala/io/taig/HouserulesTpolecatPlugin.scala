package io.taig

import org.typelevel.sbt.tpolecat._
import org.typelevel.sbt.tpolecat.TpolecatPlugin.autoImport._
import sbt._

object HouserulesTpolecatPlugin extends AutoPlugin {
  override def requires: Plugins = TpolecatPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = Def.settings()

  override def buildSettings: Seq[Def.Setting[_]] = Def.settings(
    tpolecatDefaultOptionsMode := DevMode
  )

  override def projectSettings: Seq[Def.Setting[_]] = Def.settings()
}
