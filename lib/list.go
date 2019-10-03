package main

// #include "c/_/error.h"
// #include "c/list.h"
import "C"

//export List
func List(in C.list_in, out *C.list_out) C.error {
	repos, err := state.List()
	if err != nil {
		return CError(err)
	}

	out.repos = C.repo_list_new(C.int(len(repos)))
	for i, repo := range repos {
		C.repo_list_set(out.repos, C.int(i), C.repo{
			name: CString(repo.Config.URL.Address),
		})
	}

	return CError(nil)
}
