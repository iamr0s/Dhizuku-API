package com.rosan.dhizuku.server_api;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class UserService extends IUserServiceManager.Stub {
    private final Context mContext;

    private final ConcurrentHashMap<String, IBinder> map = new ConcurrentHashMap<>();

    public UserService(Context context) {
        mContext = context;
    }

    @Override
    public void quit() throws RemoteException {
        for (IBinder service : map.values()) {
            try {
                transact(service, 2);
            } catch (RemoteException | RuntimeException ignored) {
            }
        }
        System.exit(0);
    }

    @Override
    public synchronized IBinder startService(ComponentName component) {
        String key = component.flattenToShortString();
        IBinder service = map.get(key);
        if (service != null) return service;

        try {
            Context packageContext = mContext.createPackageContext(component.getPackageName(),
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);

            Class<?> clazz = packageContext.getClassLoader().loadClass(component.getClassName());
            try {
                service = (IBinder) clazz.getConstructor(Context.class).newInstance(mContext);
            } catch (NoSuchMethodException | SecurityException ignored) {
                service = (IBinder) clazz.getConstructor().newInstance();
            }
            transact(service, 1);
            map.put(key, service);
            return service;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 InvocationTargetException | RemoteException |
                 PackageManager.NameNotFoundException | NoSuchMethodException | RuntimeException ignored) {
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
