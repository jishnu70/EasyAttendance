package com.example.attendancemanagementapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// UserDAO remains unchanged
@Dao
interface UserDAO {
    @Query("SELECT * FROM user_detail LIMIT 1")
    fun getUser(): Flow<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

// StudentDAO remains unchanged
@Dao
interface StudentDAO {
    @Query("SELECT * FROM student_detail LIMIT 1")
    fun getStudent(): Flow<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)
}

@Dao
interface SubjectDAO {
    @Query("SELECT * FROM subject_detail WHERE teacherId = :teacherId")
    fun getSubjectsForTeacher(teacherId: Int): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Query("DELETE FROM subject_detail WHERE id = :subjectId")
    suspend fun deleteSubject(subjectId: Int)

    suspend fun deleteSubject(subject: SubjectEntity) = deleteSubject(subject.id)

    // Added: Fetch teacher's email for Firestore path
    @Query("SELECT email FROM user_detail WHERE id = :teacherId")
    suspend fun getTeacherEmail(teacherId: Int): String?
}

@Dao
interface TeacherDAO {
    @Query("SELECT * FROM student_detail")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE synced = 0")
    fun getUnSyncedAttendance(): Flow<List<AttendanceEntity>>

    @Query("UPDATE attendance SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    // Added: Fetch teacher's email based on subjectId for Firestore path
    @Query("SELECT u.email FROM user_detail u JOIN subject_detail s ON u.id = s.teacherId WHERE s.id = :subjectId")
    suspend fun getTeacherEmail(subjectId: Int): String?
}