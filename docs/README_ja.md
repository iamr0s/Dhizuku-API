# Dhizuku API

[English](README.md) | [简体中文](README_zh_rCN.md) | 日本語

Dhizuku API は [Dhizuku](https://github.com/iamr0s/Dhizuku) の API です。  
Dhizuku で端末所有者を共有するために使用されます。

### 実装

![Maven Central](https://img.shields.io/maven-central/v/io.github.iamr0s/Dhizuku-API)

```groovy
def dhizuku_version = "version of api"
implementation "io.github.iamr0s:Dhizuku-API:$dhizuku_version"
```

### 初期化

Dhizuku-API を初期化し、他の API インターフェイスを呼び出せます。  
初期化が失敗すると例外がスローされる場合があります。

```java
Dhizuku.init(context) // return boolean
```

### 権限の要求

一部の API インターフェイスには実行権限が必要です。

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

### APIインターフェース

### Binder Wrapper

IBinder は、アプリとシステム間の基本的な通信によく使用されます。  
Dhizuku は、バインダプロキシ のインターフェイスを提供し、アプリは Dhizuku としてシステムと通信できます。

[Binder Wrapper のデモ](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-binder_wrapper)

### User Service

```java
Dhizuku.getVersionCode() >= 3
```

Dhizuku が提供する隔離空間で動作する AIDL 機構に基づくシンプルなサービス。

使用方法：AIDL ファイルを宣言し、サービスに実装し、Dhizuku が提供するインターフェイスを通じて起動します。

[User Service のデモ](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-user_service)

### Delegated Scopes

```java
Dhizuku.getVersionCode() >= 5
```

**Binder Wrapper** または **User Service** から DevicePolicyManager を呼び出すのは複雑になる場合がありますが、**Delegated Scopes** を使用するとこの操作を簡素化できます。

使用方法：DevicePolicyManager を通じて Delegated Scopes 自体に含まれるインターフェイスを呼び出すことができます。

[DelegatedScopes のデモ](https://github.com/iamr0s/Dhizuku-API/blob/main/demo-delegated_scopes)
