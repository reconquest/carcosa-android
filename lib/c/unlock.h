#include "_/string.h"

typedef struct {
  string id;
  string filter;
  string key;
  int cache;
} unlock_in;

typedef struct {
  int tokens;
} unlock_out;
