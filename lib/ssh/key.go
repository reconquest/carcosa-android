package ssh

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"io/ioutil"

	"github.com/reconquest/karma-go"
	"golang.org/x/crypto/ssh"
)

type Key struct {
	Public      []byte
	Private     []byte
	Fingerprint string
}

func NewKey(private *rsa.PrivateKey) (*Key, error) {
	public, err := ssh.NewPublicKey(&private.PublicKey)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to generate public key",
		)
	}

	return &Key{
		Public:      ssh.MarshalAuthorizedKey(public),
		Private:     x509.MarshalPKCS1PrivateKey(private),
		Fingerprint: ssh.FingerprintLegacyMD5(public),
	}, nil
}

func ReadKey(path string) (*Key, error) {
	facts := karma.Describe("path", path)

	data, err := ioutil.ReadFile(path)
	if err != nil {
		return nil, facts.Format(
			err,
			"unable to read private key file",
		)
	}

	block, _ := pem.Decode(data)

	private, err := x509.ParsePKCS1PrivateKey(block.Bytes)
	if err != nil {
		return nil, facts.Format(
			err,
			"unable to parse private key block",
		)
	}

	return NewKey(private)
}

func GenerateKey() (*Key, error) {
	private, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		return nil, karma.Format(
			err,
			"unable to generate private key",
		)
	}

	return NewKey(private)
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
