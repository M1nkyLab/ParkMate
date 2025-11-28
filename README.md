# ParkMate - Smart Parking System

<div align="center">
  <img src="app/src/main/res/drawable/parkmate_logo.png" alt="ParkMate Logo" width="120">
  <br>
  <b>Hassle-free parking. Book, Park, and Go.</b>
</div>

<br>

<div align="center">
  <img src="ParkMate Project.jpg" alt="ParkMate Project Banner" width="800">
</div>

---

**ParkMate** is a comprehensive Android application built with Kotlin designed to streamline the parking experience. It connects users with available parking slots while providing administrators with powerful tools to manage parking facilities, users, and bookings efficiently.

## 🌟 Key Features

Based on the project structure, the app is divided into two main roles: **User** and **Admin**.

### 🚗 User Features
* **Dual Booking Modes**:
    * **Instant Booking**: Quickly find and book a slot for immediate use.
    * **Advance Booking**: Reserve a spot ahead of time for specific dates/times.
* **Vehicle Management**: Users can add, view, and manage their registered vehicles.
* **Secure Payments**: Integrated payment simulation and booking summary screens.
* **Booking History**: View past and active parking sessions.
* **Profile Management**: Edit user details and settings.

### 🛠️ Admin Features
* **Dashboard**: A central hub to oversee system operations.
* **Parking Slot Management**: Add, edit, or remove parking slots dynamically.
* **User Administration**: View and manage registered users.
* **Booking Oversight**: Monitor all user bookings.
* **QR Scanner**: Validate bookings at the gate by scanning user QR codes.
* **Reports**: Generate and view usage reports.

## 💻 Technology Stack

* **Language**: [Kotlin](https://kotlinlang.org/)
* **UI Architecture**: XML Layouts, Fragments & Activities.
* **Backend Services**:
    * **Firebase Authentication**: For secure login/registration.
    * **Firebase Firestore**: Real-time database for storing users, slots, and bookings.
    * **Cloud Functions**: Backend logic (`parkmate-backend`).
    * **Firebase Messaging**: For push notifications.
* **IDE**: Android Studio.

## 📋 Installation Prerequisites

To run this project locally, ensure you have:

1.  **Android Studio** (Latest version recommended).
2.  **JDK 11 or higher** configured.
3.  **Firebase Project**: You must have a Firebase project set up.

## 🚀 Getting Started

Follow these steps to install and run the app:

1.  **Clone the Repository**
    ```bash
    git clone [https://github.com/your-username/ParkMate.git](https://github.com/your-username/ParkMate.git)
    cd ParkMate
    ```

2.  **Setup Firebase**
    * Go to the [Firebase Console](https://console.firebase.google.com/).
    * Create a new project.
    * Add an Android app with the package name `com.example.parkmate`.
    * Download the `google-services.json` file.
    * **Important**: Place the `google-services.json` file inside the `app/` folder.

3.  **Build and Run**
    * Open the project in **Android Studio**.
    * Let Gradle sync completely.
    * Connect your device or start an Emulator.
    * Click **Run** (green play button).

## 📂 Project Structure

```text
com.example.parkmate
├── Admin/                  # Admin Features
│   ├── Admin_MainPage.kt   # Admin Dashboard
│   ├── Admin_ScanQr.kt     # QR Code Scanner
│   ├── Admin_Manage_...    # Management screens (Slots, Users, Bookings)
│   └── ...
├── Auth/                   # Authentication
│   ├── Auth_Login.kt
│   └── Auth_Register.kt
├── User/                   # User Features
│   ├── AdvanceBooking/     # Logic for future bookings
│   ├── InstantBooking/     # Logic for immediate bookings
│   ├── User_Home.kt        # User Dashboard
│   ├── User_AddNew_Vehicle.kt
│   └── ...
└── services/               # Background Services
    └── MyFirebaseMessagingService.kt
