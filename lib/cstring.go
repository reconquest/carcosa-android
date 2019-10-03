package main

// #include "c/_/string.c"
import "C"

func CString(str string) C.string {
	return C.string_from_bytes(C.CString(str))
}

func GoString(str C.string) string {
	return C.GoStringN(str.data, str.length)
}
