#include <stdbool.h>

#include "repo.h"

#define class_CarcosaLibRepo class_CarcosaLib "/Repo"
#define class_CarcosaLibRepoL "L" class_CarcosaLibRepo ";"

#define class_CarcosaLibToken class_CarcosaLib "/Token"
#define class_CarcosaLibTokenL "L" class_CarcosaLibToken ";"

#ifndef _CARCOSA_J_REPO_H
#define _CARCOSA_J_REPO_H

jobject j_repo(JNIEnv *env, repo repo);

#endif
