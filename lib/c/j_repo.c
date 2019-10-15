#include <jni.h>

#include "_/j_list.h"
#include "_/j_object.h"
#include "_/string.h"

#include "j_repo.h"
#include "j_repo_config.h"
#include "j_ssh_key.h"
#include "j_sync_stat.h"

#include "carcosa.h"

jobject j_token(JNIEnv *env, token token) {
  jobject j_token = j_object_new_void(env, class_CarcosaLibToken);

  j_object_set_string(env, j_token, "name", token.name);
  j_object_set_string(env, j_token, "resource", token.resource);
  j_object_set_string(env, j_token, "login", token.login);
  j_object_set_string(env, j_token, "payload", token.payload);

  string_release(env, token.name);
  string_release(env, token.resource);
  string_release(env, token.login);
  string_release(env, token.payload);

  return j_token;
}

jobject j_repo(JNIEnv *env, repo repo) {
  jobject j_repo = j_object_new_void(env, class_CarcosaLibRepo);

  j_object_set_string(env, j_repo, "id", repo.id);
  j_object_set_string(env, j_repo, "name", repo.name);
  j_object_set_bool(env, j_repo, "isLocked", repo.is_locked);

  j_object_set(env, j_repo, "sshKey", class_CarcosaLibSSHKeyL,
               j_ssh_key(env, repo.ssh_key));
  j_object_set(env, j_repo, "syncStat", class_CarcosaLibSyncStatL,
               j_sync_stat(env, repo.sync_stat));
  j_object_set(env, j_repo, "config", class_CarcosaLibRepoConfigL,
               j_repo_config(env, repo.config));

  jobject j_repo_tokens = j_list_new(env);

  for (int i = 0; i < repo.tokens.length; i++) {
    j_list_add(env, j_repo_tokens, j_token(env, repo.tokens.data[i]));
  }

  j_object_set(env, j_repo, "tokens", class_ArrayListL, j_repo_tokens);

  string_release(env, repo.id);
  string_release(env, repo.name);

  return j_repo;
}
