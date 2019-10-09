#include <android/log.h>
#include <jni.h>

#include "_/j_maybe.h"
#include "_/j_object.h"
#include "_/string.h"

#include "carcosa.h"
#include "connect.h"

extern error Connect(connect_in, connect_out *);

jobject j_connect_out(JNIEnv *env, connect_out out) {
  jobject j_connect_out =
      j_object_new_void(env, class_CarcosaLib "/ConnectResult");

  j_object_set_string(env, j_connect_out, "id", out.id);
  j_object_set_int(env, j_connect_out, "tokens", out.tokens);

  return j_connect_out;
}

JNIEXPORT jobject JNICALL Java_io_reconquest_carcosa_lib_Carcosa_connect(
    JNIEnv *env, jobject this, jstring j_protocol, jstring j_address,
    jstring j_ns) {

  string protocol = string_from_jstring(env, j_protocol);
  string address = string_from_jstring(env, j_address);
  string ns = string_from_jstring(env, j_ns);

  connect_in in = {
      .protocol = protocol,
      .address = address,
      .ns = ns,
  };

  connect_out out;

  error err = Connect(in, &out);

  string_release(env, protocol);
  string_release(env, address);
  string_release(env, ns);

  if (err.is_error) {
    return j_maybe_void(env, err);
  }

  return j_maybe(env, j_connect_out(env, out), err);
}
