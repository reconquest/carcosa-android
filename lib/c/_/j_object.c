#include <jni.h>

#include "j_object.h"

jobject j_object_new_void(JNIEnv *env, const char *classname) {
  jclass j_class = (*env)->FindClass(env, classname);
  jmethodID j_class_new = (*env)->GetMethodID(env, j_class, "<init>", "()V");

  jobject j_object = (*env)->NewObject(env, j_class, j_class_new);

  (*env)->DeleteLocalRef(env, j_class);

  return j_object;
}

void j_object_set_string(JNIEnv *env, jobject j_object, const char *field,
                         string value) {
  j_object_set(env, j_object, field, class_StringL,
               string_to_jstring(env, value));
}

void j_object_set_int(JNIEnv *env, jobject j_object, const char *field,
                      int value) {
  jclass j_class = (*env)->GetObjectClass(env, j_object);

  jfieldID j_field = (*env)->GetFieldID(env, j_class, field, "I");

  (*env)->SetIntField(env, j_object, j_field, value);

  (*env)->DeleteLocalRef(env, j_class);
}

void j_object_set_bool(JNIEnv *env, jobject j_object, const char *field,
                       bool value) {
  jclass j_class = (*env)->GetObjectClass(env, j_object);

  jfieldID j_field = (*env)->GetFieldID(env, j_class, field, "Z");

  (*env)->SetBooleanField(env, j_object, j_field, value ? JNI_TRUE : JNI_FALSE);

  (*env)->DeleteLocalRef(env, j_class);
}

void j_object_set_bytes(JNIEnv *env, jobject j_object, const char *field,
                        string value) {
  jclass j_class = (*env)->GetObjectClass(env, j_object);

  jfieldID j_field = (*env)->GetFieldID(env, j_class, field, "[B");

  (*env)->SetObjectField(env, j_object, j_field, string_to_jbytes(env, value));

  (*env)->DeleteLocalRef(env, j_class);
}

void j_object_set(JNIEnv *env, jobject j_object, const char *field,
                  const char *kind, jobject j_value) {
  jclass j_class = (*env)->GetObjectClass(env, j_object);

  jfieldID j_field = (*env)->GetFieldID(env, j_class, field, kind);

  (*env)->SetObjectField(env, j_object, j_field, j_value);

  (*env)->DeleteLocalRef(env, j_class);
}

string j_object_get_string(JNIEnv *env, jobject j_object, const char *field) {
  jclass j_class = (*env)->GetObjectClass(env, j_object);

  jfieldID j_field = (*env)->GetFieldID(env, j_class, field, class_StringL);

  jstring j_string = (*env)->GetObjectField(env, j_object, j_field);

  (*env)->DeleteLocalRef(env, j_class);

  return string_from_jstring(env, j_string);
}

string j_object_get_bytes(JNIEnv *env, jobject j_object, const char *field) {
  jclass j_class = (*env)->GetObjectClass(env, j_object);

  jfieldID j_field = (*env)->GetFieldID(env, j_class, field, "[B");

  jbyteArray j_bytes = (*env)->GetObjectField(env, j_object, j_field);

  (*env)->DeleteLocalRef(env, j_class);

  return string_from_jbytes(env, j_bytes);
}
