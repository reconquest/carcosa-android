# Android UI

The Java layer owns user flow, Android lifecycle, and view updates. It
should keep business logic shallow and call native code for carcosa
state changes.

## Activity flow

`LoginActivity` is the launcher activity. It checks biometric support,
then either decrypts the existing PIN payload with Goldfinger or
generates a new random PIN and encrypts it. On success it starts
`MainActivity` with the plaintext PIN in the intent extras.

`MainActivity` creates the Java `Carcosa` facade and initializes native
state when `hasState()` is false. If the PIN extra is missing and there
is no native state, it returns to login. After init it lists repos on a
worker thread, builds a `SecretsList`, and provides navigation to repo,
settings, and about screens.

`RepoActivity` has two modes. With no repo extra it connects a new repo,
optionally generating an SSH key first. With an existing repo extra it
renders the saved config read-only and shows the unlock flow. Successful
unlock returns the user to the main list through the Done button.

`SettingsActivity` edits the session TTL in shared preferences.
`AboutActivity` only displays static app information, but it still owns
a `Session` so timeout behavior is consistent.

The activities pass `Carcosa` through intents because the facade is
serializable. The object itself is not the state; native state remains
the Go process-global singleton.

## Session handling

Every non-login activity constructs a `Session` and forwards
`onPause()` and `onResume()`. The first resume after create is ignored
because there was no prior pause. Later resumes compare the pause time
with the TTL stored in shared preferences.

When a session expires, `Session` calls `carcosa.destroy()` when it has
a facade, optionally runs a `SessionResetter`, and starts
`LoginActivity`. `MainActivity` uses the resetter to replace the current
secrets adapter with an empty one before leaving the screen, so unlocked
secrets are not left visible during the transition.

## Threading

Native calls that can touch git, network, or key generation should run
on a worker thread:

- initial list in `MainActivity`;
- sync through `SyncThread`;
- SSH key generation in `RepoActivity.KeygenThread`;
- repo connect in `RepoActivity.ConnectButton.ConnectThread`.

`RepoActivity.UnlockButton` currently calls `carcosa.unlock()` directly
from the click handler. If unlock work becomes slower or starts doing
network or git IO, move it to a worker and post updates through `UI`.

## UI helper contract

Use `UI` for view mutations from both main and worker threads. The
mutating helpers call `ui(Runnable)`, which runs immediately on the main
looper or posts to it from a worker.

The getter path of `UI.text()` reads the view synchronously. New worker
code should capture input values on the main thread before starting the
worker instead of reading widgets from inside `run()`. Existing code
already has a few background reads, so prefer improving the pattern when
nearby code is touched.

`UI.text()` also writes formatted strings. When called from a worker,
the write is posted asynchronously; do not depend on the value being
visible immediately after the call returns.

`EdgeToEdge.apply()` adjusts toolbar wrapper padding for system bars.
Use the same wrapper pattern when adding screens with a top toolbar.

## List rendering

`SecretsList` flattens repos and their tokens into one adapter. Repo
rows show sync status and an unlock action when a repo is locked. Token
rows show either parsed `resource`/`login` fields or the full token name
when parsing produced no resource.

Search filtering only changes token row view types. Repo headers remain
in the flattened list, and hidden token rows are represented by empty
views rather than removing items from the adapter.

Viewing a token opens an alert dialog with the payload. Copying a token
puts the trimmed payload on the Android clipboard and shows a toast.
Avoid adding logs or analytics around those actions because token
payloads are secret material.

## Error handling

Recoverable native errors arrive as `Maybe.error`. Screens either show a
fatal dialog, an inline repo error panel, or a sync alert depending on
whether the user can continue on the current screen.

`FatalErrorDialog` is reserved for conditions that leave the app unable
to proceed, such as missing biometric support, failed native init, or a
native list error on the main screen. Inline errors are better when the
user can edit input and retry, such as connect or unlock failures.
