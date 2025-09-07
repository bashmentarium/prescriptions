# Push Notifications for Medication Reminders

This document explains the push notification system implemented for medication reminders in the Whitelabel app.

## Features

- **Automatic Medication Reminders**: The app automatically schedules push notifications for medication events based on prescription schedules
- **Rich Notification Content**: Notifications include medication names, dosages, and detailed instructions
- **Quick Action Button**: Users can confirm medication intake directly from the notification
- **Navigation Integration**: Tapping the notification opens the medication confirmation screen
- **Background Scheduling**: Notifications are scheduled even when the app is closed
- **Boot Recovery**: Notifications are rescheduled after device reboot

## How It Works

### 1. Notification Scheduling
- When a prescription is created, the system automatically schedules push notifications for each medication event
- Notifications are scheduled using Android's WorkManager for reliable delivery
- Each notification is scheduled to appear at the medication time

### 2. Notification Content
- **Title**: "💊 Time for your medication!"
- **Content**: Prescription title and detailed medication information
- **Action Button**: "Confirm Intake" button for quick confirmation
- **Big Text Style**: Expanded view shows full medication details and instructions

### 3. User Interaction
- **Tap Notification**: Opens the medication confirmation screen
- **Tap Action Button**: Immediately marks medication as taken and dismisses notification
- **Swipe Away**: Notification is dismissed but medication remains unconfirmed

## Testing

### Test Notifications
The app includes test functionality in the Settings screen:

1. **Immediate Test**: Shows a notification immediately for testing
2. **Scheduled Test**: Schedules a test notification for 1 minute from now

### How to Test
1. Open the app and navigate to Settings
2. Scroll down to "Test Push Notifications" section
3. Tap "Send Test Notification Now" for immediate testing
4. Tap "Schedule Test Reminder (1 min)" for scheduled testing

## Technical Implementation

### Components

1. **MedicationNotificationService**: Handles Firebase Cloud Messaging
2. **MedicationReminderScheduler**: Manages notification scheduling
3. **MedicationNotificationReceiver**: Handles notification actions
4. **BootReceiver**: Restarts notifications after device reboot

### Dependencies
- Firebase Cloud Messaging
- Android WorkManager
- Room Database (for event storage)

### Permissions Required
- `POST_NOTIFICATIONS` (Android 13+)
- `WAKE_LOCK` (for background processing)
- `RECEIVE_BOOT_COMPLETED` (for boot recovery)

## Configuration

### Firebase Setup
1. Add your `google-services.json` file to the `app/` directory
2. Configure Firebase project with FCM enabled
3. Update the package name in Firebase console to match your app

### Notification Channel
The app creates a high-priority notification channel called "Medication Reminders" with:
- High importance level
- Vibration enabled
- Lights enabled
- Sound enabled

## Customization

### Notification Content
To customize notification content, modify the `showMedicationReminderNotification` method in `MedicationNotificationService.kt`.

### Scheduling Logic
To modify when notifications are sent, update the `MedicationReminderScheduler.kt` file.

### Styling
Notification appearance can be customized by modifying the `NotificationCompat.Builder` configuration.

## Troubleshooting

### Notifications Not Appearing
1. Check notification permissions are granted
2. Verify notification channel is created
3. Check device's notification settings for the app
4. Ensure the app is not in battery optimization mode

### Scheduled Notifications Not Working
1. Check WorkManager constraints (battery not low, etc.)
2. Verify device allows background app refresh
3. Check if the app is being killed by the system

### Firebase Issues
1. Verify `google-services.json` is properly configured
2. Check Firebase project settings
3. Ensure FCM is enabled in Firebase console

## Future Enhancements

- **Smart Reminders**: Adjust timing based on user behavior
- **Multiple Languages**: Localized notification content
- **Custom Sounds**: Medication-specific notification sounds
- **Rich Media**: Include medication images in notifications
- **Analytics**: Track notification effectiveness and user engagement
