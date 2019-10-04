package vault

import (
	"bytes"
	"crypto/rand"
	"encoding/json"
	"os"

	"github.com/reconquest/karma-go"
	"github.com/seletskiy/carcosa/pkg/carcosa"
	"github.com/seletskiy/carcosa/pkg/carcosa/cache"
	"github.com/seletskiy/carcosa/pkg/carcosa/crypto"
)

var (
	log = carcosa.Logger()
)

type Vault struct {
	path string
	pin  string

	Pin struct {
		Hash []byte `json:"hash,omitempty"`
		Salt []byte `json:"salt,omitempty"`
	} `json:"pin,omitempty"`

	Tokens map[string][]byte `json:"tokens,omitempty"`
}

var _ cache.Vault = (*Vault)(nil)

func New(path string) (*Vault, error) {
	vault := &Vault{
		path:   path,
		Tokens: map[string][]byte{},
	}

	err := vault.read()
	if err != nil {
		return nil, err
	}

	return vault, nil
}

func (vault *Vault) IsLocked() bool {
	return vault.Pin.Hash != nil
}

func (vault *Vault) Lock(pin string) error {
	var salt [16]byte

	_, err := rand.Read(salt[:])
	if err != nil {
		return karma.Format(
			err,
			"unable to init pin hash salt",
		)
	}

	vault.Pin.Salt = salt[:]
	vault.Pin.Hash, err = vault.hash(pin, salt[:])
	if err != nil {
		return karma.Format(
			err,
			"unable to hash pin",
		)
	}

	return vault.write()
}

func (vault *Vault) Unlock(pin string) bool {
	hash, err := vault.hash(pin, vault.Pin.Salt)
	if err != nil {
		return false
	}

	if bytes.Equal(vault.Pin.Hash, hash) {
		vault.pin = pin
		return true
	} else {
		return false
	}
}

func (vault *Vault) Key() ([]byte, error) {
	return []byte(vault.pin), nil
}

func (vault *Vault) Get(token string) ([]byte, error) {
	err := vault.read()
	if err != nil {
		return nil, err
	}

	log.Debugf("XXX token: %s", token)

	return vault.Tokens[token], nil
}

func (vault *Vault) Set(token string, body []byte) error {
	vault.Tokens[token] = body

	return vault.write()
}

func (vault *Vault) hash(pin string, salt []byte) ([]byte, error) {
	hasher := crypto.DefaultCore.Hash.New()

	_, err := hasher.Write([]byte(pin))
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to add pin to pin hash",
		)
	}

	_, err = hasher.Write(salt)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to add salt to pin hash",
		)
	}

	return hasher.Sum(nil), nil
}

func (vault *Vault) read() error {
	file, err := os.Open(vault.path)
	if err != nil {
		if os.IsNotExist(err) {
			return vault.write()
		}

		return karma.Format(
			err,
			"unable to open vault file",
		)
	}

	err = json.NewDecoder(file).Decode(vault)
	if err != nil {
		return karma.Format(
			err,
			"unable to decode file",
		)
	}

	return nil
}

func (vault *Vault) write() error {
	file, err := os.OpenFile(vault.path, os.O_WRONLY|os.O_CREATE, 0600)
	if err != nil {
		return karma.Format(
			err,
			"unable to open vault file for writing",
		)
	}

	err = json.NewEncoder(file).Encode(vault)
	if err != nil {
		return karma.Format(
			err,
			"unable to write vault file",
		)
	}

	return nil
}
