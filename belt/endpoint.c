#include <errno.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>
#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>
#include <stdbool.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>

#include "common.h"
#include "endpoint.h"
#include "log.h"
#include "stream_reader.h"
#include "vibe.h"

#define BAIL_IF_FAIL(expression) {if (!expression) return;}

static const uint32_t GABELDORSCHE_UUID[] = { 0x48b1d59b, 0x2a544992, 0xb5d1be43, 0x28f973f0 };
static const char *SERVICE_NAME = "Gabeldorsche";
static const char *SERVICE_DESCRIPTION = "Spatial vibration as a service";
static const char *SERVICE_PROVIDER = "Gabeldorsche";

static void on_connected(stream_reader_t *reader);
static void handle_vibrate(stream_reader_t *reader);
static int register_server_socket(struct sockaddr_rc *address);
static int dynamic_bind(int sock, struct sockaddr_rc *address);
static sdp_session_t *register_with_sdp(uint8_t rfcomm_channel);

void endpoint_run_blocking(void) {
  struct sockaddr_rc local_addr;
  int server = register_server_socket(&local_addr);
  if (server == INVALID_FD)
    return;

  sdp_session_t *sdp_session = register_with_sdp(local_addr.rc_channel);
  if (!sdp_session)
    return;

  bool running = true;
  while (running) {
    LOG_INFO("%s: waiting for accept", __func__);

    struct sockaddr_rc remote_addr = { 0 };
    socklen_t opt = sizeof(remote_addr);
    int client = accept(server, (struct sockaddr *)&remote_addr, &opt);
    if (client < 0) {
      running = false;
      continue;
    }

    char buffer[1024] = { 0 };
    ba2str(&remote_addr.rc_bdaddr, buffer);
    LOG_INFO("%s: accepted connection from %s", __func__, buffer);

    stream_reader_t *reader = stream_reader_new(client, &running);
    on_connected(reader);
    stream_reader_free(reader);

    close(client);
    LOG_INFO("%s: connection closed", __func__);
  }

  LOG_INFO("%s: closing server", __func__);
  sdp_close(sdp_session);
  close(server);
}

// Handles a connection to a peer device
static void on_connected(stream_reader_t *reader) {
  while (true) {
    uint8_t opcode;
    BAIL_IF_FAIL(stream_reader_read_uint8(reader, &opcode));

    switch (opcode) {
      case OPCODE_VIBRATE:
        handle_vibrate(reader);
        break;
      case OPCODE_ENABLE_WIFI:
        LOG_INFO("%s enabling wifi", __func__);
        if (system("rfkill unblock wifi") == -1) {
          LOG_INFO("%s enabling wifi failed", __func__);
        }

        if (system("systemctl start wpa_supplicant.service") == -1) {
          LOG_INFO("%s starting wpa_supplicant failed", __func__);
        }

        break;
      case OPCODE_DISABLE_WIFI:
        LOG_INFO("%s disabling wifi", __func__);
        if (system("rfkill block wifi") == -1) {
          LOG_INFO("%s disabling wifi failed", __func__);
        }
        break;
      default:
        LOG_ERROR("%s unknown opcode: %d", __func__, opcode);
        break;
    }
  }
}

// Handle a vibration command from the peer device
static void handle_vibrate(stream_reader_t *reader) {
  LOG_INFO("%s", __func__);

  vibe_t vibe;
  for (int loc = 0; loc < VIBE_LOCATIONS_COUNT; loc++) {
    vibe_sequence_t *sequence = &vibe.by_location[loc];

    sequence->steps = NULL;
    BAIL_IF_FAIL(stream_reader_read_uint16(reader, &sequence->step_count));
    if (sequence->step_count > 0)
      sequence->steps = calloc(sequence->step_count, sizeof(vibe_step_t));

    for (int i = 0; i < sequence->step_count; i++) {
      BAIL_IF_FAIL(stream_reader_read_float(reader, &sequence->steps[i].value));
      BAIL_IF_FAIL(stream_reader_read_uint16(reader, &sequence->steps[i].duration_ms));
    }
  }

  vibe_do(&vibe);

  for (int loc = 0; loc < VIBE_LOCATIONS_COUNT; loc++) {
    free(vibe.by_location[loc].steps);
  }
}

// Register a server socket, and populate |address| with the values used.
static int register_server_socket(struct sockaddr_rc *address) {
  address->rc_family = AF_BLUETOOTH;
  address->rc_bdaddr = *BDADDR_ANY;
  address->rc_channel = 0;

  int server = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
  if (server == INVALID_FD) {
    LOG_ERROR("%s: unable to open socket", __func__);
    return INVALID_FD;
  }

  if (dynamic_bind(server, address) == -1) {
    LOG_ERROR("%s: unable to bind socket", __func__);
    return INVALID_FD;
  }

  LOG_INFO("%s: listening on %d", __func__, address->rc_channel);
  listen(server, 1);
  return server;
}

// Bind the specified socket |sock| with |address|, on the first
// available port. |addr->rc_channel| will be set to the port used.
static int dynamic_bind(int sock, struct sockaddr_rc *address) {
  for (int port = 1; port < 31; port++) {
    address->rc_channel = port;
    int error = bind(sock, (struct sockaddr *)address, sizeof(struct sockaddr_rc));
    if (!error || errno == EINVAL)
      return error;
  }

  errno = EINVAL;
  return -1;
}

// Register the gabeldorshe service with sdp, on the specified
// |rfcomm_channel|
static sdp_session_t *register_with_sdp(uint8_t rfcomm_channel) {
  sdp_record_t *record = sdp_record_alloc();

  uuid_t service_uuid;
  sdp_uuid128_create(&service_uuid, GABELDORSCHE_UUID);
  sdp_set_service_id(record, service_uuid);

  uuid_t root_uuid;
  sdp_uuid16_create(&root_uuid, PUBLIC_BROWSE_GROUP);
  sdp_list_t *root_list = sdp_list_append(NULL, &root_uuid);
  sdp_set_browse_groups(record, root_list);

  uuid_t l2cap_uuid;
  sdp_uuid16_create(&l2cap_uuid, L2CAP_UUID);
  sdp_list_t *l2cap_list = sdp_list_append(NULL, &l2cap_uuid);
  sdp_list_t *protocol_list = sdp_list_append(NULL, l2cap_list);

  uuid_t rfcomm_uuid;
  sdp_uuid16_create(&rfcomm_uuid, RFCOMM_UUID);
  sdp_data_t *channel = sdp_data_alloc(SDP_UINT8, &rfcomm_channel);
  sdp_list_t *rfcomm_list = sdp_list_append(NULL, &rfcomm_uuid);
  sdp_list_append(rfcomm_list, channel);
  sdp_list_append(protocol_list, rfcomm_list);

  sdp_list_t *access_proto_list = sdp_list_append(NULL, protocol_list);
  sdp_set_access_protos(record, access_proto_list);

  sdp_set_info_attr(record, SERVICE_NAME, SERVICE_PROVIDER, SERVICE_DESCRIPTION);

  sdp_session_t *session = sdp_connect(BDADDR_ANY, BDADDR_LOCAL, SDP_RETRY_IF_BUSY);
  if (!session) {
    LOG_ERROR("%s: unable to connect to sdp - %s", __func__, strerror(errno));
    return NULL;
  }

  sdp_record_register(session, record, 0);

  sdp_list_free(access_proto_list, NULL);
  sdp_list_free(protocol_list, NULL);
  sdp_list_free(rfcomm_list, NULL);
  sdp_data_free(channel);
  sdp_list_free(l2cap_list, NULL);
  sdp_list_free(root_list, NULL);

  return session;
}
