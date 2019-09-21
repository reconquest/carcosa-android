package main

import (
	"github.com/seletskiy/carcosa/pkg/carcosa"
)

import "C"

//export Sync
func Sync() {
	err := carcosa.Run(carcosa.Opts{
		ModeSync:    true,
		FlagVerbose: 2,
	})

	if err != nil {
		log.Error(err.Error())
	}
}
