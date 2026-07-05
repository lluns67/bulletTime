package com.example.bullettimeapp

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.io.File
import java.io.FileInputStream

class NearbyManager(
    private val context: Context,
    private val onMessageReceived: (String) -> Unit,
    private val onConnectionChanged: (Int) -> Unit,
    private val onFileReceived: (String) -> Unit,
    private val onTransferProgress: (Int) -> Unit
){
    private var connectionCount = 0
    private val connectedEndpoints =
        mutableSetOf<String>()
    private val connectionsClient =
        Nearby.getConnectionsClient(context)
    private val incomingFiles =
        mutableMapOf<Long, Payload>()

    companion object {
        const val SERVICE_ID =
            "com.example.bullettimeapp"
    }

    fun sendStartCommand(startTime: Long) {

        val message = "START_AT:$startTime"

        connectedEndpoints.forEach { endpointId ->

            connectionsClient.sendPayload(
                endpointId,
                Payload.fromBytes(message.toByteArray())
            )

            Log.d(
                "Nearby",
                "START 전송 -> $endpointId"
            )
        }
    }

    fun sendStopCommand(stopTime: Long) {

        val message = "STOP_AT:$stopTime"

        connectedEndpoints.forEach { endpointId ->

            connectionsClient.sendPayload(
                endpointId,
                Payload.fromBytes(message.toByteArray())
            )

            Log.d(
                "Nearby",
                "STOP 전송 -> $endpointId"
            )
        }
    }
    fun sendVideoUri(
        context: Context,
        uriString: String
    ) {

        try {

            val uri = Uri.parse(uriString)

            val inputStream =
                context.contentResolver.openInputStream(uri)
                    ?: return

            val tempFile = File(
                context.cacheDir,
                "video_send.mp4"
            )

            inputStream.use { input ->

                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            sendFile(tempFile)

        } catch (e: Exception) {

            Log.e(
                "Nearby",
                "파일 변환 실패",
                e
            )
        }
    }
    fun sendFile(file: File) {

        connectedEndpoints.forEach { endpointId ->

            val payload =
                Payload.fromFile(file)

            connectionsClient.sendPayload(
                endpointId,
                payload
            )

            Log.d(
                "Nearby",
                "파일 전송 -> $endpointId"
            )
        }
    }




    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {
                Log.d("Nearby", "연결 요청: ${connectionInfo.endpointName}")

                connectionsClient.acceptConnection(
                    endpointId,
                    payloadCallback
                )
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {

                if (result.status.isSuccess) {

                    connectedEndpoints.add(endpointId)

                    connectionCount = connectedEndpoints.size

                    onConnectionChanged(connectionCount)

                    Log.d(
                        "Nearby",
                        "연결 성공: $endpointId"
                    )
                } else {
                    Log.d("Nearby", "연결 실패")
                }
            }

            override fun onDisconnected(
                endpointId: String
            ) {

                connectedEndpoints.remove(endpointId)

                connectionCount = connectedEndpoints.size

                onConnectionChanged(connectionCount)

                Log.d(
                    "Nearby",
                    "연결 해제: $endpointId"
                )

                onConnectionChanged(connectionCount)

                Log.d("Nearby", "연결 해제")
            }
        }

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {

                Log.d("Nearby", "발견: ${info.endpointName}")

                connectionsClient.requestConnection(
                    "CLIENT",
                    endpointId,
                    connectionLifecycleCallback
                )
            }

            override fun onEndpointLost(
                endpointId: String
            ) {

            }
        }

    fun startAdvertising() {

        Log.d("Nearby", "Advertising 시작")

        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient.startAdvertising(
            "MASTER",
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        )
            .addOnSuccessListener {
                Log.d("Nearby", "Advertising 성공")
            }
            .addOnFailureListener { e ->

                Log.e(
                    "Nearby",
                    "Advertising 실패: ${e.message}",
                    e
                )
            }
    }

    fun startDiscovery() {

        Log.d("Nearby", "Discovery 시작")

        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            options
        )
            .addOnSuccessListener {
                Log.d("Nearby", "Discovery 성공")
            }
            .addOnFailureListener { e ->

                Log.e(
                    "Nearby",
                    "Discovery 실패: ${e.message}",
                    e
                )
            }
    }

    private val payloadCallback =
        object : PayloadCallback() {

            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {
                payload.asBytes()?.let {

                    val message = String(it)

                    Log.d("Nearby", "수신: $message")

                    onMessageReceived(message)
                }
                payload.asFile()?.let {

                    incomingFiles[payload.id] = payload

                    Log.d(
                        "Nearby",
                        "파일 Payload 수신: ${payload.id}"
                    )
                }
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {

                val progress =
                    if (update.totalBytes > 0)
                        ((update.bytesTransferred * 100)
                                / update.totalBytes).toInt()
                    else
                        0

                onTransferProgress(progress)

                Log.d(
                    "Nearby",
                    "전송 진행률: $progress%"
                )

                if (
                    update.status ==
                    PayloadTransferUpdate.Status.SUCCESS
                ) {

                    Log.d(
                        "Nearby",
                        "SUCCESS 도착"
                    )

                    val payload =
                        incomingFiles.remove(update.payloadId)

                    Log.d(
                        "Nearby",
                        "payload=$payload"
                    )

                    if (payload == null) {
                        Log.e(
                            "Nearby",
                            "payload 없음"
                        )
                        return
                    }

                    val pfd =
                        payload.asFile()?.asParcelFileDescriptor()

                    Log.d(
                        "Nearby",
                        "pfd=$pfd"
                    )

                    if (pfd == null) {
                        Log.e(
                            "Nearby",
                            "ParcelFileDescriptor 없음"
                        )
                        return
                    }

                    val file =
                        File(
                            context.getExternalFilesDir(null),
                            "received_${System.currentTimeMillis()}.mp4"
                        )

                    FileInputStream(
                        pfd.fileDescriptor
                    ).use { input ->

                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    Log.d(
                        "Nearby",
                        "파일 저장 완료: ${file.absolutePath}"
                    )

                    onFileReceived(file.absolutePath)
                }
            }
        }


}