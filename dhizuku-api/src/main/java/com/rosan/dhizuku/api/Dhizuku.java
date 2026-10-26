package com.rosan.dhizuku.api;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizuku;
import com.rosan.dhizuku.shared.DhizukuVariables;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Dhizuku {

    @SuppressLint("StaticFieldLeak")
    private static volatile Context mContext = null;

    private static volatile ComponentName mOwnerComponent;

    private static final DhizukuClient CLIENT = new DhizukuClient();

    private static volatile IDhizuku remote = null;

    // serializes init() so concurrent callers don't all hit the provider
    private static final Object INIT_LOCK = new Object();

    private static volatile int mServerVersion = -1;

    public static ComponentName getOwnerComponent(Context context) {
        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return getOwnerComponent(manager);
    }

    public static ComponentName getOwnerComponent(DevicePolicyManager manager) {
        ComponentName component = null;
        List<ComponentName> admins = manager.getActiveAdmins();
        if (admins == null)
            return component;
        for (ComponentName admin : admins) {
            String packageName = admin.getPackageName();
            if (manager.isDeviceOwnerApp(packageName)) {
                component = admin;
                break;
            }
            if (manager.isProfileOwnerApp(packageName)) component = admin;
        }
        return component;
    }

    /**
     * @see #init(Context)
     */
    public static boolean init() {
        if (mContext == null) mContext = ActivityThread.currentActivityThread().getApplication();
        return init(mContext);
    }

    /**
     * Request binder from Dhizuku.
     *
     * @param context Context of this application that support ContentProvider request.
     * @return If binder is received. (If not, maybe (Dhizuku not working / Dhizuku not active / Dhizuku not installed))
     */
    public static boolean init(Context context) {
        if (context == null) return false;
        synchronized (INIT_LOCK) {
            return initLocked(context);
        }
    }

    private static boolean initLocked(Context context) {
        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        IDhizuku server = remote;
        if (server != null && server.asBinder().isBinderAlive()) {
            ComponentName owner = mOwnerComponent;
            if (owner != null) {
                String currentOwnerPackageName = owner.getPackageName();
                if (manager.isDeviceOwnerApp(currentOwnerPackageName) || manager.isProfileOwnerApp(currentOwnerPackageName))
                    return true;
            }
        }

        ComponentName ownerComponent = getOwnerComponent(context);
        if (ownerComponent == null) return false;
        mOwnerComponent = ownerComponent;
        String packageName = ownerComponent.getPackageName();
        String authority = DhizukuVariables.getProviderAuthorityName(packageName);

        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).build();
        Bundle extras = new Bundle();
        extras.putBinder(DhizukuVariables.EXTRA_CLIENT, CLIENT.asBinder());
        Bundle bundle;
        try {
            bundle = context.getContentResolver().call(uri, DhizukuVariables.PROVIDER_METHOD_CLIENT, null, extras);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (bundle == null) return false;
        IBinder iBinder = bundle.getBinder(DhizukuVariables.PARAM_DHIZUKU_BINDER);
        if (iBinder == null) return false;
        DhizukuServiceConnections.reset();
        remote = IDhizuku.Stub.asInterface(iBinder);
        mServerVersion = -1;
        try {
            iBinder.linkToDeath(() -> {
                synchronized (INIT_LOCK) {
                    IDhizuku current = remote;
                    if (current == null || current.asBinder() != iBinder) return;
                    remote = null;
                    mServerVersion = -1;
                    DhizukuServiceConnections.reset();
                }
            }, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mContext = context;
        return true;
    }

    private static Context requireContext() {
        Context context = mContext;
        if (context == null) throw new IllegalStateException("Dhizuku.init(context) must be called first");
        return context;
    }

    private static IDhizuku requireServer() {
        IDhizuku server = remote;
        // pingBinder() is a sync PING_TRANSACTION; pinging a frozen process gets it killed
        if (server != null && server.asBinder().isBinderAlive()) return server;
        Context context = mContext;
        if (context != null && init(context)) {
            server = remote;
            if (server != null) return server;
        }
        throw new IllegalStateException("binder haven't been received");
    }

    /**
     * The first server api version that ships
     * {@link #addLockTaskModeListener(DhizukuLockTaskModeListener)}.
     */
    private static final int MIN_SERVER_VERSION_LOCK_TASK_LISTENER = DhizukuVariables.SERVER_VERSION_CODE;

    /**
     * Throw when the connected server predates the requested feature.
     */
    private static void requireServerVersion(int minVersion, String feature) {
        int version = mServerVersion;
        if (version < 0) version = getVersionCode();
        if (version < minVersion)
            throw new UnsupportedOperationException(feature + " requires Dhizuku Server API version "
                    + minVersion + " or newer, but the server is version " + version);
    }

    /**
     * get the version code of Dhizuku Server, some function will changed in different Dhizuku Server.
     *
     * @return server version code
     */
    public static int getVersionCode() {
        try {
            int version = requireServer().getVersionCode();
            mServerVersion = version;
            return version;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get the version name of Dhizuku Server API, a human-readable counterpart
     * of {@link #getVersionCode()}.
     *
     * @return server api version name
     */
    public static String getVersionName() {
        try {
            return requireServer().getVersionName();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getOwnerPackageName() {
        return getOwnerComponent().getPackageName();
    }

    public static ComponentName getOwnerComponent() {
        ComponentName component = mOwnerComponent;
        if (component == null) throw new IllegalStateException("owner component haven't been received");
        return component;
    }

    /**
     * check the permission that can use privileged method.
     *
     * @return boolean
     */
    public static boolean isPermissionGranted() {
        try {
            return requireServer().isPermissionGranted();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private static final long REQUEST_PERMISSION_TIMEOUT_MS = 15_000;

    /**
     * Request the permission that can use privileged method.
     * <p>
     * If the server does not answer within 15 seconds, the listener is called with
     * {@link PackageManager#PERMISSION_DENIED}.
     */
    public static void requestPermission(DhizukuRequestPermissionListener listener) {
        requestPermission(requireContext(), listener);
    }

    private static void requestPermission(Context context, DhizukuRequestPermissionListener listener) {
        if (listener == null) throw new NullPointerException("listener == null");

        Handler handler = new Handler(Looper.getMainLooper());
        AtomicBoolean finished = new AtomicBoolean(false);
        Runnable timeout = () -> {
            if (!finished.compareAndSet(false, true)) return;
            try {
                listener.onRequestPermission(PackageManager.PERMISSION_DENIED);
            } catch (RemoteException ignored) {
            }
        };

        DhizukuRequestPermissionListener wrapper = new DhizukuRequestPermissionListener() {
            @Override
            public void onRequestPermission(int grantResult) throws RemoteException {
                if (!finished.compareAndSet(false, true)) return;
                handler.removeCallbacks(timeout);
                listener.onRequestPermission(grantResult);
            }
        };

        Bundle bundle = new Bundle();
        bundle.putInt(DhizukuVariables.PARAM_CLIENT_UID, context.getApplicationInfo().uid);
        bundle.putBinder(DhizukuVariables.PARAM_CLIENT_REQUEST_PERMISSION_BINDER, wrapper.asBinder());

        String packageName = getOwnerPackageName();
        String action = DhizukuVariables.getActionRequestPermission(packageName);

        Intent intent = new Intent(action)
                .setPackage(packageName)
                .putExtras(bundle)
                .putExtra("bundle", bundle) // Will be deprecated in the future.
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        handler.postDelayed(timeout, REQUEST_PERMISSION_TIMEOUT_MS);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            if (!finished.compareAndSet(false, true)) return;
            handler.removeCallbacks(timeout);
            try {
                listener.onRequestPermission(PackageManager.PERMISSION_DENIED);
            } catch (RemoteException unused) {
            }
        }
    }

    /**
     * you can transact the IBinder by Dhizuku Server.
     * and your can also use {@link #binderWrapper(IBinder)}.
     */
    public static boolean remoteTransact(IBinder iBinder, int code, Parcel data, Parcel reply, int flags) {
        //noinspection UnusedAssignment
        boolean result = false;
        Parcel remoteData = Parcel.obtain();
        try {
            remoteData.writeInterfaceToken(DhizukuVariables.BINDER_DESCRIPTOR);
            remoteData.writeStrongBinder(iBinder);
            remoteData.writeInt(code);
            remoteData.writeInt(flags);
            remoteData.appendFrom(data, 0, data.dataSize());
            result = requireServer().asBinder().transact(DhizukuVariables.TRANSACT_CODE_REMOTE_BINDER, remoteData, reply, 0);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            remoteData.recycle();
        }
        return result;
    }

    /**
     * Wrap the binder so that all transacts are requested by a remote server.
     */
    public static IBinder binderWrapper(IBinder iBinder) {
        return new DhizukuBinderWrapper(iBinder);
    }

    /**
     * create a process that work in remote server.
     */
    public static DhizukuRemoteProcess newProcess(String[] cmd, String[] env, File dir) {
        try {
            return new DhizukuRemoteProcess(requireServer().remoteProcess(cmd, env, dir != null ? dir.getPath() : null));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * start a UserService.
     *
     * @throws RuntimeException if the server is unreachable.
     */
    public static void startUserService(DhizukuUserServiceArgs args) {
        try {
            DhizukuServiceConnections.start(requireServer(), args);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * stop a UserService.
     *
     * @throws RuntimeException if the server is unreachable.
     */
    public static void stopUserService(DhizukuUserServiceArgs args) {
        try {
            DhizukuServiceConnections.stop(requireServer(), args);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * bind a UserService.
     *
     * @throws RuntimeException if the server is unreachable.
     */
    public static boolean bindUserService(DhizukuUserServiceArgs args, ServiceConnection connection) {
        try {
            DhizukuServiceConnections.bind(requireServer(), args, connection);
            return true;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * unbind a UserService.
     *
     * @throws RuntimeException if the server is unreachable.
     */
    public static boolean unbindUserService(ServiceConnection connection) {
        try {
            DhizukuServiceConnections.unbind(requireServer(), connection);
            return true;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] getDelegatedScopes() {
        try {
            return requireServer().getDelegatedScopes(requireContext().getPackageName());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setDelegatedScopes(String[] scopes) {
        try {
            requireServer().setDelegatedScopes(requireContext().getPackageName(), scopes);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Register a listener that is called when the device enters or exits lock task mode.
     * <p>
     * Lock task mode callbacks ({@code DeviceAdminReceiver.onLockTaskModeEntering} /
     * {@code onLockTaskModeExiting}) are only delivered to the device owner (Dhizuku Server)
     * itself. This API forwards those events to your application.
     * <p>
     * Requires permission granted by {@link #requestPermission(DhizukuRequestPermissionListener)}
     * and a server that supports this API (see {@link #getVersionCode()}).
     *
     * @see #removeLockTaskModeListener(DhizukuLockTaskModeListener)
     */
    public static void addLockTaskModeListener(DhizukuLockTaskModeListener listener) {
        requireServerVersion(MIN_SERVER_VERSION_LOCK_TASK_LISTENER, "addLockTaskModeListener");
        try {
            requireServer().addLockTaskModeListener(listener);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unregister a listener registered by {@link #addLockTaskModeListener(DhizukuLockTaskModeListener)}.
     */
    public static void removeLockTaskModeListener(DhizukuLockTaskModeListener listener) {
        requireServerVersion(MIN_SERVER_VERSION_LOCK_TASK_LISTENER, "removeLockTaskModeListener");
        try {
            requireServer().removeLockTaskModeListener(listener);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
