package com.rosan.dhizuku.server_api;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizuku;
import com.rosan.dhizuku.aidl.IDhizukuClient;
import com.rosan.dhizuku.aidl.IDhizukuLockTaskModeListener;
import com.rosan.dhizuku.aidl.IDhizukuRemoteProcess;
import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.rosan.dhizuku.shared.DhizukuVariables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class DhizukuService extends IDhizuku.Stub {
    protected Context mContext;

    protected DevicePolicyManager mManager;

    protected ComponentName mAdmin;

    protected IDhizukuClient mClient;

    /** Remote processes handed out to the current client, guarded by itself. */
    private final List<RemoteProcess> mProcesses = new ArrayList<>();

    public DhizukuService(Context context, ComponentName admin, IDhizukuClient client) {
        mContext = context;
        mManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdmin = admin;
        mClient = client;
        try {
            // registered once: destroy every remote process when the client dies
            if (client != null) client.asBinder().linkToDeath(this::destroyProcesses, 0);
        } catch (RemoteException ignored) {
            // client already dead, nothing was started yet
        }
    }

    private void destroyProcesses() {
        List<RemoteProcess> processes;
        synchronized (mProcesses) {
            processes = new ArrayList<>(mProcesses);
            mProcesses.clear();
        }
        for (RemoteProcess process : processes) {
            try {
                if (process.alive()) process.destroy();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public int getVersionCode() {
        return Variables.SERVICE_VERSION_CODE;
    }

    /**
     * Returns the version name of the Dhizuku Server API, the human-readable
     * counterpart of {@link #getVersionCode()} which returns the protocol
     * version used for feature detection.
     */
    @Override
    public String getVersionName() {
        return Variables.SERVICE_VERSION_NAME;
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
        synchronized (mProcesses) {
            mProcesses.add(process);
        }
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

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public String[] getDelegatedScopes(String packageName) throws RemoteException {
        enforceCallingPermission("get_delegated_scopes");
        return mManager.getDelegatedScopes(mAdmin, packageName).toArray(new String[0]);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void setDelegatedScopes(String packageName, String[] scopes) throws RemoteException {
        enforceCallingPermission("set_delegated_scopes");
        mManager.setDelegatedScopes(mAdmin, packageName, Arrays.asList(scopes));
    }

    @Override
    public void addLockTaskModeListener(IDhizukuLockTaskModeListener listener) throws RemoteException {
        enforceCallingPermission("lock_task_mode_listener");
        LockTaskModeListeners.add(listener);
    }

    @Override
    public void removeLockTaskModeListener(IDhizukuLockTaskModeListener listener) throws RemoteException {
        enforceCallingPermission("lock_task_mode_listener");
        LockTaskModeListeners.remove(listener);
    }
}
