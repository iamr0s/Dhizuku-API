package com.rosan.dhizuku.demo;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends ComponentActivity implements View.OnClickListener {
    private IUserService service;

    private EditText editText;

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
        editText = findViewById(R.id.edit_text);
        findViewById(R.id.uninstall_button).setOnClickListener(this);
        findViewById(R.id.disable_button).setOnClickListener(this);
        findViewById(R.id.enable_button).setOnClickListener(this);
        findViewById(R.id.organization_name_button).setOnClickListener(this);
        findViewById(R.id.switch_camera_disable).setOnClickListener(this);
        findViewById(R.id.set_global_proxy).setOnClickListener(this);
        findViewById(R.id.create_user).setOnClickListener(this);
        findViewById(R.id.remove_user).setOnClickListener(this);
    }

    void bindUserService() {
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(this, UserService.class));
        boolean bind = Dhizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                toast("connected UserService");
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
        int count = 0;
        for (Object element : objects) {
            if (++count > 1) builder.append(" ");
            builder.append(element == null ? "null" : element);
        }
        return builder.toString();
    }

    String join(Object... objects) {
        return join(Arrays.asList(objects));
    }

    @Override
    public void onClick(View view) {
        if (service == null) toast("please bind service first");
        else {
            try {
                String text = onClickInner(view.getId(), editText.getText().toString());
                if (text != null) editText.setText(text);
            } catch (RemoteException e) {
                toast(e.getLocalizedMessage());
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    private String onClickInner(int id, String text) throws RemoteException {
        String result = null;
        if (id == R.id.uninstall_button) {
            service.uninstall(text);
        } else if (id == R.id.disable_button) {
            service.setApplicationHidden(text, true);
        } else if (id == R.id.enable_button) {
            service.setApplicationHidden(text, false);
        } else if (id == R.id.organization_name_button) {
            service.setOrganizationName(text);
        } else if (id == R.id.switch_camera_disable) {
            if (Dhizuku.getVersionCode() < 4) toast("please install >= Dhizuku v2.4");
            else service.switchCameraDisabled();
        } else if (id == R.id.set_global_proxy) {
            service.setGlobalProxy(text);
        } else if (id == R.id.create_user) {
            result = "" + service.createUser(text);
        } else if (id == R.id.remove_user) {
            service.removeUser(Integer.parseInt(text));
        }
        return result;
    }
}
