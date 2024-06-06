package com.rosan.dhizuku.server_api;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rosan.dhizuku.aidl.IDhizukuClient;
import com.rosan.dhizuku.shared.DhizukuVariables;

public abstract class DhizukuProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    public abstract DhizukuService onCreateService(IDhizukuClient client);

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (!method.equals(DhizukuVariables.PROVIDER_METHOD_CLIENT)
                || extras == null)
            return null;
        IBinder binder = extras.getBinder(DhizukuVariables.EXTRA_CLIENT);
        if (binder == null) return null;
        IDhizukuClient client = IDhizukuClient.Stub.asInterface(binder);
        Bundle bundle = new Bundle();
        bundle.putBinder(DhizukuVariables.PARAM_DHIZUKU_BINDER, onCreateService(client));
        return bundle;
    }
}
