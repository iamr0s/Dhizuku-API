package com.rosan.dhizuku.api;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

class DhizukuServiceConnection {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<ServiceConnection> connections = new CopyOnWriteArrayList<>();

    private final Map<ServiceConnection, DhizukuUserServiceErrorListener> errorListeners = new LinkedHashMap<>();

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

    void add(ServiceConnection connection, DhizukuUserServiceErrorListener errorListener) {
        if (!connections.contains(connection)) connections.add(connection);
        if (errorListener != null) {
            synchronized (errorListeners) {
                errorListeners.put(connection, errorListener);
            }
        }
    }

    void remove(ServiceConnection connection) {
        connections.remove(connection);
        synchronized (errorListeners) {
            errorListeners.remove(connection);
        }
    }

    boolean isEmpty() {
        return connections.isEmpty();
    }

    /**
     * Report a failure to every connection registered for this service.
     */
    void onServiceFailed(final ComponentName name, final int reason, final Throwable cause) {
        List<DhizukuUserServiceErrorListener> listeners;
        synchronized (errorListeners) {
            listeners = new ArrayList<>(errorListeners.values());
        }
        if (listeners.isEmpty()) return;
        handler.post(() -> {
            for (DhizukuUserServiceErrorListener listener : listeners) {
                try {
                    listener.onUserServiceFailed(name, reason, cause);
                } catch (RuntimeException ignored) {
                }
            }
        });
    }
}
