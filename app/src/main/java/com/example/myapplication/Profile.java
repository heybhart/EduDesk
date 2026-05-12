package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Profile extends Fragment {

    SessionManager studentSession;
    ImageView ivProfileAvatar;
    String currentUid = "";

    static final int REQ_PICK_PHOTO = 301;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        studentSession  = new SessionManager(requireContext());
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // ── Intent se data lo ─────────────────────────────────
        Intent intent  = requireActivity().getIntent();
        String name    = intent.getStringExtra("name");
        String surname = intent.getStringExtra("surname");
        String prn     = intent.getStringExtra("prn");
        String phone   = intent.getStringExtra("phone");
        String email   = intent.getStringExtra("email");
        String gender  = intent.getStringExtra("gender");
        String course  = intent.getStringExtra("course");
        String year    = intent.getStringExtra("year");
        String college = intent.getStringExtra("collegename");

        String fullName = ((name != null ? name : "") + " " +
                (surname != null ? surname : "")).trim();

        // ── Header ────────────────────────────────────────────
        setText(view, R.id.tvHeaderName, fullName.isEmpty() ? "—" : fullName);
        setText(view, R.id.tvHeaderPrn,  "PRN: " + (prn != null ? prn : "—"));

        // ── Sabhi locked fields mein data set karo ────────────
        setText(view, R.id.tvFullName, fullName.isEmpty() ? "—" : fullName);
        setText(view, R.id.tvPrn,      prn     != null ? prn     : "—");
        setText(view, R.id.tvGender,   gender  != null ? gender  : "—");
        setText(view, R.id.tvMobile,   phone   != null ? phone   : "—");
        setText(view, R.id.tvEmail,    email   != null ? email   : "—");
        setText(view, R.id.tvCollege,  college != null ? college : "—");
        setText(view, R.id.tvCourse,   course  != null ? course  : "—");
        setText(view, R.id.tvYear,     year    != null ? year    : "—");

        // ── Profile photo Firestore se load karo ──────────────
        loadProfilePhoto();

        // ── Avatar + camera button — photo change ─────────────
        View.OnClickListener pickPhoto = v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setType("image/*");
            startActivityForResult(pickIntent, REQ_PICK_PHOTO);
        };
        ivProfileAvatar.setOnClickListener(pickPhoto);
        View cameraBtn = view.findViewById(R.id.btnCamera);
        if (cameraBtn != null) cameraBtn.setOnClickListener(pickPhoto);

        // ── Back ──────────────────────────────────────────────
        view.findViewById(R.id.btnProfileBack).setOnClickListener(v ->
                requireActivity().onBackPressed());



        // ── Sign Out ──────────────────────────────────────────
        view.findViewById(R.id.btnSignOut).setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Sign Out")
                        .setMessage("Are you sure you want to sign out?")
                        .setPositiveButton("Sign Out", (dialog, which) -> {
                            studentSession.clearstudentSession();
                            FirebaseAuth.getInstance().signOut();
                            Intent logoutIntent = new Intent(getActivity(), LoginPage.class);
                            logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(logoutIntent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    private void setText(View root, int id, String value) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(value);
    }

    private void loadProfilePhoto() {
        if (currentUid.isEmpty()) return;
        FirebaseFirestore.getInstance()
                .collection("users").document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("profilePhoto") != null) {
                        applyPhoto(doc.getString("profilePhoto"));
                    } else {
                        FirebaseFirestore.getInstance()
                                .collection("faculty_user").document(currentUid)
                                .get()
                                .addOnSuccessListener(fDoc -> {
                                    if (fDoc.exists() && fDoc.getString("profilePhoto") != null) {
                                        applyPhoto(fDoc.getString("profilePhoto"));
                                    }
                                });
                    }
                });
    }

    private void applyPhoto(String url) {
        if (url == null || url.isEmpty() || ivProfileAvatar == null) return;
        ivProfileAvatar.setPadding(0, 0, 0, 0);
        ivProfileAvatar.clearColorFilter();
        Glide.with(this)
                .load(url)
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_profile_user)
                .error(R.drawable.ic_profile_user)
                .into(ivProfileAvatar);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_PHOTO || resultCode != getActivity().RESULT_OK
                || data == null || data.getData() == null) return;

        Uri imageUri = data.getData();
        Toast.makeText(requireContext(), "Uploading...", Toast.LENGTH_SHORT).show();

        String fileName = "profile_" + currentUid + ".jpg";
        SupabaseManager.uploadFile(requireContext(), imageUri, fileName,
                new SupabaseManager.UploadCallback() {
                    @Override
                    public void onSuccess(String fileUrl) {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(currentUid)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    String col = doc.exists() ? "users" : "faculty_user";
                                    db.collection(col).document(currentUid)
                                            .update("profilePhoto", fileUrl)
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(requireContext(),
                                                        "Photo updated! ✅",
                                                        Toast.LENGTH_SHORT).show();
                                                applyPhoto(fileUrl);
                                            });
                                });
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(requireContext(),
                                "Upload failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}