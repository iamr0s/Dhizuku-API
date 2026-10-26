package com.rosan.dhizuku.api;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class DhizukuServiceConnection {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<ServiceConnection> connections = new CopyOnWriteArrayList<>();

    public void onServiceConnected(ComponentName name, IBinder service) {
        handler.post(() -> {
            for (ServiceConnection connection : connections) {
                connection.onServiceConnected(name, service);
            }
        });
    }

    public void onServiceDisconnected(ComponentName name) {
        handler.post(() -> {
            for (ServiceConnection connection : connections) {
                connection.onServiceDisconnected(name);
            }
        });
    }

    void add(ServiceConnection connection) {
        if (!connections.contains(connection)) connections.add(connection);
    }

    void remove(ServiceConnection connection) {
        connections.remove(connection);
    }

    boolean isEmpty() {
        return connections.isEmpty();
    }
}
