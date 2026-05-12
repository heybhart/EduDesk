package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class FacultyNotificationActivity extends AppCompatActivity {
    ImageView btnBack;
    RecyclerView rvNotifications;
    LinearLayout emptyState;
    NotificationAdapter adapter;
    List<NotificationModel> notificationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_faculty_notification);

        btnBack = findViewById(R.id.btnBack);
        rvNotifications = findViewById(R.id.rvNotifications);
        emptyState = findViewById(R.id.emptyState);

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, this::deleteNotification);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        loadNotifications();
    }

    private void loadNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        notificationList.clear();
                        for (DocumentSnapshot document : value.getDocuments()) {
                            NotificationModel notification = document.toObject(NotificationModel.class);
                            if (notification != null) {
                                notification.setDocumentId(document.getId());
                                notificationList.add(notification);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (notificationList.isEmpty()) {
                            emptyState.setVisibility(View.VISIBLE);
                            rvNotifications.setVisibility(View.GONE);
                        } else {
                            emptyState.setVisibility(View.GONE);
                            rvNotifications.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void deleteNotification(NotificationModel notification, int position) {
        String documentId = notification.getDocumentId();
        if (documentId == null || documentId.isEmpty()) return;

        NotificationModel removedNotification = notificationList.remove(position);
        adapter.notifyItemRemoved(position);

        if (notificationList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        }

        FirebaseFirestore.getInstance().collection("notifications")
                .document(documentId)
                .delete()
                .addOnFailureListener(e -> {
                    int restorePosition = Math.min(position, notificationList.size());
                    notificationList.add(restorePosition, removedNotification);
                    adapter.notifyItemInserted(restorePosition);
                    emptyState.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Notification delete nahi hua", Toast.LENGTH_SHORT).show();
                });
    }
}
