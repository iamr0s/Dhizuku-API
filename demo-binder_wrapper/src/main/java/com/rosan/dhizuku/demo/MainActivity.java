package com.rosan.dhizuku.demo;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.app.admin.IDevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuBinderWrapper;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.rosan.dhizuku.shared.DhizukuVariables;

import java.lang.reflect.Field;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private DevicePolicyManager devicePolicyManager;

    private EditText editText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        if (!Dhizuku.init(this)) {
            toast(R.string.dhizuku_init_failed);
            finish();
            return;
        }

        if (!Dhizuku.isPermissionGranted())
            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED)
                        devicePolicyManager = binderWrapperDevicePolicyManager();
                    else {
                        toast(R.string.dhizuku_permission_denied);
                        finish();
                    }
                }
            });
        else devicePolicyManager = binderWrapperDevicePolicyManager();

        editText = findViewById(R.id.edit_text);
        findViewById(R.id.block_uninstall_button).setOnClickListener(this);
        findViewById(R.id.unblock_uninstall_button).setOnClickListener(this);
        findViewById(R.id.disable_button).setOnClickListener(this);
        findViewById(R.id.enable_button).setOnClickListener(this);
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private DevicePolicyManager binderWrapperDevicePolicyManager() {
        try {
            Context context = createPackageContext(DhizukuVariables.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
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
    public void onClick(View view) {
        int id = view.getId();
        String packageName = editText.getText().toString();
        if (id == R.id.block_uninstall_button) {
            devicePolicyManager.setUninstallBlocked(DhizukuVariables.COMPONENT_NAME, packageName, true);
        } else if (id == R.id.unblock_uninstall_button) {
            devicePolicyManager.setUninstallBlocked(DhizukuVariables.COMPONENT_NAME, packageName, false);
        } else if (id == R.id.disable_button) {
            devicePolicyManager.setPackagesSuspended(DhizukuVariables.COMPONENT_NAME, new String[]{packageName}, true);
        } else if (id == R.id.enable_button) {
            devicePolicyManager.setPackagesSuspended(DhizukuVariables.COMPONENT_NAME, new String[]{packageName}, false);
        }
    }
}
