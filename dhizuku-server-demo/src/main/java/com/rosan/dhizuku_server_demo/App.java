package com.rosan.dhizuku_server_demo;

import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.content.Context;

public class App extends Application {
    public DevicePolicyManager manager;

    private boolean isOwner = false;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        syncOwnerStatus();
    }

    public void syncOwnerStatus() {
        isOwner = manager.isProfileOwnerApp(getPackageName())
                || manager.isDeviceOwnerApp(getPackageName());
    }

    public boolean isOwner() {
        return isOwner;
    }
}
