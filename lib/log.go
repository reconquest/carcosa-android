package main

import (
	"unsafe"

	"github.com/kovetskiy/lorg"
	"github.com/seletskiy/carcosa/pkg/carcosa"
)

// #cgo LDFLAGS: -landroid -llog
//
// #include <android/log.h>
import "C"

var (
	log = carcosa.Logger()
	tag = C.CString("carcosa")
)

type logcat struct{}

func (logcat) Write(p []byte) (n int, err error) {
	C.__android_log_write(
		C.ANDROID_LOG_ERROR,
		tag,
		(*C.char)(unsafe.Pointer(&p[0])),
	)

	return len(p), nil
}

func init() {
	log.SetOutput(&logcat{})
	log.SetLevel(lorg.LevelTrace)
}
