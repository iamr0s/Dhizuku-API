package com.rosan.dhizuku.api;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

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
            try {
                service.linkToDeath(() -> died(bundle), 0);
            } catch (RemoteException ignored) {
            }
        }

        void onServiceConnected(Bundle bundle, IBinder service) {
            DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(bundle);
            ComponentName name = args.getComponentName();
            String token = name.flattenToString();
            services.put(token, service);
            DhizukuServiceConnection serviceConnection = map.get(token);
            if (serviceConnection == null) return;
            serviceConnection.onServiceConnected(name, service);
        }

        @Override
        public void died(Bundle bundle) {
            DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(bundle);
            ComponentName name = args.getComponentName();
            String token = name.flattenToString();
            services.remove(token);
            DhizukuServiceConnection serviceConnection = map.get(token);
            if (serviceConnection == null) return;
            serviceConnection.onServiceDisconnected(name);
        }
    };

    private static final Map<String, DhizukuServiceConnection> map = new HashMap<>();

    private static final Map<String, IBinder> services = new HashMap<>();

    static void start(@NonNull IDhizuku dhizuku, @NonNull DhizukuUserServiceArgs args) throws RemoteException {
        ComponentName name = args.getComponentName();
        String token = name.flattenToString();
        IBinder service = services.get(token);
        if (service == null) dhizuku.bindUserService(iDhizukuUserServiceConnection, args.build());
    }

    static void stop(@NonNull IDhizuku dhizuku, @NonNull DhizukuUserServiceArgs args) throws RemoteException {
        dhizuku.unbindUserService(args.build());
    }

    static void bind(@NonNull IDhizuku dhizuku, @NonNull DhizukuUserServiceArgs args, @NonNull ServiceConnection connection) throws RemoteException {
        ComponentName name = args.getComponentName();
        String token = name.flattenToString();
        DhizukuServiceConnection serviceConnection = map.get(token);
        if (serviceConnection == null) {
            serviceConnection = new DhizukuServiceConnection();
            map.put(token, serviceConnection);
        }
        serviceConnection.add(connection);
        IBinder service = services.get(token);
        if (service == null) dhizuku.bindUserService(iDhizukuUserServiceConnection, args.build());
        else connection.onServiceConnected(name, service);
    }

    static void unbind(@NonNull IDhizuku dhizuku, @NonNull ServiceConnection connection) throws RemoteException {
        List<String> tokens = new ArrayList<>();
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
            ComponentName name = ComponentName.unflattenFromString(token);
            DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(name);
            stop(dhizuku, args);
        }
    }
}
