package state

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"regexp"
	"time"

	"github.com/reconquest/carcosa-android/lib/ssh"
	"github.com/reconquest/carcosa-android/lib/vault"

	"github.com/reconquest/karma-go"
	"github.com/reconquest/regexputil-go"
	"github.com/seletskiy/carcosa/pkg/carcosa"
	"github.com/seletskiy/carcosa/pkg/carcosa/auth"
	"github.com/seletskiy/carcosa/pkg/carcosa/cache"
	"github.com/seletskiy/carcosa/pkg/carcosa/crypto"
)

var (
	log = carcosa.Logger()
)

type Token struct {
	Name     string
	Resource string
	Login    string
	Payload  string
}

type Repo struct {
	IsLocked bool
	Config   *Config
	SSHKey   *ssh.Key
	Carcosa  *carcosa.Carcosa
	Tokens   []Token
}

type State struct {
	pin  string
	root string
}

func NewState(root string, pin string) *State {
	state := &State{
		pin:  pin,
		root: root,
	}

	return state
}

func (state *State) getReposDir() string {
	return filepath.Join(state.root, "repos")
}

func (state *State) getRepoDir(id string) string {
	return filepath.Join(state.getReposDir(), id)
}

func (state *State) getRepoGitDir(id string) string {
	return filepath.Join(state.getRepoDir(id), "git")
}

func (state *State) getRepoConfigPath(id string) string {
	return filepath.Join(state.getRepoDir(id), "config.json")
}

func (state *State) getRepoSSHKeyPath(id string) string {
	return filepath.Join(state.getRepoDir(id), "ssh.key")
}

func (state *State) cache(id string) *cache.Cache {
	return cache.New(
		vault.New(state.getRepoDir(id), state.pin),
		&crypto.DefaultCore,
	)
}

func (state *State) filter(filter string) (*regexp.Regexp, error) {
	re, err := regexp.Compile(`^` + filter + `$`)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to compile filter regexp",
		)
	}

	return re, nil
}

func (state *State) auth(id string, protocol string) (auth.Auth, error) {
	auth := auth.New()

	if protocol == "ssh" {
		err := auth.Add(fmt.Sprintf("ssh:%s", state.getRepoSSHKeyPath(id)))
		if err != nil {
			return nil, err
		}
	}

	return auth, nil
}

func (state *State) carcosa(id string, ns string) *carcosa.Carcosa {
	return carcosa.NewDefault(state.getRepoGitDir(id), ns)
}

func (state *State) Connect(
	protocol string,
	address string,
	ns string,
	filter string,
	key *ssh.Key,
) (*Config, error) {
	var id string
	var bytes [32]byte

	_, err := rand.Read(bytes[:])
	if err != nil {
		return nil, err
	}

	_, err = state.filter(filter)
	if err != nil {
		return nil, err
	}

	id = hex.EncodeToString(bytes[:])

	carcosa := state.carcosa(id, ns)

	var cleanup bool
	defer func() {
		if cleanup {
			os.RemoveAll(state.getRepoDir(id))
		}
	}()

	err = carcosa.Init(
		"origin",
		fmt.Sprintf("%s://%s", protocol, address),
		ns,
	)
	if err != nil {
		return nil, err
	}

	cleanup = true

	if key != nil {
		path := state.getRepoSSHKeyPath(id)

		err := ioutil.WriteFile(path, key.EncodePrivateKey(), 0600)
		if err != nil {
			return nil, karma.Describe("path", path).Format(
				err,
				"unable to write ssh private key file",
			)
		}
	}

	auth, err := state.auth(id, protocol)
	if err != nil {
		return nil, err
	}

	stats, err := carcosa.Sync("origin", auth, false)
	if err != nil {
		return nil, err
	}

	config := &Config{
		ID: id,
		URL: ConfigURL{
			Protocol: protocol,
			Address:  address,
		},
		NS:     ns,
		Filter: filter,
		SyncStatus: ConfigSyncStatus{
			Date:  time.Now(),
			Stats: *stats,
		},
	}

	err = config.Store(state.getRepoConfigPath(id))
	if err != nil {
		return nil, err
	}

	cleanup = false

	return config, nil
}

func (state *State) Unlock(
	id string,
	key string,
	cache bool,
) (int, error) {
	config, err := LoadConfig(state.getRepoConfigPath(id))
	if err != nil {
		return 0, err
	}

	carcosa := state.carcosa(id, config.NS)

	secrets, err := carcosa.List([]byte(key))
	if err != nil {
		return 0, karma.Format(
			err,
			"unable to list secrets",
		)
	}

	if len(secrets) > 0 {
		err = state.cache(id).Set(id, []byte(key))
		if err != nil {
			return 0, karma.Format(
				err,
				"unable to set master cache",
			)
		}
	}

	err = config.Store(state.getRepoConfigPath(id))
	if err != nil {
		return 0, err
	}

	return len(secrets), nil
}

func (state *State) list() ([]*Repo, error) {
	var repos []*Repo

	err := filepath.Walk(
		state.getReposDir(),
		func(path string, info os.FileInfo, err error) error {
			if path == state.getReposDir() {
				return nil
			}

			if !info.IsDir() {
				return nil
			}

			id := filepath.Base(path)

			config, err := LoadConfig(state.getRepoConfigPath(id))
			if err != nil {
				return err
			}

			repo := &Repo{
				Config:  config,
				Carcosa: state.carcosa(id, config.NS),
			}

			if config.URL.Protocol == "ssh" {
				key, err := ssh.ReadKey(state.getRepoSSHKeyPath(id))
				if err != nil {
					return err
				}

				repo.SSHKey = key
			}

			repos = append(repos, repo)

			return filepath.SkipDir
		},
	)

	return repos, err
}

func (state *State) Sync() error {
	repos, err := state.list()
	if err != nil {
		return err
	}

	for _, repo := range repos {
		auth, err := state.auth(repo.Config.ID, repo.Config.URL.Protocol)
		if err != nil {
			return err
		}

		stats, err := repo.Carcosa.Sync("origin", auth, false)
		if err != nil {
			return karma.
				Describe("repo", repo.Config.URL).
				Format(
					err,
					"unable to sync repo",
				)
		}

		repo.Config.SyncStatus = ConfigSyncStatus{
			Date:  time.Now(),
			Stats: *stats,
		}

		err = repo.Config.Store(state.getRepoConfigPath(repo.Config.ID))
		if err != nil {
			return err
		}
	}

	return nil
}

func (state *State) List() ([]*Repo, error) {
	repos, err := state.list()
	if err != nil {
		return nil, err
	}

	for _, repo := range repos {
		master, err := state.cache(repo.Config.ID).Get(repo.Config.ID)
		if err != nil {
			return nil, err
		}

		if master == nil {
			repo.IsLocked = true
			continue
		}

		secrets, err := repo.Carcosa.List(master)
		if err != nil {
			return nil, err
		}

		filter, err := state.filter(repo.Config.Filter)
		if err != nil {
			return nil, err
		}

		for _, secret := range secrets {
			payload, err := ioutil.ReadAll(secret.StreamReader)
			if err != nil {
				return nil, karma.Format(
					err,
					"unable to read secret payload",
				)
			}

			token := Token{
				Name:    string(secret.Token),
				Payload: string(payload),
			}

			matches := filter.FindStringSubmatch(token.Name)

			token.Resource = regexputil.Subexp(filter, matches, "resource")
			token.Login = regexputil.Subexp(filter, matches, "login")

			repo.Tokens = append(repo.Tokens, token)
		}
	}

	return repos, err
}
