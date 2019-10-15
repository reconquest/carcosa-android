package io.reconquest.carcosa.lib;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SyncStat implements Serializable {
  public String date;
  public int added;
  public int deleted;
}
