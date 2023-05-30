# Dhizuku API

Dhizuku API是[Dhizuku](https://github.com/iamr0s/Dhizuku)的API。你可以通过Dhizuku API调用系统接口，完成共享设备所有者功能（Device Owner）。

### 导入

![Maven Central](https://img.shields.io/maven-central/v/io.github.iamr0s/Dhizuku-API)

```groovy
def dhizuku_version = "version of api"
implementation "io.github.iamr0s:Dhizuku-API:$dhizuku_version"
```

### 初始化

执行如下代码从而初始化Dhizuku-API，如果初始化失败（Dhizuku不存在、未激活）就开始调用其余API会抛出错误

```java
Dhizuku.init(context) // return boolean
```

### 请求权限

某些API接口需要先请求权限才能运行

```java

if (Dhizuku.isPermissionGranted()) return

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

### 使用方法与更改记录

### v2.0 (3)

### 开启一个自定义服务（UserService）

会向Dhizuku服务器发起一个请求，用于开启UserService，类似于Android系统本身的Service，但是是基于AIDL。

具体实现请参考 [Demo UserService](https://github.com/iamr0s/Dhizuku-API/tree/main/demo-user_service)。

如果你用过 [Shizuku](https://shizuku.rikka.app/zh-hans/) 的UserService，那么你应该可以很好的理解这个功能，但是他们之间也存在着一些不同，请仔细阅读下列内容

一般的使用步骤为：
1. 首先自定义一个[UserService AIDL](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-user_service/src/main/aidl/com/rosan/dhizuku/demo_user_service/IUserService.aidl)文件

> 请注意，20及以内的transact code是保留给未来的Dhizuku APi使用的，您不能轻易使用它，你的自定义操作应该从21开始

2. Build你的项目，使得Android Studio生成对应于AIDL的Stub子类

3. 实现这个Stub类型，即 [UserService](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-user_service/src/main/java/com/rosan/dhizuku/demo_user_service/UserService.java)

> 你有两种构造器函数实现可选，即带Context参数或不带，当你两种构造器都存在时，有限选择带构造器的。
>
> 注意：
> 
> 1. Context无法调用Android的四大组件
> 2. UserService的生命周期与发起者有关，当所有发起者的进程处于死亡状态时，UserService自动退出
> 3. 当UserService被Dhizuku关闭或被强制退出时，UserService的onDestory方法不保证一定调用，

4. 享用一个带有设备所有者权限的 Service

### v1.0.1

### 远程执行一段命令（newProcess）

```java
String[] cmd = new String[]{"whoami"};
String[] env = null;
File dir = null;
try {
    Process process = Dhizuku.newProcess(cmd, env, dir);
    process.waitFor();
    InputStream input = process.getInputStream();
    InputStream err = process.getErrorStream();
    byte[] bytes = new byte[input.available()];
    input.read(bytes);
    Log.e("dhizuku-api", "input " + new String(bytes));
    bytes = new byte[err.available()];
    Log.e("dhizuku-api", "error " + new String(bytes));
} catch (InterruptedException | IOException e) {
        throw new RuntimeException(e);
}
```

### 代理一个IBinder（binderWrapper）

应用与应用之间、应用与系统服务之间的交流主要通过IBinder完成，Dhizuku支持代理IBinder。如果你用过[Shizuku](https://shizuku.rikka.app/zh-hans/)的api，我相信你能很好的理解这个接口。

### 方法一

直接使用IBinder、IInterface

```java
IPackageManager packageManager = IPackageManager.Stub.asInterface(Dhizuku.binderWrapper(ServiceManager.getService("package")));
IPackageInstaller packageInstaller = IPackageInstaller.Stub.asInterface(Dhizuku.binderWrapper(packageManager.getPackageInstaller().asBinder()));
// packageInstaller.uninstall(...) // do some code, use the IInterface
```
### 方法二

间接使用IBinder、IInterface

当然我更喜欢这种方法去使用binderWrapper。比如系统的PackageManager本质上是对IPackageManager的封装，因此我们完全可以替换其中的IPackageManager对象为我们代理过的，从而实现几乎无痛代理。

[PackageInstallerHelper.java](https://github.com/iamr0s/Dhizuku-API/blob/main/demo/src/main/java/com/rosan/dhizuku/demo/PackageInstallerHelper.java)

```java
PackageInstaller packageInstaller = context.getPackageManager().getPakageInstaller();
PackageInstallerHelper.proxy(packageInstaller);
// packageInstaller.uninstall(...) // do some code, just like you are Device Owner
```

比如UserManager，也是针对IUserManager的封装，同样的方法替换它的mService为经过代理的。

需要注意，Google在Android P之后对部分接口做出了限制（@hide），禁止普通应用调用（禁止反射），需要通过[AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass)解除限制。
