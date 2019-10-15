#include <jni.h>

#include "_/j_object.h"
#include "ssh_key.h"

#define class_CarcosaLibSSHKey class_CarcosaLib "/SSHKey"
#define class_CarcosaLibSSHKeyL "L" class_CarcosaLibSSHKey ";"

#ifndef _CARCOSA_J_SSH_KEY_H
#define _CARCOSA_J_SSH_KEY_H

jobject j_ssh_key(JNIEnv *env, ssh_key ssh_key);
void ssh_key_j(JNIEnv *env, jobject j_ssh_key, ssh_key *out);
void ssh_key_release(JNIEnv *env, ssh_key ssh_key);

#endif
