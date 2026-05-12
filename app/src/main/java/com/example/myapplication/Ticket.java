package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ticket extends Fragment {

    ViewFlipper viewFlipper;

    // Page 1 views
    LinearLayout catAcademics, catFees, catHostel, catLibrary, catIT, catDocuments;
    LinearLayout selectedCategory = null;
    EditText etSubject, etDescription;
    TextView tvCharCount , tvUploadedCount;
    Button btnProceed;
    FrameLayout uploadfilewrapper;
    LinearLayout UploadedFilesContainer, reviewFileContainer;
    ProgressBar uploadProgressBar;

    // Priority views
    TextView priorityLow, priorityMedium, priorityHigh;
    String selectedPriority = "Low";

    // Page 2 views
    TextView tvReviewSubject, tvReviewCategory, tvReviewDesc, tvReviewPriority;
    View reviewPriorityDot;
    CheckBox cbConfirm;
    Button btnSubmitTicket;
    ScrollView reviewScrollView;

    String selectedCategoryName = "Academics";

    // ── Multiple file handling ───────────────────────────────
    ArrayList<Uri> selectedFileUris = new ArrayList<>();
    ArrayList<String> selectedFileNames = new ArrayList<>();
    ArrayList<String> selectedFileTypes = new ArrayList<>();
    ArrayList<String> selectedFileSizes = new ArrayList<>();

    // ── Uploaded file URLs ───────────────────────────────────
    ArrayList<String> uploadedFileUrls = new ArrayList<>();
    int uploadedCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ticket, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewFlipper = view.findViewById(R.id.viewFlipper);

        etSubject = view.findViewById(R.id.etSubject);
        etDescription = view.findViewById(R.id.etDescription);
        tvCharCount = view.findViewById(R.id.tvCharCount);
        btnProceed = view.findViewById(R.id.btnProceed);
        uploadfilewrapper = view.findViewById(R.id.uploadfilewrapper);
        UploadedFilesContainer = view.findViewById(R.id.UploadedFilesContainer);
        uploadProgressBar = view.findViewById(R.id.uploadProgressBar);

        catAcademics = view.findViewById(R.id.catAcademics);
        catFees = view.findViewById(R.id.catFees);
        catHostel = view.findViewById(R.id.catHostel);
        catLibrary = view.findViewById(R.id.catLibrary);
        catIT = view.findViewById(R.id.catIT);
        catDocuments = view.findViewById(R.id.catDocuments);

        priorityLow = view.findViewById(R.id.priorityLow);
        priorityMedium = view.findViewById(R.id.priorityMedium);
        priorityHigh = view.findViewById(R.id.priorityHigh);

        tvReviewSubject = view.findViewById(R.id.tvReviewSubject);
        tvReviewCategory = view.findViewById(R.id.tvReviewCategory);
        tvReviewDesc = view.findViewById(R.id.tvReviewDesc);
        tvReviewPriority = view.findViewById(R.id.tvReviewPriority);
        reviewPriorityDot = view.findViewById(R.id.reviewPriorityDot);
        cbConfirm = view.findViewById(R.id.cbConfirm);
        btnSubmitTicket = view.findViewById(R.id.btnSubmitTicket);
        reviewFileContainer = view.findViewById(R.id.reviewFileContainer);
        reviewScrollView = view.findViewById(R.id.reviewScrollView);

        setupCategories();
        setupPriorities();
        setupCharCounter();
        setupProceedButton();
        setupSubmitButton();



        // Back button
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (viewFlipper.getDisplayedChild() == 1) {
                viewFlipper.setInAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_left));
                viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_right));
                viewFlipper.setDisplayedChild(0);
            }
        });

        selectCategory(catAcademics, "Academics");
        selectPriority("Low");

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (viewFlipper.getDisplayedChild() == 1) {
                            viewFlipper.setInAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_left));
                            viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_right));
                            viewFlipper.setDisplayedChild(0);
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                }
        );

        uploadfilewrapper.setOnClickListener(v -> {
            if (selectedFileUris.size() >= 5) {
                Toast.makeText(getContext(), "Maximum 5 files allowed!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 100);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (!isFileSizeValid(uri)) return;
            String newFileName = getRealFileName(uri);
            if (selectedFileNames.contains(newFileName)) {
                Toast.makeText(getContext(), "Ye file already add ki hai!", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedFileUris.add(uri);
            selectedFileNames.add(newFileName);
            selectedFileTypes.add(getFileType(uri));
            selectedFileSizes.add(getFileSize(uri));
            FileDisplayContainer.addFileView(requireContext(), UploadedFilesContainer, getFileType(uri), newFileName, getFileSize(uri), "Pending", null);
        }
    }

    private void setupPriorities() {
        priorityLow.setOnClickListener(v -> selectPriority("Low"));
        priorityMedium.setOnClickListener(v -> selectPriority("Medium"));
        priorityHigh.setOnClickListener(v -> selectPriority("High"));
    }

    private void selectPriority(String priority) {
        selectedPriority = priority;
        
        // Reset styles
        priorityLow.setBackgroundResource(R.drawable.bg_priority_unselected);
        priorityLow.setTextColor(Color.parseColor("#6B7280"));
        priorityMedium.setBackgroundResource(R.drawable.bg_priority_unselected);
        priorityMedium.setTextColor(Color.parseColor("#6B7280"));
        priorityHigh.setBackgroundResource(R.drawable.bg_priority_unselected);
        priorityHigh.setTextColor(Color.parseColor("#6B7280"));

        // Apply selected style
        switch (priority) {
            case "Low":
                priorityLow.setBackgroundResource(R.drawable.bg_priority_low_selected);
                priorityLow.setTextColor(Color.parseColor("#2E7D32")); // Green
                break;
            case "Medium":
                priorityMedium.setBackgroundResource(R.drawable.bg_priority_medium_selected);
                priorityMedium.setTextColor(Color.parseColor("#1565C0")); // Blue
                break;
            case "High":
                priorityHigh.setBackgroundResource(R.drawable.bg_priority_high_selected);
                priorityHigh.setTextColor(Color.parseColor("#D32F2F")); // Red
                break;
        }
    }



    private void setupProceedButton() {
        btnProceed.setOnClickListener(v -> {
            String subject = etSubject.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            if (subject.isEmpty() || desc.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all details", Toast.LENGTH_SHORT).show();
                return;
            }
            tvReviewSubject.setText(subject);
            tvReviewCategory.setText(selectedCategoryName);
            tvReviewPriority.setText(selectedPriority);
            
            // Set priority dot color in review
            switch (selectedPriority) {
                case "Low": reviewPriorityDot.setBackgroundResource(R.drawable.dot_green); break;
                case "Medium": reviewPriorityDot.setBackgroundResource(R.drawable.dot_blue); break;
                case "High": reviewPriorityDot.setBackgroundResource(R.drawable.dot_red); break;
            }

            tvReviewDesc.setText(desc.length() > 120 ? desc.substring(0, 120) + "..." : desc);
            reviewFileContainer.removeAllViews();
            for (int i = 0; i < selectedFileUris.size(); i++) {
                FileDisplayContainer.addFileView(requireContext(), reviewFileContainer, selectedFileTypes.get(i), selectedFileNames.get(i), selectedFileSizes.get(i), "Pending", null);
            }
            viewFlipper.setInAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right));
            viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_left));
            viewFlipper.setDisplayedChild(1);
            if (reviewScrollView != null) {
                reviewScrollView.post(() -> reviewScrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private void setupSubmitButton() {
        btnSubmitTicket.setOnClickListener(v -> {
            if (!cbConfirm.isChecked()) {
                Toast.makeText(requireContext(), "Please confirm the information", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSubmitTicket.setEnabled(false);
            btnSubmitTicket.setText("Uploading...");
            if (!selectedFileUris.isEmpty()) uploadAllFiles_To_Supabase();
            else saveTicketToFirestore(new ArrayList<>(), new ArrayList<>());
        });
    }

    private void uploadAllFiles_To_Supabase() {
        uploadedCount = 0;
        uploadedFileUrls.clear();
        uploadProgressBar.setVisibility(View.VISIBLE);
        int totalFiles = selectedFileUris.size();
        for (int i = 0; i < totalFiles; i++) {
            Uri fileUri = selectedFileUris.get(i);
            String fileName = selectedFileNames.get(i);
            SupabaseManager.uploadFile(requireContext(), fileUri, fileName, new SupabaseManager.UploadCallback() {
                @Override public void onSuccess(String fileUrl) {
                    uploadedFileUrls.add(fileUrl);
                    uploadedCount++;
                    if (uploadedCount == totalFiles) {
                        uploadProgressBar.setVisibility(View.GONE);
                        saveTicketToFirestore(uploadedFileUrls, selectedFileNames);
                    }
                }
                @Override public void onFailure(String error) {
                    uploadProgressBar.setVisibility(View.GONE);
                    btnSubmitTicket.setEnabled(true);
                    btnSubmitTicket.setText(getSubmitButtonLabel());
                    Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveTicketToFirestore(List<String> fileUrls, List<String> fileNames) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String studentName = requireActivity().getIntent().getStringExtra("name");
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("subject", etSubject.getText().toString().trim());
        ticket.put("category", selectedCategoryName);
        ticket.put("priority", selectedPriority);
        ticket.put("description", etDescription.getText().toString().trim());
        ticket.put("fileUrls", fileUrls);
        ticket.put("fileNames", fileNames);
        ticket.put("status", "Open");
        ticket.put("studentUid", uid);
        ticket.put("studentName", studentName);
        ticket.put("createdAt", System.currentTimeMillis());

        String subject = etSubject.getText().toString().trim();
        String priority = selectedPriority;

        FirebaseFirestore.getInstance().collection("tickets").add(ticket).addOnSuccessListener(ref -> {
            Toast.makeText(getContext(), "Ticket Submitted!", Toast.LENGTH_LONG).show();
            notifyFaculties(subject, priority, uid);
            resetForm();
        }).addOnFailureListener(e -> {
            btnSubmitTicket.setEnabled(true);
            btnSubmitTicket.setText(getSubmitButtonLabel());
        });
    }

    private void notifyFaculties(String subject, String priority, String studentUid) {
        FirebaseFirestore.getInstance().collection("users").document(studentUid).get().addOnSuccessListener(userDoc -> {
            String studentPhoto = userDoc.getString("profilePhoto");
            FirebaseFirestore.getInstance().collection("faculty_user").get().addOnSuccessListener(snapshots -> {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String token = doc.getString("fcmToken");
                    if (token != null) {
                        callFcmApi(
                                token,
                                "New Ticket Raised",
                                "A new ticket has been raised [" + priority + "]: " + subject,
                                "ticket_raised",
                                studentPhoto
                        );
                    }
                }
            });
        });
    }

    private void callFcmApi(String token, String title, String body, String type, String profileImageUrl) {
        new Thread(() -> {
            try {
                InputStream is = requireContext().getAssets().open("service_account.json");
                com.google.auth.oauth2.GoogleCredentials credentials = com.google.auth.oauth2.GoogleCredentials.fromStream(is).createScoped("https://www.googleapis.com/auth/firebase.messaging");
                credentials.refreshIfExpired();
                String accessToken = credentials.getAccessToken().getTokenValue();
                JSONObject data = new JSONObject();
                data.put("title", title);
                data.put("body",  body);
                data.put("type",  type);
                data.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
                JSONObject messageObj = new JSONObject();
                messageObj.put("token", token);
                messageObj.put("data",  data);
                JSONObject payload = new JSONObject();
                payload.put("message", messageObj);
                URL url = new URL("https://fcm.googleapis.com/v1/projects/collegehelpdeskproject/messages:send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type",  "application/json");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) { Log.e("FCM", "Error: " + e.getMessage()); }
        }).start();
    }

    private void resetForm() {
        viewFlipper.setDisplayedChild(0);
        etSubject.setText("");
        etDescription.setText("");
        cbConfirm.setChecked(true);
        uploadedCount = 0;
        uploadedFileUrls.clear();
        selectedFileUris.clear();
        selectedFileNames.clear();
        selectedFileTypes.clear();
        selectedFileSizes.clear();
        UploadedFilesContainer.removeAllViews();
        reviewFileContainer.removeAllViews();
        btnSubmitTicket.setEnabled(true);
        btnSubmitTicket.setText(getSubmitButtonLabel());
        if (reviewScrollView != null) {
            reviewScrollView.post(() -> reviewScrollView.scrollTo(0, 0));
        }
        selectCategory(catAcademics, "Academics");
        selectPriority("Low");
    }

    private String getSubmitButtonLabel() {
        return "Submit Ticket  >";
    }

    private boolean isFileSizeValid(Uri uri) { return true; }
    private String getRealFileName(Uri uri) { return "file_" + System.currentTimeMillis(); }
    private String getFileType(Uri uri) { return "File"; }
    private String getFileSize(Uri uri) { return "0 KB"; }
    private void setupCategories() {
        catAcademics.setOnClickListener(v -> selectCategory(catAcademics, "Academics"));
        catFees.setOnClickListener(v -> selectCategory(catFees, "Fees"));
        catHostel.setOnClickListener(v -> selectCategory(catHostel, "Hostel"));
        catLibrary.setOnClickListener(v -> selectCategory(catLibrary, "Document"));
        catIT.setOnClickListener(v -> selectCategory(catIT, "IT Support"));
        catDocuments.setOnClickListener(v -> selectCategory(catDocuments, "Others"));
    }
    private void selectCategory(LinearLayout cat, String name) {
        if (selectedCategory != null) selectedCategory.setBackgroundResource(R.drawable.bg_category_normal);
        cat.setBackgroundResource(R.drawable.bg_category_selected);
        selectedCategory = cat;
        selectedCategoryName = name;
    }
    private void setupCharCounter() {
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { tvCharCount.setText(s.length() + " / 500"); }
        });
    }
}
