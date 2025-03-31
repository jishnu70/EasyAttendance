package com.example.attendancemanagementapp.component.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendancemanagementapp.AuthUIState
import com.example.attendancemanagementapp.database.SubjectEntity
import com.example.attendancemanagementapp.database.UserEntity
import com.example.attendancemanagementapp.MicrosoftAuthUiClient
import com.example.attendancemanagementapp.database.StudentEntity
import com.example.attendancemanagementapp.repository.StudentRepository
import com.example.attendancemanagementapp.repository.SubjectRepository
import com.example.attendancemanagementapp.repository.TeacherRepository
import com.example.attendancemanagementapp.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authUiClient: MicrosoftAuthUiClient,
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository,
    private val subjectRepository: SubjectRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUIState>(AuthUIState.CheckingUser)
    val uiState: StateFlow<AuthUIState> = _uiState
    private val firestore = FirebaseFirestore.getInstance()

    init {
        // Launch a coroutine to handle suspend calls in init
        viewModelScope.launch {
            checkingUser()
        }
    }

    fun updateUIState(newState: AuthUIState) {
        _uiState.value = newState
    }

    suspend fun needsSubjectInput(): Boolean {
        return subjectRepository?.getSubjectsForTeacher(userRepository.getUser().firstOrNull()?.id ?: 0)?.firstOrNull()?.isEmpty() != false
    }

    private suspend fun checkingUser() {
        val firebaseUser = authUiClient.auth.currentUser
        val localUser = userRepository.getUser().firstOrNull() // Suspend call inside coroutine
        _uiState.value = when {
            firebaseUser == null -> AuthUIState.LoginRequired
            localUser == null -> AuthUIState.Success(firebaseUser, null)
            localUser.isTeacher == null -> AuthUIState.RoleSelection(localUser)
            localUser.isTeacher == true -> {
                checkTeacherSubjects(localUser)
                AuthUIState.Loading // Temporarily set to loading while checking
            }
            else -> AuthUIState.ProfileComplete(localUser)
        }
    }

    fun signInUsingFireBase() {
        viewModelScope.launch {
            _uiState.value = AuthUIState.Loading
            val result = authUiClient.signInWithMicrosoft()
            _uiState.value = when {
                result.isSuccess -> {
                    val user = result.getOrNull()
                    val userEntity = user?.let {
                        UserEntity(
                            name = it.displayName ?: "Unknown",
                            email = it.email ?: "No Email"
                        )
                    }
                    userEntity?.let { userRepository.insertUser(it) }
                    AuthUIState.Success(user, userEntity)
                }
                result.isFailure -> AuthUIState.Error(result.exceptionOrNull()?.message ?: "Unknown Error")
                else -> AuthUIState.Error("Unexpected Error")
            }
        }
    }

    fun updateUserRole(isTeacherQuery: Boolean) {
        viewModelScope.launch {
            val currentUser = userRepository.getUser().firstOrNull()
            currentUser?.let {
                val updatedUser = it.copy(isTeacher = isTeacherQuery)
                userRepository.insertUser(updatedUser)
                _uiState.value = if (isTeacherQuery) {
                    checkTeacherSubjects(updatedUser)
                    AuthUIState.Loading
                } else {
                    AuthUIState.ProfileComplete(updatedUser)
                }
            } ?: run {
                _uiState.value = AuthUIState.Error("User not found")
            }
        }
    }

    private fun checkTeacherSubjects(user: UserEntity) {
        firestore.collection("teachers").document(user.email)
            .collection("subjects").get()
            .addOnSuccessListener { snapshot ->
                viewModelScope.launch {
                    if (snapshot.isEmpty) {
                        // No subjects in Firebase, check local or prompt
                        val localSubjects = subjectRepository?.getSubjectsForTeacher(user.id)?.firstOrNull()
                        if (localSubjects.isNullOrEmpty()) {
                            _uiState.value = AuthUIState.TeacherSubjectInput
                        } else {
                            syncSubjectsToFirebase(user, localSubjects)
                            _uiState.value = AuthUIState.ProfileComplete(user)
                        }
                    } else {
                        // Sync Firebase subjects to local
                        val subjects = snapshot.documents.mapNotNull { doc ->
                            SubjectEntity(
                                subjectName = doc.getString("subjectName") ?: return@mapNotNull null,
                                subjectCode = doc.getString("subjectCode") ?: return@mapNotNull null,
                                teacherId = user.id
                            )
                        }
                        subjects.forEach { subjectRepository?.insertSubject(it) }
                        _uiState.value = AuthUIState.ProfileComplete(user)
                    }
                }
            }
            .addOnFailureListener { e ->
                _uiState.value = AuthUIState.Error("Failed to fetch subjects: ${e.message}")
            }
    }

    private fun syncSubjectsToFirebase(user: UserEntity, subjects: List<SubjectEntity>) {
        subjects.forEach { subject ->
            firestore.collection("teachers").document(user.email)
                .collection("subjects").document(subject.subjectCode)
                .set(mapOf("subjectName" to subject.subjectName, "subjectCode" to subject.subjectCode))
        }
    }

    fun saveTeacherSubject(subjectName: String, subjectCode: String) {
        viewModelScope.launch {
            val user = userRepository.getUser().firstOrNull()
            user?.let {
                val subject = SubjectEntity(
                    subjectName = subjectName,
                    subjectCode = subjectCode,
                    teacherId = it.id
                )
                subjectRepository?.insertSubject(subject)
                // Sync to Firebase
                firestore.collection("teachers").document(it.email)
                    .collection("subjects").document(subjectCode)
                    .set(mapOf("subjectName" to subjectName, "subjectCode" to subjectCode))
                _uiState.value = AuthUIState.ProfileComplete(it)
            }
        }
    }

    fun saveStudentInfo(userEntity: UserEntity, name: String, semester: Int, usn: String) {
        viewModelScope.launch {
            val updatedStudent = StudentEntity(name = userEntity.name, usn = usn, sem = semester)
            studentRepository.insertStudent(updatedStudent)
            firestore.collection("students").document(userEntity.email)
                .set(mapOf("name" to name, "semester" to semester, "usn" to usn))
            _uiState.value = AuthUIState.ProfileComplete(userEntity)
        }
    }
}