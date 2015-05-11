#pragma once

#include <stdbool.h>
#include <stdint.h>

#define VIBE_LOCATIONS_COUNT 4
typedef enum {
  FRONT_LEFT,
  FRONT_RIGHT,
  BACK_RIGHT,
  BACK_LEFT
} vibe_locations_t;

typedef struct {
  float value;
  uint16_t duration_ms;
} vibe_step_t;

typedef struct {
  vibe_step_t *steps;
  uint16_t step_count;
} vibe_sequence_t;

typedef struct {
  vibe_sequence_t by_location[VIBE_LOCATIONS_COUNT];
} vibe_t;

// Prepare the vibe module
bool vibe_init(void);

// Clean up the vibe module
void vibe_close(void);

// Run the specific |vibe|. |vibe| may not be NULL, and |vibe_init| must
// have been called prior.
//
// Ignores sequences with NULL steps.
void vibe_do(vibe_t *vibe);
