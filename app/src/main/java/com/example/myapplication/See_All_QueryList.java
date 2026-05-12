package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class See_All_QueryList extends AppCompatActivity {

    private TextView tabAll, tabOpen, tabResolved;
    private LinearLayout ticketListContainer;
    private EditText etSearch;
    private FacultyViewModel vm;
    private int currentTab = 0; // 0: All (Open + Active), 1: My Tickets (Assigned), 2: Resolved
    private List<DocumentSnapshot> allTickets = new ArrayList<>();
    private String searchQuery = "";
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_all_query_list);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        currentUid = FirebaseAuth.getInstance().getUid();
        ticketListContainer = findViewById(R.id.ticketListContainer);
        etSearch = findViewById(R.id.etSearch);
        tabAll = findViewById(R.id.tabAll);
        tabOpen = findViewById(R.id.tabOpen); // Labelled "My Tickets" in XML
        tabResolved = findViewById(R.id.tabResolved);

        vm = new ViewModelProvider(this).get(FacultyViewModel.class);
        vm.fetchIfNeeded();

        vm.getTicketsData().observe(this, tickets -> {
            if (tickets != null) {
                allTickets = tickets;
                filterAndRender();
            }
        });

        tabAll.setOnClickListener(v -> selectTab(0));
        tabOpen.setOnClickListener(v -> selectTab(1));
        tabResolved.setOnClickListener(v -> selectTab(2));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                filterAndRender();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnFilter).setOnClickListener(v ->
                Toast.makeText(this, "Filter options", Toast.LENGTH_SHORT).show());
    }

    private void selectTab(int selected) {
        currentTab = selected;

        tabAll.setBackgroundResource(R.drawable.bg_tab_normal);
        tabOpen.setBackgroundResource(R.drawable.bg_tab_normal);
        tabResolved.setBackgroundResource(R.drawable.bg_tab_normal);

        tabAll.setTextColor(0xFF555555);
        tabOpen.setTextColor(0xFF555555);
        tabResolved.setTextColor(0xFF555555);

        TextView activeTab = (selected == 0) ? tabAll :
                (selected == 1) ? tabOpen : tabResolved;

        activeTab.setBackgroundResource(R.drawable.bg_tab_selected);
        activeTab.setTextColor(0xFFFFFFFF);

        filterAndRender();
    }

    private void filterAndRender() {
        ticketListContainer.removeAllViews();
        List<DocumentSnapshot> filteredList = new ArrayList<>();

        for (DocumentSnapshot doc : allTickets) {
            String status = doc.getString("status");
            String assignedUid = doc.getString("assignedFacultyUid");
            String subject = doc.getString("subject") != null ? doc.getString("subject").toLowerCase() : "";
            String studentName = doc.getString("studentName") != null ? doc.getString("studentName").toLowerCase() : "";
            String ticketId = doc.getId().toLowerCase();

            // Filter by Tab logic
            boolean matchesTab = false;
            if (currentTab == 0) {
                if (!"Resolved".equalsIgnoreCase(status)) {
                    matchesTab = true;
                }
            } else if (currentTab == 1) {
                if (currentUid.equals(assignedUid) && !"Resolved".equalsIgnoreCase(status)) {
                    matchesTab = true;
                }
            } else if (currentTab == 2) {
                if ("Resolved".equalsIgnoreCase(status) && currentUid.equals(assignedUid)) {
                    matchesTab = true;
                }
            }

            // Filter by Search
            boolean matchesSearch = searchQuery.isEmpty() ||
                    subject.contains(searchQuery) ||
                    studentName.contains(searchQuery) ||
                    ticketId.contains(searchQuery);

            if (matchesTab && matchesSearch) {
                filteredList.add(doc);
            }
        }

        for (DocumentSnapshot doc : filteredList) {
            ticketListContainer.addView(createTicketView(doc));
        }
    }

    private View createTicketView(DocumentSnapshot doc) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_ticket_faculty, ticketListContainer, false);
        TextView tvIdSubject = v.findViewById(R.id.tvIdSubject);
        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvMeta = v.findViewById(R.id.tvMeta);
        TextView tvStatusBadge = v.findViewById(R.id.tvStatusBadge);
        TextView tvPriorityBadge = v.findViewById(R.id.tvPriorityBadge);
        View leftBar = v.findViewById(R.id.leftBar);

        String subject = doc.getString("subject");
        String category = doc.getString("category");
        String priority = doc.getString("priority");
        String status = doc.getString("status");
        String assignedUid = doc.getString("assignedFacultyUid");
        Long createdAt = doc.getLong("createdAt");

        tvIdSubject.setText("#" + doc.getId().substring(0, 4).toUpperCase() + " \u00B7 " + category);
        tvTitle.setText(subject);

        String timeStr = "Just now";
        if (createdAt != null) {
            long diff = System.currentTimeMillis() - createdAt;
            long mins = diff / (1000 * 60);
            if (mins < 60) timeStr = mins + " mins ago";
            else timeStr = (mins / 60) + " hours ago";
        }
        tvMeta.setText(category + " \u00B7 " + timeStr);
        applyPriorityBadge(priority, tvPriorityBadge);

        // Styling based on status and assignment
        if ("Open".equalsIgnoreCase(status)) {
            tvStatusBadge.setVisibility(View.VISIBLE);
            tvStatusBadge.setText("OPEN");
            tvStatusBadge.setTextColor(0xFF3F51B5);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_not_opened_badge);
        } else if ("In Progress".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status)) {
            tvStatusBadge.setVisibility(View.VISIBLE);
            if (currentUid != null && currentUid.equals(assignedUid)) {
                // If assigned to current faculty
                tvStatusBadge.setText("ACTIVE");
                tvStatusBadge.setTextColor(0xFF22A55B);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active_badge);
            } else {
                // If assigned to someone else
                tvStatusBadge.setText("ASSIGNED");
                tvStatusBadge.setTextColor(0xFF888880);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_tab_normal);
            }
        } else if ("Resolved".equalsIgnoreCase(status)) {
            tvStatusBadge.setVisibility(View.VISIBLE);
            tvStatusBadge.setText("RESOLVED");
            tvStatusBadge.setTextColor(0xFF888880);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_tab_normal);
        }

        leftBar.setBackgroundColor(getPriorityAccentColor(priority));

        v.setOnClickListener(view -> showTicketDetailDialog(doc));
        return v;
    }

    private void applyPriorityBadge(String priority, TextView tvPriorityBadge) {
        String safePriority = normalizePriority(priority);
        tvPriorityBadge.setText(safePriority.toUpperCase(Locale.ROOT) + " PRIORITY");
        tvPriorityBadge.setTextColor(getPriorityTextColor(safePriority));
        tvPriorityBadge.setBackgroundResource(getPriorityBackground(safePriority));
    }

    private String normalizePriority(String priority) {
        if (priority == null) return "Low";
        String value = priority.trim();
        if ("high".equalsIgnoreCase(value)) return "High";
        if ("medium".equalsIgnoreCase(value)) return "Medium";
        return "Low";
    }

    private int getPriorityBackground(String priority) {
        switch (normalizePriority(priority).toLowerCase(Locale.ROOT)) {
            case "high":
                return R.drawable.bg_priority_high_badge;
            case "medium":
                return R.drawable.bg_priority_medium_selected;
            default:
                return R.drawable.bg_priority_low_selected;
        }
    }

    private int getPriorityTextColor(String priority) {
        switch (normalizePriority(priority).toLowerCase(Locale.ROOT)) {
            case "high":
                return 0xFFE53935;
            case "medium":
                return 0xFF1565C0;
            default:
                return 0xFF2E7D32;
        }
    }

    private int getPriorityAccentColor(String priority) {
        return getPriorityTextColor(priority);
    }

    private void showTicketDetailDialog(DocumentSnapshot doc) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ticket_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog).setView(dialogView).create();

        TextView dialogTicketId = dialogView.findViewById(R.id.dialogTicketId);
        TextView dialogSubject = dialogView.findViewById(R.id.dialogSubject);
        TextView dialogStudentName = dialogView.findViewById(R.id.dialogStudentName);
        TextView dialogDescription = dialogView.findViewById(R.id.dialogDescription);
        TextView dialogStatus = dialogView.findViewById(R.id.dialogStatus);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnOpenTicket = dialogView.findViewById(R.id.btnOpenTicket);

        String status = doc.getString("status");
        String assignedUid = doc.getString("assignedFacultyUid");
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        dialogTicketId.setText("#TK-" + doc.getId().substring(0, 5).toUpperCase());
        dialogSubject.setText(doc.getString("subject"));
        dialogStudentName.setText(doc.getString("studentName"));
        dialogDescription.setText(doc.getString("description"));

        if ("Open".equalsIgnoreCase(status)) {
            dialogStatus.setText("OPEN");
            dialogStatus.setTextColor(0xFF3F51B5);
            btnOpenTicket.setText("Open Ticket");
            btnOpenTicket.setEnabled(true);
        } else if ("Resolved".equalsIgnoreCase(status)) {
            dialogStatus.setText("RESOLVED");
            dialogStatus.setTextColor(0xFF888880);
            btnOpenTicket.setText("View History");
            btnOpenTicket.setEnabled(true);
        } else {
            if (currentUid.equals(assignedUid)) {
                dialogStatus.setText("IN PROGRESS");
                dialogStatus.setTextColor(0xFF22A55B);
                btnOpenTicket.setText("Continue Chat");
                btnOpenTicket.setEnabled(true);
            } else {
                dialogStatus.setText("Assigned to " + doc.getString("assignedFacultyName"));
                dialogStatus.setTextColor(0xFF888880);
                btnOpenTicket.setText("Restricted");
                btnOpenTicket.setEnabled(false);
                btnOpenTicket.setAlpha(0.5f);
            }
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOpenTicket.setOnClickListener(v -> { dialog.dismiss(); handleTicketOpening(doc); });
        dialog.show();
    }

    private void handleTicketOpening(DocumentSnapshot doc) {
        String status = doc.getString("status");
        if ("Open".equalsIgnoreCase(status)) {
            String facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentSnapshot facultyDoc = vm.getFacultyData().getValue();
            String facultyName = facultyDoc != null ? (facultyDoc.getString("firstName") + " " + facultyDoc.getString("surname")) : "Faculty";

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "In Progress");
            updates.put("assignedFacultyUid", facultyUid);
            updates.put("assignedFacultyName", facultyName);

            FirebaseFirestore.getInstance().collection("tickets").document(doc.getId())
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        notifyStudent(doc.getString("studentUid"), facultyName);
                        openChat(doc.getString("studentUid"), doc.getString("studentName"), doc.getId());
                    });
        } else {
            openChat(doc.getString("studentUid"), doc.getString("studentName"), doc.getId());
        }
    }

    private void notifyStudent(String studentUid, String facultyName) {
        if (studentUid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(studentUid).get().addOnSuccessListener(doc -> {
            String token = doc.getString("fcmToken");
            if (token != null) {
                callFcmApi(token, "Ticket Assigned", "Your ticket has been assigned to " + facultyName, "ticket_assigned");
            }
        });
    }

    private void callFcmApi(String token, String title, String body, String type) {
        new Thread(() -> {
            try {
                InputStream is = getAssets().open("service_account.json");
                com.google.auth.oauth2.GoogleCredentials credentials = com.google.auth.oauth2.GoogleCredentials.fromStream(is).createScoped("https://www.googleapis.com/auth/firebase.messaging");
                credentials.refreshIfExpired();
                String accessToken = credentials.getAccessToken().getTokenValue();
                JSONObject data = new JSONObject();
                data.put("title", title);
                data.put("body",  body);
                data.put("type",  type);
                JSONObject messageObj = new JSONObject();
                messageObj.put("token", token);
                messageObj.put("data",  data);
                JSONObject payload = new JSONObject();
                payload.put("message", messageObj);
                URL url = new URL("https://fcm.googleapis.com/v1/projects/collegehelpdeskproject/messages:send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type",  "application/json");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) { Log.e("FCM", "Error: " + e.getMessage()); }
        }).start();
    }

    private void openChat(String studentUid, String studentName, String ticketId) {
        Intent intent = new Intent(this, TicketDetailActivity.class);
        intent.putExtra("TicketID", ticketId);
        startActivity(intent);
    }
}
