package com.rosan.dhizuku.aidl;

import android.content.ComponentName;
import android.os.IBinder;
import android.os.Parcel;
import com.rosan.dhizuku.aidl.IDhizukuRemoteProcess;
import com.rosan.dhizuku.aidl.IDhizukuRequestPermissionListener;
import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;

interface IDhizuku {
    int getVersionCode() = 0;

    String getVersionName() = 1;

    boolean isPermissionGranted() = 2;

    // remote binder transact: 10

    IDhizukuRemoteProcess remoteProcess(in String[] cmd, in String[] env, in String dir) = 11;

    void bindUserService(in IDhizukuUserServiceConnection connection, in Bundle bundle) = 12;

    void unbindUserService(in Bundle bundle) = 13;

    String[] getDelegatedScopes(String packageName) = 15;

    void setDelegatedScopes(String packageName,in String[] scopes) = 16;
}