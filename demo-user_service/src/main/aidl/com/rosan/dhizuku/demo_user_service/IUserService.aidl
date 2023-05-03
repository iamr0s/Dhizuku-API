package com.rosan.dhizuku.demo_user_service;

interface IUserService {
    void onCreate() = 1;
    void onDestroy() = 2;

    void uninstall(String packageName) = 21;
}