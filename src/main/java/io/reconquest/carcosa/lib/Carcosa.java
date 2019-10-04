package io.reconquest.carcosa.lib;

import java.io.Serializable;
import io.reconquest.carcosa.Maybe;

@SuppressWarnings("serial")
public class Carcosa implements Serializable {
  static {
    System.loadLibrary("carcosa");
  }

  public native Maybe<Void> init(String root);

  public native SSHKey keygen();

  public native Maybe<ConnectResult> connect(
      String protocol, String address, String ns, String filter);

  public native Maybe<UnlockResult> unlock(String id, String key, Boolean cache);

  public native Maybe<ListResult> list();
}
