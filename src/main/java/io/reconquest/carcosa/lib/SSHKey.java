package io.reconquest.carcosa.lib;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SSHKey implements Serializable {
  public byte[] privateBytes;
  public String publicKey;
  public String fingerprint;
}
