#include <android/log.h>
#include <jni.h>

#include "_/j_list.h"
#include "_/j_maybe.h"
#include "_/j_object.h"
#include "_/string.h"

#include "list.h"

extern error List(list_in, list_out *);

repo_list repo_list_new(int length) {
  repo_list list;

  list.length = length;
  list.data = malloc(sizeof(repo) * length);

  return list;
}

void repo_list_set(repo_list list, int index, repo repo) {
  list.data[index] = repo;
}

jobject j_repo(JNIEnv *env, repo repo) {
  jobject j_repo = j_object_new_void(env, "io/reconquest/carcosa/Carcosa$Repo");
  j_object_set_string(env, j_repo, "name", repo.name);

  return j_repo;
}

jobject j_list_out(JNIEnv *env, list_out out) {
  const char *class_ListResult = "io/reconquest/carcosa/Carcosa$ListResult";

  jobject j_list_out = j_object_new_void(env, class_ListResult);

  jobject j_repo_list = j_list_new(env);

  for (int i = 0; i < out.repos.length; i++) {
    j_list_add(env, j_repo_list, j_repo(env, out.repos.data[i]));
  }

  j_object_set(env, j_list_out, "repos", class_ArrayListL, j_repo_list);

  return j_list_out;
}

JNIEXPORT jobject JNICALL
Java_io_reconquest_carcosa_Carcosa_list(JNIEnv *env, jobject this) {
  list_in in = {};

  list_out out;

  error err = List(in, &out);

  return j_maybe(env, j_list_out(env, out), err);
}
/*void token_list_add(token_list list, token token);*/
