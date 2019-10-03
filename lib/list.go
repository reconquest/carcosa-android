package main

// #include "c/_/error.h"
import "C"

//export List
func List() C.error {
	return CError(nil)
}
