# Dhizuku API

English | [简体中文](README_zh_rCN.md)

Dhizuku API is the api of [Dhizuku](https://github.com/iamr0s/Dhizuku). Used to share device owner from Dhizuku.

### Import

![Maven Central](https://img.shields.io/maven-central/v/io.github.iamr0s/Dhizuku-API)

```groovy
def dhizuku_version = "version of api"
implementation "io.github.iamr0s:Dhizuku-API:$dhizuku_version"
```

### Initialize

Initialize the dhizuku-API, call other API interfaces may throws an exception when initialization fails.

```java
Dhizuku.init(context) // return boolean
```

### Request Permission

Some API interfaces require permission to run.

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

### API interface and changes

### Binder Wrapper

IBinder is often used for basic communication between applications and systems. Dhizuku provides an interface for proxy IBinder, the applications can communicate with the system as Dhizuku.

[BinderWrapper Demo](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-binder_wrapper)

### User Service

> Dhizuku.getVersionCode() >= 3

A simple Service based on the AIDL mechanism that runs in the isolated space provided by Dhizuku.

Usage: Declare an AIDL file, implement it in Service, and then launch it through the interface provided by Dhizuku.

[UserService Demo](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-user_service)

### Delegated Scopes

> Dhizuku.getVersionCode() >= 5

Invoking the DevicePolicyManager from either the Binder Wrapper or the User Service can be complicated, but Delegated Scopes can simplify this operation.

Usage: You grant Delegated Scopes to your application, which can then invoke the interface included in the Delegated Scopes itself through DevicePolicyManager.

[DelegatedScopes Demo](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-delegated_scopes)
