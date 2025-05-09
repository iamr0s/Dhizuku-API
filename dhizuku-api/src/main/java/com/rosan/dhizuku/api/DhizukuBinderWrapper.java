package com.rosan.dhizuku.api;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;
import java.util.Objects;

public class DhizukuBinderWrapper implements IBinder {
    private final IBinder target;

    DhizukuBinderWrapper(IBinder target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        return target.getInterfaceDescriptor();
    }

    @Override
    public boolean pingBinder() {
        return target.pingBinder();
    }

    @Override
    public boolean isBinderAlive() {
        return target.isBinderAlive();
    }

    @Override
    public IInterface queryLocalInterface(@NonNull String descriptor) {
        return null;
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, String[] args) throws RemoteException {
        target.dump(fd, args);
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, String[] args) throws RemoteException {
        target.dumpAsync(fd, args);
    }

    /** @noinspection RedundantThrows*/
    @Override
    public boolean transact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
        return Dhizuku.remoteTransact(target, code, data, reply, flags);
    }

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) throws RemoteException {
        target.linkToDeath(recipient, flags);
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return target.unlinkToDeath(recipient, flags);
    }
}
