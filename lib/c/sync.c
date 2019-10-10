#include <jni.h>

#include "_/j_maybe.h"
#include "_/j_object.h"
#include "_/string.h"

#include "carcosa.h"
#include "sync.h"

extern error Sync();

JNIEXPORT jobject JNICALL
Java_io_reconquest_carcosa_lib_Carcosa_sync(JNIEnv *env, jobject this) {

  error err = Sync();

  return j_maybe_void(env, err);
}
