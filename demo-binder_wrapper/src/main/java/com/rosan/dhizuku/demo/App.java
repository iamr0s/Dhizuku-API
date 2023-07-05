package com.rosan.dhizuku.demo;

import android.app.Application;

import com.rosan.dhizuku.api.Dhizuku;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Dhizuku.init();
    }
}
