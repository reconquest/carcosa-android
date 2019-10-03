package state

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/reconquest/karma-go"
	"github.com/seletskiy/carcosa/pkg/carcosa"
	"github.com/seletskiy/carcosa/pkg/carcosa/auth"
)

type Repo struct {
	Carcosa *carcosa.Carcosa
	Config  *Config
}

type State struct {
	root string
}

func NewState(root string) *State {
	return &State{
		root: root,
	}
}

func (state *State) getDir(id string) string {
	return filepath.Join(state.root, `repos`, id)
}

func (state *State) getRepoDir(id string) string {
	return filepath.Join(state.getDir(id), "repo")
}

func (state *State) getRepoConfigPath(id string) string {
	return filepath.Join(state.getRepoDir(id), "config.json")
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

	return len(secrets), nil
}

func (state *State) List() error {
	var repos []Repo

	err := filepath.Walk(state.root, func(path string, info os.FileInfo, err error) error {
		if !info.IsDir() {
			return nil
		}

		id := filepath.Base(path)

		config, err := LoadConfig(state.getRepoConfigPath(id))
		if err != nil {
			return err
		}

		repos = append(repos, Repo{
			Config:  config,
			Carcosa: carcosa.NewDefault(state.getRepoDir(id), config.NS),
		})

		return nil
	})

	return err
}
