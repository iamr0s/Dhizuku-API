# Dhizuku API

English | [简体中文](README_zh_rCN.md) | [日本語](README_ja.md)

Dhizuku API is the api of [Dhizuku](https://github.com/iamr0s/Dhizuku).  
Used to share device owner from Dhizuku.

### Import

![Maven Central](https://img.shields.io/maven-central/v/io.github.iamr0s/Dhizuku-API)

```groovy
def dhizuku_version = "Version of API"
implementation "io.github.iamr0s:Dhizuku-API:$dhizuku_version"
```

### Initialize

Initialize the Dhizuku-API, call other API interfaces may throws an exception when initialization fails.

```java
Dhizuku.init(context); // return boolean
```

### Request Permission

Some API interfaces require permission to run.

```java

if (Dhizuku.isPermissionGranted()) return;

Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
    @Override
    public void onRequestPermission(int grantResult) throws RemoteException {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            // do success code
        } else {
            // do failure code
        }
    }
});
```

## Use the API

### Binder wrapper

Create file `YOUR_PROJECT/app/src/main/java/android/app/admin/IDevicePolicyManager.java`

```java
package android.app.admin;

import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

import androidx.annotation.Keep;

// If you build the app without shrinking, you can remove the @Keep annotation
@Keep
public interface IDevicePolicyManager extends IInterface {
    @Keep
    abstract class Stub extends Binder implements IDevicePolicyManager {
        public static IDevicePolicyManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
```

Add [HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) dependency: `org.lsposed.hiddenapibypass:hiddenapibypass`

```kotlin
// Enable HiddenApiBypass at any time before you use the binder wrapper.
// It is recommended to call this in Application.onCreate()
HiddenApiBypass.setHiddenApiExemptions("")
```

```kotlin
fun binderWrapperDevicePolicyManager(appContext: Context): DevicePolicyManager? {
    try {
        val context = appContext.createPackageContext(Dhizuku.getOwnerComponent().packageName, Context.CONTEXT_IGNORE_SECURITY)
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val field = manager.javaClass.getDeclaredField("mService")
        field.isAccessible = true
        val oldInterface = field[manager] as IDevicePolicyManager
        if (oldInterface is DhizukuBinderWrapper) return manager
        val oldBinder = oldInterface.asBinder()
        val newBinder = Dhizuku.binderWrapper(oldBinder)
        val newInterface = IDevicePolicyManager.Stub.asInterface(newBinder)
        field[manager] = newInterface
        return manager
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun yourFunction(context: Context) {
    val dpm = binderWrapperDevicePolicyManager(context)
    val component = Dhizuku.getOwnerComponent()
    // Your logic
}
```

### User Service

Enable AIDL feature in your app-level `build.gradle.kts`

```kotlin
android {
    buildFeatures {
        aidl = true
    }
}
```

Create file `YOUR_PROJECT/app/src/main/aidl/com/example/app/IDhizukuUserService.aidl`

```java
package com.example.app;

interface IDhizukuUserService {
    void testFunction();
    // You can add more methods
}
```

Build the app, the corresponding java interface will be generated.

Create file `YOUR_PROJECT/app/src/main/java/android/app/ActivityThread.java`

```java
package android.app;

public class ActivityThread {
    public static ActivityThread currentActivityThread() {
        throw new RuntimeException("STUB");
    }
    
    public Application getApplication() {
        throw new RuntimeException("STUB");
    }
}
```

Create an implementation of `IDhizukuUserService`

```kotlin
package com.example.app

class MyDhizukuUserService: IDhizukuUserService.Stub() {
    override fun testFunction() {
        val context = ActivityThread.currentActivityThread().application
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        dpm.lockNow()
    }
}
```

Define a ServiceConnection

```kotlin
class MyDhizukuServiceConnection: ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val service = IDhizukuUserService.Stub.asInterface(binder)
        service.testFunction()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        println("Disconnected")
    }
}
```

Bind the service

```kotlin
Dhizuku.bindUserService(
    DhizukuUserServiceArgs(ComponentName(context, MyDhizukuUserService::class.java)), MyDhizukuServiceConnection()
)
```
