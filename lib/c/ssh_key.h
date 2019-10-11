#include <jni.h>

#include "_/string.h"

#ifndef _CARCOSA_SSH_KEY_H
#define _CARCOSA_SSH_KEY_H

typedef struct {
  string private;
  string public;
  string fingerprint;
} ssh_key;

#endif
