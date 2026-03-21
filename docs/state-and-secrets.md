# State and secrets

The Go layer owns persistent carcosa state. Java supplies only the app
files directory and the session PIN; after that, exported native calls
delegate to `lib/state`.

## Filesystem layout

`MainActivity` calls `init(filesDir, pin)`. The root is Android's app
private files directory and is expected to be readable only by the app.
The Go state uses this layout:

    <filesDir>/
      pin                         encrypted Goldfinger payload
      stderr.log                  redirected Go stderr
      repos/<repo-id>/
        git/                      carcosa git checkout
        config.json               repo metadata and sync status
        ssh.key                   SSH private key, when protocol is ssh
        master.<repo-id>.key      carcosa cache entry for the master key

The repo id is random 32-byte hex generated on connect. The id names
the repo directory, the config id, and the cache token used for that
repo's master password.

There is no current repo deletion flow. Failed connect attempts clean
up the partially initialized repo directory after carcosa has created
it, but successful repos remain until app data is removed or a future
feature deletes them.

## State lifecycle

`Init()` redirects stderr, creates the process-global `*State`, and
stores the PIN in memory. `HasState()` only reports whether that Go
singleton exists. `Destroy()` drops the singleton so later activity
resumes must return to login and obtain a new PIN.

Dropping state does not remove files. It only removes in-memory access
to the PIN and the `State` object. Disk data such as repo clones, SSH
keys, encrypted cache files, and the biometric PIN payload remains.

## Connecting a repo

`State.Connect()` performs these steps:

1. Compile the token filter as a regexp. The state layer anchors it
   with `^` and `$`, so the configured pattern must match the whole
   token name.
2. Generate a repo id and create a carcosa instance rooted at that
   repo's `git/` directory.
3. Initialize the git remote as `origin` using `<protocol>://<address>`
   and the configured token namespace.
4. For SSH repos, write the generated private key to `ssh.key` with
   mode `0600` and configure carcosa auth to use that key.
5. Run the first sync.
6. Store `config.json` with URL, namespace, filter, and sync status.

The UI makes connected repo settings read-only. The Go config model can
store changed values, but the app assumes address, protocol,
namespace, and token filter are fixed after connect.

## Syncing repos

`State.Sync()` walks the repos directory, loads each config, attaches
an SSH key when the protocol is `ssh`, and syncs each repo in sequence.
After a successful sync it updates the repo's sync date and stats in
`config.json`.

A sync error aborts the whole operation and returns the first error to
Java. The main screen then shows an error dialog and does not refresh
the list from partial results.

## Unlocking and cache

`State.Unlock(id, key, cache)` loads the repo config and asks carcosa
to list secrets with the supplied master password. Java treats a return
count of zero as an invalid master password. Native errors are reserved
for failures in IO, parsing, git/carcosa operations, or cache writes.

Current behavior always caches a master password that decrypts at least
one secret. The `cache` argument crosses Java, C, and Go, but the Go
state does not consult it. This matches the current UI: the cache
checkbox is checked and disabled.

The cache is created with `cache.New(vault.New(repoDir, pin),
&crypto.DefaultCore)`. The vault implementation supplies file IO and
the session PIN as the vault key; the carcosa cache/core layer handles
encoding and encryption before bytes are written to disk.

## Listing secrets

`State.List()` first discovers repo configs. For each repo it asks the
cache for that repo's master key. If there is no cache entry, the repo
is returned as locked and no token payloads are included.

When a cache entry is available, the state lists secrets with carcosa,
reads each secret stream into memory, and applies the configured token
filter. Named regexp groups `resource` and `login` become display
fields on the Java token. If either group is missing or the pattern does
not match, that display field is empty and the UI falls back to the full
token name.

The list operation returns payloads to Java so view and copy actions do
not need another native call. Treat the resulting Java token list as
sensitive data: avoid logging it and avoid storing it outside the
existing in-memory adapter flow.

## Security boundaries

Goldfinger protects a generated random PIN stored in the `pin` file.
The user never chooses or types that PIN. The PIN unlocks the app's
cached master passwords for the current native session.

The user's carcosa master password is typed only in `RepoActivity` and
sent to native code for unlock. After a successful unlock it is cached
through the carcosa cache layer. Session expiry removes the PIN from Go
memory, which prevents future cache reads until biometric login restores
it.

SSH private keys generated by the app are stored as app-private files
with mode `0600`. They are not separately wrapped by the biometric PIN;
the protection boundary is Android app-private storage plus file mode.

The session TTL is stored in Android shared preferences as seconds. All
non-login activities forward pause/resume events to `Session`, so any
screen can trigger expiry when the app returns from background.
