package com.example.attendancemanagementapp.repository

import android.util.Log
import com.example.attendancemanagementapp.database.UserDAO
import com.example.attendancemanagementapp.database.UserEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class UserRepository(private val userDAO: UserDAO) {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "UserRepository"

    // Get user from local Room database
    fun getUser(): Flow<UserEntity?> = userDAO.getUser()

    // Insert user into Room and sync to Firestore
    suspend fun insertUser(user: UserEntity) {
        try {
            userDAO.insertUser(user) // Save to Room
            syncUserToFirestore(user) // Sync to Firestore
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting user: ${e.message}")
        }
    }

    // Sync user data to Firestore
    private suspend fun syncUserToFirestore(user: UserEntity) {
        val userData = mapOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "isTeacher" to user.isTeacher
        )
        try {
            firestore.collection("users").document(user.email)
                .set(userData)
                .await()
            Log.d(TAG, "User synced to Firestore: ${user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user to Firestore: ${e.message}")
        }
    }

    // Fetch user from Firestore if not found locally
    suspend fun fetchUserFromFirestore(email: String): UserEntity? {
        return try {
            val snapshot = firestore.collection("users").document(email).get().await()
            snapshot.toObject(UserEntity::class.java)?.also {
                Log.d(TAG, "User fetched from Firestore: $email")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from Firestore: ${e.message}")
            null
        }
    }
}