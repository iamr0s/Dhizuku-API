package com.rosan.dhizuku_server_demo;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.app.admin.IDevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuBinderWrapper;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.rosan.dhizuku_api_demo.R;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Field;

public class MainActivity extends ComponentActivity {
    private DevicePolicyManager manager;

    public void toast(Object... objects) {
        StringBuilder builder = new StringBuilder();
        for (Object object : objects) {
            builder.append(object);
            builder.append("\n");
        }
        if (builder.length() > 0) builder.setLength(builder.length() - 1);
        Toast.makeText(this, builder.toString(), Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private DevicePolicyManager binderWrapperDevicePolicyManager() {
        try {
            Context context = createPackageContext(Dhizuku.getOwnerComponent().getPackageName(), Context.CONTEXT_IGNORE_SECURITY);
            DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            Field field = manager.getClass().getDeclaredField("mService");
            field.setAccessible(true);
            IDevicePolicyManager oldInterface = (IDevicePolicyManager) field.get(manager);
            if (oldInterface instanceof DhizukuBinderWrapper) return manager;
            assert oldInterface != null;
            IBinder oldBinder = oldInterface.asBinder();
            IBinder newBinder = Dhizuku.binderWrapper(oldBinder);
            IDevicePolicyManager newInterface = IDevicePolicyManager.Stub.asInterface(newBinder);
            field.set(manager, newInterface);
            return manager;
        } catch (NoSuchFieldException |
                 IllegalAccessException |
                 PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            HiddenApiBypass.setHiddenApiExemptions("");
        super.onCreate(savedInstanceState);

        manager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (!manager.isDeviceOwnerApp(this.getPackageName())) {
            if (!Dhizuku.init()) {
                toast("Dhizuku-Server not working!!!");
                finish();
                return;
            }
            if (!Dhizuku.isPermissionGranted()) requestPermissionsLayout();
            else dhizukuMode();
        } else managerMode();
    }

    private void requestPermissionsLayout() {
        setContentView(R.layout.request_permissions);
        Button button = findViewById(R.id.request_permissions_button);
        button.setOnClickListener(v -> {
            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult) throws RemoteException {
                    runOnUiThread(() -> {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) dhizukuMode();
                        else toast("denied");
                    });
                }
            });
        });
    }

    private void dhizukuMode() {
        manager = binderWrapperDevicePolicyManager();
        managerMode();
    }

    private void managerMode() {
        setContentView(R.layout.manager);
        TextView statusView = findViewById(R.id.camera_status);
        Button refreshView = findViewById(R.id.refresh_camera_status);
        Button enableView = findViewById(R.id.enable_camera_status);
        Button disableView = findViewById(R.id.disable_camera_status);
        refreshView.setOnClickListener(v -> {
            statusView.setText(manager.getCameraDisabled(Dhizuku.getOwnerComponent()) ? "Disabled" : "Enabled");
        });
        statusView.setText(Dhizuku.getVersionName());
        refreshView.callOnClick();
        enableView.setOnClickListener(v -> {
            manager.setCameraDisabled(Dhizuku.getOwnerComponent(), false);
            refreshView.callOnClick();
        });
        disableView.setOnClickListener(v -> {
            manager.setCameraDisabled(Dhizuku.getOwnerComponent(), true);
            refreshView.callOnClick();
        });
    }
}
