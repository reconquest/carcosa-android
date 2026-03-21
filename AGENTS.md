# carcosa-android

Android client for [carcosa](https://github.com/seletskiy/carcosa),
a secret storage system that hides encrypted tokens inside git
repositories. The app lets users connect to carcosa repos, sync
them, unlock secrets with a master password, and copy token values.

## Codebase map

The app is one Android application with three cooperating layers:

    Java (Android UI) ←JNI→ C (glue) ←cgo→ Go (core logic)

Java owns activities, lifecycle handling, background work, and DTOs
that cross into native code. C owns JNI unpacking and object
construction. Go owns carcosa operations, repository state, cache
access, SSH key generation, and Android shared-library entry points.
Keep those responsibilities separated when adding features.

The native library is built as a single Go package. `lib/main.go`
includes the C glue files in its cgo preamble, so the C code is not a
separate Gradle or Make target. Adding a native operation means adding
or updating all three faces of the same contract: the Java native
method and DTO fields, the C ABI structs and JNI function, and the
Go `//export` function.

The source of truth is the root project tree. Ignored local trees and
build outputs such as `bin/`, `build/`, `target/`, `.gradle/`, and
`src/main/jniLibs/` can look like editable source, but they should be
treated as generated artifacts or local leftovers unless the task
explicitly says otherwise.

## Native boundary contracts

`io.reconquest.carcosa.lib.Carcosa` is the Java facade for native
code. Its methods return `Maybe<T>` for recoverable native errors
instead of throwing Java exceptions. Callers must check `.error` before
reading `.result`.

C structs under `lib/c/` are the ABI between JNI and exported Go
functions. Java DTOs intentionally use public fields with names that
match the C conversion helpers. If a field is renamed on one side,
update the DTO, C getter/setter helper, C struct, and Go conversion in
one change.

Native state is process-global in Go. Java `Carcosa` objects are
serializable only so activities can pass a facade through intents; they
do not carry native state. `hasState()`, `init()`, and `destroy()`
control the underlying Go singleton.

## State and secrets

`Init()` creates a Go `State` rooted at Android's app files directory
and stores the biometric-protected session PIN in memory. The root
contains connected repos, per-repo JSON config, optional SSH private
keys, cache files, and `stderr.log` for redirected Go stderr.

Connecting a repo generates a random repo id, initializes a carcosa git
checkout, optionally writes an SSH key, performs the first sync, and
then stores config. The UI treats address, protocol, namespace, and
token filter as immutable after a successful connect.

Unlocking validates a master password by asking carcosa to list
secrets. A successful unlock caches the master password through the
carcosa cache package using the session PIN as the vault key. Later
`list()` calls mark repos locked when no cache entry can be decrypted
and include token payloads when the cache is available.

Biometrics protect the generated PIN, not the user's master password
directly. Session expiry destroys the in-memory Go state and PIN and
returns the user to `LoginActivity`; it does not delete repo data,
SSH keys, or encrypted cache files on disk.

## Android flow and threading

`LoginActivity` authenticates with Goldfinger and starts `MainActivity`
with the decrypted PIN. `MainActivity` initializes native state if the
process does not already have it, lists repos, and routes to repo,
settings, and about screens. `RepoActivity` handles both first-time
connection and unlock of an existing repo.

Each non-login activity owns a `Session` and forwards pause/resume to
it. `Session` compares the background duration with the stored TTL and
calls `destroy()` before returning to login when the session expires.

The `UI` helper is the convention for view updates. Its mutating
methods post to the main looper, which lets background threads update
views safely. Its getter path reads view state synchronously, so new
background work should capture needed input before starting a worker
instead of reading widgets from that worker.

Long native operations are intended to run away from the UI thread and
then post visual updates through `UI`. Keep this pattern for sync,
connect, key generation, and any new network or git work.

## Build and release

The Makefile drives native and APK builds. `make so` cross-compiles
`libcarcosa.so` for `x86_64` and `arm64-v8a` with the Android NDK
clang toolchain and `go build -buildmode=c-shared`. Gradle packages
the generated shared libraries into the APK.

Debug and release signing values come from variant `vars` files.
Release assets live in a private `src/release` submodule, and the
release target uploads the APK with Firebase App Distribution.

## Conventions

Prefer documenting behavior at the layer boundary that owns it. Java
docs should describe UI lifecycle and user-visible flow, C docs should
describe JNI conversion and ownership, and Go docs should describe
state, persistence, and carcosa semantics.

Keep native errors as returned `Maybe` values unless the failure is a
truly fatal Android-side condition. Fatal dialogs are for unrecoverable
state, missing platform support, or errors that leave the app unable to
continue.

Token filters are regular expressions anchored by the Go state layer.
Named groups `resource` and `login` are used only for display; the full
token name remains the fallback identity.

## Deep reference

- `docs/native-bridge.md` — JNI/cgo contract, ownership, and the steps
  for adding a native operation. Read this before touching
  `Carcosa.java`, `lib/c/`, or exported Go functions.
- `docs/state-and-secrets.md` — on-disk state, repo lifecycle,
  unlocking, cache, SSH keys, and biometric session model. Read this
  before changing storage, sync, unlock, or security-sensitive code.
- `docs/android-ui.md` — activity flow, threading conventions, list
  rendering, and session reset behavior. Read this before changing UI
  screens, adapters, or background workers.
- `docs/build-release.md` — native build chain, Gradle packaging,
  signing inputs, generated artifacts, and release commands. Read this
  before changing build files, ABIs, signing, or release automation.
