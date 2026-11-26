package com.rosan.dhizuku.api;

/**
 * Reasons a {@link Dhizuku#bindUserService(DhizukuUserServiceArgs, android.content.ServiceConnection, DhizukuUserServiceErrorListener)}
 * request can fail.
 */
public final class DhizukuUserServiceError {
    /**
     * The service did not connect before the timeout elapsed.
     */
    public static final int TIMEOUT = 1;

    /**
     * The server could not start the user service process.
     */
    public static final int START_FAILED = 2;

    /**
     * The user service died after it had been connected.
     */
    public static final int DIED = 3;

    /**
     * The Dhizuku server itself became unreachable.
     */
    public static final int SERVER_DIED = 4;

    /**
     * The request was rejected, for example because the permission was not granted.
     */
    public static final int REJECTED = 5;

    private DhizukuUserServiceError() {
    }

    public static String toString(int reason) {
        switch (reason) {
            case TIMEOUT:
                return "TIMEOUT";
            case START_FAILED:
                return "START_FAILED";
            case DIED:
                return "DIED";
            case SERVER_DIED:
                return "SERVER_DIED";
            case REJECTED:
                return "REJECTED";
            default:
                return "UNKNOWN(" + reason + ")";
        }
    }
}
