package com.rosan.dhizuku.server_api;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import com.rosan.app_process.AppProcess;

public class DhizukuProcess extends AppProcess.None {
    private DhizukuProcess() {
        init();
    }

    public Context getContext() {
        return this.mContext;
    }

    @SuppressLint("StaticFieldLeak")
    private static final DhizukuProcess mInstance = new DhizukuProcess();

    public static @NonNull DhizukuProcess get() {
        return mInstance;
    }
}
