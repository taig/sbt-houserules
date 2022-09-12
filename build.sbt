enablePlugins(BlowoutYamlPlugin)

inThisBuild(
  Def.settings(
    developers := List(Developer("taig", "Niklas Klein", "mail@taig.io", url("https://taig.io/"))),
    dynverVTagPrefix := false,
    homepage := Some(url("https://github.com/taig/sbt-houserules/")),
    licenses := List("MIT" -> url("https://raw.githubusercontent.com/taig/sbt-houserules/main/LICENSE")),
    organization := "io.taig",
    organizationHomepage := Some(url("https://taig.io/")),
    versionScheme := Some("early-semver")
  )
)

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.13")

blowoutGenerators ++= {
  val workflows = file(".github") / "workflows"
  BlowoutYamlGenerator.lzy(workflows / "main.yml", GitHubActionsGenerator.main) ::
    BlowoutYamlGenerator.lzy(workflows / "branches.yml", GitHubActionsGenerator.branches) ::
    Nil
}

name := "sbt-houserules"

organization := "io.taig"

sbtPlugin := true

scalafmtAll := {
  (Compile / scalafmt)
    .dependsOn(Test / scalafmt)
    .dependsOn(Compile / scalafmtSbt)
    .value
}

scalafmtCheckAll := {
  (Compile / scalafmtCheck)
    .dependsOn(Test / scalafmtCheck)
    .dependsOn(Compile / scalafmtSbtCheck)
    .value
}

scalaVersion := "2.12.15"

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
