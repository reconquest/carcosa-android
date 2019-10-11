#include <android/log.h>
#include <jni.h>

#include "_/error.h"
#include "_/j_maybe.h"
#include "_/j_object.h"
#include "_/string.h"
#include "carcosa.h"
#include "ssh_key.h"

extern error Keygen(ssh_key *);

JNIEXPORT jobject JNICALL
Java_io_reconquest_carcosa_lib_Carcosa_keygen(JNIEnv *env, jobject this) {
  ssh_key key;

  error err = Keygen(&key);

  if (err.is_error) {
    return j_maybe_void(env, err);
  }

  jobject j_ssh_key = j_object_new_void(env, class_CarcosaLib "/SSHKey");

  j_object_set_bytes(env, j_ssh_key, "privateBytes", key.private);
  j_object_set_string(env, j_ssh_key, "publicKey", key.public);
  j_object_set_string(env, j_ssh_key, "fingerprint", key.fingerprint);

  return j_maybe(env, j_ssh_key, err);
}
