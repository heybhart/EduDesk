package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Analytics_Faculty_fragment extends Fragment {

    private TextView tvTotalTickets, tvResolvedCount, tvMyRank;
    private TextView tvMyActiveTickets, tvMyResolvedTickets, tvChartCenterPercent, tvChartCenterLabel, tvDateRange;
    private LinearLayout categoryContainer, topStaffContainer;
    private LinearLayout btnLegendActive, btnLegendResolved, btnDateRange;
    private DonutChartView donutChart;
    private FacultyViewModel vm;
    private String currentUid;
    private int myActiveCount = 0, myResolvedCount = 0;
    private int selectedFilter = 0; // 0: All Time, 1: Today, 2: Previous Week, 3: Previous Month

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics__faculty_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUid = FirebaseAuth.getInstance().getUid();

        tvTotalTickets      = view.findViewById(R.id.tvTotalTickets);
        tvResolvedCount     = view.findViewById(R.id.tvResolvedCount);
        tvMyRank            = view.findViewById(R.id.tvMyRank);
        tvMyActiveTickets   = view.findViewById(R.id.tvMyActiveTickets);
        tvMyResolvedTickets = view.findViewById(R.id.tvMyResolvedTickets);
        tvChartCenterPercent = view.findViewById(R.id.tvChartCenterPercent);
        tvChartCenterLabel  = view.findViewById(R.id.tvChartCenterLabel);
        tvDateRange         = view.findViewById(R.id.tvDateRange);
        categoryContainer   = view.findViewById(R.id.categoryContainer);
        topStaffContainer   = view.findViewById(R.id.topStaffContainer);
        donutChart          = view.findViewById(R.id.donutChart);
        btnLegendActive     = view.findViewById(R.id.btnLegendActive);
        btnLegendResolved   = view.findViewById(R.id.btnLegendResolved);
        btnDateRange        = view.findViewById(R.id.btnDateRange);

        vm = new ViewModelProvider(requireActivity()).get(FacultyViewModel.class);
        vm.fetchIfNeeded();

        vm.getTicketsData().observe(getViewLifecycleOwner(), tickets -> {
            if (tickets != null && vm.getAllFacultiesData().getValue() != null) {
                updateAnalytics(tickets, vm.getAllFacultiesData().getValue());
            }
        });

        vm.getAllFacultiesData().observe(getViewLifecycleOwner(), faculties -> {
            if (faculties != null && vm.getTicketsData().getValue() != null) {
                updateAnalytics(vm.getTicketsData().getValue(), faculties);
            }
        });

        btnLegendActive.setOnClickListener(v -> updateCenterText(true));
        btnLegendResolved.setOnClickListener(v -> updateCenterText(false));
        btnDateRange.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        String[] options = {"All Time Data", "Today", "Previous Week", "Previous Month"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Time Range")
                .setItems(options, (dialog, which) -> {
                    selectedFilter = which;
                    tvDateRange.setText(options[which]);
                    if (vm.getTicketsData().getValue() != null && vm.getAllFacultiesData().getValue() != null) {
                        updateAnalytics(vm.getTicketsData().getValue(), vm.getAllFacultiesData().getValue());
                    }
                })
                .show();
    }

    private void updateAnalytics(List<DocumentSnapshot> allTickets, List<DocumentSnapshot> faculties) {
        List<DocumentSnapshot> tickets = filterTicketsByTime(allTickets);

        int totalTicketsCount  = tickets.size();
        int totalResolvedCount = 0; // FIX 1: track total resolved across all staff
        myResolvedCount = 0;
        myActiveCount   = 0;

        // FIX 3: Use TreeMap so categories are sorted alphabetically (consistent order)
        Map<String, Integer> categoryMap      = new TreeMap<>();
        Map<String, Integer> staffResolutionMap = new LinkedHashMap<>();

        for (DocumentSnapshot doc : tickets) {
            String status      = doc.getString("status");
            String assignedUid = doc.getString("assignedFacultyUid");
            String category    = doc.getString("category");

            if (category != null && !category.isEmpty()) {
                categoryMap.put(category, categoryMap.getOrDefault(category, 0) + 1);
            }

            if ("Resolved".equalsIgnoreCase(status)) {
                totalResolvedCount++; // FIX 1: count every resolved ticket
                if (assignedUid != null) {
                    staffResolutionMap.put(assignedUid,
                            staffResolutionMap.getOrDefault(assignedUid, 0) + 1);
                }
            }

            if (currentUid != null && currentUid.equals(assignedUid)) {
                if ("Resolved".equalsIgnoreCase(status)) {
                    myResolvedCount++;
                } else if ("In Progress".equalsIgnoreCase(status)) {
                    myActiveCount++;
                }
            }
        }

        tvTotalTickets.setText(String.valueOf(totalTicketsCount));
        tvResolvedCount.setText(String.valueOf(totalResolvedCount)); // FIX 1: show total resolved
        tvMyActiveTickets.setText("Active (" + myActiveCount + ")");
        tvMyResolvedTickets.setText("Resolved (" + myResolvedCount + ")");

        donutChart.setData(myActiveCount, myResolvedCount);
        updateCenterText(true);

        calculateAndSetRank(faculties, staffResolutionMap);
        renderCategories(categoryMap, totalTicketsCount);
        renderTopStaff(faculties, staffResolutionMap);
    }

    private List<DocumentSnapshot> filterTicketsByTime(List<DocumentSnapshot> allTickets) {
        if (selectedFilter == 0) return allTickets;

        List<DocumentSnapshot> filtered = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        long startTime;

        if (selectedFilter == 1) {
            // FIX 2: Today = from midnight of today
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            startTime = cal.getTimeInMillis();

        } else if (selectedFilter == 2) {
            // FIX 2: Previous Week = Monday to Sunday of last calendar week
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.add(Calendar.WEEK_OF_YEAR, -1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            startTime = cal.getTimeInMillis();

        } else {
            // FIX 2: Previous Month = 1st of last calendar month
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, -1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            startTime = cal.getTimeInMillis();
        }

        for (DocumentSnapshot doc : allTickets) {
            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null && createdAt >= startTime) {
                filtered.add(doc);
            }
        }
        return filtered;
    }

    private void updateCenterText(boolean showActive) {
        int total = myActiveCount + myResolvedCount;
        tvChartCenterLabel.setText(showActive ? "ACTIVE" : "RESOLVED");
        if (total == 0) {
            tvChartCenterPercent.setText("0%");
            return;
        }
        int percent = Math.round(((showActive ? myActiveCount : myResolvedCount) / (float) total) * 100);
        tvChartCenterPercent.setText(percent + "%");
    }

    private void calculateAndSetRank(List<DocumentSnapshot> faculties, Map<String, Integer> staffResolutionMap) {
        // FIX 4: Build a full list including faculty with 0 resolutions so rank is always correct
        List<Map.Entry<String, Integer>> sortedStaff = new ArrayList<>();

        for (DocumentSnapshot f : faculties) {
            String uid = f.getId();
            int count  = staffResolutionMap.getOrDefault(uid, 0);
            sortedStaff.add(new java.util.AbstractMap.SimpleEntry<>(uid, count));
        }

        Collections.sort(sortedStaff, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        int rank = -1;
        for (int i = 0; i < sortedStaff.size(); i++) {
            if (sortedStaff.get(i).getKey().equals(currentUid)) {
                rank = i + 1;
                break;
            }
        }
        tvMyRank.setText(rank != -1 ? "#" + rank : "N/A");
    }

    private void renderCategories(Map<String, Integer> categoryMap, int total) {
        categoryContainer.removeAllViews();
        if (total == 0) return;

        // Sort by count descending so highest category appears first
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(categoryMap.entrySet());
        Collections.sort(sorted, (a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : sorted) {
            View v = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_analytics_category, categoryContainer, false);
            TextView tvCatName    = v.findViewById(R.id.tvCategoryName);
            TextView tvCatPercent = v.findViewById(R.id.tvCategoryPercent);
            ProgressBar progress  = v.findViewById(R.id.categoryProgress);

            int count   = entry.getValue();
            int percent = Math.round((count / (float) total) * 100);

            tvCatName.setText(entry.getKey());
            tvCatPercent.setText(percent + "%");
            progress.setProgress(percent);
            categoryContainer.addView(v);
        }
    }

    private void renderTopStaff(List<DocumentSnapshot> faculties, Map<String, Integer> staffResolutionMap) {
        topStaffContainer.removeAllViews();

        List<StaffData> staffList = new ArrayList<>();
        for (DocumentSnapshot f : faculties) {
            int resCount = staffResolutionMap.getOrDefault(f.getId(), 0);

            // FIX 5: null-safe name building
            String firstName = f.getString("firstName");
            String surname   = f.getString("surname");
            String name;
            if (firstName != null && surname != null) {
                name = firstName + " " + surname;
            } else if (firstName != null) {
                name = firstName;
            } else if (surname != null) {
                name = surname;
            } else {
                name = "Unknown";
            }

            staffList.add(new StaffData(f.getId(), name, resCount));
        }

        Collections.sort(staffList, (s1, s2) -> Integer.compare(s2.resolutions, s1.resolutions));

        // FIX 7: maxResolutions calculated once, outside the loop
        int maxResolutions = (!staffList.isEmpty() && staffList.get(0).resolutions > 0)
                ? staffList.get(0).resolutions : 1;

        int limit = Math.min(staffList.size(), 5);
        for (int i = 0; i < limit; i++) {
            StaffData staff = staffList.get(i);
            View v = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_staff_workload, topStaffContainer, false);

            TextView tvName   = v.findViewById(R.id.staffName);
            TextView tvCount  = v.findViewById(R.id.staffStatus);
            ProgressBar progress = v.findViewById(R.id.staffProgress);

            tvName.setText(staff.name);
            tvCount.setText(staff.resolutions + " Resolved");

            int progressVal = Math.round((staff.resolutions / (float) maxResolutions) * 100);
            progress.setProgress(progressVal);

            topStaffContainer.addView(v);
        }
    }

    private static class StaffData {
        String uid, name;
        int resolutions;
        StaffData(String u, String n, int r) {
            this.uid = u;
            this.name = n;
            this.resolutions = r;
        }
    }
}