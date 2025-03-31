package com.example.attendancemanagementapp.component.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendancemanagementapp.StudentUIState
import com.example.attendancemanagementapp.component.WaveAnimation
import com.example.attendancemanagementapp.database.StudentEntity
import com.example.attendancemanagementapp.database.UserEntity
import com.example.attendancemanagementapp.repository.StudentRepository
import com.example.attendancemanagementapp.repository.UserRepository

@Composable
fun StudentScreen(
    modifier: Modifier,
    studentRepository: StudentRepository,
    userRepository: UserRepository
) {
    val context = LocalContext.current
    val factory = StudentViewModelFactory(studentRepository, userRepository, context)
    val viewModel: StudentViewModel = viewModel(factory = factory)

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleared() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = viewModel.studentUiState.collectAsState().value) {
            is StudentUIState.Idle -> {
                Text("Ready to Discover Teacher")
                Spacer(modifier = Modifier.height(16.dp))
                SessionIDInputField(onSubmit = { viewModel.updateServiceID(it) })
                Spacer(modifier = Modifier.height(16.dp))
                Box(contentAlignment = Alignment.Center) {
                    WaveAnimation(
                        waveColor = Color.Blue.copy(alpha = 0.3f),
                        waveRadius = 60.dp,
                        isAnimating = state is StudentUIState.Discovering
                    )
                    Button(
                        onClick = { viewModel.discoverTeacher() },
                        enabled = viewModel.serviceID.value.isNotBlank()
                    ) { Text("Discover Teacher") }
                }
            }
            is StudentUIState.InfoRequired -> {
                StudentInfoScreen(
                    userEntity = state.userEntity,
                    onStudentInfoSubmitted = { viewModel.saveStudentInfo(it) }
                )
            }
            is StudentUIState.Discovering -> Text("Discovering teacher...")
            is StudentUIState.TeacherDiscovered -> Text("Teacher discovered: ${state.endpoint}")
            is StudentUIState.Connected -> Text("Connected to teacher")
            is StudentUIState.SendingAttendance -> Text("Sending attendance...")
            is StudentUIState.AttendanceSent -> {
                Text("Attendance sent")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.resetToIdle() }) { Text("Back") }
            }
            is StudentUIState.Error -> Text("Error: ${state.message}")
        }
    }
}

// ... StudentInfoScreen, SessionIDInputField remain unchanged ...

@Composable
fun StudentInfoScreen(userEntity: UserEntity, onStudentInfoSubmitted: (StudentEntity) -> Unit) {
    var name by remember { mutableStateOf(userEntity.name) }
    var usn by remember { mutableStateOf("") }
    var sem by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Enter Your Details")
        TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        TextField(value = usn, onValueChange = { usn = it }, label = { Text("USN") })
        TextField(
            value = sem,
            onValueChange = { sem = it },
            label = { Text("Semester") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (name.isNotBlank() && usn.isNotBlank() && sem.isNotBlank()) {
                onStudentInfoSubmitted(StudentEntity(name = name, usn = usn, sem = sem.toInt()))
            }
        }) { Text("Submit") }
    }
}

@Composable
fun SessionIDInputField(onSubmit: (String) -> Unit) {
    val sessionLength = 6
    var sessionCode by remember { mutableStateOf(List(sessionLength) { "" }) }
    val focusRequesters = List(sessionLength) { FocusRequester() }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        sessionCode.forEachIndexed { index, value ->
            TextField(
                value = value,
                onValueChange = { input ->
                    if (input.length <= 1) {
                        val newSessionCode = sessionCode.toMutableList()
                        newSessionCode[index] = input
                        sessionCode = newSessionCode
                        if (input.isNotBlank() && index < sessionLength - 1) {
                            focusRequesters[index + 1].requestFocus()
                        }
                        if (sessionCode.all { it.isNotBlank() }) {
                            onSubmit(sessionCode.joinToString(""))
                        }
                    }
                },
                modifier = Modifier
                    .width(50.dp)
                    .focusRequester(focusRequesters[index])
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Backspace && value.isEmpty() && index > 0) {
                            val newSession = sessionCode.toMutableList()
                            newSession[index - 1] = ""
                            sessionCode = newSession
                            focusRequesters[index - 1].requestFocus()
                            true
                        } else false
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}