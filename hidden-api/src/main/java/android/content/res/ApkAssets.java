package android.content.res;

import android.content.res.loader.AssetsProvider;

import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.IOException;

/** @noinspection unused*/
public class ApkAssets {
    /** @noinspection RedundantThrows*/
    public static @Nullable ApkAssets loadFromPath(String path) throws IOException {
        return null;
    }

    /** @noinspection RedundantThrows*/
    public static @Nullable ApkAssets loadFromPath(String path, int flags) throws IOException {
        return null;
    }

    /** @noinspection RedundantThrows*/
    public static @Nullable ApkAssets loadFromPath(String path, int flags,
                                                   AssetsProvider assets) throws IOException {
        return null;
    }

    /** @noinspection RedundantThrows*/
    public static @Nullable ApkAssets loadFromFd(FileDescriptor fd,
                                                 String friendlyName,
                                                 int flags,
                                                 AssetsProvider assets) throws IOException {
        return null;
    }

    /** @noinspection RedundantThrows*/
    public static @Nullable ApkAssets loadFromFd(FileDescriptor fd,
                                                 String friendlyName,
                                                 long offset,
                                                 long length,
                                                 int flags,
                                                 AssetsProvider assets)
            throws IOException {
        return null;
    }

    public static @Nullable ApkAssets loadFromFd(FileDescriptor fd,
                                                 String friendlyName, boolean system, boolean forceSharedLibrary) {
        return null;
    }

    /** @noinspection RedundantThrows*/
    public static @Nullable ApkAssets loadOverlayFromPath(String idmapPath,
                                                          int flags) throws IOException {
        return null;
    }
}
