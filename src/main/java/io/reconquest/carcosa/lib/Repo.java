package io.reconquest.carcosa.lib;

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class Repo implements Serializable {
  public String id;
  public String name;
  public RepoConfig config;
  public SyncStat syncStat;
  public SSHKey sshKey;
  public ArrayList<Token> tokens;
  public boolean isLocked;
}
