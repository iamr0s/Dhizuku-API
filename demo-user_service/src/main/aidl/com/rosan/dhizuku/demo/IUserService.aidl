package com.rosan.dhizuku.demo;

import android.os.UserHandle;

interface IUserService {
    void onCreate() = 1;
    void onDestroy() = 2;

    // 20以内的transact code是保留给未来的Dhizuku APi使用的。
    // transact codes up to 20 are reserved for future Dhizuku API.
    void uninstall(String packageName) = 21;

    void setApplicationHidden(String packageName, boolean state) = 22;

    void setOrganizationName(String name) = 23;

    void lockNow() = 24;

    void switchCameraDisabled() = 25;

    void setGlobalProxy(String url) = 26;

    UserHandle createUser(String name) = 27;

    void removeUser(int userId) = 28;
}