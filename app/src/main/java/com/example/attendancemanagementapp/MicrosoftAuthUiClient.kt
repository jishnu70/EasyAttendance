package com.example.attendancemanagementapp

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.attendancemanagementapp.database.UserDAO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.tasks.await

class MicrosoftAuthUiClient(
    private val context: Context,
    private val activity: Activity,
    private val userDAO: UserDAO
) {
    internal val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun signInWithMicrosoft(): Result<FirebaseUser?> {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (connectivityManager == null) {
                Log.e("SignIn", "connectivityManager unavailable")
                return Result.failure(Exception("connectivityManager unavailable"))
            }
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) {
                Log.e("SignIn", "Active Network unavailable")
                return Result.failure(Exception("Active Network unavailable"))
            }
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities == null || !networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
            ) {
                Log.e("SignIn", "Internet unavailable")
                return Result.failure(Exception("Internet unavailable"))
            }

            val provider = OAuthProvider.newBuilder("microsoft.com").apply {
                try {
                    setScopes(listOf("email", "profile"))
                } catch (e: Exception) {
                    Log.e("SignIn", "Failed to set OAuth scopes: ${e.message}")
                }
            }.build()

            val authResult = auth.startActivityForSignInWithProvider(activity, provider).await()
            return if (authResult != null && authResult.user != null) {
                Result.success(authResult.user)
            } else {
                Result.failure(Exception("Signed In but user was not returned"))
            }
        } catch (e: FirebaseAuthException) {
            Log.e("SignIn", "FirebaseAuthException: ${e.message}")
            throw e // Re-throw to be handled by caller
        } catch (e: Exception) {
            Log.e("SignIn", "Unexpected error during sign-in: ${e.message}")
            throw e // Re-throw to be handled by caller
        }
    }
}