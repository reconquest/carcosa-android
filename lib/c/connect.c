#include <android/log.h>
#include <jni.h>

#include "_/j_maybe.h"
#include "_/string.h"

#include "connect.h"

extern error Connect(connect_in, connect_out *);

jobject j_connect_out(JNIEnv *env, connect_out out) {
  jclass j_connect_out_class =
      (*env)->FindClass(env, "io/reconquest/carcosa/Carcosa$ConnectResult");

  jmethodID j_connect_out_new =
      (*env)->GetMethodID(env, j_connect_out_class, "<init>", "()V");

  jobject j_connect_out =
      (*env)->NewObject(env, j_connect_out_class, j_connect_out_new);

  jfieldID j_connect_out_id =
      (*env)->GetFieldID(env, j_connect_out_class, "id", "Ljava/lang/String;");

  jfieldID j_connect_out_tokens =
      (*env)->GetFieldID(env, j_connect_out_class, "tokens", "I");

  jstring j_id = string_to_jstring(env, out.id);

  (*env)->SetObjectField(env, j_connect_out, j_connect_out_id, j_id);
  (*env)->SetIntField(env, j_connect_out, j_connect_out_tokens, out.tokens);

  (*env)->DeleteLocalRef(env, j_connect_out_id);
  (*env)->DeleteLocalRef(env, j_connect_out_tokens);
  (*env)->DeleteLocalRef(env, j_connect_out_new);
  (*env)->DeleteLocalRef(env, j_connect_out_class);

  return j_connect_out;
}

JNIEXPORT jobject JNICALL Java_io_reconquest_carcosa_Carcosa_connect(
    JNIEnv *env, jobject this, jstring j_protocol, jstring j_address,
    jstring j_ns, jstring j_filter) {

  string protocol = string_from_jstring(env, j_protocol);
  string address = string_from_jstring(env, j_address);
  string ns = string_from_jstring(env, j_ns);
  string filter = string_from_jstring(env, j_filter);

  connect_in in = {
      .protocol = protocol,
      .address = address,
      .ns = ns,
      .filter = filter,
  };

  connect_out out;

  error err = Connect(in, &out);

  string_release(env, protocol);
  string_release(env, address);
  string_release(env, ns);
  string_release(env, filter);

  return j_maybe(env, j_connect_out(env, out), err);
}
