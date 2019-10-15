#include <jni.h>

#include "_/j_object.h"
#include "_/string.h"

#include "j_repo_config.h"

#include "carcosa.h"

void repo_config_j(JNIEnv *env, jobject j_repo_config, repo_config *out) {
  out->protocol = j_object_get_string(env, j_repo_config, "protocol");
  out->address = j_object_get_string(env, j_repo_config, "address");
  out->ns = j_object_get_string(env, j_repo_config, "namespace");
  out->filter = j_object_get_string(env, j_repo_config, "filter");
}

jobject j_repo_config(JNIEnv *env, repo_config config) {
  jobject j_repo_config = j_object_new_void(env, class_CarcosaLibRepoConfig);

  j_object_set_string(env, j_repo_config, "address", config.address);
  j_object_set_string(env, j_repo_config, "protocol", config.protocol);
  j_object_set_string(env, j_repo_config, "namespace", config.ns);
  j_object_set_string(env, j_repo_config, "filter", config.filter);

  return j_repo_config;
}

void repo_config_release(JNIEnv *env, repo_config repo_config) {
  string_release(env, repo_config.protocol);
  string_release(env, repo_config.address);
  string_release(env, repo_config.ns);
  string_release(env, repo_config.filter);
}
