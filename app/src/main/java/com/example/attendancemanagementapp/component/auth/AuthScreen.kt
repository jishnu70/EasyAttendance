package com.example.attendancemanagementapp.component.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendancemanagementapp.AuthUIState
import com.example.attendancemanagementapp.MicrosoftAuthUiClient
import com.example.attendancemanagementapp.repository.UserRepository

@Composable
fun AuthScreen(
    modifier: Modifier,
    authUiClient: MicrosoftAuthUiClient,
    userRepository: UserRepository,
    onNavigateToTeacher: () -> Unit,
    onNavigateToStudent: () -> Unit,
) {
    val factory = AuthViewModelFactory(authUiClient, userRepository)
    val viewModel: AuthViewModel = viewModel(factory = factory)

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = viewModel.uiState.collectAsState().value) {
            is AuthUIState.CheckingUser -> Text("Checking user...")
            is AuthUIState.LoginRequired -> {
                Text("Please Sign In")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.signInUsingFireBase() }) {
                    Text("Sign In with Microsoft")
                }
            }
            is AuthUIState.RoleSelection -> {
                Text("Welcome, ${state.userEntity?.name ?: "User"}! Please select your role")
                Button(onClick = { viewModel.updateUserRole(true) }) {
                    Text("I am a Teacher")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.updateUserRole(false) }) {
                    Text("I am a Student")
                }
            }
            is AuthUIState.TeacherSubjectInput -> {
                TeacherSubjectInputScreen(
                    onSubjectSubmitted = { name, code ->
                        viewModel.saveTeacherSubject(name, code)
                    }
                )
            }
            is AuthUIState.StudentInfoInput -> {
                StudentInfoInputScreen(
                    onInfoSubmitted = { name, semester, usn ->
                        viewModel.saveStudentInfo(state.userEntity, name, semester, usn)
                    }
                )
            }
            is AuthUIState.ProfileComplete -> {
                LaunchedEffect(state) {
                    if (state.userEntity.isTeacher == true) onNavigateToTeacher() else onNavigateToStudent()
                }
            }
            is AuthUIState.Loading -> Text("Signing in...")
            is AuthUIState.Success -> {
                Text("Signed in as ${state.userEntity?.name ?: "Unknown"}")
                LaunchedEffect(state) {
                    val user = state.userEntity
                    if (user?.isTeacher == null) {
                        viewModel.updateUIState(AuthUIState.RoleSelection(user))
                    } else if (user.isTeacher == true && viewModel.needsSubjectInput()) {
                        viewModel.updateUIState(AuthUIState.TeacherSubjectInput)
                    } else if (user.isTeacher == false && user.usn.isNullOrEmpty()) {
                        viewModel.updateUIState(AuthUIState.StudentInfoInput(user))
                    } else {
                        viewModel.updateUIState(AuthUIState.ProfileComplete(user))
                    }
                }
            }
            is AuthUIState.Error -> Text("Error: ${state.message}")
        }
    }
}

@Composable
fun StudentInfoInputScreen(onInfoSubmitted: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var usn by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Enter Your Details")
        TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        TextField(value = semester, onValueChange = { semester = it }, label = { Text("Semester") })
        TextField(value = usn, onValueChange = { usn = it }, label = { Text("USN") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (name.isNotBlank() && semester.isNotBlank() && usn.isNotBlank()) {
                onInfoSubmitted(name, semester, usn)
            }
        }) { Text("Submit Details") }
    }
}

@Composable
fun TeacherSubjectInputScreen(onSubjectSubmitted: (String, String) -> Unit) {
    var subjectName by remember { mutableStateOf("") }
    var subjectCode by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Enter Subject Details")
        TextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject Name") })
        TextField(value = subjectCode, onValueChange = { subjectCode = it }, label = { Text("Subject Code") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (subjectName.isNotBlank() && subjectCode.isNotBlank()) {
                onSubjectSubmitted(subjectName, subjectCode)
            }
        }) { Text("Submit Subject") }
    }
}