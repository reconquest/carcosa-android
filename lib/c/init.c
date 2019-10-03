#include <jni.h>

#include "_string.h"

extern void Init(string root);

JNIEXPORT void JNICALL Java_io_reconquest_carcosa_Carcosa_init(JNIEnv *env,
                                                               jobject this,
                                                               jstring j_root) {
  string root = string_from_jstring(env, j_root);
  Init(root);
  string_release(env, root);
}
