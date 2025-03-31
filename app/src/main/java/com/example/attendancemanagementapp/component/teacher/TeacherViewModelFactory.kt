package com.example.attendancemanagementapp.component.teacher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.attendancemanagementapp.repository.SubjectRepository
import com.example.attendancemanagementapp.repository.TeacherRepository
import com.example.attendancemanagementapp.repository.UserRepository

class TeacherViewModelFactory(
    private val teacherRepository: TeacherRepository,
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherViewModel(teacherRepository, userRepository, subjectRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}