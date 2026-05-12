package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewAllTicket extends AppCompatActivity {

    ImageButton bckbtn;
    Button btnRaiseTicket;
    MaterialButton btnChipAll, btnChipOpen, btnChipInProgress, btnChipResolved;
    RecyclerView recyclerView;
    ProgressBar progressBar;
    TextView tvEmpty;

    String selectedFilter = "All Status";
    List<TicketModel> allTickets = new ArrayList<>(); // Master list
    List<TicketModel> displayedList = new ArrayList<>(); // Filtered list
    TicketAdapter adapter;

    FirebaseFirestore db;
    String currentUserId;

    static final int COLOR_SELECTED = 0xFF1A2340;
    static final int COLOR_UNSELECTED = 0xFFEEEEEE;
    static final int TEXT_SELECTED = 0xFFFFFFFF;
    static final int TEXT_UNSELECTED = 0xFF374151;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_ticket);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        bckbtn = findViewById(R.id.btnBack);
        btnRaiseTicket = findViewById(R.id.btnRaiseTicket);
        btnChipAll = findViewById(R.id.btnChipAll);
        btnChipOpen = findViewById(R.id.btnChipOpen);
        btnChipInProgress = findViewById(R.id.btnChipInProgress);
        btnChipResolved = findViewById(R.id.btnChipResolved);
        recyclerView = findViewById(R.id.recyclerTickets);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new TicketAdapter(this, displayedList, ticket -> {
            Intent intent = new Intent(this, TicketDetailActivity.class);
            intent.putExtra("TicketID", ticket.getTicketId()); // 🔥 Yahan Ticket ID bhej rahe hain
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        bckbtn.setOnClickListener(v -> finish());
        btnRaiseTicket.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("openTab", 1);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnChipAll.setOnClickListener(v -> updateFilter("All Status"));
        btnChipOpen.setOnClickListener(v -> updateFilter("Open"));
        btnChipInProgress.setOnClickListener(v -> updateFilter("In Progress"));
        btnChipResolved.setOnClickListener(v -> updateFilter("Resolved"));

        fetchInitialData();
    }

    private void updateFilter(String filter) {
        selectedFilter = filter;
        setChipStyle(btnChipAll, filter.equals("All Status"));
        setChipStyle(btnChipOpen, filter.equals("Open"));
        setChipStyle(btnChipInProgress, filter.equals("In Progress"));
        setChipStyle(btnChipResolved, filter.equals("Resolved"));
        applyFilter();
    }

    private void setChipStyle(MaterialButton btn, boolean isSelected) {
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                isSelected ? COLOR_SELECTED : COLOR_UNSELECTED));
        btn.setTextColor(isSelected ? TEXT_SELECTED : TEXT_UNSELECTED);
    }

    private void fetchInitialData() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("tickets")
                .whereEqualTo("studentUid", currentUserId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);
                    allTickets.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String ticketId = doc.getId();
                        String subject = doc.getString("subject");
                        String category = doc.getString("category");
                        String status = doc.getString("status");
                        String assignedName = doc.getString("assignedFacultyName");
                        String assignedUid = doc.getString("assignedFacultyUid");
                        
                        long createdAt = 0;
                        Object ts = doc.get("createdAt");
                        if (ts instanceof Long) createdAt = (Long) ts;
                        else if (ts instanceof Timestamp) createdAt = ((Timestamp) ts).toDate().getTime();

                        allTickets.add(new TicketModel(ticketId, subject, category, status, createdAt, assignedName, assignedUid));
                    }
                    
                    Collections.sort(allTickets, (t1, t2) -> Long.compare(t2.getCreatedAt(), t1.getCreatedAt()));
                    
                    applyFilter();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setText("Error loading data");
                    tvEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void applyFilter() {
        displayedList.clear();
        if (selectedFilter.equals("All Status")) {
            displayedList.addAll(allTickets);
        } else {
            for (TicketModel ticket : allTickets) {
                if ("Open".equalsIgnoreCase(selectedFilter)) {
                    if (ticket.getStatus() != null
                            && ticket.getStatus().equalsIgnoreCase("Open")
                            && (ticket.getAssignedUid() == null || ticket.getAssignedUid().trim().isEmpty())) {
                        displayedList.add(ticket);
                    }
                } else if ("In Progress".equalsIgnoreCase(selectedFilter)) {
                    if (ticket.getStatus() != null
                            && ("In Progress".equalsIgnoreCase(ticket.getStatus())
                            || "Active".equalsIgnoreCase(ticket.getStatus()))) {
                        displayedList.add(ticket);
                    }
                } else if (ticket.getStatus() != null && ticket.getStatus().equalsIgnoreCase(selectedFilter)) {
                    displayedList.add(ticket);
                }
            }
        }
        adapter.filterList(new ArrayList<>(displayedList));
        tvEmpty.setVisibility(displayedList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
