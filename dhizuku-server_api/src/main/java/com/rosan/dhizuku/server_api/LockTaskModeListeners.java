package com.rosan.dhizuku.server_api;

import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import com.rosan.dhizuku.aidl.IDhizukuLockTaskModeListener;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Registry of lock task mode listeners that are provided by clients.
 * <p>
 * The Dhizuku Server app should call {@link #notifyLockTaskModeChanged(boolean, String)}
 * from its {@code DeviceAdminReceiver.onLockTaskModeEntering} /
 * {@code DeviceAdminReceiver.onLockTaskModeExiting} so that all registered clients
 * receive lock task mode events.
 */
public final class LockTaskModeListeners {
    /**
     * Called whenever the number of registered listeners changes.
     * The server app can use this to keep its process alive while clients are listening.
     */
    public interface ListenerCountCallback {
        void onListenerCountChanged(int count);
    }

    private static final Object LOCK = new Object();

    private static volatile ListenerCountCallback sCountCallback;

    private static final class Entry {
        final IBinder binder;
        final IDhizukuLockTaskModeListener listener;
        final IBinder.DeathRecipient recipient;

        Object frozenCallback;
        boolean frozen;
        boolean hasPending;
        boolean pendingEntering;
        String pendingPkg;

        Entry(IBinder binder, IDhizukuLockTaskModeListener listener, IBinder.DeathRecipient recipient) {
            this.binder = binder;
            this.listener = listener;
            this.recipient = recipient;
        }
    }

    /**
     * Keyed by binder: AIDL creates a new Proxy for every call, so the interface
     * instance cannot be used to deduplicate or remove registrations.
     */
    private static final Map<IBinder, Entry> LISTENERS = new LinkedHashMap<>();

    private LockTaskModeListeners() {
    }

    public static void setListenerCountCallback(ListenerCountCallback callback) {
        sCountCallback = callback;
    }

    public static int getListenerCount() {
        synchronized (LOCK) {
            return LISTENERS.size();
        }
    }

    /**
     * Register a listener. Registering the same listener twice is a no-op.
     * Dead listeners are removed automatically.
     */
    public static void add(IDhizukuLockTaskModeListener listener) {
        if (listener == null) return;
        IBinder binder = listener.asBinder();
        if (binder == null) return;

        synchronized (LOCK) {
            if (LISTENERS.containsKey(binder)) return;
        }

        IBinder.DeathRecipient recipient = () -> remove(binder);
        try {
            binder.linkToDeath(recipient, 0);
        } catch (RemoteException ignored) {
            return;
        }

        Entry entry = new Entry(binder, listener, recipient);
        synchronized (LOCK) {
            if (LISTENERS.containsKey(binder)) {
                try {
                    binder.unlinkToDeath(recipient, 0);
                } catch (RuntimeException ignored) {
                }
                return;
            }
            LISTENERS.put(binder, entry);
        }
        registerFrozenCallback(entry);
        notifyCountChanged();
    }

    /**
     * Unregister a listener.
     */
    public static void remove(IDhizukuLockTaskModeListener listener) {
        if (listener == null) return;
        IBinder binder = listener.asBinder();
        if (binder == null) return;
        remove(binder);
    }

    private static void remove(IBinder binder) {
        Entry entry;
        synchronized (LOCK) {
            entry = LISTENERS.remove(binder);
        }
        if (entry == null) return;
        try {
            binder.unlinkToDeath(entry.recipient, 0);
        } catch (RuntimeException ignored) {
        }
        unregisterFrozenCallback(entry);
        notifyCountChanged();
    }

    /**
     * Notify all registered listeners that the device entered or exited lock task mode.
     *
     * @param entering whether the device is entering lock task mode.
     * @param pkg the package that is using lock task mode (null when exiting).
     */
    public static void notifyLockTaskModeChanged(boolean entering, String pkg) {
        for (Entry entry : snapshot()) {
            synchronized (LOCK) {
                if (LISTENERS.get(entry.binder) != entry) continue;
                if (entry.frozen) {
                    entry.hasPending = true;
                    entry.pendingEntering = entering;
                    entry.pendingPkg = pkg;
                    continue;
                }
            }
            deliver(entry, entering, pkg);
        }
    }

    private static void deliver(Entry entry, boolean entering, String pkg) {
        if (!entry.binder.isBinderAlive()) {
            remove(entry.binder);
            return;
        }
        try {
            entry.listener.onLockTaskModeChanged(entering, pkg);
        } catch (RemoteException | RuntimeException ignored) {
            remove(entry.binder);
        }
    }

    private static void notifyCountChanged() {
        ListenerCountCallback callback = sCountCallback;
        if (callback == null) return;
        int count;
        synchronized (LOCK) {
            count = LISTENERS.size();
        }
        try {
            callback.onListenerCountChanged(count);
        } catch (RuntimeException ignored) {
        }
    }

    private static List<Entry> snapshot() {
        synchronized (LOCK) {
            return new ArrayList<>(LISTENERS.values());
        }
    }

    private static void registerFrozenCallback(Entry entry) {
        if (FrozenState.ADD == null) return;
        try {
            Object callback = Proxy.newProxyInstance(
                    FrozenState.CALLBACK_CLASS.getClassLoader(),
                    new Class<?>[]{FrozenState.CALLBACK_CLASS},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "onFrozenStateChanged":
                                onFrozenStateChanged(entry, (Integer) args[1]);
                                return null;
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            case "toString":
                                return "LockTaskModeListeners.FrozenStateCallback";
                            default:
                                return null;
                        }
                    });
            FrozenState.ADD.invoke(entry.binder, (Executor) Runnable::run, callback);
            synchronized (LOCK) {
                if (LISTENERS.get(entry.binder) != entry) {
                    unregisterFrozenCallback(entry.binder, callback);
                    return;
                }
                entry.frozenCallback = callback;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void unregisterFrozenCallback(Entry entry) {
        Object callback;
        synchronized (LOCK) {
            callback = entry.frozenCallback;
            entry.frozenCallback = null;
        }
        if (callback != null) unregisterFrozenCallback(entry.binder, callback);
    }

    private static void unregisterFrozenCallback(IBinder binder, Object callback) {
        if (FrozenState.REMOVE == null) return;
        try {
            FrozenState.REMOVE.invoke(binder, callback);
        } catch (Throwable ignored) {
        }
    }

    private static void onFrozenStateChanged(Entry entry, int state) {
        boolean deliver = false;
        boolean entering = false;
        String pkg = null;
        synchronized (LOCK) {
            if (LISTENERS.get(entry.binder) != entry) return;
            entry.frozen = state == FrozenState.STATE_FROZEN;
            if (!entry.frozen && entry.hasPending) {
                deliver = true;
                entering = entry.pendingEntering;
                pkg = entry.pendingPkg;
                entry.hasPending = false;
                entry.pendingPkg = null;
            }
        }
        if (deliver) deliver(entry, entering, pkg);
    }

    private static final class FrozenState {
        static final Class<?> CALLBACK_CLASS;
        static final Method ADD;
        static final Method REMOVE;
        static final int STATE_FROZEN;

        static {
            Class<?> callbackClass = null;
            Method add = null;
            Method remove = null;
            int frozen = -1;
            if (Build.VERSION.SDK_INT >= 36) {
                try {
                    callbackClass = Class.forName("android.os.IBinder$FrozenStateChangeCallback");
                    add = IBinder.class.getMethod("addFrozenStateChangeCallback",
                            Executor.class, callbackClass);
                    remove = IBinder.class.getMethod("removeFrozenStateChangeCallback", callbackClass);
                    frozen = callbackClass.getField("STATE_FROZEN").getInt(null);
                } catch (Throwable ignored) {
                    callbackClass = null;
                    add = null;
                    remove = null;
                }
            }
            CALLBACK_CLASS = callbackClass;
            ADD = add;
            REMOVE = remove;
            STATE_FROZEN = frozen;
        }
    }
}
