# Notification Debug Guide

## Issues Fixed

### 1. **WorkManager Constraints Too Restrictive**
**Problem**: The original constraints required `setRequiresBatteryNotLow(true)`, which prevented notifications when battery was low.

**Fix**: Changed constraints to be more permissive:
```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
    .setRequiresBatteryNotLow(false) // Allow even when battery is low
    .setRequiresCharging(false) // Don't require charging
    .setRequiresDeviceIdle(false) // Don't require device to be idle
    .build()
```

### 2. **Missing Debug Logging**
**Problem**: No visibility into what was happening with notifications.

**Fix**: Added comprehensive logging throughout the notification system:
- Channel creation logging
- Notification posting logging
- WorkManager execution logging
- Permission status logging

### 3. **Notification Channel Configuration**
**Problem**: Basic channel setup without proper configuration.

**Fix**: Enhanced channel setup with:
- Badge support
- Better logging
- Proper importance level

### 4. **Permission Handling**
**Problem**: Permission request might not be working properly.

**Fix**: Added logging to track permission requests and status.

## Testing Steps

### Step 1: Test Basic Notification System
1. Open the app
2. Go to Settings
3. Scroll to "Test Push Notifications" section
4. Tap **"Simple Test (No WorkManager)"** button
5. Check if notification appears immediately

**Expected Result**: You should see a simple test notification immediately.

### Step 2: Test Medication Notification System
1. In the same section, tap **"Send Test Notification Now"** button
2. Check if medication-style notification appears

**Expected Result**: You should see a medication reminder notification with action button.

### Step 3: Test Scheduled Notifications
1. Tap **"Schedule Test Reminder (1 min)"** button
2. Wait 1 minute
3. Check if notification appears

**Expected Result**: You should see a notification after 1 minute.

## Debugging with Logs

### View Logs in Android Studio
1. Open Android Studio
2. Go to View → Tool Windows → Logcat
3. Filter by these tags:
   - `MedicationNotification`
   - `TestNotificationService`
   - `MedicationReminderScheduler`
   - `SimpleNotificationTest`
   - `MainActivity`

### Key Log Messages to Look For

#### Successful Flow:
```
MainActivity: Requesting notification permission for Android 13+
MedicationNotification: Creating notification channel...
MedicationNotification: Notification channel created successfully
SimpleNotificationTest: Attempting to show simple test notification...
SimpleNotificationTest: Notifications enabled: true
SimpleNotificationTest: Simple test notification posted successfully
```

#### Permission Issues:
```
SimpleNotificationTest: Notifications enabled: false
SimpleNotificationTest: Notifications are disabled for this app
```

#### WorkManager Issues:
```
MedicationReminderWorker: Starting medication reminder check...
MedicationReminderWorker: Found X upcoming events
SpecificMedicationReminderWorker: Starting specific medication reminder...
```

## Common Issues and Solutions

### Issue 1: "Notifications are disabled for this app"
**Solution**: 
1. Go to Android Settings → Apps → Whitelabel → Notifications
2. Enable notifications
3. Make sure the notification channel is enabled

### Issue 2: No logs appearing
**Solution**:
1. Make sure you're filtering logs correctly
2. Check if the app is actually running
3. Try restarting the app

### Issue 3: WorkManager not executing
**Solution**:
1. Check if the device has battery optimization enabled for the app
2. Go to Settings → Apps → Whitelabel → Battery → Optimize battery usage → Don't optimize
3. Check if the app is in the background app refresh list

### Issue 4: Notifications appear but don't work
**Solution**:
1. Check if the notification channel is properly configured
2. Verify the notification ID is unique
3. Check if there are any security exceptions in logs

## Manual Testing Commands

### Check Notification Permissions
```bash
adb shell dumpsys notification | grep -A 5 "com.example.whitelabel"
```

### Check WorkManager Status
```bash
adb shell dumpsys jobscheduler | grep -A 10 "com.example.whitelabel"
```

### Force Show Notification (for testing)
```bash
adb shell am broadcast -a android.intent.action.SHOW_NOTIFICATION --es "package" "com.example.whitelabel"
```

## Next Steps if Still Not Working

1. **Check Device Settings**:
   - Ensure notifications are enabled for the app
   - Check if the app is in battery optimization whitelist
   - Verify notification channels are enabled

2. **Check App Permissions**:
   - Go to Settings → Apps → Whitelabel → Permissions
   - Ensure all required permissions are granted

3. **Test on Different Device**:
   - Try on a different Android device
   - Test on different Android versions

4. **Check Firebase Configuration**:
   - Verify `google-services.json` is properly configured
   - Check Firebase console for any issues

5. **Review Logs**:
   - Look for any error messages in Logcat
   - Check for security exceptions or permission denials

## Expected Behavior After Fixes

- **Immediate Test**: Should show notification within 1-2 seconds
- **Scheduled Test**: Should show notification after exactly 1 minute
- **Medication Test**: Should show rich notification with action button
- **Logs**: Should show detailed logging of the entire process

If you're still not seeing notifications after following this guide, please share the relevant log output so we can identify the specific issue.
