package com.zachoverflow.gabeldorsche;

import android.service.notification.StatusBarNotification;

import java.util.LinkedList;
import java.util.List;

public class RecentNotifications {
    private static final int MAX_HISTORY_COUNT = 50;

    private LinkedList<SingleInstance> list = new LinkedList<>();

    static class SingleInstance {
        private String extras;
        private String packageName;
        private boolean actedUpon;

        public static SingleInstance fromStatusBarNotification(
                StatusBarNotification notification,
                boolean actedUpon) {
            SingleInstance recent = new SingleInstance();
            recent.packageName = notification.getPackageName();
            recent.extras = notification.getNotification().extras.toString();
            recent.actedUpon = actedUpon;
            return recent;
        }

        public String getExtras() {
            return this.extras;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public boolean wasActedUpon() {
            return this.actedUpon;
        }
    }

    public void add(SingleInstance notification) {
        list.addFirst(notification);
        if (list.size() > MAX_HISTORY_COUNT)
            list.removeLast();
    }

    public void add(StatusBarNotification notification, boolean actedUpon) {
        add(SingleInstance.fromStatusBarNotification(notification, actedUpon));
    }

    public List<SingleInstance> getList() {
        return this.list;
    }
}
