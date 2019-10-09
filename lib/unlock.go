package main

// #include "c/_/error.h"
// #include "c/unlock.h"
import "C"

//export Unlock
func Unlock(in C.unlock_in, out *C.unlock_out) C.error {
	var (
		id     = GoString(in.id)
		filter = GoString(in.filter)
		key    = GoString(in.key)
		cache  = in.cache > 0
	)

	tokens, err := state.Unlock(id, key, filter, cache)
	if err != nil {
		return CError(err)
	}

	out.tokens = C.int(tokens)

	return CError(nil)
}
