package com.rosan.dhizuku.demo_user_service;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.RemoteException;

import androidx.annotation.Keep;

public class UserService extends IUserService.Stub {
    private Context context;

    @Keep
    public UserService(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() throws RemoteException {
    }

    @SuppressLint("MissingPermission")
    @Override
    public void uninstall(String packageName) throws RemoteException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(), Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT);
        packageInstaller.uninstall(packageName, pendingIntent.getIntentSender());
    }
}
