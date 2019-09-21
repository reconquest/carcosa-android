#include <jni.h>

#include "_strings.h"

extern void Init(jbyte*);

JNIEXPORT void JNICALL Java_io_reconquest_carcosa_Carcosa_sync
  (JNIEnv *env, jobject this, jstring path_string) {
	jbyte *path = jstring_to_jbytes(env, path_string);
	Init(path);
    (*env)->DeleteLocalRef(env, path);
  }
