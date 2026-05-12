package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;

public class ForgotPasswordActivity extends AppCompatActivity {

    private CardView          btnBack;
    private TextView          tvGoToLogin;

    // Step 1 — Email
    private LinearLayout      stepEmailLayout;
    private TextInputEditText etResetEmail;
    private Button            btnSendOtp;

    // Step 2 — OTP
    private LinearLayout      stepOtpLayout;
    private TextView          tvOtpSentTo;
    private TextInputEditText etOtp;
    private Button            btnVerifyOtp;
    private TextView          tvResendOtp;

    // Step 3 — New Password
    private LinearLayout      stepPasswordLayout;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private Button            btnResetPassword;

    // Step dots
    private View dot1, dot2, dot3, line1, line2;

    private FirebaseAuth mAuth;
    private OtpManager   otpManager;
    private String confirmedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_passwordactivity);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mAuth      = FirebaseAuth.getInstance();
        otpManager = new OtpManager();

        bindViews();
        showStep(1);

        btnBack.setOnClickListener(v -> finish());
        tvGoToLogin.setOnClickListener(v -> finish());

        // ══════════════════════════════════════════
        // STEP 1 — Email check + OTP bhejo
        // ══════════════════════════════════════════
        btnSendOtp.setOnClickListener(v -> {
            String email = etResetEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etResetEmail.setError("Email daalo pehle");
                etResetEmail.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etResetEmail.setError("Valid email daalo");
                etResetEmail.requestFocus();
                return;
            }

            setLoading(btnSendOtp, true, "Sending OTP...");

            otpManager.generateAndSendOtpToEmail(email, new OtpManager.OtpCallback() {
                @Override
                public void onOtpSent(String sentEmail) {
                    confirmedEmail = sentEmail;
                    tvOtpSentTo.setText("OTP sent to: " + maskEmail(sentEmail));
                    showStep(2);
                    setLoading(btnSendOtp, false, "Send OTP  →");
                    Toast.makeText(ForgotPasswordActivity.this,
                            "OTP bheja gaya! Email check karo.", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onPrnNotFound() {
                    etResetEmail.setError("Ye email registered nahi hai");
                    etResetEmail.requestFocus();
                    setLoading(btnSendOtp, false, "Send OTP  →");
                }
                @Override
                public void onFailure(String error) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Error: " + error, Toast.LENGTH_LONG).show();
                    setLoading(btnSendOtp, false, "Send OTP  →");
                }
            });
        });

        // ══════════════════════════════════════════
        // STEP 2 — OTP Verify
        // ══════════════════════════════════════════
        btnVerifyOtp.setOnClickListener(v -> {
            String entered = etOtp.getText().toString().trim();

            if (entered.length() < 6) {
                etOtp.setError("6 digit OTP daalo");
                etOtp.requestFocus();
                return;
            }

            setLoading(btnVerifyOtp, true, "Verifying...");

            otpManager.verifyOtp(entered, new OtpManager.VerifyCallback() {
                @Override
                public void onVerified(String email) {
                    // OTP sahi — Step 3 pe jaao
                    showStep(3);
                    setLoading(btnVerifyOtp, false, "Verify OTP  →");
                }
                @Override
                public void onWrongOtp() {
                    etOtp.setError("OTP galat hai! Dobara try karo.");
                    etOtp.requestFocus();
                    setLoading(btnVerifyOtp, false, "Verify OTP  →");
                }
                @Override
                public void onExpired() {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "OTP expire ho gaya! Dobara bhejo.", Toast.LENGTH_LONG).show();
                    etOtp.setText("");
                    showStep(1);
                    setLoading(btnVerifyOtp, false, "Verify OTP  →");
                }
            });
        });

        // Resend OTP
        tvResendOtp.setOnClickListener(v -> {
            if (confirmedEmail.isEmpty()) { showStep(1); return; }
            setLoading(btnSendOtp, true, "Sending OTP...");
            otpManager.generateAndSendOtpToEmail(confirmedEmail, new OtpManager.OtpCallback() {
                @Override
                public void onOtpSent(String email) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "OTP dobara bheja gaya!", Toast.LENGTH_SHORT).show();
                    setLoading(btnSendOtp, false, "Send OTP  →");
                }
                @Override public void onPrnNotFound() {}
                @Override
                public void onFailure(String error) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Error: " + error, Toast.LENGTH_LONG).show();
                    setLoading(btnSendOtp, false, "Send OTP  →");
                }
            });
        });

        // ══════════════════════════════════════════
        // STEP 3 — Naya password set karo
        // ══════════════════════════════════════════
        btnResetPassword.setOnClickListener(v -> {
            String newPass     = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (newPass.isEmpty()) {
                etNewPassword.setError("Naya password daalo");
                etNewPassword.requestFocus();
                return;
            }
            if (newPass.length() < 6) {
                etNewPassword.setError("Kam se kam 6 characters chahiye");
                etNewPassword.requestFocus();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                etConfirmPassword.setError("Passwords match nahi ho rahe");
                etConfirmPassword.requestFocus();
                return;
            }

            setLoading(btnResetPassword, true, "Resetting...");

            // Firebase Auth mein sign in karo purane password se
            // Purana password Firestore faculty_user mein "password" field mein stored hai
            // Pehle wahan se lo, phir re-auth karo, phir update karo
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("faculty_user")
                    .whereEqualTo("email", confirmedEmail)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            Toast.makeText(this, "User not found!", Toast.LENGTH_LONG).show();
                            setLoading(btnResetPassword, false, "Reset Password  →");
                            return;
                        }

                        String oldPassword = snap.getDocuments().get(0).getString("password");

                        if (oldPassword == null || oldPassword.isEmpty()) {
                            // Purana password nahi mila — Firebase reset email fallback
                            fallbackResetEmail(newPass);
                            return;
                        }

                        // Re-authenticate karo purane password se
                        AuthCredential credential = EmailAuthProvider
                                .getCredential(confirmedEmail, oldPassword);

                        mAuth.signInWithEmailAndPassword(confirmedEmail, oldPassword)
                                .addOnSuccessListener(authResult -> {
                                    // Sign in hua — ab password update karo
                                    authResult.getUser().updatePassword(newPass)
                                            .addOnSuccessListener(unused -> {
                                                // Firestore mein bhi password update karo
                                                snap.getDocuments().get(0).getReference()
                                                        .update("password", newPass)
                                                        .addOnSuccessListener(u -> {
                                                            // Sign out karo — fresh login ke liye
                                                            mAuth.signOut();
                                                            Toast.makeText(this,
                                                                    "Password successfully reset! Ab login karo.",
                                                                    Toast.LENGTH_LONG).show();
                                                            Intent i = new Intent(this, LoginPage.class);
                                                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(i);
                                                            finish();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            // Firestore update fail — Auth update toh hua
                                                            mAuth.signOut();
                                                            Toast.makeText(this,
                                                                    "Password reset! Login karo.",
                                                                    Toast.LENGTH_LONG).show();
                                                            Intent i = new Intent(this, LoginPage.class);
                                                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(i);
                                                            finish();
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this,
                                                        "Password update failed: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                                setLoading(btnResetPassword, false, "Reset Password  →");
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    // Sign in fail — reset email fallback
                                    fallbackResetEmail(newPass);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setLoading(btnResetPassword, false, "Reset Password  →");
                    });
        });
    }

    // ─────────────────────────────────────────────────────────
    // Fallback — Firebase reset email (last resort)
    // ─────────────────────────────────────────────────────────
    private void fallbackResetEmail(String newPass) {
        mAuth.sendPasswordResetEmail(confirmedEmail)
                .addOnSuccessListener(u -> {
                    Toast.makeText(this,
                            "Reset link bheja gaya " + maskEmail(confirmedEmail)
                                    + " pe!\nLink se naya password set karo.",
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Reset failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(btnResetPassword, false, "Reset Password  →");
                });
    }

    // ─────────────────────────────────────────────────────────
    private void bindViews() {
        btnBack           = findViewById(R.id.btnBack);
        tvGoToLogin       = findViewById(R.id.tvGoToLogin);

        stepEmailLayout   = findViewById(R.id.stepEmailLayout);
        etResetEmail      = findViewById(R.id.etResetEmail);
        btnSendOtp        = findViewById(R.id.btnSendOtp);

        stepOtpLayout     = findViewById(R.id.stepOtpLayout);
        tvOtpSentTo       = findViewById(R.id.tvOtpSentTo);
        etOtp             = findViewById(R.id.etOtp);
        btnVerifyOtp      = findViewById(R.id.btnVerifyOtp);
        tvResendOtp       = findViewById(R.id.tvResendOtp);

        stepPasswordLayout = findViewById(R.id.stepPasswordLayout);
        etNewPassword      = findViewById(R.id.etNewPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        btnResetPassword   = findViewById(R.id.btnResetPassword);

        dot1  = findViewById(R.id.dot1);
        dot2  = findViewById(R.id.dot2);
        dot3  = findViewById(R.id.dot3);
        line1 = findViewById(R.id.line1);
        line2 = findViewById(R.id.line2);
    }

    private void showStep(int step) {
        stepEmailLayout.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        stepOtpLayout.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        stepPasswordLayout.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        dot1.setBackgroundResource(step >= 1 ? R.drawable.dot_active : R.drawable.dot_inactive);
        dot2.setBackgroundResource(step >= 2 ? R.drawable.dot_active : R.drawable.dot_inactive);
        dot3.setBackgroundResource(step >= 3 ? R.drawable.dot_active : R.drawable.dot_inactive);
        line1.setBackgroundResource(step >= 2 ? R.drawable.dot_active : R.drawable.dot_inactive);
        line2.setBackgroundResource(step >= 3 ? R.drawable.dot_active : R.drawable.dot_inactive);
    }

    private void setLoading(Button btn, boolean loading, String label) {
        btn.setEnabled(!loading);
        btn.setText(label);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] p = email.split("@");
        return (p[0].length() <= 2 ? p[0] : p[0].substring(0, 2) + "***") + "@" + p[1];
    }
}