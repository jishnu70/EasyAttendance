package com.example.attendancemanagementapp

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object GooglePlayServiceCheck {
    fun isAvailable(context: Context): Boolean {
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)

        return if (status == ConnectionResult.SUCCESS) {
            Log.d("NearbyDebug", "✅ Google Play Services is available")
            true
        } else {
            Log.e("NearbyDebug", "❌ Google Play Services is NOT available")
            false
        }
    }
}