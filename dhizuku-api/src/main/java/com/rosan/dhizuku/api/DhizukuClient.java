package com.rosan.dhizuku.api;

import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizukuClient;
import com.rosan.dhizuku.shared.DhizukuVariables;

public class DhizukuClient extends IDhizukuClient.Stub {
    @Override
    public int getVersionCode() throws RemoteException {
        return DhizukuVariables.CLIENT_VERSION_CODE;
    }
}
