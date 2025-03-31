package com.example.attendancemanagementapp.component.teacher

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendancemanagementapp.TeacherUIState
import com.example.attendancemanagementapp.component.WaveAnimation // Correct import
import com.example.attendancemanagementapp.database.SubjectEntity
import com.example.attendancemanagementapp.repository.SubjectRepository
import com.example.attendancemanagementapp.repository.TeacherRepository
import com.example.attendancemanagementapp.repository.UserRepository

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun TeacherScreen(
    modifier: Modifier = Modifier,
    teacherRepository: TeacherRepository,
    userRepository: UserRepository,
    subjectRepository: SubjectRepository
) {
    val context = LocalContext.current
    val factory = TeacherViewModelFactory(teacherRepository, userRepository, subjectRepository, context)
    val viewModel: TeacherViewModel = viewModel(factory = factory)
    val subjects by viewModel.subjects.collectAsState(initial = emptyList())
    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<SubjectEntity?>(null) }
    val uiState by viewModel.teacherUI.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState) {
            is TeacherUIState.Idle -> {
                Text("Manage Subjects")
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(subjects) { subject ->
                        SubjectItem(
                            subject = subject,
                            onEdit = { showEditDialog = it },
                            onDelete = { viewModel.deleteSubject(it) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showAddDialog = true }) {
                    Text("Add New Subject")
                }
                Spacer(modifier = Modifier.height(16.dp))
                SubjectDropdown(subjects, selectedSubject) { selectedSubject = it }
                Spacer(modifier = Modifier.height(16.dp))
                Box(contentAlignment = Alignment.Center) {
                    WaveAnimation(
                        waveColor = Color.Green.copy(alpha = 0.3f),
                        waveRadius = 60.dp,
                        isAnimating = uiState is TeacherUIState.Advertising || uiState is TeacherUIState.Starting
                    )
                    Button(
                        onClick = { selectedSubject?.let { viewModel.startAdvertising(it) } },
                        enabled = selectedSubject != null
                    ) { Text("Start Advertising") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.syncToFirebase() }) { Text("Sync to Cloud") }
            }
            is TeacherUIState.Starting -> {
                Text("Starting...")
            }
            is TeacherUIState.Advertising -> {
                Text("Advertising... Session ID: ${viewModel.serviceID.value}")
                Button(onClick = { viewModel.stopAdvertising() }) { Text("Stop Advertising") }
            }
            is TeacherUIState.Error -> Text("Error: ${(uiState as TeacherUIState.Error).message}")
            is TeacherUIState.SyncCount -> {
                Text("Synced ${(uiState as TeacherUIState.SyncCount).count} records to Firebase")
                Button(onClick = { viewModel.resetToIdle() }) { Text("Start New Session") }
            }
            is TeacherUIState.SyncError -> Text("Sync Error: ${(uiState as TeacherUIState.SyncError).message}")
        }
    }

    if (showAddDialog) {
        AddSubjectDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, code -> viewModel.addSubject(name, code); showAddDialog = false }
        )
    }

    showEditDialog?.let { subject ->
        EditSubjectDialog(
            subject = subject,
            onDismiss = { showEditDialog = null },
            onEdit = { name, code -> viewModel.editSubject(subject.copy(subjectName = name, subjectCode = code)); showEditDialog = null }
        )
    }
}

@Composable
fun SubjectItem(subject: SubjectEntity, onEdit: (SubjectEntity) -> Unit, onDelete: (SubjectEntity) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${subject.subjectName} (${subject.subjectCode})")
        Row {
            IconButton(onClick = { onEdit(subject) }) {
                Text("Edit")
            }
            IconButton(onClick = { onDelete(subject) }) {
                Text("Delete")
            }
        }
    }
}

@Composable
fun SubjectDropdown(subjects: List<SubjectEntity>, selected: SubjectEntity?, onSelect: (SubjectEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(selected?.subjectName ?: "Select Subject")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            subjects.forEach { subject ->
                DropdownMenuItem(
                    text = { Text("${subject.subjectName} (${subject.subjectCode})") },
                    onClick = {
                        onSelect(subject)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddSubjectDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var subjectName by remember { mutableStateOf("") }
    var subjectCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Subject") },
        text = {
            Column {
                TextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject Name") })
                TextField(value = subjectCode, onValueChange = { subjectCode = it }, label = { Text("Subject Code") })
            }
        },
        confirmButton = {
            Button(onClick = { if (subjectName.isNotBlank() && subjectCode.isNotBlank()) onAdd(subjectName, subjectCode) }) {
                Text("Add")
            }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditSubjectDialog(subject: SubjectEntity, onDismiss: () -> Unit, onEdit: (String, String) -> Unit) {
    var subjectName by remember { mutableStateOf(subject.subjectName) }
    var subjectCode by remember { mutableStateOf(subject.subjectCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Subject") },
        text = {
            Column {
                TextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject Name") })
                TextField(value = subjectCode, onValueChange = { subjectCode = it }, label = { Text("Subject Code") })
            }
        },
        confirmButton = {
            Button(onClick = { if (subjectName.isNotBlank() && subjectCode.isNotBlank()) onEdit(subjectName, subjectCode) }) {
                Text("Save")
            }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}