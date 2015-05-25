package com.zachoverflow.gabeldorsche;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String LOG_TAG = "Gabeldorsche UI";

    private boolean bound;
    private EndpointService.LocalInterface endpointService;

    private EditText configEditor;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!bound)
            return super.onOptionsItemSelected(item);

        int id = item.getItemId();
        if (id == R.id.action_test) {
            Vibe vibe = new Vibe();

            vibe.at(Vibe.Location.FRONT_LEFT)
                    .add(0.5f, (short)200)
                    .add(0.0f, (short)200)
                    .add(0.5f, (short)200);

            vibe.at(Vibe.Location.BACK_LEFT)
                    .add(0.0f, (short)600)
                    .add(0.5f, (short)200);

            endpointService.sendVibe(vibe);
            return true;
        } else if (id == R.id.action_enable_wifi) {
            endpointService.setWifiEnabled(true);
        } else if (id == R.id.action_disable_wifi) {
            endpointService.setWifiEnabled(false);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configEditor = (EditText)findViewById(R.id.configFile);
        int currentFlags = configEditor.getInputType();
        configEditor.setInputType(currentFlags & (~InputType.TYPE_TEXT_FLAG_AUTO_CORRECT));
        readConfigFile();

        Button saveButton = (Button)findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfigFile();
            }
        });
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

    private void readConfigFile() {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new FileReader(NotificationOracle.CONFIG_FILE))) {

            char[] buffer = new char[1024];
            int charsRead;
            while ((charsRead = reader.read(buffer)) > 0)
                builder.append(buffer, 0, charsRead);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error reading config file into UI.");
        }

        configEditor.setText(builder.toString());
    }

    private void saveConfigFile() {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(NotificationOracle.CONFIG_FILE))) {
            writer.write(configEditor.getText().toString());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error writing config file to disk: " + e.getMessage());
        }

        this.endpointService.reloadNotificationConfig();
    }
}
