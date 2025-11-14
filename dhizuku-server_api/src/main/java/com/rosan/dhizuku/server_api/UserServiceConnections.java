package com.rosan.dhizuku.server_api;

import androidx.annotation.NonNull;

import com.rosan.dhizuku.aidl.IDhizukuUserServiceConnection;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class UserServiceConnections {
    static final Map<String, UserServiceConnection> CACHE = Collections.synchronizedMap(new HashMap<>());

    static String requireToken(int uid, int pid, DhizukuUserServiceArgs args) {
        return uid + ":" + pid + "{" + args.flattenToShortString() + "}";
    }

    static void bind(int uid, int pid, @NonNull DhizukuUserServiceArgs args, @NonNull IDhizukuUserServiceConnection connection) {
        synchronized (CACHE) {
            CACHE.computeIfAbsent(requireToken(uid, pid, args), (token) -> new UserServiceConnection(pid, uid, args))
                    .bind(connection);
        }
    }

    static void unbind(int uid, int pid, @NonNull DhizukuUserServiceArgs args) {
        synchronized (CACHE) {
            CACHE.computeIfPresent(requireToken(uid, pid, args), (token, userServiceConnection) -> {
                userServiceConnection.unbindAll();
                return null;
            });
        }
    }

    static void unbind(int uid, int pid, @NonNull DhizukuUserServiceArgs args, @NonNull IDhizukuUserServiceConnection connection) {
        synchronized (CACHE) {
            CACHE.computeIfPresent(requireToken(uid, pid, args), (token, userServiceConnection) -> {
                userServiceConnection.unbind(connection);
                return userServiceConnection;
            });
        }
    }

    static void start(int uid, int pid, @NonNull DhizukuUserServiceArgs args) {
        synchronized (CACHE) {
            CACHE.computeIfAbsent(requireToken(uid, pid, args), (token) -> new UserServiceConnection(uid, pid, args))
                    .start();
        }
    }

    static void stop(int uid, int pid, @NonNull DhizukuUserServiceArgs args) {
        synchronized (CACHE) {
            CACHE.computeIfPresent(requireToken(uid, pid, args), (token, userServiceConnection) -> {
                userServiceConnection.stop();
                return userServiceConnection;
            });
        }
    }
}
