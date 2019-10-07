package state

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"time"

	"carcosa/lib/vault"

	"github.com/reconquest/karma-go"
	"github.com/seletskiy/carcosa/pkg/carcosa"
	"github.com/seletskiy/carcosa/pkg/carcosa/auth"
	"github.com/seletskiy/carcosa/pkg/carcosa/cache"
	"github.com/seletskiy/carcosa/pkg/carcosa/crypto"
)

var (
	log = carcosa.Logger()
)

type Token struct {
	Name    string
	Payload string
}

type Repo struct {
	Config  *Config
	Carcosa *carcosa.Carcosa
	Tokens  []Token
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

func (state *State) cache(id string) *cache.Cache {
	core := crypto.DefaultCore
	core.KDF.Iterations = 1048576

	return cache.New(vault.New(state.getRepoDir(id), state.pin), &core)
}

func (state *State) carcosa(id string, ns string) *carcosa.Carcosa {
	return carcosa.NewDefault(state.getRepoGitDir(id), ns)
}

func (state *State) Connect(
	protocol string,
	address string,
	ns string,
	auth auth.Auth,
) (*Config, error) {
	var id string
	var bytes [32]byte

	_, err := rand.Read(bytes[:])
	if err != nil {
		return nil, err
	}

	id = hex.EncodeToString(bytes[:])

	carcosa := state.carcosa(id, ns)

	err = carcosa.Init(
		fmt.Sprintf("%s://%s", protocol, address),
		"origin",
		nil,
	)
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
		NS: ns,
		SyncStatus: ConfigSyncStatus{
			Date:  time.Now(),
			Stats: *stats,
		},
	}

	err = config.Store(state.getRepoConfigPath(id))
	if err != nil {
		return nil, err
	}

	return config, nil
}

func (state *State) Unlock(id string, key string, cache bool) (int, error) {
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

	err = state.cache(id).Set(id, []byte(key))
	if err != nil {
		return 0, karma.Format(
			err,
			"unable to set master cache",
		)
	}

	return len(secrets), nil
}

func (state *State) List() ([]Repo, error) {
	var repos []Repo

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

			repo := Repo{
				Config:  config,
				Carcosa: state.carcosa(id, config.NS),
			}

			master, err := state.cache(id).Get(id)
			if err != nil {
				return err
			}

			secrets, err := repo.Carcosa.List(master)
			if err != nil {
				return err
			}

			for _, secret := range secrets {
				payload, err := ioutil.ReadAll(secret.StreamReader)
				if err != nil {
					return karma.Format(
						err,
						"unable to read secret payload",
					)
				}

				repo.Tokens = append(
					repo.Tokens,
					Token{
						Name:    string(secret.Token),
						Payload: string(payload),
					},
				)
			}

			repos = append(repos, repo)

			return filepath.SkipDir
		},
	)

	return repos, err
}
