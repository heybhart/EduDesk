package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG        = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "notifications_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        saveToken(token);
    }

    private void saveToken(String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("faculty_user").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        db.collection("faculty_user").document(uid).set(data, SetOptions.merge());
                    } else {
                        db.collection("users").document(uid).set(data, SetOptions.merge());
                    }
                });
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "New Notification";
        String body  = "You have a new update";
        String type  = "general";
        String profileImageUrl = null;

        if (!message.getData().isEmpty()) {
            title = message.getData().get("title") != null
                    ? message.getData().get("title")
                    : (message.getData().get("senderName") != null
                    ? message.getData().get("senderName")
                    : title);
            body = message.getData().get("body") != null
                    ? message.getData().get("body")
                    : body;
            type = message.getData().get("type") != null
                    ? message.getData().get("type")
                    : "message";
            profileImageUrl = firstNonEmpty(
                    message.getData().get("profileImageUrl"),
                    message.getData().get("senderPhoto"),
                    message.getData().get("sourcePhotoUrl")
            );

            saveNotificationToFirestore(title, body, type, profileImageUrl);
        }

        showNotificationAfterRoleCheck(title, body, profileImageUrl);
    }

    private void saveNotificationToFirestore(String title, String body, String type, String profileImageUrl) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("body", body);
        notif.put("type", type);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        notif.put("userId", uid);
        notif.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
        FirebaseFirestore.getInstance().collection("notifications").add(notif);
    }

    private void showNotificationAfterRoleCheck(String title, String body, String imageUrl) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            buildAndShowNotification(title, body, makeIntent(NotificationActivity.class), imageUrl);
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("faculty_user").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Class<?> targetActivity = doc.exists() ? FacultyNotificationActivity.class : NotificationActivity.class;
                    buildAndShowNotification(title, body, makeIntent(targetActivity), imageUrl);
                })
                .addOnFailureListener(e -> {
                    buildAndShowNotification(title, body, makeIntent(NotificationActivity.class), imageUrl);
                });
    }

    private PendingIntent makeIntent(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void buildAndShowNotification(String title, String body, PendingIntent pendingIntent, String imageUrl) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Bitmap bitmap = getBitmapFromUrl(imageUrl);
            if (bitmap != null) {
                builder.setLargeIcon(bitmap);
                // Agar aap chahte hain ki bada photo bhi dikhe niche (BigPictureStyle)
                // builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null));
            }
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "App Notifications", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private Bitmap getBitmapFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Error downloading notification image: " + e.getMessage());
            return null;
        }
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
