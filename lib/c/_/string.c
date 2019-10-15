#include <android/log.h>
#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include "string.h"

const jclass jclass_String(JNIEnv *env) {
  return (*env)->FindClass(env, class_String);
}

string string_from_bytes(char *bytes) {
  return string_from_bytes_n(bytes, strlen(bytes));
}

string string_from_bytes_n(char *bytes, int length) {
  string string;
  string.length = length;
  string.data = bytes;
  string._j_byte_array = NULL;
  return string;
}

string string_from_jstring(JNIEnv *env, jstring j_string) {
  const jclass j_string_class = jclass_String(env);

  const jmethodID j_string_getBytes = (*env)->GetMethodID(
      env, j_string_class, "getBytes", "(" class_StringL ")[B");

  const jbyteArray j_byte_array = (jbyteArray)(*env)->CallObjectMethod(
      env, j_string, j_string_getBytes, (*env)->NewStringUTF(env, "UTF-8"));

  jbyte *data = (*env)->GetByteArrayElements(env, j_byte_array, NULL);

  string string = string_from_bytes_n(
      (char *)data, (size_t)(*env)->GetArrayLength(env, j_byte_array));

  string._j_byte_array = j_byte_array;

  (*env)->DeleteLocalRef(env, j_string_class);

  return string;
}

string string_from_jbytes(JNIEnv *env, jbyteArray j_bytes) {
  const jclass j_string_class = jclass_String(env);

  jsize length = (*env)->GetArrayLength(env, j_bytes);
  jbyte *bytes = (*env)->GetByteArrayElements(env, j_bytes, NULL);

  string result = string_from_bytes_n((char *)bytes, length);

  result._j_byte_array = j_bytes;

  return result;
}

jstring string_to_jstring(JNIEnv *env, string string) {
  const jclass j_string_class = jclass_String(env);

  const jmethodID j_string_new = (*env)->GetMethodID(
      env, j_string_class, "<init>", "([B" class_StringL ")V");

  jbyteArray j_byte_array = (*env)->NewByteArray(env, string.length);

  (*env)->SetByteArrayRegion(env, j_byte_array, 0, string.length,
                             (jbyte *)string.data);

  jstring j_string = (jstring)(*env)->NewObject(
      env, j_string_class, j_string_new, j_byte_array,
      (*env)->NewStringUTF(env, "UTF-8"));

  (*env)->DeleteLocalRef(env, j_byte_array);
  (*env)->DeleteLocalRef(env, j_string_class);

  return j_string;
}

jbyteArray string_to_jbytes(JNIEnv *env, string string) {
  jbyteArray j_byte_array = (*env)->NewByteArray(env, string.length);

  (*env)->SetByteArrayRegion(env, j_byte_array, 0, string.length,
                             (jbyte *)string.data);
  return j_byte_array;
}

void string_release(JNIEnv *env, string string) {
  if (string._j_byte_array != NULL) {
    (*env)->ReleaseByteArrayElements(env, string._j_byte_array,
                                     (jbyte *)string.data, JNI_ABORT);
    (*env)->DeleteLocalRef(env, string._j_byte_array);
  } else {
    if (string.length > 0) {
      free(string.data);
    }
  }
}
