# Changelog

## 0.0.2

_2019-07-31_

 * Add `skip in publish := true` to `noPublishSettings` to avoid errors during publishing multi-project builds
 * Introduce `publishAndRelease` command to that runs `publishSigned` and afterwards `sonatypeRelease` if it's not a snapshot build
 * Fix unused import compiler flag for 2.11 projects

## 0.0.1

_2019-07-31_

 * Initial release