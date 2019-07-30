import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import xerial.sbt.Sonatype.GitLabHosting

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.2")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.5")

name := "sbt-houserules"

organization := "io.taig"

releaseCommitMessage := s"Release ${releaseTagName.value}"

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  releaseStepCommandAndRemaining("scalafmtCheckAll"),
  runClean,
  inquireVersions,
  setReleaseVersion,
  ReleaseSteps.updateChangelog,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  ReleaseSteps.commitNextVersion,
  pushChanges
)

releaseTagComment := s"Release ${releaseTagName.value}"

releaseTagName := version.value

sbtPlugin := true

scalafmtAll := {
  (scalafmt in Compile)
    .dependsOn(scalafmt in Test)
    .dependsOn(scalafmtSbt in Compile)
    .value
}

scalafmtCheckAll := {
  (scalafmtCheck in Compile)
    .dependsOn(scalafmtCheck in Test)
    .dependsOn(scalafmtSbtCheck in Compile)
    .value
}

credentials ++= {
  (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )).toList
}
homepage := Some(url(s"https://github.com/taig/sbt-houserules"))
licenses := Seq(
  "MIT" -> url(
    s"https://raw.githubusercontent.com/taig/sbt-houserules/master/LICENSE"
  )
)
pgpPassphrase := sys.env
  .get("PGP_PASSWORD")
  .fold(Array.empty[Char])(_.toCharArray)
  .some
pgpSecretRing := {
  val secring = file("/tmp/secring.asc")
  sys.env.get("PGP_SECRING").foreach(IO.write(secring, _))
  secring
}
pomIncludeRepository := { _ =>
  false
}
publishArtifact in Test := false
publishMavenStyle := true
publishTo := sonatypePublishTo.value
sonatypeProjectHosting := Some(
  GitLabHosting("taig", "sbt-houserules", "mail@taig.io")
)
sonatypeProfileName := "io.taig"
