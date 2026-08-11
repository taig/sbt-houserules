inThisBuild(
  Def.settings(
    developers := List(Developer("taig", "Niklas Klein", "mail@taig.io", uri("https://taig.io/"))),
    dynverVTagPrefix := false,
    homepage := Some(uri("https://github.com/taig/sbt-houserules/")),
    licenses := List("MIT" -> uri("https://raw.githubusercontent.com/taig/sbt-houserules/main/LICENSE")),
    organization := "io.taig",
    organizationHomepage := Some(uri("https://taig.io/")),
    versionScheme := Some("early-semver")
  )
)

// enablePlugins(SbtPlugin, BlowoutYamlPlugin)
enablePlugins(SbtPlugin)

// sbt-git's GitPlugin is auto-enabled by sbt-ci-release, but only its GitVersioning
// companion consumes these keys, and versioning is handled by sbt-dynver instead.
Global / excludeLintKeys ++= Set(git.gitUncommittedChanges, git.gitDescribedVersion)

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.7")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")
addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.6")

// blowoutGenerators ++= {
//   val workflows = file(".github") / "workflows"
//   BlowoutYamlGenerator.lzy(workflows / "main.yml", GitHubActionsGenerator.main) ::
//     BlowoutYamlGenerator.lzy(workflows / "tag.yml", GitHubActionsGenerator.tag) ::
//     BlowoutYamlGenerator.lzy(workflows / "pull-request.yml", GitHubActionsGenerator.pullRequest) ::
//     Nil
// }

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

scalaVersion := "3.8.4"
