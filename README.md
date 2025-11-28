# 🅿️ ParkMate - Smart Parking Management System

ParkMate is a complete, end-to-end Android application designed to modernize and streamline the parking experience. It consists of two main components: a feature-rich **User App** for finding, reserving, and paying for parking, and a powerful **Admin App** for managing the entire system, including real-time QR code scanning for gate access.

This system is built entirely on **Kotlin** for native Android performance and uses **Google Firebase** for its backend, including Authentication, Firestore (database), and ML Kit (for QR scanning).

<div align="center">
  <img src="ParkMate Project.jpg" alt="ParkMate App Screenshot" width="600">
  <br><br>
</div>

---

## 🚀 Features

### 📱 User Application

* **Firebase Authentication:** Secure login, registration, and Google Sign-In options.
* **Vehicle Management:** Users can add, view, edit, and delete their vehicle license plates in their profile.
* **Instant Booking:** View a real-time grid of all parking slots, see which are "Available" or "Occupied," and book a spot immediately.
* **Advance Booking:** Reserve a parking spot for a future date and time. The app checks for booking conflicts to only show available slots.
* **Dynamic Pricing:** Hourly rates are fetched from Firestore, and the total price is calculated based on the selected duration.
* **Payment Flow:** A multi-step (simulated) payment process to confirm and pay for bookings.
* **Booking History:** A complete list of all past and active bookings.
* **QR Code Access:** A unique QR code is generated for every active booking, used to scan at the gate for entry and exit.

### 👮 Admin Application

* **Admin Authentication:** Separate, secure login for administrators (based on a "role" field in Firestore).
* **Main Dashboard:** A central hub to navigate to all management functions.
* **User Management:** View a list of all registered users, edit user details (username, email), and change user roles (e.g., promote a user to "admin").
* **Parking Slot Management:** Full CRUD (Create, Read, Delete) for all parking slots in the system (e.g., add "A05", delete "B02").
* **Live Booking Viewer:** A real-time `RecyclerView` that listens for all new bookings and status changes as they happen.
* **Filter & Search:** Admins can filter the booking list by status (`Booked`, `Parked`, `Exited`) or type (`Instant`, `Reserve`) and search by vehicle plate number.
* **Revenue Dashboard:** A simple report screen that calculates and displays Total Users, Total Bookings, and Total Revenue (by summing the `price` field from all bookings).
* **🔑 Core Feature: QR Code Scanner:**
    * Uses **CameraX** and **Google ML Kit** to scan user QR codes at the gate.
    * **Smart Entry:** When a "Booked" QR is scanned, the app automatically:
        1.  Updates the booking `status` to "Parked".
        2.  Calculates and sets the `startTime` and `endTime`.
        3.  Updates the `parking_slots` collection to set the slot's `status` to "Occupied".
    * **Smart Exit:** When a "Parked" QR is scanned, the app automatically:
        1.  Updates the booking `status` to "Exited".
        2.  Updates the `parking_slots` collection to set the slot's `status` to "Available".

---

## 🛠️ Technology Stack

* **Language:** [Kotlin](https://kotlinlang.org/) (100%)
* **Platform:** Android Native
* **Database:** [Firebase Firestore](https://firebase.google.com/products/firestore) (Real-time NoSQL Database)
* **Authentication:** [Firebase Authentication](https://firebase.google.com/products/auth) (Email/Password & Google)
* **UI:** Android XML with Material Design 3 components (`CardView`, `BottomNavigationView`, `RecyclerView`, `ConstraintLayout`)
* **QR Code Generation (User):** `ZXing (Zebra Crossing)`
* **QR Code Scanning (Admin):** `Google ML Kit (Barcode Scanning)` & `CameraX`

---

## 📊 Data Flow & Architecture

The app's core logic revolves around the `bookings` and `parking_slots` collections in Firestore.



1.  **User Booking:**
    * A user selects an "Available" slot (from `parking_slots`).
    * Upon payment, the app performs a dual-write:
        1.  Creates a new document in the `bookings` collection (e.g., `bookingId: "12345"`, `status: "Booked"`).
        2.  Updates the document in the `parking_slots` collection (e.g., `slotId: "A01"`, `status: "Booked"`).
    * The app generates a QR code containing the `bookingId` ("12345").

2.  **Admin Scanning (Entry):**
    * Admin scans the user's QR code.
    * The app reads the `bookingId` ("12345") and fetches the document from the `bookings` collection.
    * It sees the `status` is "Booked".
    * It performs another dual-write:
        1.  Updates the `bookings` document (`status: "Parked"`, `startTime: NOW`, `endTime: NOW + duration`).
        2.  Updates the `parking_slots` document (`status: "Occupied"`).
    * The admin grants entry.

3.  **Admin Scanning (Exit):**
    * User leaves and scans the *same* QR code.
    * The app fetches the `bookings` document ("12345").
    * It sees the `status` is "Parked".
    * It performs a final dual-write:
        1.  Updates the `bookings` document (`status: "Exited"`).
        2.  Updates the `parking_slots` document (`status: "Available"`).
    * The slot is now free for the next user.

---

## 📸 Screenshots (Add your own!)

### User App
| Home Screen | Slot Selection (Instant) | Booking History (with QR) |
| :---: | :---: | :---: |
| `[Add Screenshot]` | `[Add Screenshot]` | `[Add Screenshot]` |

### Admin App
| Admin Dashboard | Manage Bookings (Live) | Manage Users | QR Code Scanner |
| :---: | :---: | :---: | :---: |
| `[Add Screenshot]` | `[Add Screenshot]` | `[Add Screenshot]` | `[Add Screenshot]` |

---

## 🚀 Getting Started

### 1. Prerequisites

* Android Studio (latest version)
* A Google account for Firebase

### 2. Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/your-repo-name.git](https://github.com/your-username/your-repo-name.git)
    ```
2.  **Open in Android Studio:**
    * Open Android Studio and select "Open an existing project."
    * Navigate to the cloned directory and open it.

### 3. Firebase Setup

This project **will not run** without connecting it to your own Firebase project.

1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Create a new project.
3.  Add a new Android app to the project:
    * Use the package name: `com.example.parkmate`
4.  Download the **`google-services.json`** file and place it in the **`app/`** directory of the project.
5.  In the Firebase Console, enable the following services:
    * **Authentication:** Enable the "Email/Password" and "Google" sign-in methods.
    * **Firestore:** Create a new Firestore database.

6.  **Create Firestore Collections:**
    You must create the following collections and documents for the app to work:

    * **Collection:** `users`
        * (Documents will be created automatically on user registration. Ensure you set a `role: "admin"` for your test admin account).
    * **Collection:** `parking_slots`
        * (Manually add a few documents, e.g., `Document ID: A01` with fields `slotId: "A01"` and `status: "Available"`).
    * **Collection:** `rates`
        * Create **one** document with the ID `standard`.
        * Add fields to this document where the **field name is the hour** and the value is the price (e.g., `1: 3.00`, `2: 5.00`, `3: 7.00`).
    * **Collection:** `bookings`
        * (This will be populated by the app).

7.  **Build and Run:**
    * Let Android Studio sync all Gradle dependencies.
    * Build and run the application on an Android device or emulator. You will need to run both the User and Admin apps.

