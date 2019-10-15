#include <jni.h>

#include "_/j_object.h"
#include "_/string.h"

#include "j_ssh_key.h"

jobject j_ssh_key(JNIEnv *env, ssh_key ssh_key) {
  jobject j_ssh_key = j_object_new_void(env, class_CarcosaLibSSHKey);

  j_object_set_string(env, j_ssh_key, "publicKey", ssh_key.public);
  j_object_set_string(env, j_ssh_key, "fingerprint", ssh_key.fingerprint);

  return j_ssh_key;
}

void ssh_key_j(JNIEnv *env, jobject j_ssh_key, ssh_key *out) {
  out->private = j_object_get_bytes(env, j_ssh_key, "privateBytes");
  out->public = j_object_get_string(env, j_ssh_key, "publicKey");
  out->fingerprint = j_object_get_string(env, j_ssh_key, "fingerprint");
}

void ssh_key_release(JNIEnv *env, ssh_key ssh_key) {
  string_release(env, ssh_key.private);
  string_release(env, ssh_key.public);
  string_release(env, ssh_key.fingerprint);
}
