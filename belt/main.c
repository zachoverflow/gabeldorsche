#include <mraa.h>
#include <signal.h>

#include "endpoint.h"
#include "log.h"
#include "vibe.h"

void sigint_handler(int param) {
  // Nothing to do, endpoint handles EINTR appropriately
}

int main() {
  struct sigaction action;
  action.sa_handler = sigint_handler;
  action.sa_flags = 0;
  sigemptyset(&action.sa_mask);
  sigaction(SIGINT, &action, NULL);

  mraa_init();
  vibe_init();
  endpoint_run_blocking();
  vibe_close();

  return 0;
}
