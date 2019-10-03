package main

// #include "c/_/error.h"
import "C"

func CError(err error) C.error {
	if err != nil {
		return C.error{
			is_error: true,
			message:  CString(err.Error()),
		}

	} else {
		return C.error{
			is_error: false,
		}
	}
}
