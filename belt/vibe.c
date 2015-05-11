#include <assert.h>
#include <mraa.h>
#include <time.h>

#include "log.h"
#include "time_helpers.h"
#include "vibe.h"

// MRAA pin ids (see http://iotdk.intel.com/docs/master/mraa/edison.html)
static const int PWM_IDS[] = {
  [FRONT_LEFT] = 20,  // GPIO 12
  [FRONT_RIGHT] = 21, // GPIO 183
  [BACK_RIGHT] = 14,  // GPIO 13
  [BACK_LEFT] = 0     // GPIO 182
};

static const int PWM_PERIOD_MS = 20;

static mraa_pwm_context pwm[VIBE_LOCATIONS_COUNT];
static bool initialized;

typedef struct {
  time_ms_t step_end;
  int step_index;
  vibe_sequence_t *sequence;
} sequence_state_t;

static void init_sequence_states(sequence_state_t *states, vibe_t *vibe, time_ms_t start_time);
static void update_pwm_values(sequence_state_t *states);
static void update_states(sequence_state_t *states, time_ms_t current_time);
static time_ms_t nearest_step_end(sequence_state_t *states);

bool vibe_init(void) {
  assert(!initialized);

  for (int i = 0; i < VIBE_LOCATIONS_COUNT; i++) {
    pwm[i] = mraa_pwm_init(PWM_IDS[i]);
    if (!pwm[i]) {
      LOG_ERROR("%s: error opening PWM %d", __func__, PWM_IDS[i]);
      return false;
    }

    mraa_pwm_period_ms(pwm[i], PWM_PERIOD_MS);
    mraa_pwm_enable(pwm[i], 1);
  }

  initialized = true;
  return true;
}

void vibe_close(void) {
  assert(initialized);

  for (int i = 0; i < VIBE_LOCATIONS_COUNT; i++)
    mraa_pwm_write(pwm[i], 0.0f);

  sleep_for_ms(100);

  for (int i = 0; i < VIBE_LOCATIONS_COUNT; i++)
    mraa_pwm_close(pwm[i]);

  initialized = false;
}

void vibe_do(vibe_t *vibe) {
  assert(initialized);
  assert(vibe != NULL);
  
  sequence_state_t states[VIBE_LOCATIONS_COUNT];

  time_ms_t current_time = get_current_ms();
  init_sequence_states(states, vibe, current_time);

  while (true) {
    update_pwm_values(states);
    
    time_ms_t next_wakeup = nearest_step_end(states);
    if (next_wakeup == -1)
      break;

    LOG_VERBOSE("%s: sleeping for %ld ms", __func__, next_wakeup - current_time);
    sleep_for_ms(next_wakeup - current_time);
    current_time = get_current_ms();
    update_states(states, current_time);
  }
}

static void init_sequence_states(sequence_state_t *states, vibe_t *vibe, time_ms_t start_time) {
  for (int i = 0; i < VIBE_LOCATIONS_COUNT; i++) {
    sequence_state_t *state = &states[i];

    state->step_index = 0;
    state->sequence = &vibe->by_location[i];

    if (!state->sequence->steps)
      continue;

    state->step_end = start_time + state->sequence->steps[0].duration_ms;
  }
}

// Update the PWM pins with the values for the current step, or 0.0f if
// the the sequence for the particular pin does not exist or has finished.
static void update_pwm_values(sequence_state_t *states) {
  for (int i = 0; i < VIBE_LOCATIONS_COUNT; i++) {
    sequence_state_t *state = &states[i];

    float value = 0.0f;
    if (state->sequence->steps && state->step_index < state->sequence->step_count)
      value = state->sequence->steps[state->step_index].value;

    LOG_VERBOSE("%s: writing %f to PWM%d", __func__, value, i);
    mraa_pwm_write(pwm[i], value);
  }
}

// Update |states| according to |current_time|, incrementing step indexes as needed.
// Fast-forwards to the step that expires next in time, if necessary.
static void update_states(sequence_state_t *states, time_ms_t current_time) {
  for (int i = 0; i < VIBE_LOCATIONS_COUNT; i++) {
    sequence_state_t *state = &states[i];
    if (!state->sequence->steps)
      continue;

    while (state->step_index < state->sequence->step_count && state->step_end < current_time) {
      state->step_index++;

      if (state->step_index < state->sequence->step_count)
        state->step_end += state->sequence->steps[state->step_index].duration_ms;
    }
  }
}

// Find the closest step end timestamp, ignoring sequences that have ended
// or have no steps. Returns -1 when all sequences are over.
static time_ms_t nearest_step_end(sequence_state_t *states) {
  time_ms_t nearest = -1;

  for (int i = 0; i < VIBE_LOCATIONS_COUNT; i++) {
    sequence_state_t *state = &states[i];
    if (!state->sequence->steps || state->step_index >= state->sequence->step_count)
      continue;

    if (nearest == -1 || state->step_end < nearest)
        nearest = state->step_end;
  }

  return nearest;
}
