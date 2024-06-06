package com.rosan.dhizuku.server_api;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.Keep;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Objects;

public class UserService extends IUserServiceManager.Stub {
    private final Context mContext;

    private final HashMap<String, IBinder> map = new HashMap<>();

    @Keep
    public UserService(Context context) {
        mContext = context;
    }

    @Override
    public void quit() throws RemoteException {
        for (IBinder service : map.values()) {
            transact(service, 2);
        }
        System.exit(0);
    }

    @Override
    public IBinder startService(ComponentName component) {
        String key = component.flattenToShortString();
        IBinder service = map.get(key);
        if (service != null) return service;

        try {
            Context packageContext = mContext.createPackageContext(component.getPackageName(),
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);

            Class<?> clazz = packageContext.getClassLoader().loadClass(component.getClassName());
            Constructor<?> constructor = null;
            try {
                constructor = clazz.getConstructor(Context.class);
            } catch (NoSuchMethodException | SecurityException ignored) {
            }
            if (constructor != null) service = (IBinder) constructor.newInstance(mContext);
            else service = (IBinder) clazz.newInstance();
            transact(service, 1);
            map.put(key, service);
            return service;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 InvocationTargetException | RemoteException |
                 PackageManager.NameNotFoundException ignored) {
        }
        return null;
    }

    private void transact(IBinder service, int code) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(Objects.requireNonNull(service.getInterfaceDescriptor()));
            service.transact(IBinder.FIRST_CALL_TRANSACTION + code, data, reply, Binder.FLAG_ONEWAY);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
