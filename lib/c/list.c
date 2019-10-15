#include <android/log.h>
#include <jni.h>
#include <string.h>

#include "_/j_list.h"
#include "_/j_maybe.h"
#include "_/j_object.h"
#include "_/string.h"

#include "j_repo.h"
#include "j_sync_stat.h"

#include "carcosa.h"
#include "list.h"

extern error List(list_in, list_out *);

jobject j_list_out(JNIEnv *env, list_out out) {
  const char *class_ListResult = class_CarcosaLib "/ListResult";

  jobject j_list_out = j_object_new_void(env, class_ListResult);

  jobject j_repo_list = j_list_new(env);

  for (int i = 0; i < out.repos.length; i++) {
    j_list_add(env, j_repo_list, j_repo(env, out.repos.data[i]));
  }

  j_object_set(env, j_list_out, "repos", class_ArrayListL, j_repo_list);

  return j_list_out;
}

JNIEXPORT jobject JNICALL
Java_io_reconquest_carcosa_lib_Carcosa_list(JNIEnv *env, jobject this) {
  list_in in = {};

  list_out out;

  error err = List(in, &out);

  return j_maybe(env, j_list_out(env, out), err);
}
