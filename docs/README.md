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

### Check State

These never throw, so they are safe to use when deciding what UI to show.

```java
Dhizuku.init(context);

if (!Dhizuku.isAvailable()) {
    // Dhizuku is not installed, not activated, or not reachable
    return;
}

if (!Dhizuku.isPermissionGranted()) {
    // ask for the permission first
    return;
}

int version = Dhizuku.getVersionCodeOr(0);
```

| Method | Returns |
| --- | --- |
| `isInitialized()` | a live binder has already been obtained |
| `isAvailable()` | Dhizuku is installed, activated and reachable |
| `isPermissionGranted()` | this app may call the privileged APIs |
| `getVersionCodeOr(int)` | the server protocol version, or the fallback |
| `getVersionNameOr(String)` | the server api version name, or the fallback |

### UserService

A UserService runs your own code inside the device owner process, which is how you get
access to the full `DevicePolicyManager` without holding the permission yourself.

```java
DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(
        new ComponentName(context, MyUserService.class));

Dhizuku.bindUserService(args, new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        IMyService remote = IMyService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
});
```

### Handle Start Failures

`bindUserService` only reports that the request reached the server. The service process
may still fail to start, which looks like a connection that never arrives. Pass an error
listener to be told about it.

```java
Dhizuku.bindUserService(args, connection, 15_000, (name, reason, cause) -> {
    switch (reason) {
        case DhizukuUserServiceError.TIMEOUT:
            // the service did not come up in time
            break;
        case DhizukuUserServiceError.START_FAILED:
            // the server could not start the service process
            break;
        case DhizukuUserServiceError.DIED:
            // the service was connected and then died
            break;
        case DhizukuUserServiceError.SERVER_DIED:
            // Dhizuku itself became unreachable
            break;
        case DhizukuUserServiceError.REJECTED:
            // the request was refused, for example by a missing permission
            break;
    }
});
```

Pass a timeout of `0` or less to disable it. A server older than api version 10 does not
report `START_FAILED`, so only the timeout applies there.

### Remote Process

Runs a command inside the device owner process.

```java
DhizukuRemoteProcess process = Dhizuku.newProcess(
        new String[]{"/system/bin/id"}, null, null);
process.waitFor();
```

### Binder Wrapper

Forwards every `transact` call to Dhizuku, so a hidden system service can be called with
the device owner identity.

```java
IBinder binder = Dhizuku.binderWrapper(
        ServiceManager.getService("device_policy"));
IDevicePolicyManager manager = IDevicePolicyManager.Stub.asInterface(binder);
```

### Delegated Scopes

```java
String[] scopes = Dhizuku.getDelegatedScopes();
Dhizuku.setDelegatedScopes(new String[]{"delegated_admin"});
```

### Lock Task Mode

`DeviceAdminReceiver.onLockTaskModeEntering` / `onLockTaskModeExiting` are only delivered
to the device owner. This API forwards them to your app. Requires api version 9 or newer.

```java
DhizukuLockTaskModeListener listener = new DhizukuLockTaskModeListener() {
    @Override
    public void onLockTaskModeChanged(boolean entering, String pkg) {
    }
};

Dhizuku.addLockTaskModeListener(listener);
Dhizuku.removeLockTaskModeListener(listener);
```
