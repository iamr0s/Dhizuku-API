package com.rosan.dhizuku.demo;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private static final String[] REQUIRED_DELEGATED_SCOPES = new String[]{
            DevicePolicyManager.DELEGATION_BLOCK_UNINSTALL,
            DevicePolicyManager.DELEGATION_PACKAGE_ACCESS
    };

    private DevicePolicyManager devicePolicyManager;

    private EditText editText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        setContentView(R.layout.main_activity);
        if (!Dhizuku.init(this)) {
            toast(R.string.dhizuku_init_failed);
            finish();
            return;
        }
        if (Dhizuku.getVersionCode() < 5) {
            toast(R.string.dhizuku_version_too_older);
            finish();
            return;
        }
        if (!Dhizuku.isPermissionGranted())
            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED)
                        setDelegatedScopes();
                    else {
                        toast(R.string.dhizuku_permission_denied);
                        finish();
                    }
                }
            });
        else setDelegatedScopes();
        editText = findViewById(R.id.edit_text);
        findViewById(R.id.block_uninstall_button).setOnClickListener(this);
        findViewById(R.id.unblock_uninstall_button).setOnClickListener(this);
        findViewById(R.id.disable_button).setOnClickListener(this);
        findViewById(R.id.enable_button).setOnClickListener(this);
    }

    private boolean checkDelegatedScopes() {
        return new ArrayList<>(Arrays.asList(Dhizuku.getDelegatedScopes())).containsAll(Arrays.asList(REQUIRED_DELEGATED_SCOPES));
    }

    private void setDelegatedScopes() {
        if (checkDelegatedScopes()) return;
        Dhizuku.setDelegatedScopes(REQUIRED_DELEGATED_SCOPES);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        String packageName = editText.getText().toString();
        if (id == R.id.block_uninstall_button) {
            devicePolicyManager.setUninstallBlocked(null, packageName, true);
        } else if (id == R.id.unblock_uninstall_button) {
            devicePolicyManager.setUninstallBlocked(null, packageName, false);
        } else if (id == R.id.disable_button) {
            devicePolicyManager.setPackagesSuspended(null, new String[]{packageName}, true);
        } else if (id == R.id.enable_button) {
            devicePolicyManager.setPackagesSuspended(null, new String[]{packageName}, false);
        }
    }
}
