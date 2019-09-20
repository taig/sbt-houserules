# Changelog

## 0.0.7

_2019-09-20_

 * Enable sonatypeBundleRelease
 * Upgrade to sbt-pgp 2.0.0
 * Upgrade to sbt-scalafmt 2.0.5

## 0.0.6

_2019-09-20_

No release available, due to a misconfiguration. Use 0.0.7 instead.

## 0.0.5

_2019-09-13_

 * Upgrade to sbt-sonatype 3.7
 * Upgrade to sbt-scalafmt 2.0.4
 * Upgrade to sbt-micorsites 0.9.4
 * Upgrade to sbt 1.3.0

## 0.0.4

_2019-08-01_

 * Apply sbt-release settings to every project

## 0.0.3

_2019-08-01_

 * Add `micrositeSettings` to publish pre-configured sbt-microsites modules

## 0.0.2

_2019-07-31_

 * Add `skip in publish := true` to `noPublishSettings` to avoid errors during publishing multi-project builds
 * Introduce `publishAndRelease` command to that runs `publishSigned` and afterwards `sonatypeRelease` if it's not a snapshot build
 * Fix unused import compiler flag for 2.11 projects

## 0.0.1

_2019-07-31_

 * Initial release
