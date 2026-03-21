package ssh

import (
	"crypto/ed25519"
	"crypto/rand"
	"crypto/x509"
	"encoding/pem"
	"os"

	"github.com/reconquest/karma-go"
	"golang.org/x/crypto/ssh"
)

type Key struct {
	Public      []byte
	Private     []byte
	Fingerprint string
}

func newKey(
	signer ssh.Signer,
	privateDER []byte,
) (*Key, error) {
	return &Key{
		Public:      ssh.MarshalAuthorizedKey(signer.PublicKey()),
		Private:     privateDER,
		Fingerprint: ssh.FingerprintSHA256(signer.PublicKey()),
	}, nil
}

func ReadKey(path string) (*Key, error) {
	facts := karma.Describe("path", path)

	data, err := os.ReadFile(path)
	if err != nil {
		return nil, facts.Format(
			err,
			"unable to read private key file",
		)
	}

	signer, err := ssh.ParsePrivateKey(data)
	if err != nil {
		return nil, facts.Format(
			err,
			"unable to parse private key",
		)
	}

	block, _ := pem.Decode(data)
	if block == nil {
		return nil, facts.Format(
			err,
			"unable to decode PEM block",
		)
	}

	return newKey(signer, block.Bytes)
}

func GenerateKey() (*Key, error) {
	_, private, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to generate ed25519 private key",
		)
	}

	der, err := x509.MarshalPKCS8PrivateKey(private)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to marshal private key",
		)
	}

	signer, err := ssh.NewSignerFromKey(private)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to create SSH signer",
		)
	}

	return newKey(signer, der)
}

func (key *Key) EncodePrivateKey() []byte {
	return pem.EncodeToMemory(
		&pem.Block{
			Type:  "PRIVATE KEY",
			Bytes: key.Private,
		},
	)
}
