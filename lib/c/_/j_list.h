#include <jni.h>

#ifndef _CARCOSA_J_LIST_JNI_H
#define _CARCOSA_J_LIST_JNI_H

#define class_ArrayList "java/util/ArrayList"
#define class_ArrayListL "L" class_ArrayList ";"

jobject j_list_new(JNIEnv *env);
jboolean j_list_add(JNIEnv *env, jobject j_list, jobject j_item);

#endif
