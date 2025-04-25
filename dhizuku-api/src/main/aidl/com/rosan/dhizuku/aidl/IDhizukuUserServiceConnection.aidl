package com.rosan.dhizuku.aidl;

import android.os.Bundle;
import android.os.IBinder;

interface IDhizukuUserServiceConnection {
    oneway void connected(in Bundle bundle, IBinder service);

    oneway void died(in Bundle bundle);
}