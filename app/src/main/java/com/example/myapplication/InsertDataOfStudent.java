package com.example.myapplication;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class InsertDataOfStudent {

    private final FirebaseFirestore db    = FirebaseFirestore.getInstance();
    private final FirebaseAuth      mAuth = FirebaseAuth.getInstance();

    public interface InsertCallback {
        void onSuccess();
        void onFailure(String errorMessage);
        void onPrnExists(); // ← PRN already hai toh ye call hoga
    }

    public void insertDataOfStudent(String firstName,
                                    String surname,
                                    String prn,
                                    String email,
                                    String phone,
                                    String course,
                                    String gender,
                                    String year,
                                    String college,
                                    InsertCallback callback) {

        // ── STEP 1: Pehle check karo PRN already exist karta hai kya ──
        db.collection("users")
                .whereEqualTo("prn", prn)   // Firestore mein dhundo same PRN
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (!querySnapshot.isEmpty()) {
                        // PRN mil gaya — already registered hai
                        callback.onPrnExists();
                        return;
                    }

                    // ── STEP 2: PRN nahi mila — Auth mein register karo ──
                    String password = prn; // Password = PRN

                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {

                                FirebaseUser newUser = authResult.getUser();
                                if (newUser == null) {
                                    callback.onFailure("Registration failed. Try again.");
                                    return;
                                }

                                // ── STEP 3: Firestore mein data save karo ────
                                Map<String, Object> studentData = new HashMap<>();
                                studentData.put("firstName", firstName);
                                studentData.put("surname",   surname);
                                studentData.put("fullName",  firstName + " " + surname);
                                studentData.put("prn",       prn);
                                studentData.put("email",     email);
                                studentData.put("phone",     phone);
                                studentData.put("course",    course);
                                studentData.put("gender",    gender);
                                studentData.put("year",      year);
                                studentData.put("college",   college);
                                studentData.put("role",      "student");
                                studentData.put("uid",       newUser.getUid());
                                studentData.put("createdAt", System.currentTimeMillis());

                                db.collection("users")
                                        .document(newUser.getUid())
                                        .set(studentData)
                                        .addOnSuccessListener(unused -> callback.onSuccess())
                                        .addOnFailureListener(e ->
                                                callback.onFailure("Data save failed: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> {
                                String msg = e.getMessage();
                                if (msg != null && msg.contains("email address is already in use")) {
                                    callback.onFailure("Ye email already registered hai. Login karo.");
                                } else if (msg != null && msg.contains("badly formatted")) {
                                    callback.onFailure("Valid email daalo.");
                                } else {
                                    callback.onFailure("Registration failed: " + msg);
                                }
                            });
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Network error: " + e.getMessage()));
    }
}