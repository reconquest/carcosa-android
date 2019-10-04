package main

// #include "c/_/string.h"
// #include "c/_/error.h"
import "C"

import (
	lib_state "carcosa/lib/state"
	"errors"
	"os"
	"path/filepath"
)

var state *lib_state.State

//export Init
func Init(c_root C.string) C.error {
	var err error

	root := GoString(c_root)

	state, err = lib_state.NewState(root, log)
	if err != nil {
		return CError(err)
	}

	os.Stderr.Close()
	os.Stderr, _ = os.OpenFile(
		filepath.Join(root, "stderr.log"),
		os.O_CREATE|os.O_WRONLY|os.O_APPEND,
		0644,
	)

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
