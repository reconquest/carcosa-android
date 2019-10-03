#include "_/string.h"
#include <stdbool.h>

#ifndef _CARCOSA_LIST_H
#define _CARCOSA_LIST_H

typedef struct {
  string name;
} token;

typedef struct {
  int length;
  token *data;
} token_list;

typedef struct {
  string id;
  string name;
  bool is_locked;
  token_list tokens;
} repo;

typedef struct {
} list_in;

typedef struct {
  int length;
  repo *data;
} repo_list;

typedef struct {
  repo_list repos;
} list_out;

repo_list repo_list_new(int length);
void repo_list_set(repo_list list, int index, repo repo);

token_list token_list_new(int length);
void token_list_set(token_list list, int index, token token);

#endif
