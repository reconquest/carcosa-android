package state

import (
	"encoding/json"
	"os"
	"time"

	"github.com/reconquest/karma-go"
	"github.com/seletskiy/carcosa/pkg/carcosa"
)

type ConfigURL struct {
	Protocol string `json:"protocol"`
	Address  string `json:"address"`
}

type ConfigSyncStatus struct {
	Date  time.Time         `json:"date"`
	Stats carcosa.SyncStats `json:"stats"`
}

type Config struct {
	ID         string           `json:"id"`
	URL        ConfigURL        `json:"url"`
	NS         string           `json:"ns"`
	Filter     string           `json:"filter"`
	SyncStatus ConfigSyncStatus `json:"sync_status"`
}

func LoadConfig(path string) (*Config, error) {
	var config Config

	file, err := os.Open(path)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to open repo config",
		)
	}

	err = json.NewDecoder(file).Decode(&config)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to decode repo config",
		)
	}

	return &config, nil
}

func (config *Config) Store(path string) error {
	file, err := os.Create(path)
	if err != nil {
		return karma.Format(
			err,
			"unable to create repo config file",
		)
	}

	err = json.NewEncoder(file).Encode(config)
	if err != nil {
		return karma.Format(
			err,
			"unable to encode repo config",
		)
	}

	return nil
}
