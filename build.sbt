val Version = new {
  val SbtPgp = "2.1.2"
  val SbtRelease = "1.1.0"
  val SbtRevolver = "0.9.1"
  val SbtScalafmt = "2.4.6"
  val SbtScoverage = "2.0.2"
  val SbtSonatype = "3.9.13"
  val SbtTpolecat = "0.4.1"
  val Scala = "2.12.16"
}

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

enablePlugins(SbtPlugin, BlowoutYamlPlugin)

addSbtPlugin("com.github.sbt" % "sbt-release" % Version.SbtRelease)
addSbtPlugin("com.github.sbt" % "sbt-pgp" % Version.SbtPgp)
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % Version.SbtTpolecat)
addSbtPlugin("io.spray" % "sbt-revolver" % Version.SbtRevolver)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % Version.SbtScalafmt)
addSbtPlugin("org.scoverage" % "sbt-scoverage" % Version.SbtScoverage)
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % Version.SbtSonatype)

blowoutGenerators ++= {
  val workflows = file(".github") / "workflows"
  BlowoutYamlGenerator.lzy(workflows / "main.yml", GitHubActionsGenerator.main) ::
    BlowoutYamlGenerator.lzy(workflows / "branches.yml", GitHubActionsGenerator.branches) ::
    Nil
}

name := "sbt-houserules"

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

scalaVersion := Version.Scala
