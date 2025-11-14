package com.rosan.dhizuku.server_api;

import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UserServiceConnection {
    private final List<IDhizukuUserServiceConnection> connections = new ArrayList<>();

    private IUserServiceManager mManager = null;

    private IBinder mService = null;

    private final int mUid;

    private final int mPid;

    private final @NonNull DhizukuUserServiceArgs mArgs;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean startRequested = false;

    UserServiceConnection(int uid, int pid, @NonNull DhizukuUserServiceArgs args) {
        this.mUid = uid;
        this.mPid = pid;
        this.mArgs = args;
    }

    void connected(@NonNull IBinder service) {
        synchronized (connections) {
            for (IDhizukuUserServiceConnection connection : connections) {
                try {
                    connection.connected(this.mArgs.build(), service);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void start() {
        synchronized (connections) {
            this.startRequested = true;
            bringUpService();
        }
    }

    void stop() {
        synchronized (connections) {
            this.startRequested = false;
            asyncShutdownService();
        }
    }

    void bind(IDhizukuUserServiceConnection connection) {
        synchronized (connections) {
            try {
                // Auto unbind when the client dies
                connection.asBinder().linkToDeath(() -> unbind(connection), 0);
            } catch (RemoteException ignored) {
                unbind(connection);
                // bind failed, return directly
                return;
            }
            connections.add(connection);

            IBinder service = this.mService;
            if (service != null && service.pingBinder()) {
                // service is alive and is triggered directly
                try {
                    connection.connected(this.mArgs.build(), service);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return;
            }

            bringUpService();
        }
    }

    void unbind(IDhizukuUserServiceConnection connection) {
        synchronized (connections) {
            connections.remove(connection);
            checkShutdownService();
        }
    }

    void unbindAll() {
        synchronized (connections) {
            connections.clear();
            checkShutdownService();
        }
    }

    private void checkShutdownService() {
        synchronized (connections) {
            if (this.startRequested || !connections.isEmpty()) return;
            UserServiceConnections.unbind(this.mUid, this.mPid, this.mArgs);
            asyncShutdownService();
        }
    }

    private void bringUpService() {
        synchronized (connections) {
            IBinder service = this.mService;
            if (service != null && service.pingBinder()) {
                // service is alive and is triggered directly
                connected(service);
                return;
            }
            this.mManager = null;
            this.mService = null;
            asyncBringUpService();
        }
    }

    private void asyncBringUpService() {
        executor.submit(() -> {
            synchronized (connections) {
                IUserServiceManager manager = this.mManager;
                if (manager != null && manager.asBinder().pingBinder()) {
                    return;
                }
            }

            DhizukuProcess process = DhizukuProcess.get();
            IBinder binder = process.isolatedServiceBinder(new ComponentName(process.getContext(), UserService.class));
            if (binder == null) return;
            IUserServiceManager manager = IUserServiceManager.Stub.asInterface(binder);
            IBinder service = null;
            try {
                service = manager.startService(this.mArgs.getComponentName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (service == null) return;

            try {
                service.linkToDeath(() -> onServiceDeath(manager), 0);
            } catch (RemoteException e) {
                e.printStackTrace();
                return;
            }

            this.mManager = manager;
            this.mService = service;
            connected(service);
        });
    }

    private void onServiceDeath(IUserServiceManager manager) {
        synchronized (connections) {
            if (this.mManager != manager) return;
            this.mManager = null;
            this.mService = null;
            this.startRequested = false;
            connections.clear();
        }
    }

    private void asyncShutdownService() {
        executor.submit(() -> {
            IUserServiceManager manager = this.mManager;
            if (manager == null) return;
            // clean up first.
            onServiceDeath(mManager);

            try {
                manager.quit();
            } catch (RemoteException ignored) {
                // maybe throw, DeadObjectException
            }
        });
    }
}
