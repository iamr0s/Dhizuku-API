package com.rosan.dhizuku_server_demo;

import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

public class MainActivity extends ComponentActivity {
    public void toast(Object... objects) {
        StringBuilder builder = new StringBuilder();
        for (Object object : objects) {
            builder.append(object);
            builder.append("\n");
        }
        if (builder.length() > 0) builder.setLength(builder.length() - 1);
        Toast.makeText(this, builder.toString(), Toast.LENGTH_SHORT).show();
    }

    private ComponentName admin;

    private App app;

    private TextView statusTextView;

    private Button clearOwnerButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        admin = new ComponentName(this, DemoReceiver.class);
        app = (App) getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusTextView = findViewById(R.id.status_text);
        Button refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> {
            refreshStatus();
        });

        clearOwnerButton = findViewById(R.id.clear_owner_button);
        clearOwnerButton.setOnClickListener(v -> {
            try {
                app.manager.clearDeviceOwnerApp(admin.getPackageName());
            } catch (Throwable ignored) {
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    app.manager.clearProfileOwner(admin);
            } catch (Throwable ignored) {
            }
        });

        Button forceStopButton = findViewById(R.id.force_stop_button);
        forceStopButton.setOnClickListener(v -> {
            System.exit(0);
        });

        refreshStatus();
    }

    private void refreshStatus() {
        app.syncOwnerStatus();
        statusTextView.setText(app.isOwner() ? "yes, the owner!" : "sorry, you are not owner!\n" +
                "adb shell dpm set-device-owner " + admin.flattenToShortString());
        clearOwnerButton.setVisibility(app.isOwner() ? View.VISIBLE : View.GONE);
    }
}
