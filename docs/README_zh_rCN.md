# Dhizuku API

简体中文 | [English](README.md)

Dhizuku API 是 [Dhizuku](https://github.com/iamr0s/Dhizuku) 的 API。

## 导入


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
            }else {
                // do failure code
            }
        }
    });
```

### 接口与更改记录

### Binder Wrapper

IBinder 常用于普通应用与系统的基础通信方式。Dhizuku 提供一个接口用于代理 IBinder，应用可以通过此接口以 Dhizuku 的身份与系统通信。

[BinderWrapper Demo](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-binder_wrapper)

### User Service

> Dhizuku.getVersionCode() >= 3

一个基于 AIDL 机制的简易 Service，Service 运行于 Dhizuku 提供的隔离空间。

通常的用法是先声明一个 AIDL 文件，再于 Service 中将其实现，随后通过 Dhizuku 提供的接口将其启动。

[UserService Demo](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-user_service)

### Delegated Scopes

> Dhizuku.getVersionCode() >= 5

不论是在 Binder Wrapper 还是 User Service 调用 DevicePolicyManager 都显得颇为复杂，通过 Delegated Scopes 可以简化这个操作。

通常的用法是给应用授予 Delegated Scopes，随后应用可以自行通过 DevicePolicyManager 调用被纳入 Delegated Scopes 的接口。

[DelegatedScopes Demo](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-delegated_scopes)
