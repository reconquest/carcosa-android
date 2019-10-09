package main

// #include "c/_/error.h"
// #include "c/list.h"
import "C"
import "time"

//export List
func List(in C.list_in, out *C.list_out) C.error {
	repos, err := state.List()
	if err != nil {
		return CError(err)
	}

	out.repos = C.repo_list_new(C.int(len(repos)))
	for i, repo := range repos {
		sync := repo.Config.SyncStatus
		c_repo := C.repo{
			name: CString(repo.Config.URL.Address),
			sync_stat: C.sync_stat{
				date:    CString(sync.Date.Format(time.ANSIC)),
				added:   C.int(sync.Stats.Ours.Add),
				deleted: C.int(sync.Stats.Ours.Del),
			},
		}

		c_repo.tokens = C.token_list_new(C.int(len(repo.Tokens)))

		for i, token := range repo.Tokens {
			C.token_list_set(c_repo.tokens, C.int(i), C.token{
				name:     CString(token.Name),
				resource: CString(token.Resource),
				login:    CString(token.Login),
				payload:  CString(token.Payload),
			})
		}

		C.repo_list_set(out.repos, C.int(i), c_repo)
	}

	return CError(nil)
}
