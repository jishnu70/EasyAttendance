package com.example.attendancemanagementapp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.attendancemanagementapp.database.StudentEntity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.gson.Gson

class NearByManager(private val context: Context) {

    private val connectionClient = Nearby.getConnectionsClient(context)
    private val gson = Gson()

    fun startAdvertising(
        serviceID: String,
        onAdvertisingStarted: (String) -> Unit,
        onStudentConnected: (String) -> Unit,
        onAttendanceReceived: (StudentEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(
                endpointID: String,
                info: ConnectionInfo
            ) {
                connectionClient.acceptConnection(endpointID, object : PayloadCallback() {
                    override fun onPayloadReceived(
                        endpoint: String,
                        payload: Payload
                    ) {
                        val receivedData = payload.asBytes()?.let { String(it) }
                        receivedData?.let {
                            try {
                                val student = gson.fromJson(it, StudentEntity::class.java)
                                onAttendanceReceived(student)
                            } catch (e: Exception) {
                                onError("Failed to parse student data: ${e.message}")
                            }
                        }
                    }

                    override fun onPayloadTransferUpdate(
                        endpoint: String,
                        update: PayloadTransferUpdate
                    ) {
                        TODO("Not yet implemented")
                    }

                })
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {
                if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    Log.d("NearbyConnections", "Connection successful with $endpointId")
                    onStudentConnected(endpointId)
                    Toast.makeText(context, "Connected to student: $endpointId", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.e(
                        "NearbyConnections",
                        "Connection failed with $endpointId: ${result.status.statusMessage}"
                    )
                    onError("Connection failed with $endpointId")
                    Toast.makeText(context, "Connection failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDisconnected(endpointId: String) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Student disconnected: $endpointId", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        connectionClient.startAdvertising(
            "Teacher",
            serviceID,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        ).addOnSuccessListener {
            onAdvertisingStarted(serviceID)
            Log.d("NearbyConnections", "Advertising started successfully")
        }.addOnFailureListener { e ->
            onError("Failed to start advertising: ${e.message}")
        }
    }

    fun discoverTeacher(
        serviceID: String,
        studentEntity: StudentEntity,
        onError: (String) -> Unit,
        onTeacherDiscovered: (String) -> Unit,
        onConnected: () -> Unit,
        onAttendanceSent: () -> Unit
    ) {
        val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {
                if (info.endpointName == "Teacher") {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Teacher found", Toast.LENGTH_SHORT).show()
                        onTeacherDiscovered(endpointId)
                        requestConnection(
                            endpointId,
                            studentEntity,
                            onConnected,
                            onAttendanceSent,
                            onError
                        )
                    }
                }
            }

            override fun onEndpointLost(p0: String) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Teacher Connection Lost", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        connectionClient.stopDiscovery()
        connectionClient.startDiscovery(
            serviceID,
            endpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        ).addOnFailureListener { e ->
            onError("Discovery failed: ${e.message}")
        }
    }

    private fun requestConnection(
        endpointId: String,
        studentEntity: StudentEntity,
        onConnected: () -> Unit,
        onAttendanceSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(
                endpointId: String,
                info: ConnectionInfo
            ) {
                connectionClient.acceptConnection(endpointId, object : PayloadCallback() {
                    override fun onPayloadReceived(
                        endpointId: String,
                        payload: Payload
                    ) {
                        TODO("Not yet implemented")
                    }

                    override fun onPayloadTransferUpdate(
                        endpointId: String,
                        update: PayloadTransferUpdate
                    ) {
                        if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                            Log.d("NearbyConnections", "Payload transfer to $endpointId completed")
                        }
                    }
                })
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {
                Handler(Looper.getMainLooper()).post {
                    if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                        onConnected()
                        sendAttendance(endpointId, studentEntity, onAttendanceSent, onError)
                    } else {
                        onError("Connection Failed")
                        Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Disconnected from teacher", Toast.LENGTH_SHORT).show()
                }
            }
        }
        connectionClient.requestConnection("student", endpointId, connectionLifecycleCallback)
    }

    private fun sendAttendance(
        endpointId: String,
        studentEntity: StudentEntity,
        onAttendanceSent: () -> Unit,
        onError: (String) -> Unit
    ) {
        val payload = Payload.fromBytes(gson.toJson(studentEntity).toByteArray())
        connectionClient.sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Attendance sent", Toast.LENGTH_SHORT).show()
                    onAttendanceSent()
                    connectionClient.disconnectFromEndpoint(endpointId)
                }
            }
            .addOnFailureListener { e ->
                onError("Failed to send attendance: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Not connected to Teacher", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun stopAll() {
        connectionClient.stopAllEndpoints()
    }

    fun stopAdvertising() {
        connectionClient.stopAdvertising()
    }

    fun stopDiscovering() {
        connectionClient.stopDiscovery()
    }
}