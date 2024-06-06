package com.rosan.dhizuku.server_api;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizukuRemoteProcess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class RemoteProcess extends IDhizukuRemoteProcess.Stub {
    protected Process mProcess;

    private ParcelFileDescriptor mOut;

    private ParcelFileDescriptor mIn;

    private ParcelFileDescriptor mErr;

    public RemoteProcess(Process process) {
        mProcess = process;
    }

    @Override
    public ParcelFileDescriptor getOutputStream() throws RemoteException {
        if (mOut != null) return mOut;
        mOut = parcelable(mProcess.getOutputStream());
        return mOut;
    }

    @Override
    public ParcelFileDescriptor getInputStream() throws RemoteException {
        if (mIn != null) return mIn;
        mIn = parcelable(mProcess.getInputStream());
        return mIn;
    }

    @Override
    public ParcelFileDescriptor getErrorStream() throws RemoteException {
        if (mErr != null) return mErr;
        mErr = parcelable(mProcess.getErrorStream());
        return mErr;
    }

    @Override
    public int exitValue() throws RemoteException {
        return mProcess.exitValue();
    }

    @Override
    public void destroy() throws RemoteException {
        mProcess.destroy();
    }

    @Override
    public boolean alive() throws RemoteException {
        try {
            exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    @Override
    public int waitFor() throws RemoteException {
        try {
            return mProcess.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean waitForTimeout(long timeout, String unitName) throws RemoteException {
        TimeUnit unit = TimeUnit.valueOf(unitName);
        long startTime = System.nanoTime();
        long rem = unit.toNanos(timeout);

        do {
            try {
                exitValue();
                return true;
            } catch (IllegalThreadStateException ex) {
                if (rem > 0) {
                    try {
                        Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
        } while (rem > 0);
        return false;
    }

    public static ParcelFileDescriptor parcelable(InputStream stream) {
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            transfer(stream, new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]));
            return pipe[0];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ParcelFileDescriptor parcelable(OutputStream stream) {
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            transfer(new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]), stream);
            return pipe[1];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void transfer(InputStream in, OutputStream out) {
        new Thread(() -> {
            byte[] buf = new byte[8 * 1024];
            int len = 0;
            try {
                while (true) {
                    len = in.read(buf);
                    if (len <= 0) break;
                    out.write(buf, 0, len);
                    out.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
