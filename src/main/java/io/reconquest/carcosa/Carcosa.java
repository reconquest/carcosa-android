package io.reconquest.carcosa;

public class Carcosa {
  static {
    System.loadLibrary("carcosa");
  }

  public native void sync(String path);
}
