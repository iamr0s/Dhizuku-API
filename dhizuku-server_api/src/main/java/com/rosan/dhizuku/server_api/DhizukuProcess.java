package com.rosan.dhizuku.server_api;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import com.rosan.app_process.AppProcess;

public class DhizukuProcess extends AppProcess.Default {
    private DhizukuProcess(Context context) {
        init(context);
    }

    @SuppressLint("StaticFieldLeak")
    private static DhizukuProcess mInstance;

    public static @NonNull DhizukuProcess get(Context context) {
        if (mInstance != null) return mInstance;
        mInstance = new DhizukuProcess(context);
        return mInstance;
    }
}
