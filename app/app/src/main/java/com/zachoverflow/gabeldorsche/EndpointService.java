package com.zachoverflow.gabeldorsche;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.UUID;

public class EndpointService extends Service {
    private static final String LOG_TAG = EndpointService.class.getSimpleName();

    private static final int ACTION_VIBE = 0;
    private static final int ACTION_SET_WIFI_ENABLED = 1;

    private static final byte OPCODE_VIBRATE = 1;
    private static final byte OPCODE_ENABLE_WIFI = 2;
    private static final byte OPCODE_DISABLE_WIFI = 3;

    private static final UUID GABELDORSCHE_UUID = UUID.fromString("9bd5b148-9249-542a-43be-d1b5f073f928");

    private static final int MAX_CONNECT_TRY = 3;
    private static final int CONNECT_RETRY_DELAY_MILLIS = 50;

    private static final int MAX_VIBE_SEND_ATTEMPTS = 2;

    private Binder binder = new LocalInterface();

    private BluetoothSocket socket;
    private Looper dispatchLooper;
    private DispatchHandler dispatchHandler;

    private LinkedList<Vibe> pendingVibes = new LinkedList<>();

    private NotificationOracle notificationOracle;
    private RecentNotifications recents = new RecentNotifications();

    public EndpointService() {
    }

    private final class DispatchHandler extends Handler {
        public DispatchHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case ACTION_VIBE:
                    pendingVibes.add((Vibe)message.obj);
                    submitPendingVibes();
                    break;
                case ACTION_SET_WIFI_ENABLED:
                    ByteBuffer buffer = ByteBuffer.allocate(1);
                    boolean enable = (boolean)message.obj;
                    buffer.put(enable ? OPCODE_ENABLE_WIFI : OPCODE_DISABLE_WIFI);

                    ensureSocketUp();
                    boolean sent = sendByteBuffer(buffer);
                    Log.i(LOG_TAG, "Setting wifi enabled: " + enable + " sent: " + sent);
                    break;
                default:
                    Log.e(LOG_TAG, "Unexpcted message: " + message.what);
                    break;
            }
        }

        private void ensureSocketUp() {
            if (socket != null)
                return;

            for (int i = 0; i < MAX_CONNECT_TRY; i++) {
                // TODO(zachoverflow): don't assume the device is the one with "edison" in it
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

                BluetoothDevice peer = null;
                for (BluetoothDevice device : adapter.getBondedDevices()) {
                    if (device.getName().contains("edison")) {
                        peer = device;
                        break;
                    }
                }

                adapter.cancelDiscovery();
                try {
                    socket = peer.createRfcommSocketToServiceRecord(GABELDORSCHE_UUID);
                    if (socket != null) {
                        // Woot! Found it, so connect and then we're done here
                        socket.connect();
                        break;
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error opening socket to peer: " + e.getMessage());
                }

                try {
                    Log.i(LOG_TAG, "Could not connect. Retrying in " + CONNECT_RETRY_DELAY_MILLIS + " milliseconds");
                    Thread.sleep(CONNECT_RETRY_DELAY_MILLIS);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, "Error sleeping to retry connection: " + e.getMessage());
                }
            }
        }

        private boolean sendByteBuffer(ByteBuffer buffer) {
            if (socket == null) {
                Log.e(LOG_TAG, "Can't send bytes because socket is not open.");
                return false;
            }

            try {
                OutputStream outStream = socket.getOutputStream();
                outStream.write(buffer.array(), 0, buffer.position());
                outStream.flush();

                return true;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error writing peer socket: " + e.getMessage());
                closeSocket();
            }

            return false;
        }

        private void submitPendingVibes() {
            for (int i = 0; i < MAX_VIBE_SEND_ATTEMPTS; i++) {
                ensureSocketUp();

                boolean success = true;
                while (pendingVibes.size() > 0) {
                    Vibe vibe = pendingVibes.element();

                    ByteBuffer buffer = ByteBuffer.allocate(vibe.getSerializedLength() + 1);
                    buffer.put(OPCODE_VIBRATE);
                    vibe.serializeTo(buffer);

                    if (!sendByteBuffer(buffer)) {
                        success = false;
                        break;
                    }

                    pendingVibes.remove();
                }

                if (success)
                    return;
            }
        }
    }

    @Override
    public void onCreate() {
        HandlerThread dispatchThread = new HandlerThread("dispatch thread");
        dispatchThread.start();

        dispatchLooper = dispatchThread.getLooper();
        dispatchHandler = new DispatchHandler(dispatchLooper);

        notificationOracle = new NotificationOracle(recents);
    }

    @Override
    public void onDestroy() {
        closeSocket();
        dispatchLooper.quit();
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Error closing socket: " + ex.getMessage());
        }

        socket = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public class LocalInterface extends Binder {
        public void sendVibe(Vibe vibe) {
            dispatchHandler.sendMessage(dispatchHandler.obtainMessage(ACTION_VIBE, vibe));
        }

        public void handleNotification(StatusBarNotification notification) {
            Vibe vibe = notificationOracle.generateVibeFor(notification);
            if (vibe != null)
                sendVibe(vibe);
        }

        public void setWifiEnabled(boolean value) {
            dispatchHandler.sendMessage((dispatchHandler.obtainMessage(ACTION_SET_WIFI_ENABLED, value)));
        }

        public void reloadNotificationConfig() {
            Log.i(LOG_TAG, "Reloading notification config...");
            notificationOracle = new NotificationOracle(recents);
        }

        public RecentNotifications getRecentNotifications() {
            return recents;
        }
    }
}
