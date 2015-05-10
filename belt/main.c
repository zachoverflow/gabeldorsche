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

static const int GPIO_RAW_IDS[] = {
  12,
  183,
  13,
  182
};

// MRAA pin ids (see http://iotdk.intel.com/docs/master/mraa/edison.html)
static const int PWM_IDS[] = {
  20,
  21,
  14,
  0
};

static mraa_pwm_context pwm[LOCATION_COUNT];

static void init_pwms() {
 for (int i = 0; i < LOCATION_COUNT; i++) {
    pwm[i] = mraa_pwm_init(PWM_IDS[i]);
    if (!pwm[i]) { 
      fprintf(stderr, "Error opening PWM %d\n", PWM_IDS[i]);
      exit(1);
    }

    mraa_pwm_period_ms(pwm[i], 20);
    mraa_pwm_enable(pwm[i], 1);
  }
}

static void close_pwms() {
 for (int i = 0; i < LOCATION_COUNT; i++)
    mraa_pwm_close(pwm[i]);
}

int main() {
  mraa_init();
  init_pwms();

  struct timespec sleep_time;
  sleep_time.tv_sec = 0;
  sleep_time.tv_nsec = 300 * 1000000L;

  for (int i = 0; i < LOCATION_COUNT; i++) {
    mraa_pwm_write(pwm[i], 0.5f);
    nanosleep(&sleep_time, NULL);
    mraa_pwm_write(pwm[i], 0.0f);
  }

  close_pwms();
  return 0;
}
