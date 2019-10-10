package main

// #include "c/_/error.h"
// #include "c/sync.h"
import "C"

//export Sync
func Sync() C.error {
	err := state.Sync()
	if err != nil {
		return CError(err)
	}

	return CError(nil)
}
