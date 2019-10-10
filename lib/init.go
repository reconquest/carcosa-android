package main

// #include "c/_/string.h"
// #include "c/_/error.h"
import "C"

import (
	"os"
	"path/filepath"

	lib_state "github.com/reconquest/carcosa-android/lib/state"
)

var state *lib_state.State

//export Init
func Init(c_root C.string) C.error {
	root := GoString(c_root)

	os.Stderr.Close()
	os.Stderr, _ = os.OpenFile(
		filepath.Join(root, "stderr.log"),
		os.O_CREATE|os.O_WRONLY|os.O_APPEND,
		0644,
	)

	state = lib_state.NewState(root, "1234")

	return CError(nil)
}
