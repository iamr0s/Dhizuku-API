# Rules applied automatically to apps that depend on Dhizuku-SERVER_API.

# UserService is started in an isolated process through its ComponentName and is then
# instantiated reflectively via getConstructor(Context.class). Its name and constructor
# must not be removed or renamed.
-keep class com.rosan.dhizuku.server_api.UserService {
    public <init>(android.content.Context);
}

# AIDL interfaces are exchanged between processes. Keep their names and members so the
# interface descriptors and transaction codes stay stable.
-keep class com.rosan.dhizuku.server_api.IUserServiceManager { *; }
-keep class com.rosan.dhizuku.server_api.IUserServiceManager$* { *; }
-keep class com.rosan.dhizuku.aidl.** { *; }

# DhizukuProvider is subclassed by the server app and declared in its manifest.
-keep public class * extends com.rosan.dhizuku.server_api.DhizukuProvider {
    public <init>();
}
