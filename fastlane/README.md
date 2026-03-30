fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Run unit tests

### android build_debug

```sh
[bundle exec] fastlane android build_debug
```

Build a debug APK

### android build_release

```sh
[bundle exec] fastlane android build_release
```

Build an unsigned release APK

### android screenshots

```sh
[bundle exec] fastlane android screenshots
```

Capture screenshots on a connected device/emulator (requires Bluetooth)

### android graphics

```sh
[bundle exec] fastlane android graphics
```

Regenerate store graphics (icon.png + featureGraphic.png) from their SVG sources

### android ci

```sh
[bundle exec] fastlane android ci
```

Run tests and build a release APK

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
