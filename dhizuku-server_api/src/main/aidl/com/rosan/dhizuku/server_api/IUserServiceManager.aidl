package com.rosan.dhizuku.server_api;

import android.content.ComponentName;
import android.os.IBinder;

interface IUserServiceManager {
    void quit();

    IBinder startService(in ComponentName component);
}