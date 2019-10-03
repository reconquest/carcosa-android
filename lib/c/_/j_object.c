#include <jni.h>

#include "j_object.h"

jobject j_object_new_void(JNIEnv *env, const char *classname) {
  jclass j_class = (*env)->FindClass(env, classname);
  jmethodID j_class_new = (*env)->GetMethodID(env, j_class, "<init>", "()V");

  jobject j_object = (*env)->NewObject(env, j_class, j_class_new);

  (*env)->DeleteLocalRef(env, j_class_new);

  return j_object;
}

void j_object_set_string(JNIEnv *env, jobject j_object, const char *field,
                         string value) {
  j_object_set(env, j_object, field, "Ljava/lang/String;",
               string_to_jstring(env, value));
  /*jclass j_class = (*env)->GetObjectClass(env, j_object);*/

  /*jfieldID j_field =*/
  /*    (*env)->GetFieldID(env, j_class, field, "Ljava/lang/String;");*/

  /*jstring j_value = string_to_jstring(env, value);*/

  /*(*env)->SetObjectField(env, j_object, j_field, j_value);*/

  /*(*env)->DeleteLocalRef(env, j_field);*/
  /*(*env)->DeleteLocalRef(env, j_class);*/
}

void j_object_set(JNIEnv *env, jobject j_object, const char *field,
                  const char *kind, jobject j_value) {
  jclass j_class = (*env)->GetObjectClass(env, j_object);

  jfieldID j_field = (*env)->GetFieldID(env, j_class, field, kind);

  (*env)->SetObjectField(env, j_object, j_field, j_value);

  (*env)->DeleteLocalRef(env, j_field);
  (*env)->DeleteLocalRef(env, j_class);
}
