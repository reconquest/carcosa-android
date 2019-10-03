package main

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"

	"github.com/reconquest/karma-go"
	"golang.org/x/crypto/ssh"
)

// #include "c/_/error.h"
// #include "c/ssh_key.h"
import "C"

func keygen() ([]byte, []byte, []byte, error) {
	private, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		return nil, nil, nil, karma.Format(
			err,
			"unable to generate private key",
		)
	}

	public, err := ssh.NewPublicKey(&private.PublicKey)
	if err != nil {
		return nil, nil, nil, karma.Format(
			err,
			"unable to generate public key",
		)
	}

	var (
		publicBytes  = ssh.MarshalAuthorizedKey(public)
		privateBytes = x509.MarshalPKCS1PrivateKey(private)
		fingerprint  = ssh.FingerprintLegacyMD5(public)
	)

	return publicBytes, privateBytes, []byte(fingerprint), nil

	//var (
	//publicBytes  = ssh.MarshalAuthorizedKey(public)
	//fingerprint  = ssh.FingerprintLegacyMD5(public)
	//privateBytes = pem.EncodeToMemory(
	//    &pem.Block{
	//        Type:    "RSA PRIVATE KEY",
	//        Headers: nil,
	//        Bytes:   x509.MarshalPKCS1PrivateKey(private),
	//    },
	//)
	//)

	//return string(publicBytes), string(privateBytes), fingerprint, nil
}

//export Keygen
func Keygen(key *C.ssh_key) C.error {
	public, private, fingerprint, err := keygen()
	if err != nil {
		return CError(err)
	}

	key.private = CString(string(private))
	key.public = CString(string(public))
	key.fingerprint = CString(string(fingerprint))

	return CError(nil)
}
