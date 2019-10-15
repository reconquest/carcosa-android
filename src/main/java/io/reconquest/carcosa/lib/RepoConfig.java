package io.reconquest.carcosa.lib;

import java.io.Serializable;

@SuppressWarnings("serial")
public class RepoConfig implements Serializable {
  public String address;
  public String protocol;
  public String namespace;
  public String filter;
}
