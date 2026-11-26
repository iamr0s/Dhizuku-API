package com.rosan.dhizuku.api;

import android.content.ComponentName;

/**
 * Receives failures that happen while a user service is starting.
 * <p>
 * Register it through
 * {@link Dhizuku#bindUserService(DhizukuUserServiceArgs, android.content.ServiceConnection, long, DhizukuUserServiceErrorListener)}.
 * Unlike {@link android.content.ServiceConnection}, which is only told about a service
 * that did connect, this listener also reports the cases where the service never comes up.
 */
public interface DhizukuUserServiceErrorListener {
    /**
     * @param name the service that failed.
     * @param reason one of the constants in {@link DhizukuUserServiceError}.
     * @param cause the underlying error, or null when there is none.
     */
    void onUserServiceFailed(ComponentName name, int reason, Throwable cause);
}
