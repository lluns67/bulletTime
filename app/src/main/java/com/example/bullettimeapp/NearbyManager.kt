package com.example.bullettimeapp

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

import android.util.Log

class NearbyManager(
    context: Context,
    private val onMessageReceived: (String) -> Unit,
) {
    private var connectedEndpointId: String? = null
    private val connectionsClient =
        Nearby.getConnectionsClient(context)

    companion object {
        const val SERVICE_ID =
            "com.example.bullettimeapp"
    }

    fun sendStartCommand(startTime: Long) {

        connectedEndpointId?.let { endpointId ->

            val message =
                "START_AT:$startTime"

            connectionsClient.sendPayload(
                endpointId,
                Payload.fromBytes(message.toByteArray())
            )

            Log.d("Nearby", "전송: $message")
        }
    }

    fun sendStopCommand(stopTime: Long) {

        connectedEndpointId?.let { endpointId ->

            val message =
                "STOP_AT:$stopTime"

            connectionsClient.sendPayload(
                endpointId,
                Payload.fromBytes(message.toByteArray())
            )

            Log.d("Nearby", "전송: $message")
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

                    connectedEndpointId = endpointId

                    Log.d("Nearby", "연결 성공")
                } else {
                    Log.d("Nearby", "연결 실패")
                }
            }

            override fun onDisconnected(
                endpointId: String
            ) {

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
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
            }
        }


}