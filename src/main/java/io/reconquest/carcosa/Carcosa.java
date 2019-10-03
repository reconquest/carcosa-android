package io.reconquest.carcosa;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Carcosa implements Serializable {
  static {
    System.loadLibrary("carcosa");
  }

  public class SyncStat {
    int added;
    int deleted;
  }

  public native void init(String root);

  public class SSHKey {
    public String privateKey;
    public String publicKey;
    public String fingerprint;
  }

  public native SSHKey keygen();

  public class ConnectResult {
    String id;
    int tokens;
  }

  public native Maybe<ConnectResult> connect(
      String protocol, String address, String ns, String filter);

  public class UnlockResult {
    int tokens;
  }

  public native Maybe<UnlockResult> unlock(String id, String key, Boolean cache);
}
