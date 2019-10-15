package io.reconquest.carcosa.lib;

import java.io.Serializable;
import io.reconquest.carcosa.Maybe;

@SuppressWarnings("serial")
public class Carcosa implements Serializable {
  static {
    System.loadLibrary("carcosa");
  }

  public native Maybe<Void> init(String root, String pin);

  public native boolean hasState();

  public native Maybe<Void> sync();

  public native Maybe<SSHKey> keygen();

  public native Maybe<ConnectResult> connect(RepoConfig repoConfig, SSHKey sshKey);

  public native Maybe<UnlockResult> unlock(String id, String key, String filter, Boolean cache);

  public native Maybe<ListResult> list();
}
