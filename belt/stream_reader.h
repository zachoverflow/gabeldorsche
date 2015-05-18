#pragma once

#include <stdbool.h>
#include <stdint.h>

typedef struct stream_reader_t stream_reader_t;

// Creates a new stream reader for |fd|. If |uninterrupted| is not NULL,
// interruptions while reading will be reported by setting
// |*uninterrupted| to false. |fd| must be a valid file descriptor.
stream_reader_t *stream_reader_new(int fd, bool *uninterrupted);

// Free a previously created stream reader. |reader| may be NULL.
void stream_reader_free(stream_reader_t *reader);

// Read a uint8 from the stream. |reader| may not be NULL.
bool stream_reader_read_uint8(stream_reader_t *reader, uint8_t *value);

// Read a uint16 from the stream. |reader| may not be NULL.
bool stream_reader_read_uint16(stream_reader_t *reader, uint16_t *value);

// Read a float from the stream. |reader| may not be NULL.
bool stream_reader_read_float(stream_reader_t *reader, float *value);
