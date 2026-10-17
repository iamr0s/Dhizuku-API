package com.rosan.dhizuku.api;

import com.rosan.dhizuku.aidl.IDhizukuLockTaskModeListener;

public abstract class DhizukuLockTaskModeListener extends IDhizukuLockTaskModeListener.Stub {
    /**
     * Called when the device enters or exits lock task mode.
     *
     * @param entering whether the device is entering lock task mode.
     * @param pkg the package that is using lock task mode (null when exiting).
     */
    public abstract void onLockTaskModeChanged(boolean entering, String pkg);
}
