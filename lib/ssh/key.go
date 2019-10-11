package ssh

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"

	"github.com/reconquest/karma-go"
	"golang.org/x/crypto/ssh"
)

type Key struct {
	Public      []byte
	Private     []byte
	Fingerprint string
}

func Keygen() (*Key, error) {
	private, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to generate private key",
		)
	}

	public, err := ssh.NewPublicKey(&private.PublicKey)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to generate public key",
		)
	}

	key := &Key{
		Public:      ssh.MarshalAuthorizedKey(public),
		Private:     x509.MarshalPKCS1PrivateKey(private),
		Fingerprint: ssh.FingerprintLegacyMD5(public),
	}

	return key, nil
}

func (key *Key) EncodePrivateKey() []byte {
	return pem.EncodeToMemory(
		&pem.Block{
			Type:    "RSA PRIVATE KEY",
			Headers: nil,
			Bytes:   key.Private,
		},
	)
}
