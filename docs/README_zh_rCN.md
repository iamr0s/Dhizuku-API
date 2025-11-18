# Dhizuku API

[English](README.md) | 简体中文 | [日本語](README_ja.md)

Dhizuku API 是 [Dhizuku](https://github.com/iamr0s/Dhizuku) 的 API。

## 导入

![Maven Central](https://img.shields.io/maven-central/v/io.github.iamr0s/Dhizuku-API)

```groovy
def dhizuku_version = "Version of API"
implementation "io.github.iamr0s:Dhizuku-API:$dhizuku_version"
```

### 初始化

执行如下代码从而初始化Dhizuku-API，如果初始化失败（Dhizuku不存在、未激活）就开始调用其余API会抛出错误

```java
Dhizuku.init(context); // return boolean
```

### 请求权限

某些API接口需要先请求权限才能运行

```java
if (Dhizuku.isPermissionGranted()) return;

Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
    @Override
    public void onRequestPermission(int grantResult) throws RemoteException {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            ComponentName component = Dhizuku.getOwnerComponent(); // current Dhizuku Server Admin Receiver
            // do success code
        } else {
            // do failure code
        }
    }
});
```


## 使用API

### Binder wrapper

创建文件 `YOUR_PROJECT/app/src/main/java/android/app/admin/IDevicePolicyManager.java`

```java
package android.app.admin;

import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

import androidx.annotation.Keep;

// 如果你构建app时不启用shrink，你可以去掉@Keep注解
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

添加[HiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)依赖： `org.lsposed.hiddenapibypass:hiddenapibypass`

```kotlin
// 在使用binder wrapper前开启绕过隐藏API
// 建议在 Application.onCreate() 中调用此函数
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
    // 你的代码
}
```


### User Service

在app级的`build.gradle.kts`中启用AIDL

```kotlin
android {
    buildFeatures {
        aidl = true
    }
}
```

创建文件`YOUR_PROJECT/app/src/main/aidl/com/example/app/IDhizukuUserService.aidl`

```java
package com.example.app;

interface IDhizukuUserService {
    void testFunction();
    // 你可以添加更多方法
}
```

构建app，会生成对应的java interface。

创建文件`YOUR_PROJECT/app/src/main/java/android/app/ActivityThread.java`

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

实现`IDhizukuUserService`

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

定义一个ServiceConnection

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

绑定服务

```kotlin
Dhizuku.bindUserService(
    DhizukuUserServiceArgs(ComponentName(context, MyDhizukuUserService::class.java)), MyDhizukuServiceConnection()
)
```
