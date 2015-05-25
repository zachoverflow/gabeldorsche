package com.zachoverflow.gabeldorsche;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class RecentNotificationsActivity extends Activity {
    private boolean bound;
    private EndpointService.LocalInterface endpointService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_notifications);

        Intent intent = new Intent(this, EndpointService.class);
        bindService(intent, endpointServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bound) {
            unbindService(endpointServiceConnection);
            bound = false;
        }
    }

    private void setListContents() {
        final List<RecentNotifications.SingleInstance> list = endpointService.getRecentNotifications().getList();
        ListView listView = (ListView)findViewById(R.id.notification_list);
        listView.setAdapter(new ArrayAdapter<RecentNotifications.SingleInstance>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                list) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                RecentNotifications.SingleInstance notification = list.get(position);
                String title = notification.getPackageName();
                if (!notification.wasActedUpon())
                    title += " (was ignored)";

                text1.setText(title);
                text2.setText(notification.getExtras());
                return view;
            }
        });
    }

    private ServiceConnection endpointServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            endpointService = (EndpointService.LocalInterface) service;
            bound = true;
            setListContents();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };
}
