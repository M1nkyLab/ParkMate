// This is the corrected code for index.js using the new v2 syntax.
// It fixes the "functions.pubsub.schedule is not a function" error.

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");

// Initialize Firebase Admin SDK
initializeApp();
const db = getFirestore();

/**
 * This function runs automatically every 10 minutes.
 * It checks for "Active" bookings where the 'endTime' has passed.
 * It then updates the booking to "Expired" and sets the slot to "Available".
 */
exports.checkExpiredBookings = onSchedule("every 10 minutes", async (event) => {
    
    console.log('Running checkExpiredBookings function (v2)...');
    
    // Get the current time
    const now = Timestamp.now();

    // 1. Find all bookings that are "Active" AND where their "endTime" is in the past.
    const query = db.collection('bookings')
        .where('status', '==', 'Active') // Find cars that are currently parked
        .where('endTime', '<=', now);    // Find bookings where their time is up

    const snapshot = await query.get();

    if (snapshot.empty) {
        console.log('No expired bookings found.');
        return null; // Nothing to do
    }

    // Use a batch write to update all documents at once
    const batch = db.batch();
    let expiredCount = 0;

    snapshot.forEach(doc => {
        console.log(`Found expired booking: ${doc.id}`);
        expiredCount++;

        // 2. Update the booking status to "Expired"
        const bookingRef = doc.ref;
        batch.update(bookingRef, { status: 'Expired' });

        // 3. Update the parking_slot status to "Available"
        const slotName = doc.data().slotName; // Get the slot name (e.g., "A01")
        if (slotName) {
            const slotRef = db.collection('parking_slots').document(slotName);
            batch.update(slotRef, { status: 'Available' });
        } else {
            console.warn(`Booking ${doc.id} is missing a slotName!`);
        }
    });


    exports.sendManualOverstayNotification = onCall(async (request) => {
      const bookingId = request.data.bookingId;
      if (!bookingId) {
        throw new functions.https.HttpsError('invalid-argument', 'The function must be called with a "bookingId".');
      }

      // Step 1: Find the booking
      const bookingRef = db.collection("bookings").doc(bookingId);
      const bookingDoc = await bookingRef.get();

      if (!bookingDoc.exists) {
        // More specific error
        throw new functions.https.HttpsError('not-found', `Booking with ID '${bookingId}' was not found.`);
      }

      const booking = bookingDoc.data();
      const userId = booking.userId;

      if (!userId) {
        // More specific error
        throw new functions.https.HttpsError('not-found', `Booking '${bookingId}' is missing the 'userId' field.`);
      }

      // Step 2: Find the user
      const userRef = db.collection("users").doc(userId);
      const userDoc = await userRef.get();

      if (!userDoc.exists) {
        // More specific error
        throw new functions.https.HttpsError('not-found', `User with ID '${userId}' was not found.`);
      }

      const fcmToken = userDoc.data().fcmToken;

      if (!fcmToken) {
        // More specific error
        throw new functions.https.HttpsError('not-found', `User '${userId}' is missing the 'fcmToken' field.`);
      }

      // If we get here, everything was found. Now, send the message.
      const message = {
        notification: {
          title: "Parking Alert",
          body: "An admin has noted that your parking session has expired. Please move your vehicle.",
        },
        token: fcmToken,
      };

      try {
        await admin.messaging().send(message);
        await bookingRef.update({ manualNotificationSent: true });
        return { success: true, message: "Notification sent successfully!" };
      } catch (error) {
        console.error("Error sending manual notification:", error);
        // This error happens if the FCM token is invalid
        throw new functions.https.HttpsError('internal', 'FCM Send Failed: ' + error.message);
      }
    });

    // 4. Commit all the changes to the database
    await batch.commit();
    console.log(`Successfully processed ${expiredCount} expired bookings.`);
    return null;
});

