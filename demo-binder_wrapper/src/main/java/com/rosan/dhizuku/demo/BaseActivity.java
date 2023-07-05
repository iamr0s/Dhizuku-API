package com.rosan.dhizuku.demo;

import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.List;

public class BaseActivity extends ComponentActivity {
    protected void toast(@StringRes int resId) {
        toast(getString(resId));
    }

    protected void toast(Object... objects) {
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
}
