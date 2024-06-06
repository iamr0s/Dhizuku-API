package com.rosan.dhizuku_server_demo;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizukuClient;
import com.rosan.dhizuku.server_api.DhizukuService;

public class MyDhizukuService extends DhizukuService {
    public MyDhizukuService(Context context, ComponentName admin, IDhizukuClient client) {
        super(context, admin, client);
    }

    @Override
    public String getVersionName() throws RemoteException {
        return "demo_0.0.1";
    }

    @Override
    public boolean isPermissionGranted() throws RemoteException {
        return true;
    }
}
