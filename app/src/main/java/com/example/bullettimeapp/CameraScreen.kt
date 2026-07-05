package com.example.bullettimeapp

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner



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
                "SYNC",
                "실제 녹화 시작 ${System.currentTimeMillis()}"
            )

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
