package io.reconquest.carcosa.lib;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Token implements Serializable {
  public String name;
  public String resource;
  public String login;
  public String payload;
}
