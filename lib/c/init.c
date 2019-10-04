#include <android/log.h>
#include <jni.h>

#include "_/error.h"
#include "_/j_maybe.h"
#include "_/string.h"

extern error Init(string root);

JNIEXPORT jobject JNICALL Java_io_reconquest_carcosa_Carcosa_init(
    JNIEnv *env, jobject this, jstring j_root) {
  string root = string_from_jstring(env, j_root);

  error err = Init(root);

  string_release(env, root);

  return j_maybe_void(env, err);
}
