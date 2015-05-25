package com.zachoverflow.gabeldorsche;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {
    private static final String LOG_TAG = "Gabeldorsche";
    private boolean bound;
    private EndpointService.LocalInterface endpointService;

    @Override
    public void onCreate() {
        super.onCreate();

        Intent intent = new Intent(this, EndpointService.class);
        startService(intent);
        bindService(intent, endpointServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (bound) {
            unbindService(endpointServiceConnection);
            bound = false;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        if (!bound) {
            Log.d(LOG_TAG, "got notification, but not bound to endpoint service :(");
            return;
        }

        Log.d(LOG_TAG, "got notification");

        Vibe vibe = new Vibe();

        vibe.at(Vibe.Location.FRONT_LEFT)
                .add(0.5f, (short)200)
                .add(0.0f, (short)200)
                .add(0.5f, (short)200);

        vibe.at(Vibe.Location.BACK_LEFT)
                .add(0.0f, (short)600)
                .add(0.5f, (short)200);

        endpointService.sendVibe(vibe);
    }

    private ServiceConnection endpointServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            endpointService = (EndpointService.LocalInterface) service;
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };
}
