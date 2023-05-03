package com.rosan.dhizuku.api;

import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;

import com.rosan.dhizuku.shared.DhizukuVariables;

public class DhizukuUserServiceArgs {
    private final Bundle bundle;

    public DhizukuUserServiceArgs(DhizukuUserServiceArgs args) {
        this(args.bundle);
    }

    public DhizukuUserServiceArgs(ComponentName name) {
        bundle = new Bundle();
        setComponentName(name);
    }

    DhizukuUserServiceArgs(Bundle bundle) {
        this.bundle = bundle;
    }

    public DhizukuUserServiceArgs setComponentName(ComponentName name) {
        bundle.putParcelable(DhizukuVariables.PARAM_COMPONENT, name);
        return this;
    }

    public ComponentName getComponentName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return bundle.getParcelable(DhizukuVariables.PARAM_COMPONENT, ComponentName.class);
        else return bundle.getParcelable(DhizukuVariables.PARAM_COMPONENT);
    }

    Bundle build() {
        Bundle bundle = new Bundle();
        bundle.putAll(this.bundle);
        return bundle;
    }
}
