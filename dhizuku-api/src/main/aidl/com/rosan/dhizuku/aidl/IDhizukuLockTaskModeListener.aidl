package com.rosan.dhizuku.aidl;

oneway interface IDhizukuLockTaskModeListener {
    void onLockTaskModeChanged(boolean entering, String pkg);
}
