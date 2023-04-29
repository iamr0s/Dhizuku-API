package com.rosan.dhizuku.demo;

import android.annotation.SuppressLint;
import android.content.pm.IPackageInstaller;
import android.content.pm.PackageInstaller;
import android.os.Build;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.shared.DhizukuVariables;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Field;
import java.util.Objects;

public class PackageInstallerHelper {
    @SuppressLint("SoonBlockedPrivateApi")
    public static void proxy(PackageInstaller packageInstaller) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("");
        }
        try {
            Field nameField = PackageInstaller.class.getDeclaredField("mInstallerPackageName");
            nameField.setAccessible(true);
            String name = (String) nameField.get(packageInstaller);
            if (Objects.equals(name, DhizukuVariables.PACKAGE_NAME)) return;
            nameField.set(packageInstaller, DhizukuVariables.PACKAGE_NAME);
            Field field = PackageInstaller.class.getDeclaredField("mInstaller");
            field.setAccessible(true);
            IPackageInstaller origin = (IPackageInstaller) field.get(packageInstaller);
            IPackageInstaller proxy = IPackageInstaller.Stub.asInterface(Dhizuku.binderWrapper(origin.asBinder()));
            field.set(packageInstaller, proxy);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
