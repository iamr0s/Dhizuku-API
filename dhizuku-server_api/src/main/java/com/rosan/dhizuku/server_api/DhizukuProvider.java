package com.rosan.dhizuku.server_api;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizukuClient;
import com.rosan.dhizuku.shared.DhizukuVariables;

import java.util.HashMap;
import java.util.Map;

public abstract class DhizukuProvider extends ContentProvider {
    private final Map<String, DhizukuService> mServices = new HashMap<>();

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

        String key = Binder.getCallingUid() + ":" + Binder.getCallingPid();
        DhizukuService service;
        synchronized (mServices) {
            service = mServices.get(key);
            if (service == null) {
                service = onCreateService(IDhizukuClient.Stub.asInterface(binder));
                if (service == null) return null;
                mServices.put(key, service);
                DhizukuService created = service;
                try {
                    binder.linkToDeath(() -> {
                        synchronized (mServices) {
                            if (mServices.get(key) == created) mServices.remove(key);
                        }
                    }, 0);
                } catch (RemoteException ignored) {
                    mServices.remove(key);
                    return null;
                }
            }
        }

        Bundle bundle = new Bundle();
        bundle.putBinder(DhizukuVariables.PARAM_DHIZUKU_BINDER, service);
        return bundle;
    }
}
