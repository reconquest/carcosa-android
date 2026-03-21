# Build and release

The build has two stages: cross-compile the Go+C shared library, then
let Gradle package the Android app.

## Native library build

`make so` builds `libcarcosa.so` for both supported ABIs:

- `x86_64`, using `GOARCH=amd64` and `x86_64-linux-android21-clang`;
- `arm64-v8a`, using `GOARCH=arm64` and
  `aarch64-linux-android21-clang`.

The Makefile sets `GOOS=android`, enables cgo, and runs:

    go build -buildmode=c-shared ./lib

The output is written under `src/main/jniLibs/<abi>/`. Go also emits a
C header next to the shared library. Treat both files as generated; do
not edit them by hand. Re-run `make so` after changing Go code, C glue,
or C headers under `lib/`.

The C glue is compiled by cgo because `lib/main.go` includes the `.c`
files in its import-C preamble. There is no separate C compiler target
in Gradle or Make for those files.

## Toolchain requirements

The Makefile expects the Android SDK and NDK on disk and prepends the
NDK LLVM toolchain to `PATH`. The default variables are:

    JAVA_HOME=/usr/lib/jvm/default
    ANDROID_HOME=/opt/android-sdk
    NDK_VERSION=28.2.13676358

Override them in the environment when the local installation differs.
The clang binaries named by the ABI rules must be reachable after those
variables are applied.

## APK targets

Common targets:

    make debug@apk       build native libs and debug APK
    make debug@install   install build/debug.apk to the first adb device
    make debug@run       install and start LoginActivity
    make release@apk     build native libs and release APK
    make release         build release APK and upload to Firebase

`make debug` is an alias for `debug@run`. `make release@run` installs
and starts the release package. `FASTBUILD=1` skips Android lint tasks
used by the debug Gradle build.

`make clean` removes the Gradle build directory and Go build cache. It
does not remove generated native libraries under `src/main/jniLibs/`.
Delete those manually when you need to force a clean native packaging
state.

## Gradle configuration

Gradle applies the Android application plugin, uses Java 17 source and
target compatibility, and packages under namespace
`io.reconquest.carcosa`. Debug builds use application id suffix
`.debug`; release builds use the base package id.

Signing values are read from variant `vars` files. Gradle loads
`src/debug/vars` and, when present, `src/release/vars`, then exposes
values as `env.DEBUG_*` or `env.RELEASE_*` system properties when the
same keys are not already set in the process environment.

The Makefile can create a missing variant keystore from its `vars` file
with the `%/keystore` rule. Keep secret release material in the private
`src/release` submodule rather than the public project tree.

## Release flow

The release target builds `build/release.apk`, derives a release version
from the commit count and short commit hash, uses the latest commit
subject as the release note, and uploads through Firebase App
Distribution to the configured beta group.

Because release notes come from git, commit intended release changes
before running `make release`. The Makefile emits a warning, but it does
not enforce a clean worktree.

## Generated and ignored paths

The repository may contain local or generated paths that look like
source trees. In normal development, edit the root `src/`, `lib/`,
Gradle, and Make files. Do not base changes on ignored copies or build
outputs such as `bin/`, `build/`, `target/`, `.gradle/`, or generated
`src/main/jniLibs/` content unless the task is explicitly about those
artifacts.

## Validation

A packaging smoke test is:

    make debug@apk

Use `FASTBUILD=1 make debug@apk` when you only need to confirm native
compilation and APK assembly quickly. Full release validation should use
`make release@apk` with the release submodule and signing material
available.
