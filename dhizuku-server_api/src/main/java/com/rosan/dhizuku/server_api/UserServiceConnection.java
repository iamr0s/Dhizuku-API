package com.rosan.dhizuku.server_api;

import android.content.Context;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import java.util.ArrayList;
import java.util.List;

public class UserServiceConnection {
    public static class UserServiceInfo {
        private final @NonNull IUserServiceManager mManager;

        private final @NonNull IBinder mService;

        public UserServiceInfo(@NonNull IUserServiceManager manager, @NonNull IBinder service) {
            mManager = manager;
            mService = service;
        }

        public @NonNull IUserServiceManager getManager() {
            return mManager;
        }

        public @NonNull IBinder getService() {
            return mService;
        }
    }

    private Context mContext;

    private int uid;

    private int pid;

    private final IBinder.DeathRecipient recipient = () -> UserServiceConnections.get(mContext).unbind(uid, pid);

    private IDhizukuUserServiceConnection mConnection;

    private final List<String> tokens = new ArrayList<>();

    public UserServiceConnection(Context context,
                                 int uid,
                                 int pid,
                                 IDhizukuUserServiceConnection connection) {
        mContext = context;
        this.uid = uid;
        this.pid = pid;
        mConnection = connection;
        linkConnection();
    }

    public void setConnection(IDhizukuUserServiceConnection connection) {
        try {
            mConnection.asBinder().unlinkToDeath(recipient, 0);
        } catch (Throwable ignored) {
        }
        mConnection = connection;
        linkConnection();
    }

    private void linkConnection() {
        try {
            mConnection.asBinder().linkToDeath(recipient, 0);
        } catch (Throwable ignored) {
        }
    }

    public void register(@NonNull DhizukuUserServiceArgs args) {
        synchronized (tokens) {
            String token = args.getComponentName().flattenToShortString();
            tokens.remove(token);
            tokens.add(token);
        }
    }

    public void unregister(@NonNull DhizukuUserServiceArgs args) {
        synchronized (tokens) {
            String token = args.getComponentName().flattenToShortString();
            tokens.remove(token);
        }
    }

    public boolean isRegister(String token) {
        return tokens.contains(token);
    }

    public void connected(DhizukuUserServiceArgs args,
                          UserServiceInfo info) {
        synchronized (tokens) {
            try {
                mConnection.connected(args.build(), info.getService());
            } catch (Throwable ignored) {
            }
        }
    }

    public void died(DhizukuUserServiceArgs args) {
        synchronized (tokens) {
            try {
                mConnection.died(args.build());
            } catch (Throwable ignored) {
            }
        }
    }
}
