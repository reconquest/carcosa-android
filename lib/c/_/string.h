#include <jni.h>

#ifndef _CARCOSA_STRING_JNI_H
#define _CARCOSA_STRING_JNI_H

#define class_String "java/lang/String"
#define class_StringL "L" class_String ";"

typedef struct {
  int length;
  char *data;

  jbyteArray *_j_byte_array;
} string;

string string_from_bytes(char *bytes);
string string_from_bytes_n(char *bytes, int length);
string string_from_jstring(JNIEnv *env, jstring j_string);
jstring string_to_jstring(JNIEnv *env, string string);
void string_release(JNIEnv *env, string string);

#endif
