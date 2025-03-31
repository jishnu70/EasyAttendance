package com.example.attendancemanagementapp

import com.example.attendancemanagementapp.database.UserEntity
import com.google.firebase.auth.FirebaseUser

sealed class AuthUIState {
    object CheckingUser : AuthUIState()
    object LoginRequired : AuthUIState()
    data class RoleSelection(val userEntity: UserEntity?) : AuthUIState()
    object TeacherSubjectInput : AuthUIState()
    data class StudentInfoInput(val userEntity: UserEntity) : AuthUIState() // New state
    data class ProfileComplete(val userEntity: UserEntity) : AuthUIState()
    object Loading : AuthUIState()
    data class Success(val firebaseUser: FirebaseUser?, val userEntity: UserEntity?) : AuthUIState()
    data class Error(val message: String) : AuthUIState()
}

sealed class TeacherUIState {
    object Idle : TeacherUIState()
    object Starting : TeacherUIState()
    data class Advertising(val endpoint: String) : TeacherUIState()
    data class Error(val message: String) : TeacherUIState()
    data class SyncCount(val count: Int) : TeacherUIState()
    data class SyncError(val message: String) : TeacherUIState()
}

sealed class StudentUIState {
    object Idle : StudentUIState()
    data class InfoRequired(val userEntity: UserEntity) : StudentUIState()
    object Discovering : StudentUIState()
    data class TeacherDiscovered(val endpoint: String) : StudentUIState()
    object Connected : StudentUIState()
    object SendingAttendance : StudentUIState()
    object AttendanceSent : StudentUIState()
    data class Error(val message: String) : StudentUIState()
}