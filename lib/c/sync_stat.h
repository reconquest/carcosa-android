#include "_/string.h"

#define class_CarcosaLibSyncStat class_CarcosaLib "/SyncStat"
#define class_CarcosaLibSyncStatL "L" class_CarcosaLibSyncStat ";"

#ifndef _CARCOSA_SYNC_STAT_H
#define _CARCOSA_SYNC_STAT_H

typedef struct {
  string date;
  int added;
  int deleted;
} sync_stat;

#endif
