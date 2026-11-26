package com.rosan.dhizuku.server_api;

import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.rosan.dhizuku.api.DhizukuUserServiceError;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UserServiceConnection {

    private static final String TAG = "DhizukuUserService";

    /**
     * The first client api version that understands the failure callback.
     */
    static final int MIN_CLIENT_VERSION_FAILED = 2;

    private final int mClientVersion;

    private final List<IDhizukuUserServiceConnection> connections = new ArrayList<>();

    private IUserServiceManager mManager = null;

    private IBinder mService = null;

    private final int mUid;

    private final int mPid;

    private final DhizukuUserServiceArgs mArgs;

    private ExecutorService executor = newExecutor();

    private static ExecutorService newExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "DhizukuUserService");
            thread.setDaemon(true);
            return thread;
        });
    }

    private ExecutorService executor() {
        synchronized (connections) {
            if (executor == null || executor.isShutdown()) executor = newExecutor();
            return executor;
        }
    }

    private boolean startRequested = false;

    UserServiceConnection(int uid, int pid, int clientVersion, DhizukuUserServiceArgs args) {
        this.mUid = uid;
        this.mPid = pid;
        this.mClientVersion = clientVersion;
        this.mArgs = args;
    }

    void connected(IBinder service) {
        synchronized (connections) {
            for (IDhizukuUserServiceConnection connection : connections) {
                try {
                    connection.connected(this.mArgs.build(), service);
                } catch (RemoteException e) {
                    Log.e(TAG, "failed to notify the connection", e);
                }
            }
        }
    }

    /**
     * Report a start failure. Clients that predate the failure callback are skipped, they
     * roll their own timeout instead.
     */
    void failed(int reason) {
        synchronized (connections) {
            if (this.mClientVersion < MIN_CLIENT_VERSION_FAILED) return;
            for (IDhizukuUserServiceConnection connection : connections) {
                try {
                    connection.failed(this.mArgs.build(), reason);
                } catch (RemoteException e) {
                    Log.e(TAG, "failed to report the user service failure", e);
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
            if (service != null && service.isBinderAlive()) {
                // service is alive and is triggered directly
                try {
                    connection.connected(this.mArgs.build(), service);
                } catch (RemoteException e) {
                    Log.e(TAG, "failed to notify the connection", e);
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
        }
        UserServiceConnections.removeToken(this.mUid, this.mPid, this.mArgs);
        asyncShutdownService();
    }

    private void bringUpService() {
        synchronized (connections) {
            IBinder service = this.mService;
            if (service != null && service.isBinderAlive()) {
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
        executor().submit(() -> {
            synchronized (connections) {
                IUserServiceManager manager = this.mManager;
                if (manager != null && manager.asBinder().pingBinder()) {
                    return;
                }
            }

            DhizukuProcess process = DhizukuProcess.get();
            IBinder binder = process.isolatedServiceBinder(new ComponentName(process.getContext(), UserService.class));
            if (binder == null) {
                failed(DhizukuUserServiceError.START_FAILED);
                return;
            }
            IUserServiceManager manager = IUserServiceManager.Stub.asInterface(binder);
            IBinder service = null;
            try {
                service = manager.startService(this.mArgs.getComponentName());
            } catch (RemoteException e) {
                Log.e(TAG, "failed to start the user service", e);
            }
            if (service == null) {
                failed(DhizukuUserServiceError.START_FAILED);
                return;
            }

            try {
                service.linkToDeath(() -> onServiceDeath(manager), 0);
            } catch (RemoteException e) {
                Log.e(TAG, "failed to link the user service death recipient", e);
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
        IUserServiceManager manager;
        synchronized (connections) {
            manager = this.mManager;
            this.mManager = null;
            this.mService = null;
            this.startRequested = false;
        }
        ExecutorService serviceExecutor = executor();
        serviceExecutor.submit(() -> {
            if (manager != null) {
                try {
                    manager.quit();
                } catch (RemoteException ignored) {
                }
            }
            serviceExecutor.shutdown();
        });
    }
}
