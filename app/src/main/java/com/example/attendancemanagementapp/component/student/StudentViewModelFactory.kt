package com.example.attendancemanagementapp.component.student

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.attendancemanagementapp.repository.StudentRepository
import com.example.attendancemanagementapp.repository.UserRepository

class StudentViewModelFactory(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
    private val context: Context
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentViewModel(studentRepository, userRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}