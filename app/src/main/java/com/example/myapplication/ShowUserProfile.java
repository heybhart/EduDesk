package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ShowUserProfile extends AppCompatActivity {

    // ── Firebase ──────────────────────────────────────────────
    FirebaseFirestore db;

    // ── Views ─────────────────────────────────────────────────
    ImageView ivAvatar;
    TextView tvProfileName, tvProfileRole;
    TextView tvEmail, tvGender;

    // ── Student card views ────────────────────────────────────
    LinearLayout cardStudent;
    TextView tvCollege, tvCourse, tvPrn, tvYear;

    // ── Faculty card views ────────────────────────────────────
    LinearLayout cardFaculty;
    TextView tvDepartment, tvSubject, tvFacultyCollege;

    // ─────────────────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_user_profile);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();

        // Bind views
        ivAvatar         = findViewById(R.id.ivAvatar);
        tvProfileName    = findViewById(R.id.tvProfileName);
        tvProfileRole    = findViewById(R.id.tvProfileRole);
        tvEmail          = findViewById(R.id.tvEmail);
        tvGender         = findViewById(R.id.tvGender);

        cardStudent      = findViewById(R.id.cardStudent);
        tvCollege        = findViewById(R.id.tvCollege);
        tvCourse         = findViewById(R.id.tvCourse);
        tvPrn            = findViewById(R.id.tvPrn);
        tvYear           = findViewById(R.id.tvYear);

        cardFaculty      = findViewById(R.id.cardFaculty);
        tvDepartment     = findViewById(R.id.tvDepartment);
        tvSubject        = findViewById(R.id.tvSubject);
        tvFacultyCollege = findViewById(R.id.tvFacultyCollege);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Get uid from intent
        String otherUid = getIntent().getStringExtra("otherUID");
        if (otherUid == null || otherUid.isEmpty()) {
            Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load user data
        loadUserData(otherUid);
    }

    // ─────────────────────────────────────────────────────────
    // Load user data — check students first, then faculty
    // ─────────────────────────────────────────────────────────

    private void loadUserData(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Student found
                        fillCommonData(doc, "users");
                        fillStudentData(doc);
                    } else {
                        // Check faculty
                        db.collection("faculty_user").document(uid)
                                .get()
                                .addOnSuccessListener(facultyDoc -> {
                                    if (facultyDoc.exists()) {
                                        fillCommonData(facultyDoc, "faculty_user");
                                        fillFacultyData(facultyDoc);
                                    } else {
                                        Toast.makeText(this,
                                                "User not found!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Error: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ─────────────────────────────────────────────────────────
    // Fill common data — name, email, gender, role, photo
    // ─────────────────────────────────────────────────────────

    private void fillCommonData(DocumentSnapshot doc, String collection) {
        String firstName = doc.getString("firstName");
        String surname   = doc.getString("surname");
        String email     = doc.getString("email");
        String gender    = doc.getString("gender");
        String photoUrl  = doc.getString("profilePhoto");

        // Full name
        String fullName = ((firstName != null ? firstName : "") + " " +
                (surname != null ? surname : "")).trim();
        tvProfileName.setText(fullName.isEmpty() ? "—" : fullName);

        // Email and gender
        tvEmail.setText(email  != null ? email  : "—");
        tvGender.setText(gender != null ? gender : "—");

        // Role badge
        tvProfileRole.setText("faculty_user".equals(collection) ? "FACULTY" : "STUDENT");

        // Profile photo
        if (photoUrl != null && !photoUrl.isEmpty()) {
            loadProfilePhoto(photoUrl);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Fill student specific data
    // ─────────────────────────────────────────────────────────

    private void fillStudentData(DocumentSnapshot doc) {
        cardStudent.setVisibility(View.VISIBLE);
        cardFaculty.setVisibility(View.GONE);

        tvCollege.setText(doc.getString("college") != null ? doc.getString("college") : "—");
        tvCourse.setText(doc.getString("course")   != null ? doc.getString("course")  : "—");
        tvPrn.setText(doc.getString("prn")         != null ? doc.getString("prn")     : "—");
        tvYear.setText(doc.getString("year")       != null ? doc.getString("year")    : "—");
    }

    // ─────────────────────────────────────────────────────────
    // Fill faculty specific data
    // ─────────────────────────────────────────────────────────

    private void fillFacultyData(DocumentSnapshot doc) {
        cardFaculty.setVisibility(View.VISIBLE);
        cardStudent.setVisibility(View.GONE);

        tvDepartment.setText(doc.getString("department") != null ? doc.getString("department") : "—");
        tvSubject.setText(doc.getString("subject")       != null ? doc.getString("subject")    : "—");
        tvFacultyCollege.setText(doc.getString("college") != null ? doc.getString("college")   : "—");
    }

    // ─────────────────────────────────────────────────────────
    // Load profile photo using Glide with circle crop
    // ─────────────────────────────────────────────────────────

    private void loadProfilePhoto(String url) {
        ivAvatar.setPadding(0, 0, 0, 0);
        ivAvatar.clearColorFilter();
        Glide.with(this)
                .load(url)
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_profile_user)
                .error(R.drawable.ic_profile_user)
                .into(ivAvatar);
    }
}