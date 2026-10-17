# Rules applied automatically to apps that depend on Dhizuku-API.

# This library is compiled against hidden platform stubs (see the :hidden-api module)
# which are not present in the consumer's android.jar. Ignore the missing classes.
-dontwarn android.app.ActivityThread

# AIDL interfaces are exchanged between processes. Keep their names and members so the
# interface descriptors and transaction codes stay stable.
-keep class com.rosan.dhizuku.aidl.** { *; }
