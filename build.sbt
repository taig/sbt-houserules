import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import xerial.sbt.Sonatype.GitLabHosting

addSbtPlugin("com.47deg" % "sbt-microsites" % "0.9.4")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.5")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.7")

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
useGpg := false
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
publishTo := sonatypePublishToBundle.value
sonatypeProjectHosting := Some(
  GitLabHosting("taig", "sbt-houserules", "mail@taig.io")
)
sonatypeProfileName := "io.taig"
commands += Command.command("publishAndRelease") { state =>
  val validateEnv: String => Unit =
    key => if (sys.env.get(key).isEmpty) sys.error(s"$$$key is not defined")

  validateEnv("SONATYPE_USERNAME")
  validateEnv("SONATYPE_PASSWORD")

  val snapshot: Boolean = Project.extract(state).get(isSnapshot)

  if (snapshot) "+publishSigned" :: state
  else {
    validateEnv("PGP_SECRING")
    validateEnv("PGP_PASSWORD")
    "+publishSigned" :: "sonatypeBundleRelease" :: state
  }
}
