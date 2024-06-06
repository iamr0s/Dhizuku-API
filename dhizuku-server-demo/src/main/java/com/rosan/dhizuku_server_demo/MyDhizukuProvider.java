package com.rosan.dhizuku_server_demo;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rosan.dhizuku.aidl.IDhizukuClient;
import com.rosan.dhizuku.server_api.DhizukuProvider;
import com.rosan.dhizuku.server_api.DhizukuService;

public class MyDhizukuProvider extends DhizukuProvider {

    @Override
    public DhizukuService onCreateService(IDhizukuClient client) {
        Context context = getContext();
        assert context != null;
        ComponentName admin = new ComponentName(context, DemoReceiver.class);
        return new MyDhizukuService(context, admin, client);
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        return super.call(method, arg, extras);
    }
}
