const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { onCall } = require("firebase-functions/v2/https");

admin.initializeApp();
const db = admin.firestore();

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
  // if want to update just edit the message here and then in shell type "firebase deploy --only functions"
  const message = {
    notification: {
      title: "Parking Alert",
      body: "⏰ Time’s up! Please leave your parking spots.",
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