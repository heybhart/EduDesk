package com.example.myapplication;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OtpManager {

    private static final String TAG = "OtpManager";

    private static final String EMAILJS_SERVICE_ID  = "service_4udgqae";
    private static final String EMAILJS_TEMPLATE_ID = "template_ncd370l";
    private static final String EMAILJS_PUBLIC_KEY  = "leL4BPI3gnbUJZvCC";
    private static final String EMAILJS_PRIVATE_KEY = "jJ_rUx1N5WFJGZCRALTTK";
    private static final String EMAILJS_API_URL     = "https://api.emailjs.com/api/v1.0/email/send";

    private final FirebaseFirestore db       = FirebaseFirestore.getInstance();
    private final ExecutorService   executor = Executors.newSingleThreadExecutor();

    private String generatedOtp     = "";
    private String studentEmail     = "";
    private String studentName      = "";
    private long   otpGeneratedTime = 0;

    private static final long OTP_EXPIRY_MS = 5 * 60 * 1000;

    public interface OtpCallback {
        void onOtpSent(String email);
        void onPrnNotFound();
        void onFailure(String error);
    }

    public interface VerifyCallback {
        void onVerified(String email);
        void onWrongOtp();
        void onExpired();
    }

    // ─────────────────────────────────────────────────────────
    // STUDENT LOGIN — PRN se email dhundho (purana method)
    // ─────────────────────────────────────────────────────────
    public void generateAndSendOtp(String prn, OtpCallback callback) {

        db.collection("users")
                .whereEqualTo("prn", prn)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        callback.onPrnNotFound();
                        return;
                    }

                    String role = querySnapshot.getDocuments().get(0).getString("role");
                    if (!"student".equals(role)) {
                        callback.onPrnNotFound();
                        return;
                    }

                    studentEmail = querySnapshot.getDocuments().get(0).getString("email");
                    studentName  = querySnapshot.getDocuments().get(0).getString("firstName");

                    if (studentEmail == null || studentEmail.isEmpty()) {
                        callback.onFailure("Email not found for this PRN");
                        return;
                    }

                    generatedOtp     = String.format("%06d", new Random().nextInt(999999));
                    otpGeneratedTime = System.currentTimeMillis();

                    Log.d(TAG, "OTP: " + generatedOtp + " to: " + studentEmail);
                    sendEmail(studentEmail, studentName, generatedOtp, callback);
                })
                .addOnFailureListener(e -> callback.onFailure("Network error: " + e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────
    // FORGOT PASSWORD — direct email se OTP bhejo
    // Firestore check nahi — Firebase Auth khud verify karega
    // ─────────────────────────────────────────────────────────
    public void generateAndSendOtpToEmail(String email, OtpCallback callback) {

        // faculty_user collection mein email verify karo
        db.collection("faculty_user")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        // Email Firestore mein nahi mili
                        callback.onPrnNotFound();
                        return;
                    }

                    // Email mili — name bhi lo agar available ho
                    String name = snap.getDocuments().get(0).getString("firstName");
                    if (name == null) name = snap.getDocuments().get(0).getString("fullName");
                    if (name == null) name = "Faculty";

                    // OTP generate karo
                    studentEmail     = email;
                    studentName      = name;
                    generatedOtp     = String.format("%06d", new Random().nextInt(999999));
                    otpGeneratedTime = System.currentTimeMillis();

                    Log.d(TAG, "Faculty OTP: " + generatedOtp + " to: " + email);
                    sendEmail(email, name, generatedOtp, callback);
                })
                .addOnFailureListener(e -> callback.onFailure("Error: " + e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────
    // EmailJS se email bhejo (shared by both methods)
    // ─────────────────────────────────────────────────────────
    private void sendEmail(String email, String name, String otp, OtpCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject templateParams = new JSONObject();
                templateParams.put("to_email",     email);
                templateParams.put("student_name", name != null ? name : "Faculty");
                templateParams.put("otp",          otp);

                JSONObject body = new JSONObject();
                body.put("service_id",      EMAILJS_SERVICE_ID);
                body.put("template_id",     EMAILJS_TEMPLATE_ID);
                body.put("user_id",         EMAILJS_PUBLIC_KEY);
                body.put("accessToken",     EMAILJS_PRIVATE_KEY);
                body.put("template_params", templateParams);

                String jsonBody = body.toString();
                Log.d(TAG, "Sending: " + jsonBody);

                URL url = new URL(EMAILJS_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        code == 200 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();
                conn.disconnect();

                Log.d(TAG, "Response " + code + ": " + response);

                android.os.Handler handler =
                        new android.os.Handler(android.os.Looper.getMainLooper());

                if (code == 200) {
                    handler.post(() -> callback.onOtpSent(email));
                } else {
                    handler.post(() -> callback.onFailure(
                            "Email failed (" + code + "): " + response));
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onFailure("Error: " + e.getMessage()));
            }
        });
    }

    public void verifyOtp(String enteredOtp, VerifyCallback callback) {
        long elapsed = System.currentTimeMillis() - otpGeneratedTime;

        if (elapsed > OTP_EXPIRY_MS) {
            callback.onExpired();
            return;
        }

        if (enteredOtp.trim().equals(generatedOtp)) {
            callback.onVerified(studentEmail);
        } else {
            callback.onWrongOtp();
        }
    }

    public String getStudentEmail() { return studentEmail; }
}