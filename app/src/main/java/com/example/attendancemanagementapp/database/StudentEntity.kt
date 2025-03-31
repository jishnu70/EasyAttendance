package com.example.attendancemanagementapp.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "user_detail")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val isTeacher: Boolean? = null
)

@Entity(tableName = "student_detail")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val usn: String,
    val sem: Int
)

@Entity(tableName = "subject_detail")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val subjectCode: String,
    val teacherId: Int // Links to UserEntity.id
)

@Entity(tableName = "attendance",
    foreignKeys = [
        ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"]),
        ForeignKey(entity = SubjectEntity::class, parentColumns = ["id"], childColumns = ["subjectId"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentUSN: String,
    val subjectId: Int,
    val sessionCode: String,
    val date: String, // "yyyy-MM-dd"
    val classHour: String, // "HHam" or "HHpm", e.g., "11am"
    val synced: Boolean = false
)