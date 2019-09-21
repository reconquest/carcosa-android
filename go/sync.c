#include <jni.h>

extern void Sync();

JNIEXPORT void JNICALL Java_io_reconquest_carcosa_Carcosa_sync
  (JNIEnv *env, jobject this) {
	  Sync();
  }
