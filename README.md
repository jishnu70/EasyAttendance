# EasyAttendance
## Attendance Management App

The Attendance Management App is an Android application designed to streamline attendance tracking for teachers and students. Built with Jetpack Compose, Room, and Firebase Firestore, it supports role-based access where teachers can manage attendance and view all students, while students can only access their own data.

## Table of Contents
- [Features](#features)
- [Architecture](#architecture)
- [Data Flow](#data-flow)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## Features
- **Role-Based Access**
  - **Teachers**: View the full list of students, add subjects, and mark attendance.
  - **Students**: Access only their own student data (e.g., attendance records).
- **Local and Cloud Storage**
  - Uses Room for offline data persistence.
  - Syncs with Firebase Firestore for real-time data across devices.
- **Secure Authentication**: Integrates Microsoft Outlook (via Azure AD) for user login.
- **Error Handling**: Robust handling of network issues, unauthorized access, and database errors.
- **Device Switching**: Automatically syncs data from Firestore when users log in on a new device.

## Architecture
The app follows the MVVM (Model-View-ViewModel) architecture with a repository pattern:
- **Model**: Entities (`UserEntity`, `StudentEntity`, `SubjectEntity`, `AttendanceEntity`) stored in Room and Firestore.
- **View**: Jetpack Compose UI for teacher and student screens.
- **ViewModel**: `TeacherViewModel` and `StudentViewModel` manage UI state and data retrieval.
- **Repository**: `UserRepository`, `StudentRepository`, `SubjectRepository`, `TeacherRepository` handle data operations with Room and Firestore.
- **Database**: Room for local storage, Firestore for cloud syncing.

## Data Flow
1. **Local Storage**: Data is first saved to Room for offline access.
2. **Cloud Sync**: Repositories sync Room data to Firestore with error handling.
3. **Retrieval**: Teachers fetch all students, students fetch their own data, with role checks enforced.

## Prerequisites
- **Android Studio**: Version 2023.1.1 or later.
- **Kotlin**: Version 1.9.0 or higher.
- **Firebase Account**: For Firestore and Authentication setup.
- **Microsoft Azure AD**: For Outlook authentication (optional, replace with Firebase Auth if preferred).
- **Dependencies**:
  - Jetpack Compose
  - Room
  - Firebase Firestore
  - Kotlin Coroutines
  - ViewModel/LiveData

## Setup Instructions
### 1. Clone the Repository
```bash
git clone https://github.com/jishnu70/EasyAttendance.git
cd AttendanceManagementApp
```
### 2. Configure Firebase
- Go to the Firebase Console and create a new project.
- Add an Android app with package name `com.example.attendancemanagementapp`.
- Download `google-services.json` and place it in `app/`.
- Enable Firestore in "Firestore Database" > "Create Database" > Start in test mode.
- Enable Authentication (if using Microsoft Outlook):
  - Set up Microsoft as a sign-in provider with your Azure AD credentials.

### 3. Update Gradle Files
Edit `app/build.gradle` to include dependencies:
```gradle
dependencies {
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.activity:activity-compose:1.8.0"
    implementation "androidx.compose.ui:ui:1.5.4"
    implementation "androidx.compose.material3:material3:1.1.2"
    implementation "androidx.room:room-runtime:2.6.1"
    kapt "androidx.room:room-compiler:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
    implementation "com.google.firebase:firebase-firestore-ktx:24.10.0"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0"
    implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.8.0"
}
```
Sync the project in Android Studio.

### 4. Build and Run
- Open the project in Android Studio.
- Connect an emulator or physical device.
- Click "Run" to build and install the app.

### 5. Firestore Structure
The app automatically creates:
- `users`: Stores `UserEntity` (email as document ID).
- `students`: Stores `StudentEntity` (ID as document ID).
- `teachers/<teacher_email>/subjects`: Stores `SubjectEntity`.
- `teachers/<teacher_email>/attendance`: Stores `AttendanceEntity`.

## Usage
- **Login**: Use Microsoft Outlook credentials to authenticate.
- **Teacher Mode**: View all students, add subjects, and mark attendance.
- **Student Mode**: View personal data in StudentScreen.
- **Switch Devices**: Log in on a new device; data syncs from Firestore automatically.

## Contributing
We welcome contributions! Follow these steps:
### 1. Fork the Repository
Click "Fork" on GitHub.

### 2. Create a Branch
```bash
git checkout -b feature/your-feature-name
```
### 3. Commit Changes
```bash
git commit -m "Add your feature"
```
### 4. Push to Your Fork
```bash
git push origin feature/your-feature-name
```
### 5. Open a Pull Request
Submit a PR with a description of your changes.

### Guidelines
- Follow Kotlin coding conventions.
- Add unit tests for new features.
- Update this README if necessary.

## Contact
For questions or support:
- **GitHub**: jishnu70
