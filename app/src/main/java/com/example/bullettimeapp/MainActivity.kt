package com.example.bullettimeapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.camera.video.*
import java.io.File

import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import androidx.compose.foundation.layout.Column

import android.util.Log
import kotlinx.coroutines.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp


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

                    savedFileName = path
                }
            )
        }

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

                    if(savedVideoUri.isNotEmpty()) {

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

                        remoteStartRecording = true

                        isRecordingMaster = true

                    } else {

                        val stopTime =
                            System.currentTimeMillis() + 3000

                        nearbyManager.sendStopCommand(stopTime)

                        remoteStopRecording = true

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



        }
    }
}
}

@Composable
fun CameraScreen(
    remoteStartRecording: Boolean,
    remoteStopRecording: Boolean,

    onRecordingStateChanged: (Boolean) -> Unit,

    onSaved: (String) -> Unit,

    onRemoteStartConsumed: () -> Unit,
    onRemoteStopConsumed: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        androidx.camera.view.PreviewView(context)
    }

    var videoCapture by remember {
        mutableStateOf<VideoCapture<Recorder>?>(null)
    }

    var isRecordingMaster by remember {
        mutableStateOf(false)
    }


    AndroidView(
        factory = { previewView },
        modifier = Modifier.height(400.dp)
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder().build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
            Log.d(
                "Camera",
                "bind camera"
            )

        }, ContextCompat.getMainExecutor(context))

    }

    // 🎥 녹화 버튼
    var isRecording by remember { mutableStateOf(false) }
    var recording: Recording? by remember { mutableStateOf(null) }

    fun startRecording() {


        Log.d(
            "Camera",
            "startRecording videoCapture=$videoCapture"
        )

        if (videoCapture == null) {
            Log.e("Camera", "videoCapture null")
            return
        }


        if (isRecording) return

        val name = "video_${System.currentTimeMillis()}.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/BulletTimeApp")
            }
        }

        val outputOptions = MediaStoreOutputOptions
            .Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            .setContentValues(contentValues)
            .build()

        val recordingBuilder =
            videoCapture?.output?.prepareRecording(context, outputOptions)

        recording = recordingBuilder
            ?.start(
                ContextCompat.getMainExecutor(context)
            ) { event ->

                if (event is VideoRecordEvent.Finalize) {

                    Log.d(
                        "Camera",
                        "uri=${event.outputResults.outputUri}"
                    )

                    onSaved(
                        event.outputResults.outputUri.toString()
                    )
                }
            }

        isRecording = true
        onRecordingStateChanged(true)
    }
    fun stopRecording() {
        recording?.stop()
        recording = null
        isRecording = false

        onRecordingStateChanged(false)
    }

    LaunchedEffect(remoteStartRecording) {

        Log.d(
            "Camera",
            "LaunchedEffect fired = $remoteStartRecording"
        )
        if (remoteStartRecording) {

            Log.d(
                "Camera",
                "원격 녹화 시작"
            )

            startRecording()

            onRemoteStartConsumed()
        }
    }
    LaunchedEffect(remoteStopRecording) {

        if (remoteStopRecording) {

            Log.d(
                "Camera",
                "원격 녹화 종료"
            )

            stopRecording()

            onRemoteStopConsumed()
        }
    }

    LaunchedEffect(remoteStopRecording) {

        if (remoteStopRecording) {

            Log.d(
                "Camera",
                "원격 녹화 중지"
            )

            stopRecording()

            onRemoteStopConsumed()
        }
    }





}