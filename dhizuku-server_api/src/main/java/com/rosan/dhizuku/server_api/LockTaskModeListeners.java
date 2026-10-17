package com.rosan.dhizuku.server_api;

import android.os.IBinder;
import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizukuLockTaskModeListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of lock task mode listeners that are provided by clients.
 * <p>
 * The Dhizuku Server app should call {@link #notifyLockTaskModeChanged(boolean, String)}
 * from its {@code DeviceAdminReceiver.onLockTaskModeEntering} /
 * {@code DeviceAdminReceiver.onLockTaskModeExiting} so that all registered clients
 * receive lock task mode events.
 */
public final class LockTaskModeListeners {
    private static final Object LOCK = new Object();

    /** listener -> its death recipient, so remove() can unlink it. Guarded by {@link #LOCK}. */
    private static final Map<IDhizukuLockTaskModeListener, IBinder.DeathRecipient> LISTENERS = new LinkedHashMap<>();

    private LockTaskModeListeners() {
    }

    /**
     * Register a listener. Registering the same listener twice is a no-op.
     * Dead listeners are removed automatically.
     */
    public static void add(IDhizukuLockTaskModeListener listener) {
        if (listener == null) return;
        synchronized (LOCK) {
            if (LISTENERS.containsKey(listener)) return;
        }
        IBinder.DeathRecipient recipient = () -> remove(listener);
        try {
            listener.asBinder().linkToDeath(recipient, 0);
        } catch (RemoteException e) {
            // already dead, ignore
            return;
        }
        synchronized (LOCK) {
            if (LISTENERS.containsKey(listener)) {
                // another thread won the race, drop the duplicate registration
                listener.asBinder().unlinkToDeath(recipient, 0);
                return;
            }
            LISTENERS.put(listener, recipient);
        }
    }

    /**
     * Unregister a listener.
     */
    public static void remove(IDhizukuLockTaskModeListener listener) {
        if (listener == null) return;
        IBinder.DeathRecipient recipient;
        synchronized (LOCK) {
            recipient = LISTENERS.remove(listener);
        }
        if (recipient != null) listener.asBinder().unlinkToDeath(recipient, 0);
    }

    /**
     * Notify all registered listeners that the device entered or exited lock task mode.
     *
     * @param entering whether the device is entering lock task mode.
     * @param pkg the package that is using lock task mode (null when exiting).
     */
    public static void notifyLockTaskModeChanged(boolean entering, String pkg) {
        for (IDhizukuLockTaskModeListener listener : snapshot()) {
            try {
                listener.onLockTaskModeChanged(entering, pkg);
            } catch (RemoteException e) {
                // dead listener, remove it (death recipient will do the unlink)
                remove(listener);
            }
        }
    }

    private static List<IDhizukuLockTaskModeListener> snapshot() {
        synchronized (LOCK) {
            return new ArrayList<>(LISTENERS.keySet());
        }
    }
}
