#include <stdlib.h>

#include "repo.h"

repo_list repo_list_new(int length) {
  repo_list list;

  list.length = length;
  list.data = malloc(sizeof(repo) * length);

  return list;
}

void repo_list_set(repo_list list, int index, repo repo) {
  list.data[index] = repo;
}

token_list token_list_new(int length) {
  token_list list;

  list.length = length;
  list.data = malloc(sizeof(token) * length);

  return list;
}

void token_list_set(token_list list, int index, token token) {
  list.data[index] = token;
}
