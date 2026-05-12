package com.example.myapplication;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class Profile_Faculty_fragment extends Fragment {

    // ── Profile fields (tumhare XML ke IDs yahan match karo) ──
    private TextView tvProfileName, tvProfileEmail, tvProfilePhone;
    private TextView tvProfileDepartment, tvProfileSubject, tvProfileCollege;
    private TextView tvProfileGender;

    private SessionManager facultysessionexpire;
    private String currentEmail = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile__faculty_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        facultysessionexpire = new SessionManager(requireContext());

        // ── Views bind karo ───────────────────────────────
        tvProfileName       = view.findViewById(R.id.tvProfileName);
        tvProfileEmail      = view.findViewById(R.id.tvProfileEmail);
        //tvProfilePhone      = view.findViewById(R.id.tvProfilePhone);
        tvProfileDepartment = view.findViewById(R.id.tvProfileDepartment);
        //tvProfileSubject    = view.findViewById(R.id.tvProfileSubject);
        //tvProfileCollege    = view.findViewById(R.id.tvProfileCollege);
        tvProfileGender     = view.findViewById(R.id.tvProfileGender);

        // ── ViewModel se data observe karo ───────────────
        FacultyViewModel vm = new ViewModelProvider(requireActivity())
                .get(FacultyViewModel.class);
        vm.fetchIfNeeded();

        vm.getFacultyData().observe(getViewLifecycleOwner(), doc -> {
            if (doc != null) {
                String firstName  = doc.getString("firstName");
                String surname    = doc.getString("surname");
                String email      = doc.getString("email");
                String phone      = doc.getString("phone");
                String department = doc.getString("department");
                String subject    = doc.getString("subject");
                String college    = doc.getString("college");
                String gender     = doc.getString("gender");

                currentEmail = email != null ? email : "";

                // ── UI update karo ────────────────────────
                if (tvProfileName != null)
                    tvProfileName.setText(firstName + " " + surname);
                if (tvProfileEmail != null)
                    tvProfileEmail.setText(email);
                //if (tvProfilePhone != null)
                //tvProfilePhone.setText(phone);
                if (tvProfileDepartment != null)
                    tvProfileDepartment.setText(department);
                //if (tvProfileSubject != null)
                //tvProfileSubject.setText(subject);
                //if (tvProfileCollege != null)
                //tvProfileCollege.setText(college);
                if (tvProfileGender != null)
                    tvProfileGender.setText(gender);
            }
        });

        // ── Back button ───────────────────────────────────
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().onBackPressed());

        // ── Copy Email ────────────────────────────────────
        view.findViewById(R.id.btnCopyEmail).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Email", currentEmail);
            if (clipboard != null) clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Email copied!", Toast.LENGTH_SHORT).show();
        });

        // ── Sign Out ──────────────────────────────────────
        view.findViewById(R.id.btnSignOut).setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Sign Out")
                        .setMessage("Are you sure you want to sign out?")
                        .setPositiveButton("Sign Out", (dialog, which) -> {
                            facultysessionexpire.clearfacultySession();
                            Intent in = new Intent(getActivity(), LoginPage.class);
                            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(in);
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
    }
}