#include <jni.h>

#ifndef _CARCOSA_STRING_JNI_H
#define _CARCOSA_STRING_JNI_H

typedef struct {
  int length;
  char *data;

  jbyteArray *_j_byte_array;
} string;

extern string string_from_bytes(char *bytes);
extern string string_from_bytes_n(char *bytes, int length);
extern string string_from_jstring(JNIEnv *env, jstring j_string);
extern jstring string_to_jstring(JNIEnv *env, string string);
extern void string_release(JNIEnv *env, string string);

#endif
