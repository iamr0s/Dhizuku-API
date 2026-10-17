package com.rosan.dhizuku.api;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizukuRemoteProcess;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DhizukuRemoteProcess extends Process {
    private static final String TAG = "DhizukuRemoteProcess";

    private IDhizukuRemoteProcess remote;

    private OutputStream outputStream;

    private InputStream inputStream;

    private InputStream errorStream;

    DhizukuRemoteProcess(IDhizukuRemoteProcess remote) {
        this.remote = Objects.requireNonNull(remote);
        try {
            remote.asBinder().linkToDeath(() ->
                    DhizukuRemoteProcess.this.remote = null, 0);
        } catch (RemoteException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    /**
     * @return the remote binder, or null when the remote process has died.
     */
    private IDhizukuRemoteProcess remoteOrNull() {
        IDhizukuRemoteProcess remote = this.remote;
        if (remote == null || !remote.asBinder().pingBinder()) return null;
        return remote;
    }

    private IDhizukuRemoteProcess requireRemote() {
        IDhizukuRemoteProcess remote = remoteOrNull();
        if (remote == null) throw new IllegalStateException("remote process has died");
        return remote;
    }

    @Override
    public OutputStream getOutputStream() {
        if (outputStream != null) return outputStream;
        try {
            outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(requireRemote().getOutputStream());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return outputStream;
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream != null) return inputStream;
        try {
            inputStream = new ParcelFileDescriptor.AutoCloseInputStream(requireRemote().getInputStream());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return inputStream;
    }

    @Override
    public InputStream getErrorStream() {
        if (errorStream != null) return errorStream;
        try {
            errorStream = new ParcelFileDescriptor.AutoCloseInputStream(requireRemote().getErrorStream());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        return errorStream;
    }

    @Override
    public int exitValue() {
        try {
            return requireRemote().exitValue();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        IDhizukuRemoteProcess remote = remoteOrNull();
        if (remote == null) return;
        try {
            remote.destroy();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isAlive() {
        IDhizukuRemoteProcess remote = remoteOrNull();
        if (remote == null) return false;
        try {
            return remote.alive();
        } catch (RemoteException ignored) {
            return false;
        }
    }

    @Override
    public int waitFor() {
        IDhizukuRemoteProcess remote = remoteOrNull();
        // the remote process died without reporting an exit code, treat as terminated
        if (remote == null) return 0;
        try {
            return remote.waitFor();
        } catch (RemoteException ignored) {
            return 0;
        }
    }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) {
        IDhizukuRemoteProcess remote = remoteOrNull();
        if (remote == null) return true;
        try {
            return remote.waitForTimeout(timeout, unit.toString());
        } catch (RemoteException ignored) {
            return true;
        }
    }
}
