package com.rosan.dhizuku.api;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizuku;
import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DhizukuServiceConnections {
    static final IDhizukuUserServiceConnection iDhizukuUserServiceConnection = new IDhizukuUserServiceConnection.Stub() {
        /** @noinspection unused*/
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
        public void died(Bundle bundle) {
            DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(bundle);
            ComponentName name = args.getComponentName();
            String token = name.flattenToString();
            DhizukuServiceConnection serviceConnection;
            synchronized (map) {
                services.remove(token);
                serviceConnection = map.get(token);
            }
            if (serviceConnection == null) return;
            serviceConnection.onServiceDisconnected(name);
        }
    };

    private static final Map<String, DhizukuServiceConnection> map = new HashMap<>();

    private static final Map<String, IBinder> services = new HashMap<>();

    /**
     * @return the cached service binder, or null when it is absent or dead
     * (e.g. the Dhizuku server app restarted).
     */
    private static IBinder aliveService(String token) {
        synchronized (map) {
            IBinder service = services.get(token);
            if (service != null && !service.pingBinder()) {
                services.remove(token);
                return null;
            }
            return service;
        }
    }

    static void start(IDhizuku dhizuku, DhizukuUserServiceArgs args) throws RemoteException {
        ComponentName name = args.getComponentName();
        String token = name.flattenToString();
        if (aliveService(token) == null) dhizuku.bindUserService(iDhizukuUserServiceConnection, args.build());
    }

    static void stop(IDhizuku dhizuku, DhizukuUserServiceArgs args) throws RemoteException {
        dhizuku.unbindUserService(args.build());
    }

    static void bind(IDhizuku dhizuku, DhizukuUserServiceArgs args, ServiceConnection connection) throws RemoteException {
        ComponentName name = args.getComponentName();
        String token = name.flattenToString();
        IBinder service;
        synchronized (map) {
            DhizukuServiceConnection serviceConnection = map.get(token);
            if (serviceConnection == null) {
                serviceConnection = new DhizukuServiceConnection();
                map.put(token, serviceConnection);
            }
            serviceConnection.add(connection);
            service = services.get(token);
            if (service != null && !service.pingBinder()) {
                services.remove(token);
                service = null;
            }
        }
        if (service == null) dhizuku.bindUserService(iDhizukuUserServiceConnection, args.build());
        else connection.onServiceConnected(name, service);
    }

    static void unbind(IDhizuku dhizuku, ServiceConnection connection) throws RemoteException {
        List<String> tokens = new ArrayList<>();
        synchronized (map) {
            for (Map.Entry<String, DhizukuServiceConnection> entry : map.entrySet()) {
                String token = entry.getKey();
                DhizukuServiceConnection serviceConnection = entry.getValue();
                if (serviceConnection == null) {
                    tokens.add(token);
                    continue;
                }
                serviceConnection.remove(connection);
                if (serviceConnection.isEmpty()) tokens.add(token);
            }
            for (String token : tokens) {
                map.remove(token);
            }
        }
        for (String token : tokens) {
            synchronized (map) {
                map.remove(token);
                services.remove(token);
            }
            ComponentName name = ComponentName.unflattenFromString(token);
            DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(name);
            stop(dhizuku, args);
        }
    }
}
