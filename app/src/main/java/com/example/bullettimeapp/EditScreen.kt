//package com.example.bullettimeapp
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.media.MediaMetadataRetriever
//import android.net.Uri
//import android.util.Log
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.gestures.detectDragGestures
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableFloatStateOf
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.mutableLongStateOf
//import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.layout.onSizeChanged
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.media3.common.MediaItem
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.ui.PlayerView
//
//data class TimelineFrame(
//    val timeMs: Long,
//    var selectedCamera: Int
//)
//data class CameraVideo(
//    val cameraId: Int,
//    val path: String
//)
//data class PreviewState(
//    val camera: Int,
//    val sourceTimeMs: Long
//)
//
//data class BulletEvent(
//    val timeMs: Long,
//    val startCamera: Int,
//    val endCamera: Int,
//    val speedMsPerCamera: Long
//)
//data class VideoCut(
//    val camera: Int,
//    val startMs: Long,
//    val endMs: Long
//)
//data class RenderFrame(
//    val bitmap: Bitmap,
//    val durationMs: Long
//)
//
//@Composable
//fun EditScreen(
//    videoPaths: List<String>
//) {
//    if (videoPaths.isEmpty()) {
//
//        Text("영상 없음")
//        return
//    }
//
//
//
//
//    var popupFrameIndex by remember {
//        mutableStateOf<Int?>(null)
//    }
//
//    var popupSelectedCamera by remember {
//        mutableIntStateOf(0)
//    }
//    var currentPreviewCamera by remember {
//        mutableIntStateOf(0)
//    }
//    var currentTimelineTime by remember {
//        mutableLongStateOf(0L)
//    }
////    var previewState by remember {
////        mutableStateOf(
////            PreviewState(
////                timeMs = 0L,
////                camera = 0
////            )
////        )
////    }
//    var isPlaying by remember {
//        mutableStateOf(false)
//    }
//
//    var playTimeMs by remember {
//        mutableLongStateOf(0L)
//    }
//    var dragAccumulator by remember {
//        mutableFloatStateOf(0f)
//    }
//    var bulletPreviewCamera by remember {
//        mutableIntStateOf(0)
//    }
//    var timelineWidth by remember {
//        mutableFloatStateOf(1f)
//    }
//    var lastTriggeredEventTime by remember {
//        mutableLongStateOf(-1L)
//    }
//    val context = LocalContext.current
//
//    val durationMs = remember(videoPaths) {
//
//        if (videoPaths.isEmpty()) {
//            0L
//        } else {
//
//            val retriever =
//                MediaMetadataRetriever()
//
//            retriever.setDataSource(
//                context,
//                Uri.parse(videoPaths.first())
//            )
//
//            val duration =
//                retriever.extractMetadata(
//                    MediaMetadataRetriever.METADATA_KEY_DURATION
//                )?.toLongOrNull() ?: 0L
//
//            retriever.release()
//
//            duration
//        }
//    }
//    val frameCount =
//        (durationMs / 100L).toInt() + 1
//
//    val timelineFrames = remember(frameCount) {
//
//        MutableList(frameCount) { index ->
//
//            TimelineFrame(
//                timeMs = index * 100L,
//                selectedCamera = 0
//            )
//        }
//    }
//
//
//    val players = remember {
//
//        videoPaths.map { path ->
//
//            ExoPlayer.Builder(context)
//                .build()
//                .apply {
//
//                    setMediaItem(
//                        MediaItem.fromUri(
//                            Uri.parse(path)
//                        )
//                    )
//
//                    prepare()
//                }
//        }
//    }
//    val thumbnail = remember {
//
//        if (videoPaths.isNotEmpty()) {
//
//            getThumbnail(
//                context,
//                videoPaths[0],
//                1000L
//            )
//
//        } else {
//
//            null
//        }
//    }
//    val scope = rememberCoroutineScope()
//    val bulletEvents = remember {
//        mutableStateListOf<BulletEvent>()
//    }
//    val frames = mutableListOf<RenderFrame>()
//    val eventTime = 2000L
//
//
//
//    Column(
//        modifier = Modifier.fillMaxSize()
//    ) {
//
//
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(300.dp)
//                .background(Color.LightGray),
//            contentAlignment = Alignment.Center
//        ) {
//            AndroidView(
//                factory = { ctx ->
//                    PlayerView(ctx)
//                },
//
//                update = { playerView ->
//
//                    val player = players.getOrNull(currentPreviewCamera)
//
//                    playerView.player = player
//
//                    player?.seekTo(currentTimelineTime)
//
//
//                    player?.play()
//
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(300.dp)
//            )
//
//        }
//
//
//        Spacer(
//            modifier = Modifier.height(20.dp)
//        )
//
//        Text(
//            text = "프레임을 길게 눌러보세요"
//        )
//
//        Spacer(
//            modifier = Modifier.height(20.dp)
//        )
//
////        Button(
////            onClick = {
////
////                if (!isPlaying) {
////
////                    // 영상 끝까지 간 상태면 처음부터
////                    if (playTimeMs >= timelineFrames.last().timeMs) {
////                        playTimeMs = 0L
////                    }
////                    players
////                        .getOrNull(currentPreviewCamera)
////                        ?.play()
////                } else {
////
////                    players.forEach {
////                        it.pause()
////                    }
////                }
////
////                isPlaying = !isPlaying
////
////            }
////        ) {
////            Text(
////                if (isPlaying)
////                    "STOP"
////                else
////                    "PLAY"
////            )
////        }
//
//        LaunchedEffect(isPlaying) {
//
//            if (!isPlaying) return@LaunchedEffect
//
//            val startTime = System.currentTimeMillis() - playTimeMs
//                .coerceAtLeast(
//                    timelineFrames.firstOrNull()?.timeMs ?: 0L
//                )
//
//            while (isPlaying) {
//
//                playTimeMs = System.currentTimeMillis() - startTime
//
//                // 마지막 컷 찾기
//                val currentCut = timelineFrames
//                    .lastOrNull { it.timeMs <= playTimeMs }
//                    ?: timelineFrames.first()
//
//                val currentEvent = bulletEvents.firstOrNull {
//
//                    kotlin.math.abs(
//                        it.timeMs - playTimeMs
//                    ) < 50
//                }
//
////                // 카메라 변경
////                if (currentPreviewCamera != currentCut.selectedCamera) {
////                    Log.d(
////                        "SWITCH",
////                        "cam=${currentCut.selectedCamera}, seek=$playTimeMs"
////                    )
////                    currentPreviewCamera =
////                        currentCut.selectedCamera
////
////                    players
////                        .getOrNull(currentPreviewCamera)
////                        ?.seekTo(playTimeMs)
////
////                    players
////                        .getOrNull(currentPreviewCamera)
////                        ?.play()
////                }
////                if ( currentEvent != null &&
////                    currentEvent.timeMs != lastTriggeredEventTime) {
////                    lastTriggeredEventTime =
////                        currentEvent.timeMs
////                    for (
////                    cam in currentEvent.startCamera..
////                            currentEvent.endCamera
////                    ) {
////                        Log.d(
////                            "BULLET",
////                            "switch to cam $cam"
////                        )
////
////                        currentPreviewCamera = cam
////
////                        players
////                            .getOrNull(cam)
////                            ?.seekTo(currentEvent.timeMs)
////
////                        kotlinx.coroutines.delay(
////                            currentEvent.speedMsPerCamera
////                        )
////                    }
////                    val lastCam = currentEvent.endCamera
////
////                    currentPreviewCamera = lastCam
////
////                    players[lastCam].seekTo(
////                        currentEvent.timeMs
////                    )
////
////                    players[lastCam].play()
////                }
//
//
////                players
////                    .getOrNull(currentPreviewCamera)
////                    ?.seekTo(playTimeMs)
//
//                kotlinx.coroutines.delay(16L) // ~60fps
//                if (playTimeMs > timelineFrames.last().timeMs) {
//
//                    isPlaying = false
//
//                    break
//                }
//            }
//        }
//        Text(
//            "Camera: $currentPreviewCamera"
//        )
//        Text(
//            "playTime = $playTimeMs"
//        )
//        Row {
//
//            Button(
//                onClick = {
//                    isPlaying = !isPlaying
//                }
//            ) {
//                Text(if (isPlaying) "STOP" else "PLAY")
//            }
//
//            Spacer(
//                modifier = Modifier.width(8.dp)
//            )
//
//            Button(
//                onClick = {
//
//                    bulletEvents.add(
//                        BulletEvent(
//                            timeMs = currentTimelineTime,
//                            startCamera = 0,
//                            endCamera = videoPaths.lastIndex,
//                            speedMsPerCamera = 1000
//                        )
//                    )
//                }
//            ) {
//                Text(
//                    text = "events = ${bulletEvents.size}"
//                )
//            }
//        }
//
////        val bulletThumb = remember(
////            bulletPreviewCamera,
////            previewState.timeMs
////        ) {
////
////            getThumbnail(
////                context,
////                videoPaths[bulletPreviewCamera],
////                previewState.timeMs
////            )
////        }
////        bulletThumb?.let {
////            Text(
////                text = "Cam$bulletPreviewCamera",
////                fontSize = 30.sp
////            )
////            Image(
////                bitmap = it.asImageBitmap(),
////                contentDescription = null,
////                modifier = Modifier
////                    .fillMaxWidth()
////                    .height(100.dp)
////            )
////        }
////        Button(
////
////            onClick = {
////
////                scope.launch {
////
////                    for (cam in videoPaths.indices) {
////
////                        bulletPreviewCamera = cam
////
////                        delay(100)
////                    }
////                }
////            }
////        ) {
////
////            Text("Bullet Test")
////        }
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(80.dp)
//                .padding(horizontal = 16.dp)
//                .background(Color.DarkGray)
//                .onSizeChanged {
//                    timelineWidth = it.width.toFloat()
//                }
//                .pointerInput(durationMs, timelineWidth) {
//
//                    detectTapGestures { offset ->
//
//                        val progress =
//                            (offset.x / timelineWidth)
//                                .coerceIn(0f, 1f)
//
//                        currentTimelineTime =
//                            (durationMs * progress).toLong()
//
//                        players
//                            .getOrNull(currentPreviewCamera)
//                            ?.seekTo(currentTimelineTime)
//                    }
//                }
//                .pointerInput(durationMs, timelineWidth) {
//
//                    detectDragGestures(
//
//                        onDragStart = { offset ->
//
//                            val progress =
//                                (offset.x / timelineWidth)
//                                    .coerceIn(0f, 1f)
//
//                            currentTimelineTime =
//                                (durationMs * progress).toLong()
//
//                            players
//                                .getOrNull(currentPreviewCamera)
//                                ?.seekTo(currentTimelineTime)
//                        },
//
//                        onDrag = { change, _ ->
//
//                            val progress =
//                                (change.position.x / timelineWidth)
//                                    .coerceIn(0f, 1f)
//
//                            currentTimelineTime =
//                                (durationMs * progress).toLong()
//
////                            previewState = previewState.copy(
////                                timeMs = currentTimelineTime
////                            )
//
//                            players
//                                .getOrNull(currentPreviewCamera)
//                                ?.seekTo(currentTimelineTime)
//                        }
//                    )
//                }
//        ){
//
//        Canvas(
//                modifier = Modifier.fillMaxSize()
//            ) {
//
//                // 타임라인 선
//                drawLine(
//                    color = Color.White,
//                    start = Offset(
//                        0f,
//                        size.height / 2
//                    ),
//                    end = Offset(
//                        size.width,
//                        size.height / 2
//                    ),
//                    strokeWidth = 6f
//                )
//
//                if (durationMs > 0) {
//
//                    val progress =
//                        currentTimelineTime.toFloat() /
//                                durationMs.toFloat()
//
//                    val x =
//                        size.width * progress
//
//                    // 플레이헤드
//                    drawLine(
//                        color = Color.Red,
//                        start = Offset(x, 0f),
//                        end = Offset(x, size.height),
//                        strokeWidth = 8f
//                    )
//                }
//            for (event in bulletEvents) {
//
//                val eventX =
//                    size.width *
//                            (event.timeMs.toFloat() /
//                                    durationMs.toFloat())
//
//                drawCircle(
//                    color = Color.Cyan,
//                    radius = 10f,
//                    center = Offset(
//                        eventX,
//                        size.height / 2
//                    )
//                )
//            }
//            }
//            Button(
//                onClick = {
//
//                    bulletEvents.forEach {
//
//                        Log.d(
//                            "EVENT",
//                            "time=${it.timeMs}"
//                        )
//                    }
//                }
//            ) {
//                Text("BUILD CUTS")
//            }
//        }
//
////        LazyRow(
////            modifier = Modifier.fillMaxWidth()
////        ) {
////
////            itemsIndexed(timelineFrames) { index, frame ->
////
////                Box(
////                    contentAlignment = Alignment.Center
////                ) {
////
////                    Column(
////                        horizontalAlignment =
////                            Alignment.CenterHorizontally
////                    ) {
////
////                        // 길게 누른 프레임이면 카메라 목록 표시
////                        if (popupFrameIndex == index) {
////
////                            Box(
////                                modifier = Modifier
////                                    .offset(y = (-120).dp)
////                                    .background(Color(0xCC000000))
////                            ) {
////
////                                Column(
////                                    horizontalAlignment =
////                                        Alignment.CenterHorizontally
////                                ) {
////
////                                    val startCam =
////                                        (popupSelectedCamera - 1)
////                                            .coerceAtLeast(0)
////
////                                    val endCam =
////                                        (popupSelectedCamera + 1)
////                                            .coerceAtMost(videoPaths.lastIndex)
////
////                                    for (cam in startCam..endCam) {
////
////                                        val thumb = remember(
////                                            cam,
////                                            frame.timeMs
////                                        ) {
////                                            getThumbnail(
////                                                context,
////                                                videoPaths[cam],
////                                                frame.timeMs
////                                            )
////                                        }
////
////                                        thumb?.let { bmp ->
////
////                                            Image(
////                                                bitmap = bmp.asImageBitmap(),
////                                                contentDescription = null,
////                                                modifier = Modifier
////                                                    .width(
////                                                        if (cam == popupSelectedCamera)
////                                                            70.dp
////                                                        else
////                                                            60.dp
////                                                    )
////                                                    .height(
////                                                        if (cam == popupSelectedCamera)
////                                                            55.dp
////                                                        else
////                                                            45.dp
////                                                    ).clickable {
////
////                                                        for (
////                                                        i in index until timelineFrames.size
////                                                        ) {
////
////                                                            timelineFrames[i].selectedCamera =
////                                                                cam
////                                                        }
////
////                                                        currentPreviewCamera = cam
////
////                                                        popupFrameIndex = null
////                                                    }
////                                            )
////
////                                            Text(
////                                                text = "Cam$cam",
////                                                color = Color.White,
////                                                fontSize =
////                                                    if (cam == popupSelectedCamera)
////                                                        14.sp
////                                                    else
////                                                        12.sp
////                                            )
////
////                                            Spacer(
////                                                modifier =
////                                                    Modifier.height(4.dp)
////                                            )
////                                        }
////                                    }
////                                }
////                            }
////                        }
////
////                        Box(
////                            modifier = Modifier
////                                .size(
////                                    width = 80.dp,
////                                    height = 60.dp
////                                )
////                                .background(Color.Gray)
////                                .combinedClickable(
////
////                                    onClick = {
////
////                                        currentPreviewCamera =
////                                            frame.selectedCamera
////
////
////
////                                        previewState = previewState.copy(
////                                            timeMs = frame.timeMs,
////                                            camera = frame.selectedCamera
////                                        )
////                                    },
////
////                                    onLongClick = {
////
////                                        popupFrameIndex = index
////
////                                        popupSelectedCamera =
////                                            frame.selectedCamera
////                                    }
////                                ),
////
////                            contentAlignment =
////                                Alignment.Center
////                        ) {
////                            val thumbnail = remember(
////                                frame.timeMs,
////                                frame.selectedCamera
////                            ) {
////
////                                if (
////                                    videoPaths.isNotEmpty() &&
////                                    index % 5 == 0
////                                ) {
////
////                                    getThumbnail(
////                                        context,
////                                        videoPaths[frame.selectedCamera],
////                                        frame.timeMs
////                                    )
////
////                                } else {
////
////                                    null
////                                }
////                            }
////
////                            Column(
////                                horizontalAlignment =
////                                    Alignment.CenterHorizontally
////                            ) {
////
////                                thumbnail?.let {bmp->
////
////                                    Image(
////                                        bitmap = bmp.asImageBitmap(),
////                                        contentDescription = null,
////
////                                    )
////                                }
////
////                                Text(
////                                    text = "${frame.timeMs / 1000f}s",
////                                    color =
////                                        if (frame.selectedCamera == 0)
////                                            Color.White
////                                        else
////                                            Color.Yellow
////                                )
////                            }
////                        }
////                    }
////                }
////
////
////            }
////
////        }
//
//    }
//}
//
//fun getThumbnail(
//    context: Context,
//    videoPath: String,
//    timeMs: Long
//): Bitmap? {
//
//    return try {
//
//        val retriever = MediaMetadataRetriever()
//
//        retriever.setDataSource(
//            context,
//            Uri.parse(videoPath)
//        )
//
//        val bitmap =
//            retriever.getFrameAtTime(
//                timeMs * 1000,
//                MediaMetadataRetriever.OPTION_CLOSEST
//            )
//
//        retriever.release()
//
//        bitmap
//
//    } catch (e: Exception) {
//
//        e.printStackTrace()
//        null
//    }
//}
//fun getPreviewState(
//    playTimeMs: Long,
//    bulletEvents: List<BulletEvent>
//): PreviewState {
//
//    val event =
//        bulletEvents.firstOrNull {
//            playTimeMs >= it.timeMs &&
//                    playTimeMs <
//                    it.timeMs +
//                    (it.endCamera - it.startCamera + 1) *
//                    it.speedMsPerCamera
//        }
//
//    if (event != null) {
//
//        val offset =
//            playTimeMs - event.timeMs
//
//        val camIndex =
//            (offset / event.speedMsPerCamera)
//                .toInt()
//
//        val cam =
//            (event.startCamera + camIndex)
//                .coerceAtMost(event.endCamera)
//
//        return PreviewState(
//            camera = cam,
//            sourceTimeMs = event.timeMs
//        )
//    }
//
//    return PreviewState(
//        camera = 0,
//        sourceTimeMs = playTimeMs
//    )
//}