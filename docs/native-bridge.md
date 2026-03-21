# Native bridge

The native bridge is a three-part contract. Java declares the public
shape, C translates between JNI and plain C structs, and Go performs
the operation. Keep those pieces in sync; none of them is optional.

## Call shape

A native call starts on `io.reconquest.carcosa.lib.Carcosa`. The Java
method accepts Java DTOs or primitive values and returns `Maybe<T>` for
operations that can fail. `Maybe.error` is the only recoverable error
channel exposed to the UI. Java callers should not assume `.result` is
valid until `.error` is null.

The C function name must match JNI's package/class/method convention:

    Java_io_reconquest_carcosa_lib_Carcosa_<method>

That function unpacks Java objects into C structs, calls the exported
Go function, releases input strings or byte arrays, and packs the C
result back into Java DTOs. The exported Go function lives in package
`main`, has a `//export` directive, and uses the C structs declared in
`lib/c/*.h`.

`lib/main.go` includes every C implementation file. When adding a C
file for a new operation or helper, include it there or it will not be
compiled into `libcarcosa.so`.

## Adding an operation

Use the existing operations as the template:

1. Add a `native` method to `Carcosa.java` and create or extend DTOs
   with public fields.
2. Define C input and output structs in `lib/c/<operation>.h`.
3. Implement the JNI function in `lib/c/<operation>.c`.
4. Add C helpers for Java DTO conversion when the operation introduces
   a new Java object shape.
5. Add the Go `//export` function in `lib/<operation>.go`.
6. Delegate actual business logic to `lib/state` or another Go package
   instead of putting it in the exported cgo wrapper.
7. Include the new `.c` file from `lib/main.go`.

The DTO field names are part of the ABI. C helpers look up fields by
string name and JNI signature, so a Java rename without a matching C
change fails at runtime.

## String and byte ownership

The bridge uses a custom C `string` struct: length, data pointer, and
an optional JNI byte-array handle. It is length-based, so it can carry
binary data such as PKCS#8 private-key bytes; do not replace it with
NUL-terminated string handling for byte fields.

Inputs from Java are created with `string_from_jstring()` or
`string_from_jbytes()`. They pin or allocate JNI byte-array storage and
must be released with `string_release()` after the Go call has copied
what it needs. The Go wrappers call `GoString()` immediately, which
copies into a Go string.

Outputs from Go are created with `CString()`, which allocates C memory.
The C-to-Java conversion helper that consumes the field is responsible
for releasing it after setting the Java field. For nested return data,
release only after all Java objects have been constructed from that
field.

`j_object_set_string()` and `j_object_set_bytes()` convert data into
new Java objects. They do not own the input `string` after the call;
the caller still decides when to release the C-side value.

## Result objects and errors

`j_maybe()` always creates a `Maybe` object, sets `.error` when the C
`error` says the operation failed, and sets `.result` to the supplied
Java object. Most JNI functions return `j_maybe_void()` on error and
skip result conversion so they do not read uninitialized output fields.

Domain errors should cross this path as `Maybe.error`. Throwing JNI
exceptions would bypass the convention used by the activities and make
error handling inconsistent.

## Native state

Java `Carcosa` implements `Serializable` so it can be passed through
intents with the rest of an activity payload. This does not serialize
native state. The Go package holds a single process-global `*State`,
and Java checks or changes that singleton through `hasState()`,
`init()`, and `destroy()`.

Because the state is global, exported Go functions assume `Init()` has
succeeded. If a future feature can call native code before login, it
must either initialize state first or add explicit nil-state handling at
the exported boundary.

## Logging

The Go `init()` in `lib/log.go` sends the upstream carcosa logger to
Android logcat. `Init()` also redirects Go stderr to `stderr.log` under
the app files directory. Native code should return errors through
`CError()` and reserve logging for diagnostics that help explain those
errors.
