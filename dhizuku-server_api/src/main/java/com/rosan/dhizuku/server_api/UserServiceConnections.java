package com.rosan.dhizuku.server_api;

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

    static UserServiceConnection require(int uid, int pid, DhizukuUserServiceArgs args) {
        synchronized (CACHE) {
            String token = requireToken(uid, pid, args);
            UserServiceConnection userServiceConnection = CACHE.get(token);
            if (userServiceConnection == null) {
                userServiceConnection = new UserServiceConnection(uid, pid, args);
                CACHE.put(token, userServiceConnection);
            }
            return userServiceConnection;
        }
    }

    static UserServiceConnection get(int uid, int pid, DhizukuUserServiceArgs args) {
        synchronized (CACHE) {
            return CACHE.get(requireToken(uid, pid, args));
        }
    }

    static void removeToken(int uid, int pid, DhizukuUserServiceArgs args) {
        synchronized (CACHE) {
            CACHE.remove(requireToken(uid, pid, args));
        }
    }

    static void bind(int uid, int pid, DhizukuUserServiceArgs args, IDhizukuUserServiceConnection connection) {
        require(uid, pid, args).bind(connection);
    }

    static void unbind(int uid, int pid, DhizukuUserServiceArgs args) {
        UserServiceConnection userServiceConnection;
        synchronized (CACHE) {
            userServiceConnection = CACHE.remove(requireToken(uid, pid, args));
        }
        if (userServiceConnection != null) userServiceConnection.unbindAll();
    }

    static void unbind(int uid, int pid, DhizukuUserServiceArgs args, IDhizukuUserServiceConnection connection) {
        UserServiceConnection userServiceConnection = get(uid, pid, args);
        if (userServiceConnection != null) userServiceConnection.unbind(connection);
    }

    static void start(int uid, int pid, DhizukuUserServiceArgs args) {
        require(uid, pid, args).start();
    }

    static void stop(int uid, int pid, DhizukuUserServiceArgs args) {
        UserServiceConnection userServiceConnection;
        synchronized (CACHE) {
            userServiceConnection = CACHE.remove(requireToken(uid, pid, args));
        }
        if (userServiceConnection != null) userServiceConnection.stop();
    }
}
