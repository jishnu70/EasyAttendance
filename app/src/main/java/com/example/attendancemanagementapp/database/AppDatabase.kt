package com.example.attendancemanagementapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserEntity::class, StudentEntity::class, SubjectEntity::class, AttendanceEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDAO(): UserDAO
    abstract fun studentDAO(): StudentDAO
    abstract fun subjectDAO(): SubjectDAO
    abstract fun teacherDAO(): TeacherDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2 to add attendance table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE attendance (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "studentId INTEGER NOT NULL, " +
                            "date TEXT NOT NULL, " +
                            "classHour TEXT NOT NULL, " +
                            "sessionCode TEXT NOT NULL)"
                )
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE subject_detail (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "subjectName TEXT NOT NULL, " +
                            "subjectCode TEXT NOT NULL, " +
                            "teacherId INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE new_attendance (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "studentId INTEGER NOT NULL, " +
                            "subjectId INTEGER NOT NULL, " +
                            "sessionCode TEXT NOT NULL, " +
                            "date TEXT NOT NULL, " +
                            "classHour TEXT NOT NULL, " +
                            "synced INTEGER NOT NULL DEFAULT 0, " +
                            "FOREIGN KEY (studentId) REFERENCES student_detail(id), " +
                            "FOREIGN KEY (subjectId) REFERENCES subject_detail(id))"
                )
                database.execSQL("INSERT INTO new_attendance (id, studentId, sessionCode, date, synced) SELECT id, studentId, sessionCode, date, synced FROM attendance")
                database.execSQL("DROP TABLE attendance")
                database.execSQL("ALTER TABLE new_attendance RENAME TO attendance")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context = context,
                    klass = AppDatabase::class.java,
                    name = "attendance_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}