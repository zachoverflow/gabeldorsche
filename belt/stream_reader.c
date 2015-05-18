#include <assert.h>
#include <arpa/inet.h>
#include <errno.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "common.h"
#include "log.h"
#include "stream_reader.h"

struct stream_reader_t {
  int fd;
  bool *uninterrupted;
  uint8_t buffer[1024];
  int index;
  size_t bytes_in_buffer;
};

static bool ensure_byte_count(stream_reader_t *reader, size_t count);

stream_reader_t *stream_reader_new(int fd, bool *uninterrupted) {
  assert(fd != INVALID_FD);

  stream_reader_t *reader = calloc(1, sizeof(stream_reader_t));
  reader->fd = fd;
  reader->uninterrupted = uninterrupted;
  return reader;
}

void stream_reader_free(stream_reader_t *reader) {
  free(reader);
}

bool stream_reader_read_uint8(stream_reader_t *reader, uint8_t *value) {
  assert(reader != NULL);

  if (!ensure_byte_count(reader, 1))
    return false;

  if (value)
    *value = reader->buffer[reader->index];

  reader->index++;
  return true;
}

bool stream_reader_read_uint16(stream_reader_t *reader, uint16_t *value) {
  assert(reader != NULL);

  if (!ensure_byte_count(reader, 2))
    return false;

  if (value)
    *value = ntohs(*(uint16_t *)&reader->buffer[reader->index]);

  reader->index += 2;
  return true;
}

bool stream_reader_read_float(stream_reader_t *reader, float *value) {
  assert(reader != NULL);

  if (!ensure_byte_count(reader, 4))
    return false;

  if (value) {
    uint32_t byte_value = ntohl(*(uint32_t *)&reader->buffer[reader->index]);
    *value = *(float *)&byte_value;
  }

  reader->index += 4;
  return true;
}

// Ensure we have at least |count| bytes in the buffer, filling it as
// necessary.
static bool ensure_byte_count(stream_reader_t *reader, size_t count) {
  size_t bytes_left = reader->bytes_in_buffer - reader->index;
  if (bytes_left >= count)
    return true;

  memcpy(reader->buffer, reader->buffer + reader->index, bytes_left);
  reader->bytes_in_buffer = bytes_left;
  reader->index = 0;

  while (reader->bytes_in_buffer < count) {
    int bytes_read = read(
        reader->fd, 
        reader->buffer + reader->bytes_in_buffer,
        sizeof(reader->buffer) - reader->bytes_in_buffer
    );

    if (bytes_read <= 0) {
      if (reader->uninterrupted)
        *reader->uninterrupted = bytes_read >= 0 || errno != EINTR;
      return false;
    }

    reader->bytes_in_buffer += bytes_read;
  }

  return true;
}
