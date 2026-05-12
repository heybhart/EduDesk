package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Home_Faculty_fragment extends Fragment {

    private TextView tvWelcome, tvDate, tvActiveCount, tvResolvedCountHome;
    private LinearLayout ticketContainer, staffContainer;
    private LinearLayout cardActive, cardResolved;
    private View notificationDot;
    private FacultyViewModel vm;
    private String currentUid;
    private ListenerRegistration notificationListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home__faculty_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUid = FirebaseAuth.getInstance().getUid();

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvDate    = view.findViewById(R.id.tvDate);
        tvActiveCount = view.findViewById(R.id.tvActiveCount);
        tvResolvedCountHome = view.findViewById(R.id.tvResolvedCountHome);
        ticketContainer = view.findViewById(R.id.ticketContainer);
        staffContainer = view.findViewById(R.id.staffContainer);
        cardActive = view.findViewById(R.id.cardActive);
        cardResolved = view.findViewById(R.id.cardResolved);
        notificationDot = view.findViewById(R.id.viewFacultyNotificationDot);

        String today = new SimpleDateFormat(
                "EEEE, MMMM d, yyyy", Locale.getDefault()
        ).format(new Date());
        tvDate.setText(today);

        vm = new ViewModelProvider(requireActivity())
                .get(FacultyViewModel.class);

        vm.fetchIfNeeded();

        vm.getFacultyData().observe(getViewLifecycleOwner(), doc -> {
            if (doc != null) {
                String firstName = doc.getString("firstName");
                String surname   = doc.getString("surname");
                tvWelcome.setText("Welcome, " + firstName + " " + surname);
            }
        });

        vm.getTicketsData().observe(getViewLifecycleOwner(), tickets -> {
            if (tickets != null) {
                updateStats(tickets);
                renderTickets(tickets);
                if (vm.getAllFacultiesData().getValue() != null) {
                    renderStaffWorkload(vm.getAllFacultiesData().getValue(), tickets);
                }
            }
        });

        vm.getAllFacultiesData().observe(getViewLifecycleOwner(), faculties -> {
            if (faculties != null && vm.getTicketsData().getValue() != null) {
                renderStaffWorkload(faculties, vm.getTicketsData().getValue());
            }
        });

        view.findViewById(R.id.btnBell).setOnClickListener(v -> {
            if (notificationDot != null) {
                notificationDot.setVisibility(View.GONE);
            }
            markNotificationsAsRead();
            startActivity(new Intent(getActivity(), FacultyNotificationActivity.class));
        });

        view.findViewById(R.id.btnSeeAll).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), See_All_QueryList.class));
        });

        cardActive.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), See_All_QueryList.class));
        });

        cardResolved.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), See_All_QueryList.class));
        });

        view.findViewById(R.id.btnManage).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Manage Staff", Toast.LENGTH_SHORT).show());

        observeUnreadNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    private void updateStats(List<DocumentSnapshot> tickets) {
        int activeCount = 0;
        int resolvedCount = 0;

        for (DocumentSnapshot doc : tickets) {
            String status = doc.getString("status");
            String assignedUid = doc.getString("assignedFacultyUid");

            if (currentUid.equals(assignedUid)) {
                if (isInProgressStatus(status)) {
                    activeCount++;
                } else if ("Resolved".equalsIgnoreCase(status)) {
                    resolvedCount++;
                }
            }
        }

        tvActiveCount.setText(String.valueOf(activeCount));
        tvResolvedCountHome.setText(String.valueOf(resolvedCount));
    }

    private void renderTickets(List<DocumentSnapshot> tickets) {
        ticketContainer.removeAllViews();
        List<DocumentSnapshot> filteredTickets = new ArrayList<>();
        for (DocumentSnapshot doc : tickets) {
            String status = doc.getString("status");
            if (!"Resolved".equalsIgnoreCase(status)) {
                filteredTickets.add(doc);
            }
        }
        int limit = Math.min(filteredTickets.size(), 3);
        for (int i = 0; i < limit; i++) {
            ticketContainer.addView(createTicketView(filteredTickets.get(i)));
        }
    }

    private void renderStaffWorkload(List<DocumentSnapshot> faculties, List<DocumentSnapshot> tickets) {
        staffContainer.removeAllViews();
        Map<String, Integer> facultyTicketCount = new HashMap<>();
        int totalActiveTickets = 0;

        for (DocumentSnapshot ticket : tickets) {
            String status = ticket.getString("status");
            if (isInProgressStatus(status)) {
                totalActiveTickets++;
                String assignedUid = ticket.getString("assignedFacultyUid");
                if (assignedUid != null) {
                    facultyTicketCount.put(assignedUid, facultyTicketCount.getOrDefault(assignedUid, 0) + 1);
                }
            }
        }

        List<StaffWorkload> staffList = new ArrayList<>();
        for (DocumentSnapshot fDoc : faculties) {
            String uid = fDoc.getId();
            String name = fDoc.getString("firstName") + " " + fDoc.getString("surname");
            int count = facultyTicketCount.getOrDefault(uid, 0);
            int percentage = totalActiveTickets > 0 ? (int) ((count / (float) totalActiveTickets) * 100) : 0;
            staffList.add(new StaffWorkload(name, percentage));
        }

        Collections.sort(staffList, (s1, s2) -> Integer.compare(s2.percentage, s1.percentage));

        for (StaffWorkload staff : staffList) {
            staffContainer.addView(createStaffView(staff.name, staff.percentage));
        }
    }

    private View createStaffView(String name, int percentage) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.item_staff_workload, staffContainer, false);
        TextView tvName = v.findViewById(R.id.staffName);
        TextView tvStatus = v.findViewById(R.id.staffStatus);
        ProgressBar progressBar = v.findViewById(R.id.staffProgress);
        tvName.setText(name);
        if (percentage > 0) {
            tvStatus.setText(percentage + "% Busy");
            tvStatus.setTextColor(0xFFE53935);
            progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFE53935));
        } else {
            tvStatus.setText("0% Available");
            tvStatus.setTextColor(0xFF22A55B);
            progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF22A55B));
        }
        progressBar.setProgress(percentage > 0 ? percentage : 5);
        return v;
    }

    private static class StaffWorkload {
        String name;
        int percentage;
        StaffWorkload(String n, int p) { this.name = n; this.percentage = p; }
    }

    private View createTicketView(DocumentSnapshot doc) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.item_ticket_faculty, ticketContainer, false);
        View leftBar = v.findViewById(R.id.leftBar);
        TextView tvIdSubject = v.findViewById(R.id.tvIdSubject);
        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvMeta = v.findViewById(R.id.tvMeta);
        TextView tvStatusBadge = v.findViewById(R.id.tvStatusBadge);
        TextView tvPriorityBadge = v.findViewById(R.id.tvPriorityBadge);

        String subject = doc.getString("subject");
        String category = doc.getString("category");
        String priority = doc.getString("priority");
        String status = doc.getString("status");
        Long createdAt = doc.getLong("createdAt");
        String assignedUid = doc.getString("assignedFacultyUid");
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
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
        applyPriorityBadge(priority, leftBar, tvPriorityBadge);

        if ("Open".equalsIgnoreCase(status)) {
            tvStatusBadge.setVisibility(View.VISIBLE);
            tvStatusBadge.setText("NOT OPEN");
            tvStatusBadge.setTextColor(0xFF3F51B5);
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_not_opened_badge);
        } else if (isInProgressStatus(status)) {
            tvStatusBadge.setVisibility(View.VISIBLE);
            if (currentUid.equals(assignedUid)) {
                tvStatusBadge.setText("ACTIVE");
                tvStatusBadge.setTextColor(0xFF22A55B);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active_badge);
            } else {
                tvStatusBadge.setText("ASSIGNED");
                tvStatusBadge.setTextColor(0xFF888880);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_tab_normal); 
            }
        } else {
            tvStatusBadge.setVisibility(View.GONE);
        }

        v.setOnClickListener(view -> showTicketDetailDialog(doc));
        return v;
    }

    private void applyPriorityBadge(String priority, View leftBar, TextView tvPriorityBadge) {
        String safePriority = priority == null ? "Low" : priority.trim();
        int textColor;
        int backgroundRes;
        int accentColor;

        switch (safePriority.toLowerCase(Locale.ROOT)) {
            case "high":
                safePriority = "High";
                textColor = 0xFFE53935;
                backgroundRes = R.drawable.bg_priority_high_badge;
                accentColor = 0xFFE53935;
                break;
            case "medium":
                safePriority = "Medium";
                textColor = 0xFF1565C0;
                backgroundRes = R.drawable.bg_priority_medium_selected;
                accentColor = 0xFF1565C0;
                break;
            default:
                safePriority = "Low";
                textColor = 0xFF2E7D32;
                backgroundRes = R.drawable.bg_priority_low_selected;
                accentColor = 0xFF2E7D32;
                break;
        }

        tvPriorityBadge.setText(safePriority.toUpperCase(Locale.ROOT) + " PRIORITY");
        tvPriorityBadge.setTextColor(textColor);
        tvPriorityBadge.setBackgroundResource(backgroundRes);
        leftBar.setBackgroundColor(accentColor);
    }

    private void showTicketDetailDialog(DocumentSnapshot doc) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ticket_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(dialogView).create();

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
            dialogStatus.setText("NOT OPEN");
            dialogStatus.setTextColor(0xFF3F51B5);
            btnOpenTicket.setText("Open Ticket");
            btnOpenTicket.setEnabled(true);
        } else {
            if (currentUid.equals(assignedUid)) {
                dialogStatus.setText("ACTIVE");
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
                    .addOnSuccessListener(unused -> openChat(doc.getString("studentUid"), doc.getString("studentName"), doc.getId()));
        } else {
            openChat(doc.getString("studentUid"), doc.getString("studentName"), doc.getId());
        }
    }

    private void openChat(String studentUid, String studentName, String ticketId) {
        Intent intent = new Intent(requireContext(), TicketDetailActivity.class);
        intent.putExtra("TicketID", ticketId);
        startActivity(intent);
    }

    private void observeUnreadNotifications() {
        if (currentUid == null || currentUid.isEmpty()) return;

        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", currentUid)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || notificationDot == null) return;
                    boolean hasUnread = snapshots != null && !snapshots.isEmpty();
                    notificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                });
    }

    private void markNotificationsAsRead() {
        if (currentUid == null || currentUid.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", currentUid)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) return;

                    WriteBatch batch = FirebaseFirestore.getInstance().batch();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        batch.update(document.getReference(), "read", true);
                    }
                    batch.commit();
                });
    }

    private boolean isInProgressStatus(String status) {
        return "In Progress".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status);
    }
}
