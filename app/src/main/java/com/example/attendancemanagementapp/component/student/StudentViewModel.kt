package com.example.attendancemanagementapp.component.student

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancemanagementapp.NearByManager
import com.example.attendancemanagementapp.StudentUIState
import com.example.attendancemanagementapp.database.StudentEntity
import com.example.attendancemanagementapp.repository.StudentRepository
import com.example.attendancemanagementapp.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class StudentViewModel(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
    context: Context
) : ViewModel() {
    private val _studentUiState = MutableStateFlow<StudentUIState>(StudentUIState.Idle)
    val studentUiState: StateFlow<StudentUIState> = _studentUiState
    private val _serviceID = mutableStateOf("")
    val serviceID: State<String> = _serviceID
    private val nearByManager = NearByManager(context)
    private var studentEntity: StudentEntity? = null

    init {
        viewModelScope.launch {
            checkStudentInfo()
        }
    }

    fun resetToIdle() {
        _studentUiState.value = StudentUIState.Idle
    }

    private fun checkStudentInfo() {
        viewModelScope.launch {
            val user = userRepository.getUser().firstOrNull()
            if (user != null && user.isTeacher == false) {
                studentEntity = studentRepository.getStudent().firstOrNull()
                _studentUiState.value = if (studentEntity == null) {
                    StudentUIState.InfoRequired(user)
                } else {
                    StudentUIState.Idle
                }
            }
        }
    }

    fun updateServiceID(serviceID: String) {
        _serviceID.value = serviceID
    }

    fun saveStudentInfo(studentEntity: StudentEntity) {
        viewModelScope.launch {
            studentRepository.insertStudent(studentEntity)
            this@StudentViewModel.studentEntity = studentEntity
            _studentUiState.value = StudentUIState.Idle
        }
    }

    fun discoverTeacher() {
        if (_serviceID.value.length == 6) {
            viewModelScope.launch {
                studentEntity?.let { student ->
                    _studentUiState.value = StudentUIState.Discovering
                    nearByManager.discoverTeacher(
                        serviceID = _serviceID.value,
                        studentEntity = student,
                        onError = { message ->
                            _studentUiState.value = StudentUIState.Error(message)
                        },
                        onTeacherDiscovered = { endpoint ->
                            _studentUiState.value = StudentUIState.TeacherDiscovered(endpoint)
                        },
                        onConnected = { _studentUiState.value = StudentUIState.Connected },
                        onAttendanceSent = { _studentUiState.value = StudentUIState.AttendanceSent }
                    )
                } ?: run {
                    _studentUiState.value = StudentUIState.Error("Student info not found")
                }
            }
        } else {
            _studentUiState.value = StudentUIState.Error("Invalid session code (must be 6 digits)")
        }
    }

    public override fun onCleared() {
        nearByManager.stopDiscovering()
        nearByManager.stopAll()
        super.onCleared()
    }
}