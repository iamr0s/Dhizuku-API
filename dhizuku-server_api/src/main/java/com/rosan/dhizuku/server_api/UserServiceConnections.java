package com.rosan.dhizuku.server_api;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserServiceConnections {
    @SuppressLint("StaticFieldLeak")
    private static UserServiceConnections mInstance;

    public static UserServiceConnections get(Context context) {
        if (mInstance != null) return mInstance;
        mInstance = new UserServiceConnections(context);
        return mInstance;
    }

    private final Context mContext;

    private final Map<String, UserServiceConnection> connectionMap = new HashMap<>();

    private final Map<String, UserServiceConnection.UserServiceInfo> infoMap = new HashMap<>();

    public UserServiceConnections(Context context) {
        mContext = context;
    }

    private void onServiceConnected(DhizukuUserServiceArgs args, UserServiceConnection.UserServiceInfo info) {
        for (UserServiceConnection value : connectionMap.values()) {
            value.connected(args, info);
        }
        infoMap.put(args.flattenToShortString(), info);
        try {
            info.getService().linkToDeath(() -> onServiceDisconnected(args), 0);
        } catch (Throwable ignored) {
        }
    }

    private void onServiceDisconnected(DhizukuUserServiceArgs args) {
        for (UserServiceConnection value : connectionMap.values()) {
            value.died(args);
            infoMap.remove(args.getComponentName().flattenToShortString());
        }
    }

    private void start(@NonNull DhizukuUserServiceArgs args) {
        String token = args.getComponentName().flattenToShortString();
        UserServiceConnection.UserServiceInfo service = infoMap.get(token);
        if (service != null) return;
        startInner(args);
    }

    private void startInner(DhizukuUserServiceArgs args) {
        ComponentName component = new ComponentName(mContext, UserService.class);
        IBinder binder = DhizukuProcess.get(mContext).isolatedServiceBinder(component, false);
        if (binder == null) return;
        IUserServiceManager manager = IUserServiceManager.Stub.asInterface(binder);
        try {
            UserServiceConnection.UserServiceInfo info = new UserServiceConnection.UserServiceInfo(manager, manager.startService(args.getComponentName()));
            onServiceConnected(args, info);
        } catch (RemoteException ignored) {
        }
    }

    /** @noinspection unused*/
    public void stop(@NonNull DhizukuUserServiceArgs args) {
        String token = args.getComponentName().flattenToShortString();
        stop(token);
    }

    private void stop(String token) {
        UserServiceConnection.UserServiceInfo info = infoMap.get(token);
        if (info == null) return;
        try {
            info.getManager().quit();
        } catch (Throwable ignored) {
        }
    }

    public void bind(int uid, int pid, @NonNull DhizukuUserServiceArgs
            args, IDhizukuUserServiceConnection remote) {
        String owner = uid + ":" + pid;
        String token = args.getComponentName().flattenToShortString();
        UserServiceConnection connection = connectionMap.get(owner);
        if (connection == null) {
            connection = new UserServiceConnection(mContext, uid, pid, remote);
            connectionMap.put(owner, connection);
        }
        connection.setConnection(remote);
        connection.register(args);
        UserServiceConnection.UserServiceInfo info = infoMap.get(token);
        if (info == null) start(args);
        else connection.connected(args, info);
    }

    public void unbind(int uid, int pid, DhizukuUserServiceArgs args) {
        String owner = uid + ":" + pid;

        UserServiceConnection connection = connectionMap.get(owner);
        if (connection == null) return;
        connection.unregister(args);
        afterUnbind();
    }

    public void unbind(int uid, int pid) {
        String owner = uid + ":" + pid;
        connectionMap.remove(owner);
        afterUnbind();
    }

    private void afterUnbind() {
        List<String> tokens = new ArrayList<>(infoMap.keySet());
        for (String token : infoMap.keySet()) {
            for (UserServiceConnection connection : connectionMap.values()) {
                if (connection.isRegister(token)) {
                    tokens.remove(token);
                    break;
                }
            }
        }
        for (String token : tokens) {
            stop(token);
        }
    }
}
