package com.rosan.dhizuku.api;

import android.content.ComponentName;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.os.BundleCompat;

import com.rosan.dhizuku.shared.DhizukuVariables;

public class DhizukuUserServiceArgs {
    private final Bundle bundle;

    /** @noinspection unused*/
    public DhizukuUserServiceArgs(@NonNull DhizukuUserServiceArgs args) {
        this(args.bundle);
    }

    public DhizukuUserServiceArgs(ComponentName name) {
        bundle = new Bundle();
        setComponentName(name);
    }

    public DhizukuUserServiceArgs(Bundle bundle) {
        this.bundle = bundle;
    }

    /** @noinspection UnusedReturnValue*/
    public DhizukuUserServiceArgs setComponentName(ComponentName name) {
        bundle.putParcelable(DhizukuVariables.PARAM_COMPONENT, name);
        return this;
    }

    public ComponentName getComponentName() {
        return BundleCompat.getParcelable(bundle, DhizukuVariables.PARAM_COMPONENT, ComponentName.class);
    }

    public Bundle build() {
        Bundle bundle = new Bundle();
        bundle.putAll(this.bundle);
        return bundle;
    }

    public String flattenToShortString() {
        return getComponentName().flattenToShortString();
    }
}
