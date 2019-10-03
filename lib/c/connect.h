#include "_/string.h"

typedef struct {
  string protocol;
  string address;
  string ns;
  string filter;
} connect_in;

typedef struct {
  string id;
  int tokens;
} connect_out;
