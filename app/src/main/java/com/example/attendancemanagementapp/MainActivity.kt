package com.example.attendancemanagementapp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.attendancemanagementapp.component.auth.AuthScreen
import com.example.attendancemanagementapp.component.student.StudentScreen
import com.example.attendancemanagementapp.component.teacher.TeacherScreen
import com.example.attendancemanagementapp.database.AppDatabase
import com.example.attendancemanagementapp.repository.StudentRepository
import com.example.attendancemanagementapp.repository.SubjectRepository
import com.example.attendancemanagementapp.repository.TeacherRepository
import com.example.attendancemanagementapp.repository.UserRepository
import com.example.attendancemanagementapp.ui.theme.AttendanceManagementAppTheme

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE_PERMISSION = 101
    private lateinit var authUiClient: MicrosoftAuthUiClient
    private lateinit var userRepository: UserRepository
    private lateinit var studentRepository: StudentRepository
    private lateinit var teacherRepository: TeacherRepository

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        userRepository = UserRepository(db.userDAO())
        studentRepository = StudentRepository(db.studentDAO())
        teacherRepository = TeacherRepository(db.teacherDAO())
        val subjectRepository = SubjectRepository(db.subjectDAO())
        authUiClient = MicrosoftAuthUiClient(this, this, db.userDAO())

        checkAndRequestPermissions() // Initial check

        if (!GooglePlayServiceCheck.isAvailable(this)) {
            Toast.makeText(this, "Google Play Services unavailable", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            AttendanceManagementAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AttendanceApp(
                        modifier = Modifier.padding(innerPadding),
                        authUiClient = authUiClient,
                        userRepository = userRepository,
                        studentRepository = studentRepository,
                        teacherRepository = teacherRepository,
                        subjectRepository = subjectRepository
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!PermissionHelper.hasPermission(this)) {
            PermissionHelper.requestPermission(this, REQUEST_CODE_PERMISSION)
        } else {
            Toast.makeText(this, "All Permissions Granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions denied! App may not work as expected.", Toast.LENGTH_LONG).show()
                checkAndRequestPermissions() // Retry prompt
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AttendanceApp(
    modifier: Modifier,
    authUiClient: MicrosoftAuthUiClient,
    userRepository: UserRepository,
    studentRepository: StudentRepository,
    teacherRepository: TeacherRepository,
    subjectRepository: SubjectRepository // Already added
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(
                modifier = modifier,
                authUiClient = authUiClient,
                userRepository = userRepository,
                onNavigateToTeacher = { navController.navigate("teacher") },
                onNavigateToStudent = { navController.navigate("student") }
            )
        }
        composable("teacher") {
            TeacherScreen(
                modifier = modifier,
                teacherRepository = teacherRepository,
                userRepository = userRepository,
                subjectRepository = subjectRepository // Pass it here
            )
        }
        composable("student") {
            StudentScreen(
                modifier = modifier,
                studentRepository = studentRepository,
                userRepository = userRepository
            )
        }
    }
}