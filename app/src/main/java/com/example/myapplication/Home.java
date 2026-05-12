package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Home extends Fragment {

    TextView tvGreeting, tvDate, tvViewAll;
    TextView tvTotalCount, tvOpenCount, tvInProgressCount, tvResolvedCount;
    TextView row1Id, row1Title, row1Status;
    TextView row2Id, row2Title, row2Status;
    TextView row3Id, row3Title, row3Status;
    TextView tvAnnouncementEmpty;
    ImageView btnNotification;
    View notificationDot;
    LinearLayout announcementContainer;

    FirebaseFirestore db;
    String currentUserId;
    ListenerRegistration ticketListener;
    ListenerRegistration notificationListener;
    ListenerRegistration announcementListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvDate = view.findViewById(R.id.tvdate);
        tvViewAll = view.findViewById(R.id.tvViewAll);
        tvTotalCount = view.findViewById(R.id.tvTotalTicketCount);
        tvOpenCount = view.findViewById(R.id.tvTotalTicketOpenCount);
        tvInProgressCount = view.findViewById(R.id.tvTotalTicketInProgressCount);
        tvResolvedCount = view.findViewById(R.id.tvTotalTicketResolvedCount);
        btnNotification = view.findViewById(R.id.btnNotification);
        notificationDot = view.findViewById(R.id.viewNotificationDot);
        announcementContainer = view.findViewById(R.id.announcementContainer);
        tvAnnouncementEmpty = view.findViewById(R.id.tvAnnouncementEmpty);

        row1Id = view.findViewById(R.id.row1ticketid);
        row1Title = view.findViewById(R.id.row1ticket_title);
        row1Status = view.findViewById(R.id.row1ticket_status);

        row2Id = view.findViewById(R.id.row2ticketid);
        row2Title = view.findViewById(R.id.row2ticket_title);
        row2Status = view.findViewById(R.id.rowticket_status);

        row3Id = view.findViewById(R.id.row3ticketid);
        row3Title = view.findViewById(R.id.row3ticket_title);
        row3Status = view.findViewById(R.id.row3ticket_status);

        String studentName = requireActivity().getIntent().getStringExtra("name");
        setGreeting(studentName);

        String currentDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date());
        tvDate.setText(currentDate);

        tvViewAll.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ViewAllTicket.class)));

        btnNotification.setOnClickListener(v -> {
            if (notificationDot != null) {
                notificationDot.setVisibility(View.GONE);
            }
            markNotificationsAsRead();
            startActivity(new Intent(getActivity(), NotificationActivity.class));
        });

        loadData();
        observeUnreadNotifications();
        observeAnnouncements();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTicketListening();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTicketListening();
        stopNotificationListening();
        stopAnnouncementListening();
    }

    private void stopTicketListening() {
        if (ticketListener != null) {
            ticketListener.remove();
            ticketListener = null;
        }
    }

    private void stopNotificationListening() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    private void stopAnnouncementListening() {
        if (announcementListener != null) {
            announcementListener.remove();
            announcementListener = null;
        }
    }

    private void loadData() {
        if (currentUserId.isEmpty()) return;

        stopTicketListening();

        ticketListener = db.collection("tickets")
                .whereEqualTo("studentUid", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<DocumentSnapshot> allDocs = snapshots.getDocuments();

                    int total = 0;
                    int open = 0;
                    int inProgress = 0;
                    int resolved = 0;

                    for (DocumentSnapshot doc : allDocs) {
                        total++;
                        String status = doc.getString("status");
                        if (status != null) {
                            switch (status.toLowerCase()) {
                                case "open":
                                    open++;
                                    break;
                                case "in progress":
                                    inProgress++;
                                    break;
                                case "resolved":
                                    resolved++;
                                    break;
                            }
                        }
                    }

                    if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(total));
                    if (tvOpenCount != null) tvOpenCount.setText(String.valueOf(open));
                    if (tvInProgressCount != null) tvInProgressCount.setText(String.valueOf(inProgress));
                    if (tvResolvedCount != null) tvResolvedCount.setText(String.valueOf(resolved));

                    displayRecentTickets(allDocs);
                });
    }

    private void observeUnreadNotifications() {
        if (currentUserId.isEmpty()) return;

        stopNotificationListening();

        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || notificationDot == null) return;
                    boolean hasUnread = snapshots != null && !snapshots.isEmpty();
                    notificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                });
    }

    private void observeAnnouncements() {
        stopAnnouncementListening();

        announcementListener = db.collection("announcements")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || announcementContainer == null) return;
                    List<DocumentSnapshot> docs = snapshots.getDocuments();
                    Collections.sort(docs, (d1, d2) -> Long.compare(getTimestamp(d2), getTimestamp(d1)));
                    renderAnnouncements(docs);
                });
    }

    private void markNotificationsAsRead() {
        if (currentUserId.isEmpty()) return;

        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) return;

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        batch.update(document.getReference(), "read", true);
                    }
                    batch.commit();
                });
    }

    private void setGreeting(String name) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 0 && hour < 12) greeting = "Good Morning, ";
        else if (hour < 17) greeting = "Good Afternoon, ";
        else if (hour < 22) greeting = "Good Evening, ";
        else greeting = "Good Night, ";
        tvGreeting.setText(greeting + (name != null ? name : ""));
    }

    private void displayRecentTickets(List<DocumentSnapshot> docs) {
        Collections.sort(docs, (d1, d2) -> Long.compare(getTimestamp(d2), getTimestamp(d1)));

        if (docs.size() >= 1) fillRow(docs.get(0), row1Id, row1Title, row1Status);
        else clearRow(row1Id, row1Title, row1Status);

        if (docs.size() >= 2) fillRow(docs.get(1), row2Id, row2Title, row2Status);
        else clearRow(row2Id, row2Title, row2Status);

        if (docs.size() >= 3) fillRow(docs.get(2), row3Id, row3Title, row3Status);
        else clearRow(row3Id, row3Title, row3Status);
    }

    private void renderAnnouncements(List<DocumentSnapshot> docs) {
        announcementContainer.removeAllViews();
        int visibleCount = 0;

        for (DocumentSnapshot doc : docs) {
            String audience = doc.getString("audience");
            if ("Faculty Only".equalsIgnoreCase(audience)) {
                continue;
            }
            announcementContainer.addView(createAnnouncementView(doc, visibleCount > 0));
            visibleCount++;
        }

        if (tvAnnouncementEmpty != null) {
            tvAnnouncementEmpty.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private View createAnnouncementView(DocumentSnapshot doc, boolean addDivider) {
        View item = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_student_announcement, announcementContainer, false);
        TextView chip = item.findViewById(R.id.tvAnnouncementCategory);
        TextView title = item.findViewById(R.id.tvAnnouncementTitle);
        TextView meta = item.findViewById(R.id.tvAnnouncementMeta);
        int[] colors = getAnnouncementColors(getAnnouncementCategory(doc));
        chip.setTextColor(colors[1]);
        chip.setText(getAnnouncementCategory(doc).toUpperCase(Locale.getDefault()));
        title.setText(valueOrFallback(doc.getString("title"), "Untitled announcement"));
        meta.setText("Posted " + formatAnnouncementDate(getTimestamp(doc)));
        return item;
    }

    private long getTimestamp(DocumentSnapshot doc) {
        Object ts = doc.get("createdAt");
        if (ts instanceof Long) return (Long) ts;
        if (ts instanceof Timestamp) return ((Timestamp) ts).toDate().getTime();
        return 0;
    }

    private String getAnnouncementCategory(DocumentSnapshot doc) {
        return valueOrFallback(doc.getString("category"), "General");
    }

    private int[] getAnnouncementColors(String category) {
        if ("Academic".equalsIgnoreCase(category)) return new int[]{0xFFEBF0FD, 0xFF3B6FE8};
        if ("Events".equalsIgnoreCase(category)) return new int[]{0xFFD1FAE5, 0xFF1A6645};
        if ("Faculty".equalsIgnoreCase(category)) return new int[]{0xFFF3E8FF, 0xFF7C3AED};
        if ("Exams".equalsIgnoreCase(category)) return new int[]{0xFFFEE2E2, 0xFFB91C1C};
        if ("Admin".equalsIgnoreCase(category)) return new int[]{0xFFE0F2FE, 0xFF0369A1};
        if ("Placement".equalsIgnoreCase(category)) return new int[]{0xFFECFCCB, 0xFF4D7C0F};
        if ("Emergency".equalsIgnoreCase(category)) return new int[]{0xFFFDE68A, 0xFFB45309};
        return new int[]{0xFFFEF3C7, 0xFFD4720A};
    }

    private String formatAnnouncementDate(long createdAt) {
        if (createdAt <= 0) return "recently";
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(createdAt));
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void fillRow(DocumentSnapshot doc, TextView tvId, TextView tvTitle, TextView tvStatus) {
        if (tvId == null || tvTitle == null || tvStatus == null) return;

        String id = "#" + doc.getId().substring(0, 6).toUpperCase();
        String subject = doc.getString("subject");
        String status = doc.getString("status");

        tvId.setText(id);
        tvTitle.setText(subject != null ? subject : "-");

        if (status != null) {
            switch (status.toLowerCase()) {
                case "open":
                    tvStatus.setText("ACTIVE");
                    tvStatus.setTextColor(0xFF3B6FE8);
                    tvStatus.setBackgroundResource(R.drawable.bg_status_not_opened_badge);
                    break;
                case "active":
                case "in progress":
                    tvStatus.setText("IN PROGRESS");
                    tvStatus.setTextColor(0xFFD4720A);
                    tvStatus.setBackgroundResource(R.drawable.bg_status_progress);
                    break;
                case "resolved":
                    tvStatus.setText("RESOLVED");
                    tvStatus.setTextColor(0xFF1A6645);
                    tvStatus.setBackgroundResource(R.drawable.bg_status_resolved);
                    break;
                default:
                    tvStatus.setText(status.toUpperCase());
                    tvStatus.setTextColor(0xFFC9A84C);
                    tvStatus.setBackgroundResource(R.drawable.bg_status_open);
                    break;
            }
        }
    }

    private void clearRow(TextView tvId, TextView tvTitle, TextView tvStatus) {
        if (tvId != null) tvId.setText("-");
        if (tvTitle != null) tvTitle.setText("No ticket");
        if (tvStatus != null) {
            tvStatus.setText("");
            tvStatus.setBackground(null);
        }
    }
}
