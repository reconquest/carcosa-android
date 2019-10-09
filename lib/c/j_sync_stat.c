#include <jni.h>

#include "_/j_object.h"
#include "carcosa.h"
#include "j_sync_stat.h"

jobject j_sync_stat(JNIEnv *env, sync_stat stat) {
  jobject j_sync_stat = j_object_new_void(env, class_CarcosaLib "/SyncStat");

  j_object_set_string(env, j_sync_stat, "date", stat.date);
  j_object_set_int(env, j_sync_stat, "added", stat.added);
  j_object_set_int(env, j_sync_stat, "deleted", stat.deleted);

  return j_sync_stat;
}
