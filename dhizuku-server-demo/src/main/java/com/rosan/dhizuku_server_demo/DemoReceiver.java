package com.rosan.dhizuku_server_demo;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class DemoReceiver extends DeviceAdminReceiver {
    private App app;

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        app = (App) context.getApplicationContext();
        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        app.syncOwnerStatus();
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        super.onDisabled(context, intent);
        app.syncOwnerStatus();
    }
}
