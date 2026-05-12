package com.example.myapplication;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class InsertDataOfFaculty {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public interface InsertCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public void insertFaculty(String firstName,
                              String surname,
                              String email,
                              String password,
                              String phone,
                              String gender,
                              String department,
                              String subject,
                              String college,
                              InsertCallback callback) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onFailure("Registration Failed");
                        return;
                    }

                    Map<String, Object> faculty = new HashMap<>();
                    faculty.put("firstName", firstName);
                    faculty.put("surname", surname);
                    faculty.put("fullName", firstName + " " + surname);
                    faculty.put("email", email);
                    faculty.put("password", password);
                    faculty.put("phone", phone);
                    faculty.put("gender", gender);
                    faculty.put("department", department);
                    faculty.put("subject", subject);
                    faculty.put("college", college);
                    faculty.put("role", "faculty");
                    faculty.put("uid", user.getUid());
                    faculty.put("createdAt", System.currentTimeMillis());

                    db.collection("faculty_user")
                            .document(user.getUid())
                            .set(faculty)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e ->
                                    callback.onFailure("Data Save Failed: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Auth Failed: " + e.getMessage()));
    }
}