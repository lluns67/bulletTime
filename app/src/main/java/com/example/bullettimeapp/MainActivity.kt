package com.example.bullettimeapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // 권한 요청
    val permissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)

    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    ActivityCompat.requestPermissions(
        this,
        permissions.toTypedArray(),
        100
    )

    setContent {

        val context = LocalContext.current

        var remoteStartRecording by remember {
            mutableStateOf(false)
        }

        var remoteStopRecording by remember {
            mutableStateOf(false)
        }

        var isRecordingUi by remember {
            mutableStateOf(false)
        }

        var savedFileName by remember {
            mutableStateOf("")
        }
        var isRecordingMaster by remember {
            mutableStateOf(false)
        }
        var connectionCount by remember {
            mutableStateOf(0)
        }
        var savedVideoUri by remember {
            mutableStateOf("")
        }
        var currentScreen by remember {
            mutableStateOf(Screen.CAMERA)
        }
        var transferProgress by remember {
            mutableIntStateOf(0)
        }

        val receivedVideos = remember {
            mutableStateListOf<String>()
        }

        val videoPaths = buildList {

            if (savedVideoUri.isNotEmpty()) {
                add(savedVideoUri)
            }

            addAll(receivedVideos)
        }

        val nearbyManager = remember {
            NearbyManager(

                context = context,

                onMessageReceived = { message ->

                    Log.d("Nearby", "수신: $message")

                    when {

                        message.startsWith("START_AT:") -> {

                            val targetTime =
                                message.removePrefix("START_AT:")
                                    .toLong()

                            CoroutineScope(Dispatchers.Default).launch {

                                val waitTime =
                                    targetTime - System.currentTimeMillis()

                                if (waitTime > 0) {
                                    delay(waitTime)
                                }

                                withContext(Dispatchers.Main) {
                                    Log.d(
                                        "Remote",
                                        "remoteStartRecording=true"
                                    )
                                    remoteStartRecording = true
                                }
                            }
                        }

                        message.startsWith("STOP_AT:") -> {

                            val targetTime =
                                message.removePrefix("STOP_AT:")
                                    .toLong()

                            CoroutineScope(Dispatchers.Default).launch {

                                val waitTime =
                                    targetTime - System.currentTimeMillis()

                                if (waitTime > 0) {
                                    delay(waitTime)
                                }

                                withContext(Dispatchers.Main) {
                                    remoteStopRecording = true
                                }
                            }
                        }
                    }
                },

                onConnectionChanged = { count ->

                    connectionCount = count

                    Log.d(
                        "Nearby",
                        "현재 연결 수: $count"
                    )
                },
                onFileReceived = { path ->

                    Log.d(
                        "Nearby",
                        "파일 도착: $path"
                    )

                    receivedVideos.add(path)
                },
                onTransferProgress = {
                    transferProgress = it
                }
            )
        }
        when (currentScreen) {

            Screen.CAMERA -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("remote=$remoteStartRecording")
                    CameraScreen(

                        remoteStartRecording = remoteStartRecording,
                        remoteStopRecording = remoteStopRecording,

                        onRecordingStateChanged = {
                            isRecordingUi = it
                            isRecordingMaster = it
                        },

                        onSaved = { uri ->

                            savedVideoUri = uri

                            Log.d(
                                "Camera",
                                "저장됨: $uri"
                            )
                        },

                        onRemoteStartConsumed = {
                            remoteStartRecording = false
                        },

                        onRemoteStopConsumed = {
                            remoteStopRecording = false
                        }
                    )
                    //상태표시
                    Text(
                        when {
                            isRecordingUi ->
                                "🔴 녹화중..."

                            savedFileName.isNotEmpty() ->
                                "✅ 저장 완료\n$savedFileName"

                            else ->
                                "대기중"
                        }
                    )
                    Text(
                        text = "연결 수 : $connectionCount"
                    )
                    Text(
                        "연결된 기기: $connectionCount"
                    )
                    Button(
                        onClick = {

                            if (savedVideoUri.isNotEmpty()) {

                                nearbyManager.sendVideoUri(
                                    context,
                                    savedVideoUri
                                )
                            }
                        }
                    ) {
                        Text("파일 전송")
                    }
                    Button(
                        onClick = {
                            nearbyManager.startAdvertising()
                        }
                    ) {
                        Text("마스터 시작")
                    }

                    Button(
                        onClick = {
                            nearbyManager.startDiscovery()
                        }
                    ) {
                        Text("클라이언트 시작")
                    }

                    Button(
                        onClick = {

                            if (!isRecordingMaster) {

                                val startTime =
                                    System.currentTimeMillis() + 5000

                                nearbyManager.sendStartCommand(startTime)

                                CoroutineScope(Dispatchers.Default).launch {

                                    val waitTime =
                                        startTime - System.currentTimeMillis()

                                    if (waitTime > 0) {
                                        delay(waitTime)
                                    }

                                    withContext(Dispatchers.Main) {
                                        remoteStartRecording = true
                                    }
                                }

                                isRecordingMaster = true

                            } else {

                                val stopTime =
                                    System.currentTimeMillis() + 3000

                                nearbyManager.sendStopCommand(stopTime)

                                CoroutineScope(Dispatchers.Default).launch {

                                    val waitTime =
                                        stopTime - System.currentTimeMillis()

                                    if (waitTime > 0) {
                                        delay(waitTime)
                                    }

                                    withContext(Dispatchers.Main) {
                                        remoteStopRecording = true
                                    }
                                }

                                isRecordingMaster = false
                            }
                        }
                    ) {
                        Text(
                            if (isRecordingMaster)
                                "촬영 종료"
                            else
                                "촬영 시작"
                        )
                    }



                    Button(
                        onClick = {

                            val videoPaths = buildList {

                                if (savedVideoUri.isNotEmpty()) {
                                    add(savedVideoUri)
                                }

                                addAll(receivedVideos)
                            }

                            Log.d(
                                "EDITOR",
                                "videoPaths = $videoPaths"
                            )
                            Log.d(
                                "EDITOR",
                                "receivedVideos = $receivedVideos"
                            )

                            currentScreen = Screen.EDITOR
                        }
                    ) {
                        Text("편집하기")
                    }

                    Text(
                        text = "전송률 : $transferProgress%"

                    )
                    LinearProgressIndicator(
                        progress = { transferProgress / 100f }
                    )


                }
            }
            Screen.EDITOR -> {

                val videoPaths = buildList {

                    if (savedVideoUri.isNotEmpty()) {
                        add(savedVideoUri)
                    }

                    addAll(receivedVideos)
                }
                Log.d("dummy",videoPaths.toString())
                BulletPreviewTest(
                    videoPaths = videoPaths
//                    videoPaths = listOf("content://media/external/video/media/1000002131", "content://media/external/video/media/1000002131","content://media/external/video/media/1000002131")
                )
            }
        }
    }
}
}

