#include <jni.h>

#include "j_list.h"
#include "j_object.h"

jobject j_list_new(JNIEnv *env) {
  return j_object_new_void(env, class_ArrayList);
}

jboolean j_list_add(JNIEnv *env, jobject j_list, jobject j_item) {
  jclass j_list_class = (*env)->FindClass(env, class_ArrayList);

  jmethodID j_list_add =
      (*env)->GetMethodID(env, j_list_class, "add", "(Ljava/lang/Object;)Z");

  jboolean j_result =
      (*env)->CallBooleanMethod(env, j_list, j_list_add, j_item);

  (*env)->DeleteLocalRef(env, j_list_class);

  return j_result;
}
