#include <android/log.h>
#include <jni.h>

#include "_/error.h"
#include "_/j_maybe.h"
#include "_/string.h"
#include "init.h"

extern error Init(init_in);
extern int HasState();

JNIEXPORT jobject JNICALL Java_io_reconquest_carcosa_lib_Carcosa_init(
    JNIEnv *env, jobject this, jstring j_root, jstring j_pin) {

  init_in in = {
      .root = string_from_jstring(env, j_root),
      .pin = string_from_jstring(env, j_pin),
  };

  error err = Init(in);

  string_release(env, in.root);

  return j_maybe_void(env, err);
}

JNIEXPORT jboolean JNICALL Java_io_reconquest_carcosa_lib_Carcosa_hasState(
    JNIEnv *env, jobject this) {
    int result = HasState();
    return result != 0;
}
