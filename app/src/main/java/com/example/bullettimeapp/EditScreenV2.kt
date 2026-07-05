//package com.example.bullettimeapp
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.media.MediaMetadataRetriever
//import android.net.Uri
//import android.util.Log
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.material3.Button
//import androidx.compose.material3.Slider
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.mutableLongStateOf
//import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.media3.common.MediaItem
//import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.ui.PlayerView
//import kotlinx.coroutines.delay
//
//data class BulletEvent(
//    val timeMs: Long,
//    val startCamera: Int,
//    val endCamera: Int,
//    val speedMsPerCamera: Long
//)
//data class PreviewFrame(
//    val camera: Int,
//    val sourceTimeMs: Long
//)
//data class TimelineSegment(
//    val camera: Int,
//    val startMs: Long,
//    val endMs: Long
//)
//sealed class TimelineItem
//
//data class VideoItem(
//    val camera: Int,
//    val startMs: Long,
//    val endMs: Long
//) : TimelineItem()
//
//data class BulletItem(
//    val camera: Int,
//    val sourceTimeMs: Long,
//    val durationMs: Long
//) : TimelineItem()
//@Composable
//fun EditScreenV2(
//    videoPaths: List<String>
//) {
//    if (videoPaths.isEmpty()) {
//
//        Text("영상 없음")
//        return
//    }
//    var playTimeMs by remember {
//        mutableLongStateOf(0L)
//    }
//
//    var isPlaying by remember {
//        mutableStateOf(false)
//    }
//
//    var currentPreviewCamera by remember {
//        mutableIntStateOf(0)
//    }
//    var bulletMode by remember {
//        mutableStateOf(false)
//    }
//
//    var bulletFrameIndex by remember {
//        mutableIntStateOf(0)
//    }
//    var triggeredEventTime by remember {
//        mutableLongStateOf(-1L)
//    }
//
//    val bulletFrames =
//        remember {
//            mutableStateListOf<Bitmap>()
//        }
//    val context = LocalContext.current
//
//    val bulletEvents =
//        remember {
//            mutableStateListOf<BulletEvent>()
//        }
//    val players = remember(videoPaths) {
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
//    val timeline = remember(
//        bulletEvents.size,
//        durationMs
//    ) {
//        buildTimeline(
//            durationMs,
//            bulletEvents
//        )
//    }
//
//
//    Column(modifier = Modifier.fillMaxSize()){
//        if (bulletMode) {
//
//            bulletFrames
//                .getOrNull(bulletFrameIndex)
//
//                ?.let { bmp ->
//
//                    Image(
//                        bitmap = bmp.asImageBitmap(),
//                        contentDescription = null,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(250.dp)
//                    )
//                }
//
//        } else {
//
//            AndroidView(
//                factory = { ctx ->
//                    PlayerView(ctx)
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(250.dp),
//                update = { view ->
//                    view.player =
//                        players.getOrNull(
//                            currentPreviewCamera
//                        )
//                }
//            )
//        }
//        LaunchedEffect(isPlaying) {
//
//            val event =
//                bulletEvents.firstOrNull {
//
//                    playTimeMs >= it.timeMs &&
//                            triggeredEventTime != it.timeMs
//                }
//
//            if (!isPlaying) return@LaunchedEffect
//
//
//            val startTime =
//                System.currentTimeMillis() - playTimeMs
//            if (
//                event != null &&
//                !bulletMode
//            ) {
//
//                bulletMode = true
//
//                bulletFrames.clear()
//
//                for (
//                cam in event.startCamera..
//                        event.endCamera
//                ) {
//
//                    getThumbnail(
//                        context,
//                        videoPaths[cam],
//                        event.timeMs
//                    )?.let {
//
//                        bulletFrames.add(it)
//                    }
//                }
//
//                for (
//                i in bulletFrames.indices
//                ) {
//
//                    bulletFrameIndex = i
//
//                    delay(
//                        event.speedMsPerCamera
//                    )
//                }
//
//                bulletMode = false
//
//                currentPreviewCamera =
//                    event.endCamera
//
//                players[event.endCamera]
//                    .seekTo(event.timeMs)
//
//                players[event.endCamera]
//                    .play()
//                triggeredEventTime =
//                    event.timeMs
//            }
//            while (isPlaying) {
//
//                val player =
//                    players.getOrNull(currentPreviewCamera)
//
//                playTimeMs =
//                    player?.currentPosition ?: 0L
//
//                Log.d(
//                    "TIME",
//                    "playTime=$playTimeMs"
//                )
//
//
//
//                if (
//                    player != null &&
//                    !player.isPlaying &&
//                    player.currentPosition > 0 &&
//                    player.currentPosition >= player.duration - 100
//                ) {
//                    isPlaying = false
//                }
//
//                delay(33)
//            }
//        }
//        Slider(
//            value =
//                playTimeMs.toFloat(),
//
//            onValueChange = {
//
//                playTimeMs = it.toLong()
//            },
//
//            valueRange =
//                0f..durationMs.toFloat()
//        )
//        Button(
//            onClick = {
//
//                bulletEvents.add(
//                    BulletEvent(
//                        timeMs = playTimeMs,
//                        startCamera = 0,
//                        endCamera = videoPaths.lastIndex,
//                        speedMsPerCamera = 1000
//                    )
//                )
//            }
//        ) {
//            Text("ADD EVENT")
//        }
//
//
//        timeline.forEach {
//
//            when (it) {
//
//                is VideoItem -> {
//
//                    Text(
//                        "VIDEO Cam${it.camera} " +
//                                "${it.startMs}~${it.endMs}"
//                    )
//                }
//
//                is BulletItem -> {
//
//                    Text(
//                        "BULLET Cam${it.camera} " +
//                                "@${it.sourceTimeMs}"
//                    )
//                }
//            }
//        }
//        Button(
//            onClick = {
//
//                if (!isPlaying) {
//
//
//
//                    val player =
//                        players.getOrNull(currentPreviewCamera)
//
//                    if (!isPlaying) {
//
//                        if (
//                            player != null &&
//                            player.currentPosition >=
//                            player.duration - 100
//                        ) {
//                            player.seekTo(0)
//                        }
//
//                        player?.play()
//
//                    } else {
//
//                        player?.pause()
//                    }
//
//                    Log.d(
//                        "PLAYER",
//                        "pos=${player?.currentPosition} dur=${player?.duration}"
//                    )
//
//                } else {
//
//                    players
//                        .getOrNull(currentPreviewCamera)
//                        ?.pause()
//                }
//
//                isPlaying = !isPlaying
//            }
//        ) {
//            Text(
//                if (isPlaying)
//                    "STOP"
//                else
//                    "PLAY"
//            )
//        }
//        Text(
//            "Camera = $currentPreviewCamera"
//        )
//
//        Text(
//            "Time = $playTimeMs"
//        )
//
//        Text(
//            "Events = ${bulletEvents.size}"
//        )
//
//    }
//
//}
//
//fun getPreviewFrame(
//    playTimeMs: Long,
//    events: List<BulletEvent>
//): PreviewFrame {
//
//    val event =
//        events.firstOrNull {
//
//            val duration =
//                (it.endCamera -
//                        it.startCamera + 1) *
//                    it.speedMsPerCamera
//
//            playTimeMs in
//                    it.timeMs until
//                    (it.timeMs + duration)
//        }
//
//    if (event != null) {
//
//        val offset =
//            playTimeMs - event.timeMs
//
//        val camOffset =
//            (offset / event.speedMsPerCamera)
//                .toInt()
//
//        val cam =
//            event.startCamera + camOffset
//
//        return PreviewFrame(
//            camera = cam,
//            sourceTimeMs = event.timeMs
//        )
//    }
//
//    return PreviewFrame(
//        camera = 0,
//        sourceTimeMs = playTimeMs
//    )
//}
//fun buildTimeline(
//    durationMs: Long,
//    events: List<BulletEvent>
//): List<TimelineSegment> {
//
//    if (events.isEmpty()) {
//
//        return listOf(
//            TimelineSegment(
//                camera = 0,
//                startMs = 0,
//                endMs = durationMs
//            )
//        )
//    }
//
//    val event = events.first()
//
//    return listOf(
//
//        TimelineSegment(
//            camera = 0,
//            startMs = 0,
//            endMs = event.timeMs
//        ),
//
//        TimelineSegment(
//            camera = event.endCamera,
//            startMs = event.timeMs,
//            endMs = durationMs
//        )
//    )
//}
//fun getThumbnail(
//    context: Context,
//    videoPath: String,
//    timeMs: Long
//): Bitmap? {
//
//    return try {
//
//        val retriever =
//            MediaMetadataRetriever()
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