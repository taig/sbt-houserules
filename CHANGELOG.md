# Changelog

## 0.1.9

_2020-01-30_

 * Upgrade to sbt-microsites 1.1.0
 * Upgrade to sbt-release 1.0.13
 * Upgrade to sbt-scalafmt 2.3.1

## 0.1.8

_2019-12-23_

 * Upgrade to sbt-pgp 2.0.1
 * Add scalacOption `-Ywarn-macros:after`

## 0.1.7

_2019-12-15_

 * Upgrade to sbt-microsites 1.0.2
 * Enable -Xexperimental on scala 2.11 and 2.12

## 0.1.6

_2019-12-14_

 * Add scalafmt SortModifiers rewrite rule

## 0.1.5

_2019-12-13_

 * Use sbt-tpolecat for scalacOptions
 * Upgrade to sbt-sonatype 3.8.1
 * Upgrade to scalafmt 2.3.0
 * Upgrade to sbt 1.3.5

## 0.1.4

_2019-11-20_

 * Remove `scalafmtGenerateConfig` setting
 * Upgrade to `sbt-scoverage` 1.6.1

## 0.1.3

_2019-11-18_

 * Switch from sbt `in` syntax to `/` style
 * Enable integration tests by default
 * Move scalafmt config generation to `buildSettings`
 * Upgrade to sbt 1.3.3
 * Upgrade to scalafmt 2.2.1

## 0.1.2

_2019-10-20_

 * Upgrade to sbt-scalafmt 2.0.7
 * Upgrade to sbt-release 1.0.12

 ## 0.1.1

_2019-10-07_

 * Fix kind-projector compiler plugin dependency
 * Upgrade to scala 2.12.10

## 0.1.0

_2019-10-07_

 * Upgrade to sbt-sonatype 3.8
 * Upgrade to sbt-scalafmt 2.0.6
 * Upgrade to sbt-microsites 0.9.7
 * Upgrade to kind-projector 0.11.0

## 0.0.9

_2019-09-25_

 * Provide defaults for `name` and `normalizedName` in `houserulesSettings`
 * Preset useful microsite `mdoc` variables
 * Upgrade to sbt 1.3.2

## 0.0.8

_2019-09-20_

 * Add environment validation during `publishAndRelease`

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
