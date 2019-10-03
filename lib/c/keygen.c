#include <jni.h>

#include "_/error.h"
#include "_/string.h"
#include "ssh_key.h"

extern error Keygen(ssh_key *);

JNIEXPORT jobject JNICALL
Java_io_reconquest_carcosa_Carcosa_keygen(JNIEnv *env, jobject this) {
  ssh_key key;
  error err = Keygen(&key);
  if (err.is_error) {
    // TODO
    return NULL;
  }

  jclass j_ssh_key_class =
      (*env)->FindClass(env, "io/reconquest/carcosa/SSHKey");

  jmethodID j_ssh_key_new =
      (*env)->GetMethodID(env, j_ssh_key_class, "<init>", "()V");

  jobject j_key = (*env)->NewObject(env, j_ssh_key_class, j_ssh_key_new);

  jfieldID j_ssh_key_privateKey = (*env)->GetFieldID(
      env, j_ssh_key_class, "privateKey", "Ljava/lang/String;");

  jfieldID j_ssh_key_publicKey = (*env)->GetFieldID(
      env, j_ssh_key_class, "publicKey", "Ljava/lang/String;");

  jfieldID j_ssh_key_fingerprint = (*env)->GetFieldID(
      env, j_ssh_key_class, "fingerprint", "Ljava/lang/String;");

  jstring j_publicKey = string_to_jstring(env, key.public);
  jstring j_privateKey = string_to_jstring(env, key.private);
  jstring j_fingerprint = string_to_jstring(env, key.fingerprint);

  (*env)->SetObjectField(env, j_key, j_ssh_key_publicKey, j_publicKey);
  (*env)->SetObjectField(env, j_key, j_ssh_key_privateKey, j_privateKey);
  (*env)->SetObjectField(env, j_key, j_ssh_key_fingerprint, j_fingerprint);

  (*env)->ReleaseStringUTFChars(
      env, j_publicKey, (*env)->GetStringUTFChars(env, j_publicKey, NULL));
  (*env)->ReleaseStringUTFChars(
      env, j_privateKey, (*env)->GetStringUTFChars(env, j_privateKey, NULL));
  (*env)->ReleaseStringUTFChars(
      env, j_fingerprint, (*env)->GetStringUTFChars(env, j_fingerprint, NULL));

  (*env)->DeleteLocalRef(env, j_ssh_key_publicKey);
  (*env)->DeleteLocalRef(env, j_ssh_key_privateKey);
  (*env)->DeleteLocalRef(env, j_ssh_key_fingerprint);
  (*env)->DeleteLocalRef(env, j_ssh_key_new);
  (*env)->DeleteLocalRef(env, j_ssh_key_class);

  return j_key;
}
