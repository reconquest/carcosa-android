#include <jni.h>

#include "string.h"

#ifndef _CARCOSA_J_OBJECT_JNI_H
#define _CARCOSA_J_OBJECT_JNI_H

jobject j_object_new_void(JNIEnv *env, const char *classname);
void j_object_set(JNIEnv *env, jobject j_object, const char *field,
                  const char *kind, jobject value);
void j_object_set_string(JNIEnv *env, jobject j_object, const char *field,
                         string value);

#endif
