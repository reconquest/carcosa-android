#include <jni.h>

#include "_/string.h"

typedef struct {
  string private;
  string public;
  string fingerprint;
} ssh_key;
