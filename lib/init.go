package main

// #include "c/_/string.h"
import "C"

import lib_state "carcosa/lib/state"

var state *lib_state.State

//export Init
func Init(root C.string) {
	state = lib_state.NewState(GoString(root))
}
