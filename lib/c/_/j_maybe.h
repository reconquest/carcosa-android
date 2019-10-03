#include <jni.h>

#include "error.h"
#include "string.h"

#ifndef _CARCOSA_J_MAYBE_JNI_H
#define _CARCOSA_J_MAYBE_JNI_H

extern jobject j_maybe_void(JNIEnv *env, error err);
extern jobject j_maybe(JNIEnv *env, jobject j_result, error err);

#endif
