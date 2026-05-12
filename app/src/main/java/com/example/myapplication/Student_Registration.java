package com.example.myapplication;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class Student_Registration extends AppCompatActivity {

    ViewFlipper viewFlipper;
    Button btnNextStep, btnPrevious, btnSubmit;

    // Step 1 fields
    EditText etFirstName, etSurname, etPrn, etEmail;

    // Step 2 fields
    EditText etPhone, etCollege;
    RadioGroup rgGender, rgYear;
    Spinner spinnerCourse;

    // Step indicators
    FrameLayout step1Circle, step2Circle;
    TextView step1Label, step2Label, signinlabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_registration);

        viewFlipper = findViewById(R.id.viewFlipper);

        etFirstName = findViewById(R.id.etFirstName);
        etSurname = findViewById(R.id.etSurname);
        etPrn = findViewById(R.id.etPrn);
        etEmail = findViewById(R.id.etEmail);

        etPhone = findViewById(R.id.etPhone);
        etCollege = findViewById(R.id.etCollege);
        rgGender = findViewById(R.id.rgGender);
        rgYear = findViewById(R.id.rgYear);
        spinnerCourse = findViewById(R.id.spinnerCourse);

        step1Circle = findViewById(R.id.step1Circle);
        step2Circle = findViewById(R.id.step2Circle);
        step1Label = findViewById(R.id.step1Label);
        step2Label = findViewById(R.id.step2Label);

        setupInputBehavior(etFirstName);
        setupInputBehavior(etSurname);
        setupInputBehavior(etPrn);
        setupInputBehavior(etEmail);
        setupInputBehavior(etPhone);
        setupInputBehavior(etCollege);

        setupRadioGroupTextColor(rgGender);
        setupRadioGroupTextColor(rgYear);

        String[] courses = {"Select Course / Department", "B.Tech Computer Science",
                "B.Tech IT", "B.Sc Computer Science", "BCA", "MCA",
                "MBA", "B.Com", "B.Sc Physics", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, courses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(adapter);

        btnNextStep = findViewById(R.id.btnNextStep);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnNextStep.setOnClickListener(v -> {
            if (validateStep1()) {
                viewFlipper.setInAnimation(this, android.R.anim.slide_in_left);
                viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
                viewFlipper.showNext();
                updateStepIndicator(2);
            }
        });

        btnPrevious.setOnClickListener(v -> {
            viewFlipper.setInAnimation(this, android.R.anim.slide_in_left);
            viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right);
            viewFlipper.showPrevious();
            updateStepIndicator(1);
        });

        btnSubmit.setOnClickListener(v -> {
            if (validateStep2()) {
                submitForm();
            }
        });

        signinlabel = findViewById(R.id.tvSignIn);
        signinlabel.setOnClickListener(v -> {
            Intent in = new Intent(Student_Registration.this, LoginPage.class);
            startActivity(in);
        });
    }

    private void setupInputBehavior(EditText editText) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (editText.getText().toString().trim().isEmpty()) {
                    setBorder(editText, "#1A2340", 2f);
                }
            } else {
                if (editText.getText().toString().trim().isEmpty()) {
                    setBorder(editText, "#E0E0E0", 1.5f);
                }
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    setBorder(editText, "#C9A84C", 2f);
                } else {
                    setBorder(editText, "#1A2340", 2f);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setBorder(EditText editText, String hexColor, float strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(0xFFFFFFFF);
        drawable.setCornerRadius(dpToPx(8));
        drawable.setStroke((int) dpToPx(strokeDp), android.graphics.Color.parseColor(hexColor));
        editText.setBackground(drawable);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void setupRadioGroupTextColor(RadioGroup radioGroup) {
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < group.getChildCount(); i++) {
                RadioButton rb = (RadioButton) group.getChildAt(i);
                rb.setTextColor(android.graphics.Color.parseColor("#1A2340"));
            }
            RadioButton selected = group.findViewById(checkedId);
            if (selected != null) {
                selected.setTextColor(android.graphics.Color.parseColor("#C9A84C"));
            }
        });
    }

    private boolean validateStep1() {
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("Please enter your first name");
            etFirstName.requestFocus();
            return false;
        }
        if (etSurname.getText().toString().trim().isEmpty()) {
            etSurname.setError("Please enter your surname");
            etSurname.requestFocus();
            return false;
        }
        if (etPrn.getText().toString().trim().isEmpty()) {
            etPrn.setError("Please enter your PRN number");
            etPrn.requestFocus();
            return false;
        }
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty() || !email.contains("@")) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateStep2() {
        if (etPhone.getText().toString().trim().length() < 10) {
            etPhone.setError("Please enter a valid contact number");
            etPhone.requestFocus();
            return false;
        }
        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (spinnerCourse.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select your course/department", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (rgYear.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select your year", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etCollege.getText().toString().trim().isEmpty()) {
            etCollege.setError("Please enter your college name");
            etCollege.requestFocus();
            return false;
        }
        return true;
    }

    private void updateStepIndicator(int step) {
        if (step == 1) {
            step1Circle.setBackgroundResource(R.drawable.bg_step_active);
            step2Circle.setBackgroundResource(R.drawable.bg_step_inactive);
            step1Label.setTextColor(getColor(R.color.navy));
            step2Label.setTextColor(getColor(android.R.color.darker_gray));
        } else {
            step1Circle.setBackgroundResource(R.drawable.bg_step_done);
            step2Circle.setBackgroundResource(R.drawable.bg_step_active);
            step1Label.setTextColor(getColor(android.R.color.darker_gray));
            step2Label.setTextColor(getColor(R.color.navy));
        }
    }

    // ── SUBMIT — Firebase pe data save karo ──────────────────
    // Student_Registration.java mein submitForm() method update karo:
// Sirf callback part change karna hai — baaki sab same rahega

    private void submitForm() {
        String firstName = etFirstName.getText().toString().trim();
        String surname   = etSurname.getText().toString().trim();
        String prn       = etPrn.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String phone     = etPhone.getText().toString().trim();
        String course    = spinnerCourse.getSelectedItem().toString();

        int genderId = rgGender.getCheckedRadioButtonId();
        String gender = ((RadioButton) findViewById(genderId)).getText().toString();

        int yearId = rgYear.getCheckedRadioButtonId();
        String year = ((RadioButton) findViewById(yearId)).getText().toString();

        String college = etCollege.getText().toString().trim();

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Checking...");
        

        InsertDataOfStudent obj = new InsertDataOfStudent();
        obj.insertDataOfStudent(
                firstName, surname, prn, email, phone,
                course, gender, year, college,
                new InsertDataOfStudent.InsertCallback() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(Student_Registration.this,
                                "Registration Successful! Welcome, " + firstName,
                                Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Student_Registration.this, LoginPage.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onPrnExists() {
                        // PRN field pe error dikhao
                        etPrn.setError("This PRN already exists!");
                        etPrn.requestFocus();

                        // Button wapas enable karo
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(Student_Registration.this,
                                errorMessage, Toast.LENGTH_LONG).show();
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit");
                    }
                }
        );
    }
}