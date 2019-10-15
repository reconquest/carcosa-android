package main

// #include "c/_/error.h"
// #include "c/ssh_key.h"
import "C"

import (
	"github.com/reconquest/carcosa-android/lib/ssh"
)

//export Keygen
func Keygen(out *C.ssh_key) C.error {
	key, err := ssh.GenerateKey()
	if err != nil {
		return CError(err)
	}

	out.private = CString(string(key.Private))
	out.public = CString(string(key.Public))
	out.fingerprint = CString(key.Fingerprint)

	return CError(nil)
}
