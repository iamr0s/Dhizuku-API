package com.rosan.dhizuku.aidl;

import android.os.Bundle;

interface IDhizukuUserServiceConnection {
    oneway void connected(in Bundle bundle, IBinder service);

    oneway void died(in Bundle bundle);
}