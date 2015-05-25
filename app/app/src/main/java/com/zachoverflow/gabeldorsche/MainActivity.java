package com.zachoverflow.gabeldorsche;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private boolean bound;
    private EndpointService.LocalInterface endpointService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button testButton = (Button)findViewById(R.id.test_button);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!bound)
                    return;

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
        });

        final Button disableWifiButton = (Button)findViewById(R.id.disable_wifi_button);
        final Button enableWifiButton = (Button)findViewById(R.id.enable_wifi_button);
        View.OnClickListener wifiButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!bound)
                    return;

                endpointService.setWifiEnabled(view == enableWifiButton);
            }
        };

        disableWifiButton.setOnClickListener(wifiButtonListener);
        enableWifiButton.setOnClickListener(wifiButtonListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, EndpointService.class);
        startService(intent);
        bindService(intent, endpointServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (bound) {
            unbindService(endpointServiceConnection);
            bound = false;
        }
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
