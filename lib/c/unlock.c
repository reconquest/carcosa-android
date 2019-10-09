#include <android/log.h>
#include <jni.h>

#include "_/j_maybe.h"
#include "_/j_object.h"
#include "_/string.h"

#include "carcosa.h"
#include "unlock.h"

extern error Unlock(unlock_in, unlock_out *);

jobject j_unlock_out(JNIEnv *env, unlock_out out) {
  jobject j_unlock_out =
      j_object_new_void(env, class_CarcosaLib "/UnlockResult");

  j_object_set_int(env, j_unlock_out, "tokens", out.tokens);

  return j_unlock_out;
}

JNIEXPORT jobject JNICALL Java_io_reconquest_carcosa_lib_Carcosa_unlock(
    JNIEnv *env, jobject this, jstring j_id, jstring j_key, jstring j_filter,
    jboolean j_cache) {

  string id = string_from_jstring(env, j_id);
  string key = string_from_jstring(env, j_key);
  string filter = string_from_jstring(env, j_filter);

  unlock_in in = {
      .id = id,
      .key = key,
      .filter = filter,
      .cache = (j_cache == JNI_TRUE),
  };

  unlock_out out;

  error err = Unlock(in, &out);

  string_release(env, id);
  string_release(env, key);
  string_release(env, filter);

  if (err.is_error) {
    return j_maybe_void(env, err);
  }

  return j_maybe(env, j_unlock_out(env, out), err);
}
