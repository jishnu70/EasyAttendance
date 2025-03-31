package com.example.attendancemanagementapp.repository

import android.util.Log
import com.example.attendancemanagementapp.database.SubjectDAO
import com.example.attendancemanagementapp.database.SubjectEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class SubjectRepository(private val subjectDAO: SubjectDAO) {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "SubjectRepository"

    // Get subjects for a teacher from Room
    fun getSubjectsForTeacher(teacherId: Int): Flow<List<SubjectEntity>> =
        subjectDAO.getSubjectsForTeacher(teacherId)

    // Insert subject into Room and sync to Firestore
    suspend fun insertSubject(subject: SubjectEntity) {
        try {
            subjectDAO.insertSubject(subject) // Save to Room
            syncSubjectToFirestore(subject) // Sync to Firestore
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting subject: ${e.message}")
        }
    }

    // Delete subject from Room and Firestore
    suspend fun deleteSubject(subject: SubjectEntity) {
        try {
            subjectDAO.deleteSubject(subject) // Delete from Room
            deleteSubjectFromFirestore(subject) // Delete from Firestore
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting subject: ${e.message}")
        }
    }

    // Sync subject to Firestore
    private suspend fun syncSubjectToFirestore(subject: SubjectEntity) {
        val teacherEmail = subjectDAO.getTeacherEmail(subject.teacherId)
        if (teacherEmail != null) {
            val subjectData = mapOf(
                "id" to subject.id,
                "subjectName" to subject.subjectName,
                "subjectCode" to subject.subjectCode,
                "teacherId" to subject.teacherId
            )
            try {
                firestore.collection("teachers").document(teacherEmail)
                    .collection("subjects").document(subject.subjectCode)
                    .set(subjectData)
                    .await()
                Log.d(TAG, "Subject synced to Firestore: ${subject.subjectCode}")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing subject to Firestore: ${e.message}")
            }
        } else {
            Log.e(TAG, "Teacher email not found for teacherId: ${subject.teacherId}")
        }
    }

    // Delete subject from Firestore
    private suspend fun deleteSubjectFromFirestore(subject: SubjectEntity) {
        val teacherEmail = subjectDAO.getTeacherEmail(subject.teacherId)
        if (teacherEmail != null) {
            try {
                firestore.collection("teachers").document(teacherEmail)
                    .collection("subjects").document(subject.subjectCode)
                    .delete()
                    .await()
                Log.d(TAG, "Subject deleted from Firestore: ${subject.subjectCode}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting subject from Firestore: ${e.message}")
            }
        } else {
            Log.e(TAG, "Teacher email not found for teacherId: ${subject.teacherId}")
        }
    }

    // Fetch subjects from Firestore for a teacher
    suspend fun fetchSubjectsFromFirestore(teacherEmail: String): List<SubjectEntity> {
        return try {
            val snapshot = firestore.collection("teachers").document(teacherEmail)
                .collection("subjects").get().await()
            snapshot.toObjects(SubjectEntity::class.java).also {
                Log.d(TAG, "Subjects fetched from Firestore for: $teacherEmail")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching subjects from Firestore: ${e.message}")
            emptyList()
        }
    }
}