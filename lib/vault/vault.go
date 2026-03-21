package vault

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/reconquest/karma-go"
	"github.com/seletskiy/carcosa/pkg/carcosa/vault"
)

type Vault struct {
	root string
	pin  string
}

var _ vault.Vault = (*Vault)(nil)

func New(root string, pin string) *Vault {
	return &Vault{
		root: root,
		pin:  pin,
	}
}

func (vault *Vault) Key() ([]byte, error) {
	return []byte(vault.pin), nil
}

func (vault *Vault) file(token string) string {
	return filepath.Join(
		vault.root,
		fmt.Sprintf("master.%s.key", token),
	)
}

func (vault *Vault) Get(token string) ([]byte, error) {
	body, err := os.ReadFile(vault.file(token))
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}

		return nil, karma.Format(
			err,
			"unable to open vault file",
		)
	}

	return body, nil
}

func (vault *Vault) Set(token string, body []byte) error {
	err := os.WriteFile(vault.file(token), body, 0600)
	if err != nil {
		return karma.Format(
			err,
			"unable to write vault file",
		)
	}

	return nil
}
