#include "_/string.h"

#ifndef _CARCOSA_CONNECT_H
#define _CARCOSA_CONNECT_H

typedef struct {
  string protocol;
  string address;
  string ns;
} connect_in;

typedef struct {
  string id;
  int tokens;
} connect_out;

#endif
