#include <stdbool.h>

#include "ssh_key.h"
#include "sync_stat.h"

#ifndef _CARCOSA_REPO_H
#define _CARCOSA_REPO_H

typedef struct {
  string name;
  string resource;
  string login;
  string payload;
} token;

typedef struct {
  int length;
  token *data;
} token_list;

typedef struct {
  string address;
  string protocol;
  string ns;
  string filter;
} repo_config;

typedef struct {
  string id;
  string name;
  repo_config config;
  ssh_key ssh_key;
  sync_stat sync_stat;
  bool is_locked;
  token_list tokens;
} repo;

typedef struct {
  int length;
  repo *data;
} repo_list;

#endif
