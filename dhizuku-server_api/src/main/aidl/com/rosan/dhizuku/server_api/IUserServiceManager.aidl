package com.rosan.dhizuku.server_api;

import android.content.ComponentName;

interface IUserServiceManager {
    void quit();

    IBinder startService(in ComponentName component);
}