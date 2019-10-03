#include <jni.h>

#include "j_maybe.h"
#include "string.h"

jobject j_maybe_void(JNIEnv *env, error err) {
  jclass j_void_class = (*env)->FindClass(env, "java/lang/Void");

  jmethodID j_void_new =
      (*env)->GetMethodID(env, j_void_class, "<init>", "()V");

  jobject j_void = (*env)->NewObject(env, j_void_class, j_void_new);

  return j_maybe(env, j_void, err);
}

jobject j_maybe(JNIEnv *env, jobject j_result, error err) {
  jclass j_maybe_class = (*env)->FindClass(env, "io/reconquest/carcosa/Maybe");

  jmethodID j_maybe_new =
      (*env)->GetMethodID(env, j_maybe_class, "<init>", "()V");

  jobject j_maybe = (*env)->NewObject(env, j_maybe_class, j_maybe_new);

  jfieldID j_maybe_error =
      (*env)->GetFieldID(env, j_maybe_class, "error", "Ljava/lang/String;");

  jfieldID j_maybe_result =
      (*env)->GetFieldID(env, j_maybe_class, "result", "Ljava/lang/Object;");

  if (err.is_error) {
    jstring j_maybe_message = string_to_jstring(env, err.message);
    (*env)->SetObjectField(env, j_maybe, j_maybe_error, j_maybe_message);
  }

  (*env)->SetObjectField(env, j_maybe, j_maybe_result, j_result);

  return j_maybe;
}
