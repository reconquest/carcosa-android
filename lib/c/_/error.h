#include <stdbool.h>
#include <stdlib.h>

#include "string.h"

#ifndef _CARCOSA_ERROR_JNI_H
#define _CARCOSA_ERROR_JNI_H

typedef struct {
  bool is_error;
  string message;
} error;

#endif
