package main

import (
	"github.com/seletskiy/carcosa/pkg/carcosa"
)

import "C"

//export Init
func Init(path_c *C.char) {
	path := C.GoString(path_c)

	err := carcosa.Run(carcosa.Opts{
		ModeSync:       true,
		ValuePath:      path,
		ValueRemote:    "git://github.com/seletskiy/secrets",
		ValueNamespace: "refs/tokens/",
		FlagVerbose:    2,
	})

	if err != nil {
		log.Error(err.Error())
	}
}
