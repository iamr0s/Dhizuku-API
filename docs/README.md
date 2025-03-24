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
