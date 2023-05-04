package com.rosan.dhizuku.demo_user_service;

import android.app.Application;

import com.rosan.dhizuku.api.Dhizuku;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Dhizuku.init();
    }
}
