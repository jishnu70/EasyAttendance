package com.example.attendancemanagementapp.component.teacher

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancemanagementapp.NearByManager
import com.example.attendancemanagementapp.TeacherUIState
import com.example.attendancemanagementapp.database.AttendanceEntity
import com.example.attendancemanagementapp.database.SubjectEntity
import com.example.attendancemanagementapp.repository.SubjectRepository
import com.example.attendancemanagementapp.repository.TeacherRepository
import com.example.attendancemanagementapp.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TeacherViewModel(
    private val teacherRepository: TeacherRepository,
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    context: Context
) : ViewModel() {
    private val _teacherUI = MutableStateFlow<TeacherUIState>(TeacherUIState.Idle)
    val teacherUI: StateFlow<TeacherUIState> = _teacherUI
    private val _serviceID = mutableStateOf(generateServiceID())
    val serviceID: State<String> = _serviceID
    private val _subjects = MutableStateFlow<List<SubjectEntity>>(emptyList())
    val subjects: StateFlow<List<SubjectEntity>> = _subjects
    private val nearByManager = NearByManager(context)
    private val firestore = FirebaseFirestore.getInstance()
    private var currentSubject: SubjectEntity? = null

    init {
        viewModelScope.launch {
            checkTeacherUser()
            loadSubjects()
        }
    }

    private suspend fun checkTeacherUser() {
        val user = userRepository.getUser().firstOrNull()
        if (user != null && user.isTeacher == true) {
            _teacherUI.value = TeacherUIState.Idle
        } else {
            _teacherUI.value = TeacherUIState.Error("Teacher not found")
        }
    }

    private suspend fun loadSubjects() {
        val user = userRepository.getUser().firstOrNull()
        user?.let {
            subjectRepository.getSubjectsForTeacher(it.id).collect { subjects ->
                _subjects.value = subjects
            }
        } ?: run {
            _teacherUI.value = TeacherUIState.Error("User not found, cannot load subjects")
        }
    }

    fun addSubject(subjectName: String, subjectCode: String) {
        viewModelScope.launch {
            val user = userRepository.getUser().firstOrNull()
            if (user == null) {
                _teacherUI.value = TeacherUIState.Error("User not found")
                return@launch
            }
            if (_subjects.value.any { it.subjectCode == subjectCode }) {
                _teacherUI.value = TeacherUIState.Error("Subject code '$subjectCode' already exists")
                return@launch
            }
            val subject = SubjectEntity(subjectName = subjectName, subjectCode = subjectCode, teacherId = user.id)
            subjectRepository.insertSubject(subject)
            firestore.collection("teachers").document(user.email)
                .collection("subjects").document(subjectCode)
                .set(mapOf("subjectName" to subjectName, "subjectCode" to subjectCode))
                .addOnFailureListener { e ->
                    _teacherUI.value = TeacherUIState.Error("Failed to sync subject: ${e.message}")
                }
        }
    }

    fun editSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            val user = userRepository.getUser().firstOrNull()
            if (user == null) {
                _teacherUI.value = TeacherUIState.Error("User not found")
                return@launch
            }
            if (_subjects.value.any { it.subjectCode == subject.subjectCode && it.id != subject.id }) {
                _teacherUI.value = TeacherUIState.Error("Subject code '${subject.subjectCode}' already exists")
                return@launch
            }
            subjectRepository.insertSubject(subject) // Updates due to REPLACE strategy
            firestore.collection("teachers").document(user.email)
                .collection("subjects").document(subject.subjectCode)
                .set(mapOf("subjectName" to subject.subjectName, "subjectCode" to subject.subjectCode))
                .addOnFailureListener { e ->
                    _teacherUI.value = TeacherUIState.Error("Failed to sync subject: ${e.message}")
                }
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            val user = userRepository.getUser().firstOrNull()
            if (user == null) {
                _teacherUI.value = TeacherUIState.Error("User not found")
                return@launch
            }
            subjectRepository.deleteSubject(subject)
            firestore.collection("teachers").document(user.email)
                .collection("subjects").document(subject.subjectCode)
                .delete()
                .addOnFailureListener { e ->
                    _teacherUI.value = TeacherUIState.Error("Failed to delete subject: ${e.message}")
                }
            if (currentSubject?.id == subject.id) {
                currentSubject = null // Reset if deleted subject was selected
            }
        }
    }

    fun generateServiceID(): String {
        return System.currentTimeMillis().toString().takeLast(6)
    }

    fun startAdvertising(subject: SubjectEntity) {
        currentSubject = subject
        val currentServiceID = _serviceID.value
        viewModelScope.launch {
            _teacherUI.value = TeacherUIState.Starting
            nearByManager.startAdvertising(
                serviceID = currentServiceID,
                onAdvertisingStarted = { endpoint ->
                    _teacherUI.value = TeacherUIState.Advertising(endpoint)
                },
                onStudentConnected = {},
                onAttendanceReceived = { student ->
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    val classHour = calculateClassHour(time)
                    val attendance = AttendanceEntity(
                        studentUSN = student.usn,
                        subjectId = subject.id,
                        sessionCode = currentServiceID,
                        date = date,
                        classHour = classHour
                    )
                    viewModelScope.launch {
                        teacherRepository.insertStudent(student)
                        teacherRepository.insertAttendance(attendance)
                    }
                    _teacherUI.value = TeacherUIState.Advertising(currentServiceID)
                },
                onError = { message ->
                    _teacherUI.value = TeacherUIState.Error(message)
                }
            )
        }
    }

    private fun calculateClassHour(time: String): String {
        val (hour, minute) = time.split(":").map { it.toInt() }
        val adjustedHour = if (minute >= 55) (hour + 1) % 12 else hour
        val period = if (hour < 12 || (hour == 12 && minute < 55)) "am" else "pm"
        return "${if (adjustedHour == 0) 12 else adjustedHour}$period"
    }

    fun stopAdvertising() {
        nearByManager.stopAdvertising()
        nearByManager.stopAll()
        _teacherUI.value = TeacherUIState.Idle
    }

    fun resetToIdle() {
        _serviceID.value = generateServiceID()
        _teacherUI.value = TeacherUIState.Idle
    }

    fun syncToFirebase() {
        viewModelScope.launch {
            val unsyncedAttendance = teacherRepository.getUnsyncedAttendance().firstOrNull() ?: emptyList()
            if (unsyncedAttendance.isEmpty()) {
                _teacherUI.value = TeacherUIState.SyncCount(0)
                return@launch
            }

            var successCount = 0
            unsyncedAttendance.forEach { attendance ->
                val student = teacherRepository.getAllStudent().firstOrNull()?.find { it.usn == attendance.studentUSN }
                val subject = _subjects.value.find { it.id == attendance.subjectId }
                if (student != null && subject != null) {
                    val data = mapOf(
                        "studentName" to student.name,
                        "usn" to student.usn,
                        "sem" to student.sem,
                        "subjectName" to subject.subjectName,
                        "subjectCode" to subject.subjectCode,
                        "sessionCode" to attendance.sessionCode,
                        "date" to attendance.date,
                        "classHour" to attendance.classHour
                    )
                    firestore.collection("attendance")
                        .document("${attendance.id}_${attendance.sessionCode}")
                        .set(data)
                        .addOnSuccessListener {
                            successCount++
                            viewModelScope.launch {
                                teacherRepository.markAsSynced(attendance.id)
                            }
                        }
                        .addOnFailureListener { e ->
                            _teacherUI.value = TeacherUIState.SyncError(e.message ?: "Sync failed")
                        }
                }
            }
            _teacherUI.value = TeacherUIState.SyncCount(successCount)
        }
    }

    override fun onCleared() {
        nearByManager.stopAdvertising()
        nearByManager.stopAll()
        super.onCleared()
    }
}