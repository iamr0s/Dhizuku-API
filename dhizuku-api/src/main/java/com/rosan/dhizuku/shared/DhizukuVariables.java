package com.rosan.dhizuku.shared;

import android.content.ComponentName;
import android.os.Binder;

import androidx.annotation.NonNull;

import java.util.Objects;

public class DhizukuVariables {
    public static final String OFFICIAL_PACKAGE_NAME = "com.rosan.dhizuku";

    public static final String PACKAGE_NAME = OFFICIAL_PACKAGE_NAME;

    public static final ComponentName OFFICIAL_COMPONENT_NAME = new ComponentName(OFFICIAL_PACKAGE_NAME, OFFICIAL_PACKAGE_NAME + ".server.DhizukuDAReceiver");

    public static final ComponentName COMPONENT_NAME = OFFICIAL_COMPONENT_NAME;

    public static final String OFFICIAL_PERMISSION_API = OFFICIAL_PACKAGE_NAME + ".permission.API";

    public static final String PERMISSION_API = OFFICIAL_PERMISSION_API;

    public static @NonNull String getProviderAuthorityName(String packageName) {
        if (Objects.equals(packageName, OFFICIAL_PACKAGE_NAME))
            return OFFICIAL_PACKAGE_NAME + ".server.provider";
        else return packageName + ".dhizuku_server.provider";
    }

    public static @NonNull String getActionRequestPermission(String packageName) {
        if (Objects.equals(packageName, OFFICIAL_PACKAGE_NAME))
            return OFFICIAL_PACKAGE_NAME + ".action.request.permission";
        else return packageName + ".action.REQUEST_DHIZUKU_PERMISSION";
    }

    public static final String BINDER_DESCRIPTOR = OFFICIAL_PACKAGE_NAME + ".server";

    public static final String PROVIDER_METHOD_CLIENT = "client";

    public static final String EXTRA_CLIENT = "client";

    public static final String PARAM_DHIZUKU_BINDER = "dhizuku_binder";

    public static final String PARAM_CLIENT_UID = "uid";

    public static final String PARAM_CLIENT_REQUEST_PERMISSION_BINDER = "request_permission_binder";

    public static final String PARAM_COMPONENT = "component";

    public static final int TRANSACT_CODE_REMOTE_BINDER = Binder.FIRST_CALL_TRANSACTION + 10;
}
