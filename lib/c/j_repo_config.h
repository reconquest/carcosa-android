#include <jni.h>

#include "repo.h"

#define class_CarcosaLibRepoConfig class_CarcosaLib "/RepoConfig"
#define class_CarcosaLibRepoConfigL "L" class_CarcosaLibRepoConfig ";"

#ifndef _CARCOSA_J_REPO_CONFIG_H
#define _CARCOSA_J_REPO_CONFIG_H

jobject j_repo_config(JNIEnv *env, repo_config config);
void repo_config_j(JNIEnv *env, jobject j_repo_config, repo_config *out);
void repo_config_release(JNIEnv *env, repo_config repo_config);

#endif
