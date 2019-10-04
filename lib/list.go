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
		c_repo := C.repo{
			name: CString(repo.Config.URL.Address),
		}

		c_repo.tokens = C.token_list_new(C.int(len(repo.Tokens)))

		for i, token := range repo.Tokens {
			C.token_list_set(c_repo.tokens, C.int(i), C.token{
				name: CString(token.Name),
			})
		}

		C.repo_list_set(out.repos, C.int(i), c_repo)
	}

	return CError(nil)
}
