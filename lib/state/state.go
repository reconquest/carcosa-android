package state

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"carcosa/lib/vault"

	"github.com/kovetskiy/lorg"
	"github.com/reconquest/karma-go"
	"github.com/seletskiy/carcosa/pkg/carcosa"
	"github.com/seletskiy/carcosa/pkg/carcosa/auth"
	"github.com/seletskiy/carcosa/pkg/carcosa/cache"
)

type Token struct {
	Name string
}

type Repo struct {
	Config  *Config
	Carcosa *carcosa.Carcosa
	Tokens  []Token
}

type State struct {
	log   lorg.Logger
	root  string
	vault *vault.Vault
	cache *cache.Cache
}

func NewState(root string, log lorg.Logger) (*State, error) {
	state := &State{
		log:  log,
		root: root,
	}

	var err error

	state.vault, err = vault.New(state.getVaultPath())
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to open vault",
		)
	}

	state.cache = cache.NewDefault(state.vault)

	return state, nil
}

func (state *State) getReposDir() string {
	return filepath.Join(state.root, "repos")
}

func (state *State) getRepoDir(id string) string {
	return filepath.Join(state.getReposDir(), id, "repo")
}

func (state *State) getRepoConfigPath(id string) string {
	return filepath.Join(state.getRepoDir(id), "config.json")
}

func (state *State) getVaultPath() string {
	return filepath.Join(state.root, "vault.json")
}

func (state *State) IsPinSet() bool {
	return state.vault.IsLocked()
}

func (state *State) SetPin(pin string) error {
	return state.vault.Lock(pin)
}

func (state *State) CheckPin(pin string) bool {
	return state.vault.Unlock(pin)
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

	carcosa := carcosa.NewDefault(state.getRepoDir(id), ns)

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

	//filter, err := regexp.Compile(config.Filter)
	//if err != nil {
	//    return 0, karma.Format(
	//        err,
	//        "unable to compile filter regexp",
	//    )
	//}
	carcosa := carcosa.NewDefault(state.getRepoDir(config.ID), config.NS)

	secrets, err := carcosa.List([]byte(key))
	if err != nil {
		return 0, karma.Format(
			err,
			"unable to list secrets",
		)
	}

	err = state.cache.Set(config.ID, []byte(key))
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
				Carcosa: carcosa.NewDefault(state.getRepoDir(id), config.NS),
			}

			master, err := state.vault.Get(id)
			if err != nil {
				return err
			}

			secrets, err := repo.Carcosa.List(master)
			if err != nil {
				return err
			}

			for _, secret := range secrets {
				repo.Tokens = append(
					repo.Tokens,
					Token{
						Name: string(secret.Token),
					},
				)
			}

			repos = append(repos, repo)

			return filepath.SkipDir
		},
	)

	return repos, err
}
