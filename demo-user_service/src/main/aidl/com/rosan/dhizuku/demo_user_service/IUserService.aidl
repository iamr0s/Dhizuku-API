package com.rosan.dhizuku.demo_user_service;

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
}