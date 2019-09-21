package main

import (
	"unsafe"

	"github.com/kovetskiy/lorg"
	"github.com/seletskiy/carcosa/pkg/carcosa"
)

/*
#cgo LDFLAGS: -landroid -llog

#include <android/log.h>
#include <string.h>
#include <stdlib.h>
*/
import "C"

var (
	log = carcosa.GetLogger()
	tag = C.CString("carcosa")
)

type writer struct{}

func (writer) Write(p []byte) (n int, err error) {
	text := C.CString(string(p))
	C.__android_log_write(C.ANDROID_LOG_ERROR, tag, text)
	C.free(unsafe.Pointer(text))
	return len(p), nil
}

func init() {
	log.SetOutput(&writer{})
	log.SetLevel(lorg.LevelTrace)
}
