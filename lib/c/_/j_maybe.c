#include <jni.h>

#include "j_maybe.h"
#include "j_object.h"
#include "string.h"

jobject j_maybe_void(JNIEnv *env, error err) {
  return j_maybe(env, j_object_new_void(env, "java/lang/Void"), err);
}

jobject j_maybe(JNIEnv *env, jobject j_result, error err) {
  jobject j_maybe = j_object_new_void(env, class_Maybe);

  if (err.is_error) {
    j_object_set_string(env, j_maybe, "error", err.message);
  }

  j_object_set(env, j_maybe, "result", class_ObjectL, j_result);

  return j_maybe;
}
