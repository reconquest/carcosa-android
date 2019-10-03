package main

// #include "c/_/string.h"
// #include "c/_/error.h"
import "C"

import (
	lib_state "carcosa/lib/state"
	"errors"
)

var state *lib_state.State

//export Init
func Init(root C.string) C.error {
	var err error

	state, err = lib_state.NewState(GoString(root), log)
	if err != nil {
		return CError(err)
	}

	// XXX
	if !state.IsPinSet() {
		err := state.SetPin("1234")
		if err != nil {
			return CError(err)
		}
	} else {
		if !state.CheckPin("1234") {
			return CError(errors.New("wrong pin"))
		}
	}

	return CError(nil)
}
