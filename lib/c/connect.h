#include "_/string.h"

#include "repo.h"
#include "ssh_key.h"

#ifndef _CARCOSA_CONNECT_H
#define _CARCOSA_CONNECT_H

typedef struct {
  repo_config config;
  ssh_key *ssh_key;
} connect_in;

typedef struct {
  string id;
  int tokens;
} connect_out;

#endif
