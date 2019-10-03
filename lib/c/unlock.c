#include <android/log.h>
#include <jni.h>

#include "_j_maybe.h"
#include "_string.h"
#include "unlock.h"

extern error Unlock(unlock_in, unlock_out *);

jobject j_unlock_out(JNIEnv *env, unlock_out out) {
  jclass j_unlock_out_class =
      (*env)->FindClass(env, "io/reconquest/carcosa/Carcosa$UnlockResult");

  jmethodID j_unlock_out_new =
      (*env)->GetMethodID(env, j_unlock_out_class, "<init>", "()V");

  jobject j_unlock_out =
      (*env)->NewObject(env, j_unlock_out_class, j_unlock_out_new);

  jfieldID j_unlock_out_tokens =
      (*env)->GetFieldID(env, j_unlock_out_class, "tokens", "I");

  (*env)->SetIntField(env, j_unlock_out, j_unlock_out_tokens, out.tokens);

  (*env)->DeleteLocalRef(env, j_unlock_out_tokens);
  (*env)->DeleteLocalRef(env, j_unlock_out_new);
  (*env)->DeleteLocalRef(env, j_unlock_out_class);

  return j_unlock_out;
}

JNIEXPORT jobject JNICALL Java_io_reconquest_carcosa_Carcosa_unlock(
    JNIEnv *env, jobject this, jstring j_id, jstring j_key, jboolean j_cache) {

  string id = string_from_jstring(env, j_id);
  string key = string_from_jstring(env, j_key);

  unlock_in in = {
      .id = id,
      .key = key,
      .cache = (j_cache == JNI_TRUE),
  };

  unlock_out out;

  error err = Unlock(in, &out);

  string_release(env, id);
  string_release(env, key);

  return j_maybe(env, j_unlock_out(env, out), err);
}
