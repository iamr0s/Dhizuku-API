package com.rosan.dhizuku.api;

import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizukuClient;

public class DhizukuClient extends IDhizukuClient.Stub {
    private static final int VERSION_CODE = 1;

    /** @noinspection unused, RedundantThrows */
    @Override
    public int getVersionCode() throws RemoteException {
        return VERSION_CODE;
    }
}
