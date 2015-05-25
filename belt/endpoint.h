#pragma once

// Over the air command format:
// uint8_t opcode
// [remaining data, depending on opcode]
//
// Everything is in network byte order

// Over the air commands:
#define OPCODE_VIBRATE 1
// for each motor: (0-3)
//   uint16_t step_count
//   for each step:
//     float value
//     uint16_t duration_ms
// 
// Again, everything is in network byte order

#define OPCODE_ENABLE_WIFI 2
#define OPCODE_DISABLE_WIFI 3

// Run the bluetooth service endpoint, blocking the calling thread
void endpoint_run_blocking(void);
