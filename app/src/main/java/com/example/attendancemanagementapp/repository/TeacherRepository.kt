package com.example.attendancemanagementapp.repository

import android.util.Log
import com.example.attendancemanagementapp.database.AttendanceEntity
import com.example.attendancemanagementapp.database.StudentEntity
import com.example.attendancemanagementapp.database.TeacherDAO
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class TeacherRepository(private val teacherDAO: TeacherDAO) {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "TeacherRepository"

    // Insert attendance into Room and sync to Firestore
    suspend fun insertAttendance(attendance: AttendanceEntity) {
        try {
            teacherDAO.insertAttendance(attendance) // Save to Room
            syncAttendanceToFirestore(attendance) // Sync to Firestore
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting attendance: ${e.message}")
        }
    }

    // Get unsynced attendance from Room
    fun getUnsyncedAttendance(): Flow<List<AttendanceEntity>> = teacherDAO.getUnSyncedAttendance()

    // Get all students from Room
    fun getAllStudents(): Flow<List<StudentEntity>> = teacherDAO.getAllStudents()

    // Mark attendance as synced in Room
    suspend fun markAsSynced(attendanceId: Int) {
        try {
            teacherDAO.markAsSynced(attendanceId)
            Log.d(TAG, "Attendance marked as synced: $attendanceId")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking attendance as synced: ${e.message}")
        }
    }

    // Sync attendance to Firestore
    private suspend fun syncAttendanceToFirestore(attendance: AttendanceEntity) {
        val teacherEmail = teacherDAO.getTeacherEmail(attendance.subjectId)
        if (teacherEmail != null) {
            val attendanceData = mapOf(
                "id" to attendance.id,
                "studentUSN" to attendance.studentUSN,
                "subjectId" to attendance.subjectId,
                "sessionCode" to attendance.sessionCode,
                "date" to attendance.date,
                "classHour" to attendance.classHour,
                "synced" to true
            )
            try {
                firestore.collection("teachers").document(teacherEmail)
                    .collection("attendance").document("${attendance.sessionCode}_${attendance.date}")
                    .set(attendanceData)
                    .await()
                markAsSynced(attendance.id) // Update Room after successful sync
                Log.d(TAG, "Attendance synced to Firestore: ${attendance.sessionCode}_${attendance.date}")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing attendance to Firestore: ${e.message}")
            }
        } else {
            Log.e(TAG, "Teacher email not found for subjectId: ${attendance.subjectId}")
        }
    }

    // Fetch attendance from Firestore for a teacher
    suspend fun fetchAttendanceFromFirestore(teacherEmail: String): List<AttendanceEntity> {
        return try {
            val snapshot = firestore.collection("teachers").document(teacherEmail)
                .collection("attendance").get().await()
            snapshot.toObjects(AttendanceEntity::class.java).also {
                Log.d(TAG, "Attendance fetched from Firestore for: $teacherEmail")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching attendance from Firestore: ${e.message}")
            emptyList()
        }
    }
}