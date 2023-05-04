package com.rosan.dhizuku.demo_user_service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends ComponentActivity {
    private IUserService service;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        if (!Dhizuku.init(this)) {
            toast("please install or launch Dhizuku Server Application,then relaunch this.");
            finish();
            return;
        }
        if (Dhizuku.getVersionCode() < 3) {
            toast("please install >= Dhizuku v2.0");
            finish();
            return;
        }
        if (!Dhizuku.isPermissionGranted())
            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult) {
                    bindUserService();
                }
            });
        else bindUserService();
        EditText editText = findViewById(R.id.edit_text);
        Button uninstallButton = findViewById(R.id.uninstall_button);
        Button disableButton = findViewById(R.id.disable_button);
        Button enableButton = findViewById(R.id.enable_button);
        Button organizationNameButton = findViewById(R.id.organization_name_button);
        uninstallButton.setOnClickListener(v -> {
            if (service == null) toast("please bind service first");
            else {
                try {
                    service.uninstall(editText.getText().toString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                toast("uninstall:", editText.getText());
            }
        });
        disableButton.setOnClickListener(v -> {
            if (service == null) toast("please bind service first");
            else {
                try {
                    service.setApplicationHidden(editText.getText().toString(), true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                toast("disable:", editText.getText());
            }
        });
        enableButton.setOnClickListener(v -> {
            if (service == null) toast("please bind service first");
            else {
                try {
                    service.setApplicationHidden(editText.getText().toString(), false);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                toast("enable:", editText.getText());
            }
        });
        organizationNameButton.setOnClickListener(v -> {
            if (service == null) toast("please bind service first");
            else {
                try {
                    service.setOrganizationName(editText.getText().toString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                toast("set organization name:", editText.getText());
            }
        });
    }

    void bindUserService() {
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(this, UserService.class));
        boolean bind = Dhizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                service = IUserService.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                toast("disconnected UserService");
            }
        });
        if (bind) return;
        toast("start user service failed");
    }

    void toast(Object... objects) {
        runOnUiThread(() -> {
            Toast.makeText(this, join(objects), Toast.LENGTH_SHORT).show();
        });
    }

    String join(List<Object> objects) {
        String sep = " ";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < objects.size(); i++) {
            Object object = objects.get(i);
            builder.append(object);
            if (i + 1 != objects.size())
                builder.append(sep);
        }
        return builder.toString();
    }

    String join(Object... objects) {
        return join(Arrays.asList(objects));
    }
}
