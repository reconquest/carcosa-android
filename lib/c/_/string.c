#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include "string.h"

const jclass jclass_String(JNIEnv *env) {
  return (*env)->FindClass(env, "java/lang/String");
}

string string_from_bytes(char *bytes) {
  return string_from_bytes_n(bytes, strlen(bytes));
}

string string_from_bytes_n(char *bytes, int length) {
  string string;
  string.length = length;
  string.data = bytes;
  return string;
}

string string_from_jstring(JNIEnv *env, jstring j_string) {
  const jclass j_string_class = jclass_String(env);

  const jmethodID j_string_getBytes = (*env)->GetMethodID(
      env, j_string_class, "getBytes", "(Ljava/lang/String;)[B");

  const jbyteArray j_byte_array = (jbyteArray)(*env)->CallObjectMethod(
      env, j_string, j_string_getBytes, (*env)->NewStringUTF(env, "UTF-8"));

  jbyte *data = (*env)->GetByteArrayElements(env, j_byte_array, NULL);

  string string = string_from_bytes_n(
      (char *)data, (size_t)(*env)->GetArrayLength(env, j_byte_array));

  string._j_byte_array = j_byte_array;

  (*env)->DeleteLocalRef(env, j_string_class);

  return string;
}

jstring string_to_jstring(JNIEnv *env, string string) {
  const jclass j_string_class = jclass_String(env);

  const jmethodID j_string_new = (*env)->GetMethodID(
      env, j_string_class, "<init>", "([BLjava/lang/String;)V");

  jbyteArray j_byte_array = (*env)->NewByteArray(env, string.length);

  (*env)->SetByteArrayRegion(env, j_byte_array, 0, string.length,
                             (jbyte *)string.data);

  jstring j_string = (jstring)(*env)->NewObject(
      env, j_string_class, j_string_new, j_byte_array,
      (*env)->NewStringUTF(env, "UTF-8"));

  /*(*env)->DeleteLocalRef(env, j_byte_array);*/
  (*env)->DeleteLocalRef(env, j_string_class);

  return j_string;
}

extern void string_release(JNIEnv *env, string string) {
  (*env)->ReleaseByteArrayElements(env, string._j_byte_array,
                                   (jbyte *)string.data, JNI_ABORT);
  (*env)->DeleteLocalRef(env, string._j_byte_array);
}
