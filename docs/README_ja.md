# Dhizuku API

[English](README.md) | [简体中文](README_zh_rCN.md) | 日本語

**Dhizuku API** は [**Dhizuku**](https://github.com/iamr0s/Dhizuku) の API です。

## 導入

![Maven Central](https://img.shields.io/maven-central/v/io.github.iamr0s/Dhizuku-API)

```Groovy
def dhizuku_api = "Version of Dhizuku-API"
implementation "io.github.iamr0s:Dhizuku-API:$dhizuku_api"
```

[バージョンカタログ](https://docs.gradle.org/current/userguide/version_catalogs.html) を使用する際は、次の様に書いてください：

`gradle/libs.versions.toml`
```TOML
[versions]
dhizuku-api = "Version of Dhizuku-API"
[libraries]
dhizuku-api = { group = "io.github.iamr0s", name = "Dhizuku-API", version.ref = "dhizuku.api" }
```
`project/build.gradle`
```Groovy
implementation(libs.dhizuku.api)
```

## 連携

現時点では、**AndroidManifest**.xml にサービスの使用権限を宣言する必要はありません。  
しかし、権限名は、`DhizukuVariables.PERMISSION_API` に予約されています。

## 初期化

**Dhizuku-API** を初期化するには、次のコードを実行します：

```Java
Dhizuku.init(context); // return boolean
```

> [!TIP]
> 初期化に失敗した場合 (**Dhizuku** が存在しないか有効化されていない場合)、他の API を呼び出すとエラーが発生します。

Context は必須では無いので、引数は空(**null** ではなく何も入れない)でも動作します。

## 権限の要求

一部の API では実行権限が必要です。

```Java
if (Dhizuku.isPermissionGranted()) return;

Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
    @Override
    public void onRequestPermission(int grantResult) throws RemoteException {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            ComponentName component = Dhizuku.getOwnerComponent(); // Dhizuku アプリの DeviceAdminReceiver
            // 権限昇格成功時のコード
            Toast.makeText(this, "権限は昇格されました", Toast.LENGTH_SHORT).show;
        } else {
            // 権限昇格失敗時のコード
            Toast.makeText(this, "権限の昇格に失敗しました", Toast.LENGTH_SHORT).show;
        }
    }
});
```

## サービスのアンバインド

**Dhizuku v2.9** 以降では、自動でサービスのアンバインドが行われなくなった為、処理の終了時に手動でサービスのストップとアンバインドを行う必要があります。  
この終了処理を行わなかった場合、毎回 **Dhizuku** アプリから手動でサーバーを再起動しない限り、`DeadObjectException` が発生し続けます。

下記の例では、`UserService` が、Dhizuku にバインドするクラス、`IUserService` が、それの **AIDL** とします。

```Java
IUserService mUserService = null;
DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(this, UserService.class));
ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder iBinder) {
        mUserService = IUserService.Stub.asInterface(iBinder);
        makeText("UserService に接続しました");
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
        makeText("UserService から切断されました");
    }
};

// 実行権限の確認...

// 処理の開始
Dhizuku.init();
Dhizuku.bindUserService(args, connection);

// (m)UserService を用いた処理...

// 処理の終了
Dhizuku.stopUserService(args);
Dhizuku.unbindUserService(connection));
```
