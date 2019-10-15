#include <jni.h>

#include "sync_stat.h"

#define class_CarcosaLibSyncStat class_CarcosaLib "/SyncStat"
#define class_CarcosaLibSyncStatL "L" class_CarcosaLibSyncStat ";"

#ifndef _CARCOSA_J_SYNC_STAT_H
#define _CARCOSA_J_SYNC_STAT_H

jobject j_sync_stat(JNIEnv *env, sync_stat stat);

#endif
