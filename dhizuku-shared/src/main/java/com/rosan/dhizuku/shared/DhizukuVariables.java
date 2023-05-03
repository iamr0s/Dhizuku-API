package com.rosan.dhizuku.shared;

import android.os.Binder;

public class DhizukuVariables {
    public static final String PACKAGE_NAME = "com.rosan.dhizuku";

    public static final String ACTION_REQUEST_PERMISSION = PACKAGE_NAME + ".action.request.permission";

    public static final String BINDER_DESCRIPTOR = PACKAGE_NAME + ".server";

    public static final String PROVIDER_AUTHORITY = PACKAGE_NAME + ".server.provider";

    public static final String PROVIDER_METHOD_CLIENT = "client";

    public static final String EXTRA_CLIENT = "client";

    public static final String PARAM_DHIZUKU_BINDER = "dhizuku_binder";

    public static final String PARAM_CLIENT_UID = "uid";

    public static final String PARAM_CLIENT_REQUEST_PERMISSION_BINDER = "request_permission_binder";

    public static final String PARAM_PACKAGE_NAME = "package_name";

    public static final String PARAM_CLASS_NAME = "class_name";

    public static final String PARAM_COMPONENT = "component";

    public static final int TRANSACT_CODE_REMOTE_BINDER = Binder.FIRST_CALL_TRANSACTION + 10;
}
