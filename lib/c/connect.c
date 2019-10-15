#include <android/log.h>
#include <jni.h>

#include "_/j_maybe.h"
#include "_/j_object.h"
#include "_/string.h"

#include "j_repo_config.h"
#include "j_ssh_key.h"

#include "carcosa.h"
#include "connect.h"

extern error Connect(connect_in, connect_out *);

jobject j_connect_out(JNIEnv *env, connect_out out) {
  jobject j_connect_out =
      j_object_new_void(env, class_CarcosaLib "/ConnectResult");

  j_object_set_string(env, j_connect_out, "id", out.id);
  j_object_set_int(env, j_connect_out, "tokens", out.tokens);

  string_release(env, out.id);

  return j_connect_out;
}

JNIEXPORT jobject JNICALL Java_io_reconquest_carcosa_lib_Carcosa_connect(
    JNIEnv *env, jobject this, jobject j_repo_config, jobject j_ssh_key) {

  connect_in in = {
      .ssh_key = NULL,
  };

  repo_config_j(env, j_repo_config, &in.config);

  ssh_key ssh_key;
  if (!(*env)->IsSameObject(env, j_ssh_key, NULL)) {
    ssh_key_j(env, j_ssh_key, &ssh_key);
    in.ssh_key = &ssh_key;
  };

  connect_out out;

  error err = Connect(in, &out);

  repo_config_release(env, in.config);

  if (in.ssh_key != NULL) {
    ssh_key_release(env, *in.ssh_key);
  }

  if (err.is_error) {
    return j_maybe_void(env, err);
  }

  return j_maybe(env, j_connect_out(env, out), err);
}
