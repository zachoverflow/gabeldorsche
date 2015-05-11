#include <mraa.h>
#include <stdio.h>
#include <string.h>

#include "vibe.h"

int main() {
  mraa_init();
  vibe_init();

  vibe_step_t front_left[] = {
    { .value = 0.5f, .duration_ms = 200 },
    { .value = 0.0f, .duration_ms = 200 },
    { .value = 0.5f, .duration_ms = 200 },
  };

  vibe_step_t front_right[] = {
    { .value = 0.0f, .duration_ms = 600 },
    { .value = 0.5f, .duration_ms = 200 },
  };

  vibe_t vibe;
  memset(&vibe, 0, sizeof(vibe_t));
  vibe.by_location[FRONT_LEFT].steps = front_left;
  vibe.by_location[FRONT_LEFT].step_count = sizeof(front_left) / sizeof(vibe_step_t);
  vibe.by_location[FRONT_RIGHT].steps = front_right;
  vibe.by_location[FRONT_RIGHT].step_count = sizeof(front_right) / sizeof(vibe_step_t);

  vibe_do(&vibe);

  vibe_close();
  return 0;
}
