package com.example.attendancemanagementapp.repository

import android.util.Log
import com.example.attendancemanagementapp.database.StudentDAO
import com.example.attendancemanagementapp.database.StudentEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class StudentRepository(private val studentDAO: StudentDAO) {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "StudentRepository"

    // Insert student into Room and sync to Firestore
    suspend fun insertStudent(student: StudentEntity) {
        try {
            studentDAO.insertStudent(student) // Save to Room
            syncStudentToFirestore(student) // Sync to Firestore
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting student: ${e.message}")
        }
    }

    // Get all students from Room
    fun getAllStudent(): Flow<StudentEntity> = studentDAO.getStudent()

    // Sync student data to Firestore
    private suspend fun syncStudentToFirestore(student: StudentEntity) {
        val studentData = mapOf(
            "id" to student.id,
            "name" to student.name,
            "usn" to student.usn,
            "sem" to student.sem
        )
        try {
            firestore.collection("students").document(student.usn)
                .set(studentData)
                .await()
            Log.d(TAG, "Student synced to Firestore: ${student.usn}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing student to Firestore: ${e.message}")
        }
    }

    // Fetch student from Firestore
    suspend fun fetchStudentFromFirestore(usn: String): StudentEntity? {
        return try {
            val snapshot = firestore.collection("students").document(usn).get().await()
            snapshot.toObject(StudentEntity::class.java)?.also {
                Log.d(TAG, "Student fetched from Firestore: $usn")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching student from Firestore: ${e.message}")
            null
        }
    }
}