#include <time.h>

#include "time_helpers.h"

time_ms_t get_current_ms() {
  struct timespec current_time;
  clock_gettime(CLOCK_MONOTONIC, &current_time);

  return (current_time.tv_sec * 1000) + (current_time.tv_nsec / 1000000L);
}

void sleep_for_ms(time_ms_t duration) {
  struct timespec sleep_time;
  sleep_time.tv_sec = duration / 1000;
  sleep_time.tv_nsec = (duration % 1000) * 1000000L;
  
  nanosleep(&sleep_time, NULL);
}
