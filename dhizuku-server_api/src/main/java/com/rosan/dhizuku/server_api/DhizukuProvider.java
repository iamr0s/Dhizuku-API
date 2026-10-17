package com.rosan.dhizuku.server_api;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import com.rosan.dhizuku.aidl.IDhizukuClient;
import com.rosan.dhizuku.shared.DhizukuVariables;

public abstract class DhizukuProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    public abstract DhizukuService onCreateService(IDhizukuClient client);

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!DhizukuVariables.PROVIDER_METHOD_CLIENT.equals(method)
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
