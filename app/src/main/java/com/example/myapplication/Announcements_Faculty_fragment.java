package com.example.myapplication;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Announcements_Faculty_fragment extends Fragment {
    private static final String TAG = "AnnouncementsFaculty";

    private static final String[] FILTER_CATEGORIES = {
            "All", "Academic", "Events", "Faculty", "Exams", "Admin", "Placement", "Emergency"
    };

    private static final String[] POST_CATEGORIES = {
            "Academic", "Events", "Faculty", "Exams", "Admin", "Placement", "Emergency"
    };

    private LinearLayout chipContainer;
    private LinearLayout announcementContainer;
    private TextView tvTotalAnnouncements;
    private TextView tvThisWeekAnnouncements;
    private TextView tvEmptyState;
    private ListenerRegistration announcementListener;
    private final List<DocumentSnapshot> allAnnouncements = new ArrayList<>();
    private String selectedFilter = "All";
    private String currentFacultyName = "Faculty Office";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_announcements_faculty, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chipContainer = view.findViewById(R.id.categoryChipContainer);
        announcementContainer = view.findViewById(R.id.announcementContainer);
        tvTotalAnnouncements = view.findViewById(R.id.tvTotalAnnouncements);
        tvThisWeekAnnouncements = view.findViewById(R.id.tvThisWeekAnnouncements);
        tvEmptyState = view.findViewById(R.id.tvAnnouncementEmptyState);

        setupCategoryChips();
        loadFacultyName();

        view.findViewById(R.id.btnCreateAnnouncement).setOnClickListener(v -> showCreateAnnouncementDialog());
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForAnnouncements();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (announcementListener != null) {
            announcementListener.remove();
            announcementListener = null;
        }
    }

    private void setupCategoryChips() {
        chipContainer.removeAllViews();
        for (String category : FILTER_CATEGORIES) {
            TextView chip = new TextView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMarginEnd(dpToPx(10));
            chip.setLayoutParams(params);
            chip.setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10));
            chip.setText(category);
            chip.setTextSize(13);
            chip.setSingleLine(true);
            chip.setEllipsize(TextUtils.TruncateAt.END);
            chip.setOnClickListener(v -> {
                selectedFilter = category;
                updateChipSelection();
                renderAnnouncements();
            });
            chipContainer.addView(chip);
        }
        updateChipSelection();
    }

    private void updateChipSelection() {
        for (int i = 0; i < chipContainer.getChildCount(); i++) {
            TextView chip = (TextView) chipContainer.getChildAt(i);
            boolean isSelected = selectedFilter.equals(chip.getText().toString());
            chip.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF1A2340);
        }
    }

    private void loadFacultyName() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("faculty_user")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String firstName = doc.getString("firstName");
                    String surname = doc.getString("surname");
                    String fullName = ((firstName != null ? firstName : "") + " " + (surname != null ? surname : "")).trim();
                    if (!fullName.isEmpty()) currentFacultyName = fullName;
                });
    }

    private void listenForAnnouncements() {
        if (announcementListener != null) {
            announcementListener.remove();
        }

        announcementListener = FirebaseFirestore.getInstance()
                .collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    allAnnouncements.clear();
                    allAnnouncements.addAll(value.getDocuments());
                    updateSummaryCards();
                    renderAnnouncements();
                });
    }

    private void updateSummaryCards() {
        tvTotalAnnouncements.setText(String.valueOf(allAnnouncements.size()));

        long weekStart = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        int thisWeekCount = 0;
        for (DocumentSnapshot doc : allAnnouncements) {
            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null && createdAt >= weekStart) {
                thisWeekCount++;
            }
        }
        tvThisWeekAnnouncements.setText(String.valueOf(thisWeekCount));
    }

    private void renderAnnouncements() {
        announcementContainer.removeAllViews();
        int visibleCount = 0;

        for (DocumentSnapshot doc : allAnnouncements) {
            String category = doc.getString("category");
            if (!"All".equals(selectedFilter) && !selectedFilter.equalsIgnoreCase(category)) {
                continue;
            }
            announcementContainer.addView(createAnnouncementCard(doc));
            visibleCount++;
        }

        tvEmptyState.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
    }

    private View createAnnouncementCard(DocumentSnapshot doc) {
        View itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_faculty_announcement, announcementContainer, false);

        TextView tvCategory = itemView.findViewById(R.id.tvAnnouncementCategory);
        TextView tvDate = itemView.findViewById(R.id.tvAnnouncementDate);
        TextView tvTitle = itemView.findViewById(R.id.tvAnnouncementTitle);
        TextView tvBody = itemView.findViewById(R.id.tvAnnouncementBody);
        TextView tvMeta = itemView.findViewById(R.id.tvAnnouncementMeta);
        View priorityStrip = itemView.findViewById(R.id.viewPriorityStrip);

        String category = valueOrFallback(doc.getString("category"), "General");
        String title = valueOrFallback(doc.getString("title"), "Untitled announcement");
        String body = valueOrFallback(doc.getString("body"), "No details added.");
        String author = valueOrFallback(doc.getString("authorName"), "Faculty Office");
        String audience = valueOrFallback(doc.getString("audience"), "All Campus");
        String priority = valueOrFallback(doc.getString("priority"), "Normal");
        Long createdAt = doc.getLong("createdAt");

        tvCategory.setText(category.toUpperCase(Locale.getDefault()));
        tvTitle.setText(title);
        tvBody.setText(body);
        tvMeta.setText(author + "  •  " + audience + "  •  " + priority);
        tvDate.setText(formatAnnouncementDate(createdAt));

        int accentColor = getPriorityColor(priority);
        priorityStrip.setBackgroundTintList(ColorStateList.valueOf(accentColor));
        tvCategory.setBackgroundTintList(ColorStateList.valueOf(accentColor));

        return itemView;
    }

    private void showCreateAnnouncementDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_create_announcement, null);

        EditText etTitle = dialogView.findViewById(R.id.etAnnouncementTitle);
        EditText etBody = dialogView.findViewById(R.id.etAnnouncementBody);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerAnnouncementCategory);
        Spinner spinnerAudience = dialogView.findViewById(R.id.spinnerAnnouncementAudience);
        Spinner spinnerPriority = dialogView.findViewById(R.id.spinnerAnnouncementPriority);

        spinnerCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, POST_CATEGORIES));
        spinnerAudience.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All Campus", "Students Only", "Faculty Only"}));
        spinnerPriority.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Normal", "Important", "Urgent"}));

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancelAnnouncement).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnPostAnnouncement).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String body = etBody.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String audience = spinnerAudience.getSelectedItem().toString();
            String priority = spinnerPriority.getSelectedItem().toString();

            if (title.isEmpty() || body.isEmpty()) {
                Toast.makeText(requireContext(), "Title aur announcement details dono bharna zaroori hai.", Toast.LENGTH_SHORT).show();
                return;
            }

            postAnnouncement(title, body, category, audience, priority, dialog);
        });

        dialog.show();
    }

    private void postAnnouncement(String title, String body, String category, String audience, String priority, AlertDialog dialog) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(requireContext(), "Faculty session missing hai. Dobara login karke try karo.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> announcement = new HashMap<>();
        announcement.put("title", title);
        announcement.put("body", body);
        announcement.put("category", category);
        announcement.put("audience", audience);
        announcement.put("priority", priority);
        announcement.put("authorUid", uid);
        announcement.put("authorName", currentFacultyName);
        announcement.put("createdAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("announcements")
                .add(announcement)
                .addOnSuccessListener(documentReference -> {
                    sendAnnouncementNotifications(title, body, audience);
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Announcement posted successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Announcement post nahi ho paya. Please try again.", Toast.LENGTH_SHORT).show());
    }

    private String formatAnnouncementDate(Long createdAt) {
        if (createdAt == null) return "Just now";
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(createdAt));
    }

    private int getPriorityColor(String priority) {
        if ("Urgent".equalsIgnoreCase(priority)) return 0xFFD64545;
        if ("Important".equalsIgnoreCase(priority)) return 0xFFC79A2B;
        return 0xFF1A2340;
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void sendAnnouncementNotifications(String title, String body, String audience) {
        String previewBody = body.length() > 120 ? body.substring(0, 120) + "..." : body;

        if ("Students Only".equalsIgnoreCase(audience) || "All Campus".equalsIgnoreCase(audience)) {
            notifyCollectionTokens(
                    FirebaseFirestore.getInstance().collection("users"),
                    title,
                    previewBody,
                    "announcement_posted"
            );
        }

        if ("Faculty Only".equalsIgnoreCase(audience) || "All Campus".equalsIgnoreCase(audience)) {
            notifyCollectionTokens(
                    FirebaseFirestore.getInstance().collection("faculty_user"),
                    title,
                    previewBody,
                    "announcement_posted"
            );
        }
    }

    private void notifyCollectionTokens(CollectionReference collection, String title, String body, String type) {
        collection.get().addOnSuccessListener(querySnapshot -> {
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                String token = doc.getString("fcmToken");
                if (token != null && !token.trim().isEmpty()) {
                    callFcmApi(token, "New Announcement: " + title, body, type);
                }
            }
        });
    }

    private void callFcmApi(String token, String title, String body, String type) {
        new Thread(() -> {
            try {
                InputStream is = requireContext().getAssets().open("service_account.json");
                com.google.auth.oauth2.GoogleCredentials credentials =
                        com.google.auth.oauth2.GoogleCredentials.fromStream(is)
                                .createScoped("https://www.googleapis.com/auth/firebase.messaging");
                credentials.refreshIfExpired();
                String accessToken = credentials.getAccessToken().getTokenValue();

                JSONObject data = new JSONObject();
                data.put("title", title);
                data.put("body", body);
                data.put("type", type);

                JSONObject messageObj = new JSONObject();
                messageObj.put("token", token);
                messageObj.put("data", data);

                JSONObject payload = new JSONObject();
                payload.put("message", messageObj);

                URL url = new URL("https://fcm.googleapis.com/v1/projects/collegehelpdeskproject/messages:send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "FCM error: " + e.getMessage());
            }
        }).start();
    }
}
