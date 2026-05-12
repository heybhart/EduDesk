package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class LoginPage extends AppCompatActivity {

    private TextView tabStudent, tabStaff;
    private ViewFlipper viewFlipper;

    private TextInputEditText etPrn, etOtp;
    private Button btnGenerateOtp, btnLogin;
    private TextView tvOtpSent, tvResendOtp;
    private LinearLayout otpSection;

    private TextInputEditText etStaffEmail, etStaffPassword;
    private Button btnStaffLogin;
    private Button btnCreateStudent, btnCreateFaculty;

    private FirebaseAuth mAuth;
    private OtpManager otpManager;
    private SessionManager studentSM, facultySM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        mAuth      = FirebaseAuth.getInstance();
        otpManager = new OtpManager();

        studentSM = new SessionManager(this);
        facultySM = new SessionManager(this);

        // Notification Permission Maango (Android 13+)
        askNotificationPermission();

        // â”€â”€ Session valid hai toh Firestore se data lo â”€â”€â”€â”€â”€â”€â”€â”€
        if (studentSM.isStudentSessionValid()) {
            saveFcmToken("users"); // Session valid hai toh bhi token update karo
            fetchStudentDataAndNavigate(null); 
            return;
        } else if (facultySM.is_faculty_session_valid()) {
            saveFcmToken("faculty_user");
            Intent intent = new Intent(this, Faculty_main_activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // â”€â”€ Views init â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        tabStudent   = findViewById(R.id.tabStudent);
        tabStaff     = findViewById(R.id.tabStaff);
        viewFlipper  = findViewById(R.id.viewFlipper);

        etPrn          = findViewById(R.id.etPrn);
        etOtp          = findViewById(R.id.etOtp);
        btnGenerateOtp = findViewById(R.id.btnGenerateOtp);
        btnLogin       = findViewById(R.id.btnLogin);
        tvOtpSent      = findViewById(R.id.tvOtpSent);
        tvResendOtp    = findViewById(R.id.tvResendOtp);
        otpSection     = findViewById(R.id.otpSection);

        etStaffEmail    = findViewById(R.id.etStaffEmail);
        etStaffPassword = findViewById(R.id.etStaffPassword);
        btnStaffLogin   = findViewById(R.id.btnStaffLogin);

        btnCreateStudent = findViewById(R.id.btnCreateStudent);
        btnCreateFaculty = findViewById(R.id.btnCreateFaculty);

        // â”€â”€ TAB SWITCHING â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        tabStudent.setOnClickListener(v -> {
            viewFlipper.setInAnimation(this, R.anim.slide_in_left);
            viewFlipper.setOutAnimation(this, R.anim.slide_out_right);
            viewFlipper.setDisplayedChild(0);
            setTabActive(tabStudent, tabStaff);
        });

        tabStaff.setOnClickListener(v -> {
            viewFlipper.setInAnimation(this, R.anim.slide_in_right);
            viewFlipper.setOutAnimation(this, R.anim.slide_out_left);
            viewFlipper.setDisplayedChild(1);
            setTabActive(tabStaff, tabStudent);
        });


        btnGenerateOtp.setOnClickListener(v -> {
            String prn = etPrn.getText().toString().trim();

            if (prn.isEmpty()) {
                etPrn.setError("PRN daalo pehle");
                etPrn.requestFocus();
                return;
            }

            btnGenerateOtp.setEnabled(false);
            btnGenerateOtp.setText("Sending OTP...");

            otpManager.generateAndSendOtp(prn, new OtpManager.OtpCallback() {
                @Override
                public void onOtpSent(String email) {
                    otpSection.setVisibility(View.VISIBLE);
                    tvOtpSent.setText("OTP sent to: " + maskEmail(email));
                    btnGenerateOtp.setText("Resend OTP");
                    btnGenerateOtp.setEnabled(true);
                    Toast.makeText(LoginPage.this, "OTP sent! Gmail check karo.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onPrnNotFound() {
                    etPrn.setError("Ye PRN registered nahi hai");
                    etPrn.requestFocus();
                    btnGenerateOtp.setText("Generate OTP");
                    btnGenerateOtp.setEnabled(true);
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(LoginPage.this, error, Toast.LENGTH_LONG).show();
                    btnGenerateOtp.setText("Generate OTP");
                    btnGenerateOtp.setEnabled(true);
                }
            });
        });

        // â”€â”€ RESEND OTP â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        tvResendOtp.setOnClickListener(v -> btnGenerateOtp.performClick());

        // â”€â”€ VERIFY OTP & LOGIN â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        btnLogin.setOnClickListener(v -> {
            String enteredOtp = etOtp.getText().toString().trim();

            if (enteredOtp.isEmpty() || enteredOtp.length() < 6) {
                etOtp.setError("6 digit OTP daalo");
                etOtp.requestFocus();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Verifying...");

            otpManager.verifyOtp(enteredOtp, new OtpManager.VerifyCallback() {

                @Override
                public void onVerified(String email) {
                    String prn      = etPrn.getText().toString().trim();
                    String password = prn;

                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener(result -> {
                                studentSM.save_session_of_student();
                                saveFcmToken("users"); // â† FCM TOKEN SAVE (STUDENT)
                                fetchStudentDataAndNavigate(prn);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(LoginPage.this,
                                        "Login failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                btnLogin.setEnabled(true);
                                btnLogin.setText("Verify & Login  â†’");
                            });
                }

                @Override
                public void onWrongOtp() {
                    etOtp.setError("OTP galat hai! Dobara try karo.");
                    etOtp.requestFocus();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Verify & Login  â†’");
                }

                @Override
                public void onExpired() {
                    Toast.makeText(LoginPage.this,
                            "OTP expire ho gaya! Dobara generate karo.",
                            Toast.LENGTH_LONG).show();
                    otpSection.setVisibility(View.GONE);
                    etOtp.setText("");
                    btnGenerateOtp.setText("Generate OTP");
                    btnGenerateOtp.setEnabled(true);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Verify & Login  â†’");
                }
            });
        });

        // â”€â”€ STAFF LOGIN â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        btnStaffLogin.setOnClickListener(v -> {
            String email    = etStaffEmail.getText().toString().trim();
            String password = etStaffPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etStaffEmail.setError("Email daalo");
                etStaffEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etStaffPassword.setError("Password daalo");
                etStaffPassword.requestFocus();
                return;
            }

            btnStaffLogin.setEnabled(false);
            btnStaffLogin.setText("Logging in...");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        String uid = result.getUser().getUid();

                        FirebaseFirestore.getInstance()
                                .collection("faculty_user")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        facultySM.save_session_of_faculty();
                                        saveFcmToken("faculty_user"); // â† FCM TOKEN SAVE (FACULTY)

                                        Intent intent = new Intent(LoginPage.this, Faculty_main_activity.class);
                                        intent.putExtra("name",       doc.getString("firstName"));
                                        intent.putExtra("surname",    doc.getString("surname"));
                                        intent.putExtra("email",      doc.getString("email"));
                                        intent.putExtra("department", doc.getString("department"));
                                        intent.putExtra("subject",    doc.getString("subject"));
                                        intent.putExtra("college",    doc.getString("college"));
                                        intent.putExtra("uid",        doc.getString("uid"));
                                        intent.putExtra("gender",        doc.getString("gender"));
                                        intent.putExtra("phone",        doc.getString("phone"));
                                        intent.putExtra("role",        doc.getString("role"));

                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(LoginPage.this,
                                                "Faculty record nahi mila!", Toast.LENGTH_LONG).show();
                                        btnStaffLogin.setEnabled(true);
                                        btnStaffLogin.setText("Staff Login  â†’");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LoginPage.this,
                                            "Data fetch failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    btnStaffLogin.setEnabled(true);
                                    btnStaffLogin.setText("Staff Login  â†’");
                                });
                    })
                    .addOnFailureListener(e -> {
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("password")) {
                            etStaffPassword.setError("Password galat hai");
                        } else {
                            Toast.makeText(LoginPage.this, msg, Toast.LENGTH_LONG).show();
                        }
                        btnStaffLogin.setEnabled(true);
                        btnStaffLogin.setText("Staff Login  â†’");
                    });
        });

        TextView tvStaffForgotPassword = findViewById(R.id.tvStaffForgotPassword);
        tvStaffForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(LoginPage.this, ForgotPasswordActivity.class)));

        // â”€â”€ REGISTER BUTTONS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        btnCreateStudent.setOnClickListener(v ->
                startActivity(new Intent(this, Student_Registration.class)));

        btnCreateFaculty.setOnClickListener(v ->
                startActivity(new Intent(this, FacultyRegistration.class)));
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // FCM Token fetch karke Firestore mein save karo
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private void saveFcmToken(String collection) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("fcmToken", token);
                    
                    FirebaseFirestore.getInstance()
                            .collection(collection)
                            .document(uid)
                            .set(data, SetOptions.merge()); // merge use karo safe rahega
                });
    }

    private void fetchStudentDataAndNavigate(String prn) {

        if (prn != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("prn", prn)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            navigateWithData(
                                    querySnapshot.getDocuments().get(0).getString("firstName"),
                                    querySnapshot.getDocuments().get(0).getString("surname"),
                                    querySnapshot.getDocuments().get(0).getString("prn"),
                                    querySnapshot.getDocuments().get(0).getString("phone"),
                                    querySnapshot.getDocuments().get(0).getString("email"),
                                    querySnapshot.getDocuments().get(0).getString("gender"),
                                    querySnapshot.getDocuments().get(0).getString("course"),
                                    querySnapshot.getDocuments().get(0).getString("year"),
                                    querySnapshot.getDocuments().get(0).getString("college")
                            );
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Data fetch failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        } else {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            navigateWithData(
                                    document.getString("firstName"),
                                    document.getString("surname"),
                                    document.getString("prn"),
                                    document.getString("phone"),
                                    document.getString("email"),
                                    document.getString("gender"),
                                    document.getString("course"),
                                    document.getString("year"),
                                    document.getString("college")
                            );
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Data fetch failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        }
    }

    private void navigateWithData(String name, String surname, String prn,
                                  String phone, String email, String gender,
                                  String course, String year, String college) {
        Intent intent = new Intent(LoginPage.this, MainActivity.class);
        intent.putExtra("name",        name);
        intent.putExtra("surname",     surname);
        intent.putExtra("prn",         prn);
        intent.putExtra("phone",       phone);
        intent.putExtra("email",       email);
        intent.putExtra("gender",      gender);
        intent.putExtra("course",      course);
        intent.putExtra("year",        year);
        intent.putExtra("collegename", college);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setTabActive(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.bg_tab_active);
        active.setTextColor(getResources().getColor(R.color.gold, null));
        active.setTypeface(ResourcesCompat.getFont(this, R.font.dmsans_semibold));

        inactive.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        inactive.setTextColor(getResources().getColor(R.color.gray_500, null));
        inactive.setTypeface(ResourcesCompat.getFont(this, R.font.dmsans_regular));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String name    = parts[0];
        String domain  = parts[1];
        if (name.length() <= 2) return email;
        return name.substring(0, 2) + "***@" + domain;
    }
}
