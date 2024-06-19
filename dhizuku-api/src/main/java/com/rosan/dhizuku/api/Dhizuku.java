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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.rosan.dhizuku.aidl.IDhizuku;
import com.rosan.dhizuku.shared.DhizukuVariables;

import java.io.File;
import java.util.List;

public class Dhizuku {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext = null;

    @SuppressLint("StaticFieldLeak")
    private static ComponentName mOwnerComponent;

    private static IDhizuku remote = null;

    public static @Nullable ComponentName getOwnerComponent(Context context) {
        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return getOwnerComponent(manager);
    }

    public static @Nullable ComponentName getOwnerComponent(DevicePolicyManager manager) {
        ComponentName component = null;
        List<ComponentName> admins = manager.getActiveAdmins();
        if (admins == null) return component;
        for (ComponentName admin : admins) {
            String packageName = admin.getPackageName();
            if (manager.isDeviceOwnerApp(packageName)) {
                component = admin;
                break;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && manager.isProfileOwnerApp(packageName)) component = admin;
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
    public static boolean init(@NonNull Context context) {
        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (remote != null && remote.asBinder().pingBinder()
                && (manager.isDeviceOwnerApp(getOwnerPackageName()) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && manager.isProfileOwnerApp(getOwnerPackageName())))
            return true;

        mOwnerComponent = getOwnerComponent(context);
        if (mOwnerComponent == null) return false;
        String packageName = getOwnerPackageName();
        String authority = DhizukuVariables.getProviderAuthorityName(packageName);

        Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).build();
        Bundle extras = new Bundle();
        extras.putBinder(DhizukuVariables.EXTRA_CLIENT, new DhizukuClient().asBinder());
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
        remote = IDhizuku.Stub.asInterface(iBinder);
        try {
            iBinder.linkToDeath(() -> {
                if (remote.asBinder() != iBinder) return;
                remote = null;
            }, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mContext = context;
        return true;
    }

    private static @NonNull IDhizuku requireServer() {
        if (remote != null && remote.asBinder().pingBinder()) return remote;
        if (mContext != null && init(mContext)) return remote;
        throw new IllegalStateException("binder haven't been received");
    }

    /**
     * get the version code of Dhizuku Server, some function will changed in different Dhizuku Server.
     *
     * @return server version code
     */
    public static int getVersionCode() {
        try {
            return requireServer().getVersionCode();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * get the version name of Dhizuku Server.
     *
     * @return server version name
     */
    public static String getVersionName() {
        try {
            return requireServer().getVersionName();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NonNull String getOwnerPackageName() {
        return getOwnerComponent().getPackageName();
    }

    public static @NonNull ComponentName getOwnerComponent() {
        assert mOwnerComponent != null;
        return mOwnerComponent;
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

    /**
     * request the permission that can use privileged method.
     */
    public static void requestPermission(DhizukuRequestPermissionListener listener) {
        requestPermission(mContext, listener);
    }

    private static void requestPermission(Context context, DhizukuRequestPermissionListener listener) {
        Bundle bundle = new Bundle();
        bundle.putInt(DhizukuVariables.PARAM_CLIENT_UID, context.getApplicationInfo().uid);
        bundle.putBinder(DhizukuVariables.PARAM_CLIENT_REQUEST_PERMISSION_BINDER, listener.asBinder());

        String packageName = getOwnerPackageName();
        String action = DhizukuVariables.getActionRequestPermission(packageName);

        Intent intent = new Intent(action)
                .setPackage(packageName)
                .putExtras(bundle)
                .putExtra("bundle", bundle) // Will be deprecated in the future.
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                listener.onRequestPermission(PackageManager.PERMISSION_DENIED);
            } catch (RemoteException ignored) {
            }
        }
    }

    /**
     * you can transact the IBinder by Dhizuku Server.
     * and your can also use {@link #binderWrapper(IBinder)}.
     */
    public static boolean remoteTransact(IBinder iBinder, int code, Parcel data, Parcel reply, int flags) {
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
     */
    public static void startUserService(@NonNull DhizukuUserServiceArgs args) {
        try {
            DhizukuServiceConnections.start(requireServer(), args);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * stop a UserService.
     */
    public static void stopUserService(@NonNull DhizukuUserServiceArgs args) {
        try {
            DhizukuServiceConnections.stop(requireServer(), args);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * bind a UserService.
     */
    public static boolean bindUserService(@NonNull DhizukuUserServiceArgs args, @NonNull ServiceConnection connection) {
        try {
            DhizukuServiceConnections.bind(requireServer(), args, connection);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * unbind a UserService.
     */
    public static boolean unbindUserService(@NonNull ServiceConnection connection) {
        try {
            DhizukuServiceConnections.unbind(requireServer(), connection);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static String[] getDelegatedScopes() {
        try {
            return requireServer().getDelegatedScopes(mContext.getPackageName());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static void setDelegatedScopes(String[] scopes) {
        try {
            requireServer().setDelegatedScopes(mContext.getPackageName(), scopes);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
