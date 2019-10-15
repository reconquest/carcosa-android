#include <android/log.h>
#include <jni.h>

#include "_/error.h"
#include "_/j_maybe.h"
#include "_/j_object.h"
#include "_/string.h"

#include "j_ssh_key.h"

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

  return j_maybe(env, j_ssh_key(env, key), err);
}
