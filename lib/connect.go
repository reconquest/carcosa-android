package main

import (
	"github.com/seletskiy/carcosa/pkg/carcosa/auth"
)

// #include "c/_/error.h"
// #include "c/connect.h"
import "C"

//export Connect
func Connect(in C.connect_in, out *C.connect_out) C.error {
	var (
		protocol = GoString(in.protocol)
		address  = GoString(in.address)
		ns       = GoString(in.ns)
	)

	var auth auth.Auth

	config, err := state.Connect(protocol, address, ns, auth)
	if err != nil {
		return CError(err)
	}

	out.id = CString(config.ID)
	out.tokens = C.int(config.SyncStatus.Stats.Ours.Add)

	return CError(nil)
}
