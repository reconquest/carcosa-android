package main

// #include "c/_/string.h"
import "C"

func CString(str string) C.string {
	return C.string_from_bytes_n(C.CString(str), C.int(len(str)))
}

func GoString(str C.string) string {
	return C.GoStringN(str.data, str.length)
}
