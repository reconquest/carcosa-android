package main

// #include "c/_/error.h"
// #include "c/list.h"
import "C"
import (
	"fmt"
	"time"
)

//export List
func List(in C.list_in, out *C.list_out) C.error {
	repos, err := state.List()
	if err != nil {
		return CError(err)
	}

	out.repos = C.repo_list_new(C.int(len(repos)))
	for i, repo := range repos {
		sync := repo.Config.SyncStatus

		c_ssh_key := C.ssh_key{}

		if repo.SSHKey != nil {
			c_ssh_key.public = CString(string(repo.SSHKey.Public))
			c_ssh_key.fingerprint = CString(repo.SSHKey.Fingerprint)
		}

		c_repo := C.repo{
			id: CString(repo.Config.ID),
			name: CString(
				fmt.Sprintf(
					"%s://%s",
					repo.Config.URL.Protocol,
					repo.Config.URL.Address,
				),
			),
			ssh_key:   c_ssh_key,
			is_locked: C.bool(repo.IsLocked),
			config: C.repo_config{
				address:  CString(repo.Config.URL.Address),
				protocol: CString(repo.Config.URL.Protocol),
				ns:       CString(repo.Config.NS),
				filter:   CString(repo.Config.Filter),
			},
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
