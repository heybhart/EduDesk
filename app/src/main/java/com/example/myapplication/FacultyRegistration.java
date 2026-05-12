package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class FacultyRegistration extends AppCompatActivity {

    ViewFlipper viewFlipper;
    Button btnNextStep, btnPrevious, btnSubmit;
    Spinner spinnerSubject;

    EditText etFirstName, etSurname, etPhone, etEmail, etPassword, etCollege;
    RadioGroup rgGender, rgDepartment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_registration);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        viewFlipper = findViewById(R.id.viewFlipper);
        btnNextStep = findViewById(R.id.btnNextStep);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnSubmit   = findViewById(R.id.btnSubmit);
        spinnerSubject = findViewById(R.id.spinnerSubject);

        etFirstName = findViewById(R.id.etFirstName);
        etSurname   = findViewById(R.id.etSurname);
        etPhone     = findViewById(R.id.etPhone);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPass);
        etCollege   = findViewById(R.id.etCollege);

        rgGender     = findViewById(R.id.rgGender);
        rgDepartment = findViewById(R.id.rgDepartment);

        setupSubjectSpinner();
        setupButtons();
    }

    private void setupSubjectSpinner() {
        String[] subjects = {
                "Select Subject",
                "Mathematics",
                "Physics",
                "Chemistry",
                "Computer Science",
                "Programming",
                "Database",
                "Business Studies",
                "Economics",
                "English",
                "Statistics",
                "Web Development",
                "Networking",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                subjects
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);
    }

    private void setupButtons() {

        btnNextStep.setOnClickListener(v -> {
            if (validateStep1()) {
                viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_right));
                viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_left));
                viewFlipper.setDisplayedChild(1);
            }
        });

        btnPrevious.setOnClickListener(v -> {
            viewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
            viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out_right));
            viewFlipper.setDisplayedChild(0);
        });

        btnSubmit.setOnClickListener(v -> {
            if (validateStep2()) {

                String firstName = etFirstName.getText().toString().trim();
                String surname   = etSurname.getText().toString().trim();
                String phone     = etPhone.getText().toString().trim();
                String email     = etEmail.getText().toString().trim();
                String password  = etPassword.getText().toString().trim();
                String college   = etCollege.getText().toString().trim();
                String subject   = spinnerSubject.getSelectedItem().toString();

                int genderId = rgGender.getCheckedRadioButtonId();
                RadioButton genderBtn = findViewById(genderId);
                String gender = genderBtn.getText().toString();

                int deptId = rgDepartment.getCheckedRadioButtonId();
                RadioButton deptBtn = findViewById(deptId);
                String department = deptBtn.getText().toString();

                InsertDataOfFaculty obj = new InsertDataOfFaculty();

                btnSubmit.setEnabled(false);

                obj.insertFaculty(
                        firstName,
                        surname,
                        email,
                        password,
                        phone,
                        gender,
                        department,
                        subject,
                        college,
                        new InsertDataOfFaculty.InsertCallback() {

                            @Override
                            public void onSuccess() {
                                Toast.makeText(FacultyRegistration.this,
                                        "Faculty Registration Successful!",
                                        Toast.LENGTH_LONG).show();

                                Intent in  = new Intent(FacultyRegistration.this, LoginPage.class);
                                in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                finish();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(FacultyRegistration.this,
                                        "LoL"+errorMessage,
                                        Toast.LENGTH_LONG).show();
                                btnSubmit.setEnabled(true);
                            }
                        }
                );
            }
        });

        findViewById(R.id.tvSignIn).setOnClickListener(v -> finish());
    }

    private boolean validateStep1() {

        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("First name required");
            return false;
        }

        if (etSurname.getText().toString().trim().isEmpty()) {
            etSurname.setError("Surname required");
            return false;
        }

        if (etPhone.getText().toString().trim().length() < 10) {
            etPhone.setError("Enter valid 10-digit number");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(etEmail.getText().toString().trim())
                .matches()) {
            etEmail.setError("Enter valid email");
            return false;
        }

        if (etPassword.getText().toString().trim().length() < 6) {
            etPassword.setError("Password must be 6+ characters");
            return false;
        }

        return true;
    }

    private boolean validateStep2() {

        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Select Gender", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (rgDepartment.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Select Department", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerSubject.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select Subject", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (etCollege.getText().toString().trim().isEmpty()) {
            etCollege.setError("College required");
            return false;
        }

        return true;
    }
}