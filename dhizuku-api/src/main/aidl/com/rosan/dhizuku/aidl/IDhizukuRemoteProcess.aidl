package com.rosan.dhizuku.aidl;

import android.os.ParcelFileDescriptor;

interface IDhizukuRemoteProcess {
    ParcelFileDescriptor getOutputStream();

    ParcelFileDescriptor getInputStream();

    ParcelFileDescriptor getErrorStream();

    int exitValue();

    void destroy();

    boolean alive();

    int waitFor();

    boolean waitForTimeout(long timeout, String unit);
}