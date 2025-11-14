package com.rosan.dhizuku.server_api;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.rosan.dhizuku.aidl.IDhizuku;
import com.rosan.dhizuku.aidl.IDhizukuClient;
import com.rosan.dhizuku.aidl.IDhizukuRemoteProcess;
import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.rosan.dhizuku.shared.DhizukuVariables;

import java.util.Arrays;
import java.util.LinkedHashMap;

@SuppressWarnings("RedundantThrows")
public abstract class DhizukuService extends IDhizuku.Stub {
    protected Context mContext;

    protected DevicePolicyManager mManager;

    protected ComponentName mAdmin;

    protected IDhizukuClient mClient;

    public DhizukuService(@NonNull Context context, ComponentName admin, IDhizukuClient client) {
        mContext = context;
        mManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdmin = admin;
        mClient = client;
    }

    @Override
    public int getVersionCode() {
        return Variables.SERVICE_VERSION_CODE;
    }

    public boolean isPermissionGranted() {
        try {
            enforceCallingPermission(null);
            return true;
        } catch (SecurityException ignored) {
            return false;
        }
    }

    public abstract boolean checkCallingPermission(String func, int callingUid, int callingPid);

    public final void enforceCallingPermission(String func) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingUid == android.os.Process.myUid()) return;

        if (checkCallingPermission(func, callingUid, callingPid)) return;

        throw new SecurityException("Permission Denial: " + func
                + " is not allowed from pid="
                + callingPid + ", uid=" + callingUid);
    }

    public boolean onRemoteTransact(IBinder binder, int code, Parcel data, Parcel reply, int flags) {
        enforceCallingPermission("remote_transact");
        return DhizukuProcess.get().remoteTransact(binder, code, data, reply, flags);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == DhizukuVariables.TRANSACT_CODE_REMOTE_BINDER) {
            Parcel remoteData = Parcel.obtain();
            try {
                data.enforceInterface(DhizukuVariables.BINDER_DESCRIPTOR);
                IBinder binder = data.readStrongBinder();
                int remoteCode = data.readInt();
                int remoteFlags = data.readInt();
                remoteData.appendFrom(data, data.dataPosition(), data.dataAvail());
                return onRemoteTransact(binder, remoteCode, remoteData, reply, remoteFlags);
            } finally {
                remoteData.recycle();
            }
        }
        return super.onTransact(code, data, reply, flags);
    }

    @Override
    public IDhizukuRemoteProcess remoteProcess(String[] cmd, String[] env, String dir) throws RemoteException {
        enforceCallingPermission("remote_process");
        LinkedHashMap<String, String> environment = new LinkedHashMap<>();
        if (env != null) for (String envstring : env) {
            if (envstring.indexOf('\u0000') != -1)
                envstring = envstring.replaceFirst("\u0000.*", "");
            int sign =
                    envstring.indexOf('=');
            if (sign != -1)
                environment.put(envstring.substring(0, sign),
                        envstring.substring(sign + 1));
        }
        RemoteProcess process = new RemoteProcess(DhizukuProcess.get().remoteProcess(Arrays.asList(cmd), environment, dir));
        mClient.asBinder().linkToDeath(() -> {
            try {
                if (process.alive()) process.destroy();
            } catch (Throwable ignored) {
            }
        }, 0);
        return process;
    }

    @Override
    public void bindUserService(IDhizukuUserServiceConnection connection, Bundle bundle) throws RemoteException {
        enforceCallingPermission("bind_user_service");
        if (connection == null || bundle == null) return;
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(bundle);
        UserServiceConnections.bind(uid, pid, args, connection);
    }

    @Override
    public void unbindUserService(Bundle bundle) throws RemoteException {
        enforceCallingPermission("unbind_user_service");
        if (bundle == null) return;
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(bundle);
        UserServiceConnections.unbind(uid, pid, args);
    }

    @Override
    public void unbindUserServiceByConnection(IDhizukuUserServiceConnection connection, Bundle bundle) throws RemoteException {
        enforceCallingPermission("unbind_user_service");
        if (bundle == null) return;
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(bundle);
        UserServiceConnections.unbind(uid, pid, args, connection);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public String[] getDelegatedScopes(String packageName) throws RemoteException {
        enforceCallingPermission("get_delegated_scopes");
        return mManager.getDelegatedScopes(mAdmin, packageName).toArray(new String[0]);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public void setDelegatedScopes(String packageName, String[] scopes) throws RemoteException {
        enforceCallingPermission("set_delegated_scopes");
        mManager.setDelegatedScopes(mAdmin, packageName, Arrays.asList(scopes));
    }
}
