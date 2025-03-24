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
