package main

// #include "c/_/error.h"
// #include "c/connect.h"
import "C"

import (
	"github.com/reconquest/carcosa-android/lib/ssh"
)

//export Connect
func Connect(in C.connect_in, out *C.connect_out) C.error {
	var (
		protocol = GoString(in.protocol)
		address  = GoString(in.address)
		ns       = GoString(in.ns)
	)

	var key *ssh.Key

	if in.ssh_key != nil {
		key = &ssh.Key{
			Public:      []byte(GoString(in.ssh_key.public)),
			Private:     []byte(GoString(in.ssh_key.private)),
			Fingerprint: GoString(in.ssh_key.fingerprint),
		}
	}

	config, err := state.Connect(protocol, address, ns, key)
	if err != nil {
		return CError(err)
	}

	out.id = CString(config.ID)
	out.tokens = C.int(config.SyncStatus.Stats.Ours.Add)

	return CError(nil)
}
