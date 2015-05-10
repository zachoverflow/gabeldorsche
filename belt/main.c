#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#include "mraa.h"

typedef enum {
  FRONT_LEFT,
  FRONT_RIGHT,
  BACK_RIGHT,
  BACK_LEFT
} locations_t;

#define LOCATION_COUNT 4

static const int GPIO_IDS[] = {
  12,
  183,
  13,
  182
};

static mraa_gpio_context gpios[LOCATION_COUNT];

static void init_gpios() {
 for (int i = 0; i < LOCATION_COUNT; i++) {
    gpios[i] = mraa_gpio_init_raw(GPIO_IDS[i]);
    if (!gpios[i]) { 
      fprintf(stderr, "Error opening GPIO %d\n", GPIO_IDS[i]);
      exit(1);
    }

    mraa_gpio_dir(gpios[i], MRAA_GPIO_OUT);
  }
}

static void close_gpios() {
 for (int i = 0; i < LOCATION_COUNT; i++)
    mraa_gpio_close(gpios[i]);
}

int main() {
  mraa_init();

  init_gpios();

  struct timespec sleep_time;
  sleep_time.tv_sec = 0;
  sleep_time.tv_nsec = 300 * 1000000L;

  for (int i = 0; i < LOCATION_COUNT; i++) {
    mraa_gpio_write(gpios[i], 1);

    nanosleep(&sleep_time, NULL);

    mraa_gpio_write(gpios[i], 0);
  }

  close_gpios();
  return 0;
}
