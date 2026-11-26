package com.rosan.dhizuku.api;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.rosan.dhizuku.aidl.IDhizuku;
import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DhizukuServiceConnections {
    private static final String TAG = "DhizukuUserService";

    static final IDhizukuUserServiceConnection iDhizukuUserServiceConnection = new IDhizukuUserServiceConnection.Stub() {
        @Override
        public void connected(Bundle bundle, IBinder service) {
            onServiceConnected(bundle, service);
        }

        void onServiceConnected(Bundle bundle, IBinder service) {
            DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(bundle);
            ComponentName name = args.getComponentName();
            String token = name.flattenToString();
            DhizukuServiceConnection serviceConnection;
            boolean linkDeath;
            synchronized (map) {
                cancelTimeoutLocked(token);
                IBinder previous = services.put(token, service);
                serviceConnection = map.get(token);
                // connected() is invoked again for every new client connection,
                // only register a death recipient for a genuinely new binder
                linkDeath = previous != service;
            }
            if (linkDeath) {
                try {
                    service.linkToDeath(() -> died(bundle), 0);
                } catch (RemoteException ignored) {
                }
            }
            if (serviceConnection == null) return;
            serviceConnection.onServiceConnected(name, service);
        }

        @Override
        public void failed(Bundle bundle, int reason) {
            onServiceFailed(bundle, reason);
        }

        void onServiceFailed(Bundle bundle, int reason) {
            DhizukuUserServiceArgs args;
            try {
                args = new DhizukuUserServiceArgs(bundle);
            } catch (RuntimeException ignored) {
                return;
            }
            ComponentName name = args.getComponentName();
            if (name == null) return;
            String token = name.flattenToString();
            DhizukuServiceConnection serviceConnection;
            synchronized (map) {
                serviceConnection = map.get(token);
            }
            if (serviceConnection == null) return;
            serviceConnection.onServiceFailed(name, reason, null);
        }

        @Override
        public void died(Bundle bundle) {
            DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(bundle);
            ComponentName name = args.getComponentName();
            String token = name.flattenToString();
            DhizukuServiceConnection serviceConnection;
            synchronized (map) {
                cancelTimeoutLocked(token);
                services.remove(token);
                serviceConnection = map.get(token);
            }
            if (serviceConnection == null) return;
            serviceConnection.onServiceFailed(name, DhizukuUserServiceError.DIED, null);
            serviceConnection.onServiceDisconnected(name);
        }
    };

    private static final Map<String, DhizukuServiceConnection> map = new HashMap<>();

    private static final Map<String, IBinder> services = new HashMap<>();

    private static final Map<String, Runnable> pendingTimeouts = new HashMap<>();

    private static final Handler handler = new Handler(Looper.getMainLooper());

    static final long DEFAULT_BIND_TIMEOUT_MS = 15_000;

    private static void cancelTimeoutLocked(String token) {
        Runnable timeout = pendingTimeouts.remove(token);
        if (timeout != null) handler.removeCallbacks(timeout);
    }

    private static void scheduleTimeoutLocked(String token, ComponentName name, long timeoutMs) {
        cancelTimeoutLocked(token);
        Runnable timeout = () -> {
            DhizukuServiceConnection serviceConnection;
            synchronized (map) {
                if (pendingTimeouts.remove(token) == null) return;
                serviceConnection = map.get(token);
                if (serviceConnection == null) return;
                if (services.get(token) != null) return;
            }
            serviceConnection.onServiceFailed(name, DhizukuUserServiceError.TIMEOUT, null);
        };
        pendingTimeouts.put(token, timeout);
        handler.postDelayed(timeout, timeoutMs);
    }

    /**
     * @return the cached service binder, or null when it is absent or dead
     * (e.g. the Dhizuku server app restarted).
     */
    private static IBinder aliveService(String token) {
        synchronized (map) {
            IBinder service = services.get(token);
            if (service != null && !service.isBinderAlive()) {
                services.remove(token);
                return null;
            }
            return service;
        }
    }

    /**
     * Forget every cached user service after the Dhizuku server binder died.
     */
    static void reset() {
        List<Map.Entry<String, DhizukuServiceConnection>> entries;
        synchronized (map) {
            entries = new ArrayList<>(map.entrySet());
            map.clear();
            services.clear();
            for (Runnable timeout : pendingTimeouts.values()) handler.removeCallbacks(timeout);
            pendingTimeouts.clear();
        }
        for (Map.Entry<String, DhizukuServiceConnection> entry : entries) {
            ComponentName name = ComponentName.unflattenFromString(entry.getKey());
            if (name == null) continue;
            entry.getValue().onServiceFailed(name, DhizukuUserServiceError.SERVER_DIED, null);
            entry.getValue().onServiceDisconnected(name);
        }
    }

    static void start(IDhizuku dhizuku, DhizukuUserServiceArgs args) throws RemoteException {
        ComponentName name = args.getComponentName();
        String token = name.flattenToString();
        if (aliveService(token) == null) dhizuku.bindUserService(iDhizukuUserServiceConnection, args.build());
    }

    static void stop(IDhizuku dhizuku, DhizukuUserServiceArgs args) throws RemoteException {
        ComponentName name = args.getComponentName();
        String token = name.flattenToString();
        synchronized (map) {
            cancelTimeoutLocked(token);
            map.remove(token);
            services.remove(token);
        }
        dhizuku.unbindUserService(args.build());
    }

    static void bind(IDhizuku dhizuku, DhizukuUserServiceArgs args, ServiceConnection connection, DhizukuUserServiceErrorListener errorListener, long timeoutMs) throws RemoteException {
        ComponentName name = args.getComponentName();
        String token = name.flattenToString();
        IBinder service;
        synchronized (map) {
            DhizukuServiceConnection serviceConnection = map.get(token);
            if (serviceConnection == null) {
                serviceConnection = new DhizukuServiceConnection();
                map.put(token, serviceConnection);
            }
            serviceConnection.add(connection, errorListener);
            service = services.get(token);
            if (service != null && !service.isBinderAlive()) {
                services.remove(token);
                service = null;
            }
            if (service == null) scheduleTimeoutLocked(token, name, timeoutMs);
        }
        if (service != null) {
            connection.onServiceConnected(name, service);
            return;
        }
        try {
            dhizuku.bindUserService(iDhizukuUserServiceConnection, args.build());
        } catch (RemoteException | RuntimeException e) {
            DhizukuServiceConnection serviceConnection;
            synchronized (map) {
                cancelTimeoutLocked(token);
                serviceConnection = map.get(token);
            }
            if (serviceConnection != null) {
                serviceConnection.onServiceFailed(name, DhizukuUserServiceError.REJECTED, e);
            }
            throw e;
        }
    }

    static void unbind(IDhizuku dhizuku, ServiceConnection connection, boolean releaseServer) throws RemoteException {
        List<String> tokens = new ArrayList<>();
        synchronized (map) {
            for (Map.Entry<String, DhizukuServiceConnection> entry : map.entrySet()) {
                DhizukuServiceConnection serviceConnection = entry.getValue();
                if (serviceConnection == null) continue;
                serviceConnection.remove(connection);
                if (serviceConnection.isEmpty()) tokens.add(entry.getKey());
            }
            for (String token : tokens) {
                cancelTimeoutLocked(token);
                map.remove(token);
                services.remove(token);
            }
        }
        if (!releaseServer) return;
        for (String token : tokens) {
            ComponentName name = ComponentName.unflattenFromString(token);
            if (name == null) continue;
            release(dhizuku, new DhizukuUserServiceArgs(name));
        }
    }

    /**
     * Detach a single connection on the server, so the service keeps running for the
     * other connections that still use the same component.
     */
    private static void release(IDhizuku dhizuku, DhizukuUserServiceArgs args) throws RemoteException {
        try {
            dhizuku.unbindUserServiceByConnection(iDhizukuUserServiceConnection, args.build());
        } catch (RuntimeException e) {
            Log.w(TAG, "server cannot unbind a single connection", e);
            dhizuku.unbindUserService(args.build());
        }
    }
}
