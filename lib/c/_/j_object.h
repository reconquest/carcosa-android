#include <jni.h>

#include "string.h"

#define class_Object "java/lang/Object"
#define class_ObjectL "L" class_Object ";"

#ifndef _CARCOSA_J_OBJECT_JNI_H
#define _CARCOSA_J_OBJECT_JNI_H

jobject j_object_new_void(JNIEnv *env, const char *classname);
void j_object_set(JNIEnv *env, jobject j_object, const char *field,
                  const char *kind, jobject value);
void j_object_set_string(JNIEnv *env, jobject j_object, const char *field,
                         string value);
void j_object_set_int(JNIEnv *env, jobject j_object, const char *field,
                      int value);

#endif
