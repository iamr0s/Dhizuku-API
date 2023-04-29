package com.rosan.dhizuku.demo

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import com.rosan.dhizuku.demo.ui.theme.DhizukuAPITheme

class MainActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DhizukuAPITheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val value = remember {
                        mutableStateOf("demo")
                    }
                    val outValue = remember {
                        mutableStateOf("")
                    }
                    val text by value
                    var out by outValue
                    val onClick = { name: String ->
                        out = ""
                        val exception = kotlin.runCatching {
                            when (name) {
                                "init" -> {
                                    toast("init: ${Dhizuku.init(this)}")
                                }

                                "request_permission" -> {
                                    Dhizuku.requestPermission(object :
                                        DhizukuRequestPermissionListener() {
                                        override fun onRequestPermission(grantResult: Int) {
                                            toast(
                                                if (grantResult == PackageManager.PERMISSION_GRANTED) "ok, permission granted"
                                                else ">_<, permission denied"
                                            )
                                        }
                                    })
                                }

                                "shell" -> {

                                    val process =
                                        Dhizuku.newProcess(
                                            text.split(" ").toTypedArray(),
                                            null,
                                            null
                                        )
                                    val code = process.waitFor()
                                    val input = process.inputStream.readBytes().decodeToString()
                                    val error = process.errorStream.readBytes().decodeToString()
                                    out = "code: $code\ninput: $input\nerror: $error"
                                }

                                "binder" -> {
                                    val packageInstaller = packageManager.packageInstaller
                                    PackageInstallerHelper.proxy(packageInstaller)
                                    val pending = PendingIntent.getBroadcast(
                                        this, 1, Intent(),
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        else PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    packageInstaller.uninstall(text, pending.intentSender)
                                    toast("请求卸载：$text")
                                }
                            }
                        }.exceptionOrNull()?.stackTraceToString()
                        if (exception != null) out = exception
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            TextField("shell or PackageName", value)
                        }
                        item {
                            InitButton(onClick)
                        }
                        item {
                            RequestPermissionButton(onClick)
                        }
                        item {
                            ShellButton(value, onClick)
                        }
                        item {
                            BinderWrapperButton(value, onClick)
                        }
                        item {
                            TextField("output", outValue)
                        }
                    }
                }
            }
        }
    }

    fun toast(text: String) {
        runOnUiThread {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextField(name: String, value: MutableState<String>) {
    var text by value
    TextField(
        value = text,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(name)
        },
        onValueChange = {
            text = it
        })
}

@Composable
fun InitButton(onClick: (name: String) -> Unit) {
    Button(onClick = {
        onClick("init")
    }, modifier = Modifier.fillMaxWidth()) {
        Text("init dhizuku, if not will throw exception")
    }
}

@Composable
fun RequestPermissionButton(onClick: (name: String) -> Unit) {
    Button(onClick = {
        onClick("request_permission")
    }, modifier = Modifier.fillMaxWidth()) {
        Text("request dhizuku permission")
    }
}

@Composable
fun ShellButton(value: MutableState<String>, onClick: (name: String) -> Unit) {
    val text by value
    Button(onClick = {
        onClick("shell")
    }, modifier = Modifier.fillMaxWidth()) {
        Text("shell: $text")
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BinderWrapperButton(
    value: MutableState<String>,
    onClick: (name: String) -> Unit
) {
    val text by value
    Button(onClick = {
        onClick("binder")
    }, modifier = Modifier.fillMaxWidth()) {
        Text("uninstall: $text")
    }
}
