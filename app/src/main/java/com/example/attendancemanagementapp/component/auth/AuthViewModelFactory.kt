package com.example.attendancemanagementapp.component.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.attendancemanagementapp.MicrosoftAuthUiClient
import com.example.attendancemanagementapp.repository.StudentRepository
import com.example.attendancemanagementapp.repository.SubjectRepository
import com.example.attendancemanagementapp.repository.TeacherRepository
import com.example.attendancemanagementapp.repository.UserRepository

class AuthViewModelFactory(
    private val authClient: MicrosoftAuthUiClient,
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val subjectRepository: SubjectRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                authClient,
                userRepository,
                studentRepository,
                teacherRepository,
                subjectRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}