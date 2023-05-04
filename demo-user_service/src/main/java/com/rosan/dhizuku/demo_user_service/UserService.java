package com.rosan.dhizuku.demo_user_service;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.RemoteException;

import androidx.annotation.Keep;

import java.util.List;

public class UserService extends IUserService.Stub {
    private Context context;

    private DevicePolicyManager devicePolicyManager;

    private ComponentName currentAdmin;

    @Keep
    public UserService(Context context) {
        this.context = context;
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    @Override
    public void onCreate() {
        List<ComponentName> admins = devicePolicyManager.getActiveAdmins();
        for (ComponentName admin : admins) {
            if (devicePolicyManager.isDeviceOwnerApp(admin.getPackageName())) {
                currentAdmin = admin;
                break;
            }
        }
    }

    @Override
    public void onDestroy() throws RemoteException {
    }

    @SuppressLint("MissingPermission")
    @Override
    public void uninstall(String packageName) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(), Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT);
        packageInstaller.uninstall(packageName, pendingIntent.getIntentSender());
    }

    @Override
    @SuppressLint("DiscouragedPrivateApi")
    public void setApplicationHidden(String packageName, boolean state) throws RemoteException {
        devicePolicyManager.setApplicationHidden(currentAdmin, packageName, state);
    }

    @Override
    public void setOrganizationName(String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            devicePolicyManager.setOrganizationName(currentAdmin, name);
        }
    }
}
