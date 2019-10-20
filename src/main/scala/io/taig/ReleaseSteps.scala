package io.taig

import sbt._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations

import scala.language.postfixOps

object ReleaseSteps {
  val updateChangelog: ReleaseStep = ReleaseStep { state =>
    val extracted = Project.extract(state)
    val version = extracted.get(Keys.version)

    SimpleReader.readLine(s"Update changelog to $version : ") match {
      case Some(_) => //
      case None    => sys.error("No version provided!")
    }

    val vcs = extracted.get(releaseVcs).get
    val base = vcs.baseDir.getCanonicalFile

    val changelog = IO.relativize(base, base / "CHANGELOG.md").get

    vcs.add(changelog) !!

    state
  }

  val commitNextVersion: ReleaseStep = ReleaseStep { state =>
    val state1 = Project.extract(state)
    val (newState1, version) = state1.runTask(releaseTagName, state)
    val state2 = Project.extract(newState1)
    val newState2 =
      state2.appendWithSession(
        Seq(releaseCommitMessage := s"Prepare $version"),
        newState1
      )
    ReleaseStateTransformations.commitNextVersion(newState2)
  }
}
