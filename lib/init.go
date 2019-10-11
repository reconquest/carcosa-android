package main

// #include "c/_/string.h"
// #include "c/_/error.h"
// #include "c/init.h"
import "C"

import (
	"os"
	"path/filepath"

	lib_state "github.com/reconquest/carcosa-android/lib/state"
)

var state *lib_state.State

//export Init
func Init(in C.init_in) C.error {
	var (
		root = GoString(in.root)
		pin  = GoString(in.pin)
	)

	os.Stderr.Close()
	os.Stderr, _ = os.OpenFile(
		filepath.Join(root, "stderr.log"),
		os.O_CREATE|os.O_WRONLY|os.O_APPEND,
		0644,
	)

	state = lib_state.NewState(root, pin)

	return CError(nil)
}
