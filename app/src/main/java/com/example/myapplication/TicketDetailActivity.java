package com.example.myapplication;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TicketDetailActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────
    TextView tvTicketNumber, tvStatus, tvSubject, tvDescription, tvCreatedDate;
    TextView tvStaffName, tvStaffCategory, tvPriority, tvResolutionTitle, tvResolutionBody;
    ImageView facultyPP, addAttachment;
    LinearLayout layoutAttachments, llMessages, layoutResolutionActions;
    FrameLayout btnSend;
    Button btnResolutionPrimary, btnResolutionSecondary;
    EditText etReply;
    ScrollView svMessages;
    View cardResolutionPanel;

    // ── Firebase ──────────────────────────────────────────────
    FirebaseFirestore db;
    String ticketId, currentUid, chatId, facultyUid, facultyName, currentUserName = "", currentUserPhotoUrl = "";
    ListenerRegistration messageListener;
    ListenerRegistration ticketListener;

    // ── Media ─────────────────────────────────────────────────
    Uri cameraPhotoUri;
    static final int REQ_GALLERY = 101;
    static final int REQ_CAMERA = 102;
    static final int REQ_DOCUMENT = 103;
    static final int REQ_AUDIO = 104;
    static final int REQ_PHOTO_PREVIEW = 105;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ticketId = getIntent().getStringExtra("TicketID");
        chatId = "ticket_" + ticketId;

        // Bind views
        tvTicketNumber = findViewById(R.id.tvTicketNumber);
        tvStatus = findViewById(R.id.tvStatus);
        tvSubject = findViewById(R.id.tvSubject);
        tvDescription = findViewById(R.id.tvDescription);
        tvCreatedDate = findViewById(R.id.tvCreatedDate);
        tvStaffName = findViewById(R.id.tvStaffName);
        tvStaffCategory = findViewById(R.id.faculty_category);
        tvPriority = findViewById(R.id.tvPriority);
        facultyPP = findViewById(R.id.facultyPP);
        layoutAttachments = findViewById(R.id.layoutAttachments);
        llMessages = findViewById(R.id.llMessages);
        svMessages = findViewById(R.id.svMessages);
        btnSend = findViewById(R.id.btnSend);
        etReply = findViewById(R.id.etReply);
        addAttachment = findViewById(R.id.addAttachment);
        cardResolutionPanel = findViewById(R.id.cardResolutionPanel);
        tvResolutionTitle = findViewById(R.id.tvResolutionTitle);
        tvResolutionBody = findViewById(R.id.tvResolutionBody);
        layoutResolutionActions = findViewById(R.id.layoutResolutionActions);
        btnResolutionPrimary = findViewById(R.id.btnResolutionPrimary);
        btnResolutionSecondary = findViewById(R.id.btnResolutionSecondary);

        findViewById(R.id.btnBackLayout).setOnClickListener(v -> finish());
        addAttachment.setOnClickListener(v -> showAttachmentSheet());
        btnSend.setOnClickListener(v -> sendMessage());

        fetchCurrentUserName();

        if (ticketId != null && !ticketId.isEmpty()) {
            loadTicketDetails();
            loadMessages();
        }
    }

    private void fetchCurrentUserName() {
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                currentUserName = (doc.getString("firstName") + " " + doc.getString("surname")).trim();
                currentUserPhotoUrl = doc.getString("profilePhoto") != null ? doc.getString("profilePhoto") : "";
            } else
                db.collection("faculty_user").document(currentUid).get().addOnSuccessListener(fDoc -> {
                    if (fDoc.exists()) {
                        currentUserName = (fDoc.getString("firstName") + " " + fDoc.getString("surname")).trim();
                        currentUserPhotoUrl = fDoc.getString("profilePhoto") != null ? fDoc.getString("profilePhoto") : "";
                    }
                });
        });
    }

    private void loadTicketDetails() {
        if (ticketListener != null) ticketListener.remove();

        ticketListener = db.collection("tickets").document(ticketId).addSnapshotListener((doc, error) -> {
            if (error != null || doc == null || !doc.exists()) return;
            tvTicketNumber.setText("#" + ticketId.substring(0, 6).toUpperCase());
            tvSubject.setText(doc.getString("subject"));
            tvDescription.setText(doc.getString("description"));
            applyPriorityStyle(doc.getString("priority"));
            String status = doc.getString("status");
            if (status != null) {
                tvStatus.setText(status.toUpperCase());
                applyStatusStyle(tvStatus, status);
            }
            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null)
                tvCreatedDate.setText(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(createdAt)));
            facultyUid = doc.getString("assignedFacultyUid");
            facultyName = doc.getString("assignedFacultyName");
            if (facultyUid != null) loadFacultyDetails(facultyUid);
            loadAttachments((List<String>) doc.get("fileUrls"), (List<String>) doc.get("fileNames"));
            updateResolutionPanel(doc);
        });
    }

    private void updateResolutionPanel(com.google.firebase.firestore.DocumentSnapshot doc) {
        if (cardResolutionPanel == null) return;

        String status = doc.getString("status");
        boolean resolutionRequested = Boolean.TRUE.equals(doc.getBoolean("resolutionRequested"));
        boolean isFacultyOwner = facultyUid != null && facultyUid.equals(currentUid);
        boolean canResolve = "In Progress".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status);

        cardResolutionPanel.setVisibility(View.GONE);
        layoutResolutionActions.setVisibility(View.GONE);

        if (isFacultyOwner && canResolve && !resolutionRequested) {
            cardResolutionPanel.setVisibility(View.VISIBLE);
            tvResolutionTitle.setText("Ready To Resolve?");
            tvResolutionBody.setText("Send a confirmation request once the issue looks solved. The ticket will close only after the student says yes.");
            layoutResolutionActions.setVisibility(View.VISIBLE);
            btnResolutionSecondary.setVisibility(View.GONE);
            btnResolutionPrimary.setText("Resolve Ticket");
            btnResolutionPrimary.setOnClickListener(v -> requestTicketResolution(doc));
            return;
        }

        if (isFacultyOwner && resolutionRequested) {
            cardResolutionPanel.setVisibility(View.VISIBLE);
            tvResolutionTitle.setText("Waiting For Student");
            tvResolutionBody.setText("Resolution request sent. The ticket will move to resolved after the student confirms it.");
            return;
        }

        if (!isFacultyOwner && resolutionRequested && canResolve) {
            cardResolutionPanel.setVisibility(View.VISIBLE);
            tvResolutionTitle.setText("Has Your Problem Been Solved?");
            tvResolutionBody.setText("Your faculty marked this ticket as resolved. Choose Yes if the issue is fixed, or No to continue the discussion.");
            layoutResolutionActions.setVisibility(View.VISIBLE);
            btnResolutionSecondary.setVisibility(View.VISIBLE);
            btnResolutionSecondary.setText("No");
            btnResolutionPrimary.setText("Yes");
            btnResolutionSecondary.setOnClickListener(v -> respondToResolutionRequest(false));
            btnResolutionPrimary.setOnClickListener(v -> respondToResolutionRequest(true));
        }
    }

    private void requestTicketResolution(com.google.firebase.firestore.DocumentSnapshot doc) {
        String studentUid = doc.getString("studentUid");
        if (studentUid == null || studentUid.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("resolutionRequested", true);
        updates.put("resolutionRequestedAt", System.currentTimeMillis());
        updates.put("resolutionRequestedByUid", currentUid);
        updates.put("resolutionRequestedByName", currentUserName);

        db.collection("tickets").document(ticketId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    String shortTicketId = ticketId.substring(0, Math.min(6, ticketId.length())).toUpperCase();
                    sendTicketNotification(
                            studentUid,
                            "Resolution Requested",
                            "Faculty asked to close ticket #" + shortTicketId + ". Confirm with Yes or No.",
                            "ticket_resolution_request"
                    );
                    Toast.makeText(this, "Resolution request sent", Toast.LENGTH_SHORT).show();
                });
    }

    private void respondToResolutionRequest(boolean isResolved) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("resolutionRequested", false);
        updates.put("resolutionRequestedAt", FieldValue.delete());
        updates.put("resolutionRequestedByUid", FieldValue.delete());
        updates.put("resolutionRequestedByName", FieldValue.delete());
        updates.put("status", isResolved ? "Resolved" : "In Progress");

        db.collection("tickets").document(ticketId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    String shortTicketId = ticketId.substring(0, Math.min(6, ticketId.length())).toUpperCase();
                    if (facultyUid != null && !facultyUid.isEmpty()) {
                        sendTicketNotification(
                                facultyUid,
                                isResolved ? "Ticket Resolved" : "Resolution Rejected",
                                isResolved
                                        ? "Student confirmed ticket #" + shortTicketId + " is resolved."
                                        : "Student said ticket #" + shortTicketId + " is not solved yet.",
                                isResolved ? "ticket_resolved_confirmed" : "ticket_resolution_rejected"
                        );
                    }
                    Toast.makeText(this, isResolved ? "Ticket resolved" : "Ticket moved back to In Progress", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMessages() {
        messageListener = db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    markCurrentChatAsRead();
                    llMessages.removeAllViews();
                    if (snapshots.isEmpty()) showEmptyState();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String msgId = doc.getId();
                        String senderUid = doc.getString("senderUid");
                        String text = doc.getString("text");
                        String fileUrl = doc.getString("fileUrl");
                        String fileType = doc.getString("fileType");
                        String fileName = doc.getString("fileName");
                        Long timestamp = doc.getLong("timestamp");
                        boolean isSent = currentUid.equals(senderUid);
                        List<String> deletedFor = (List<String>) doc.get("deletedFor");
                        if (deletedFor != null && deletedFor.contains(currentUid)) continue;

                        if (fileUrl != null && !fileUrl.isEmpty())
                            addMediaBubble(msgId, fileUrl, fileType, text, fileName, timestamp != null ? timestamp : 0L, isSent);
                        else
                            addMessageBubble(msgId, text != null ? text : "", timestamp != null ? timestamp : 0L, isSent);
                    }
                    svMessages.post(() -> svMessages.fullScroll(ScrollView.FOCUS_DOWN));
                });
    }

    private void addMessageBubble(String msgId, String text, long timestamp, boolean isSent) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(isSent ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wp.setMargins(0, dp(10), 0, dp(4));
        wrapper.setLayoutParams(wp);

        TextView tvMessage = new TextView(this);
        tvMessage.setText(text);
        tvMessage.setTextSize(14f);
        tvMessage.setLineSpacing(0, 1.2f);
        tvMessage.setPadding(dp(16), dp(12), dp(16), dp(12));
        tvMessage.setMaxWidth(dp(260));
        tvMessage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvMessage.setBackgroundResource(isSent ? R.drawable.bg_ticket_chat_sent : R.drawable.bg_ticket_chat_received);
        tvMessage.setTextColor(isSent ? 0xFFFFFFFF : 0xFF1A2340);
        try {
            tvMessage.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.dmsans_regular));
        } catch (Exception e) {
        }
        tvMessage.setOnLongClickListener(v -> {
            showDeleteDialog(msgId, isSent);
            return true;
        });

        TextView tvTime = new TextView(this);
        tvTime.setText(formatMessageDateTime(timestamp));
        tvTime.setTextSize(10f);
        tvTime.setTextColor(0xFF9CA3AF);
        try {
            tvTime.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.dmsans_regular));
        } catch (Exception e) {
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(isSent ? 0 : dp(4), dp(4), isSent ? dp(4) : 0, 0);
        tvTime.setLayoutParams(tp);

        wrapper.addView(tvMessage);
        wrapper.addView(tvTime);
        llMessages.addView(wrapper);
    }

    private void addMediaBubble(String msgId, String fileUrl, String fileType, String caption, String fileName, long timestamp, boolean isSent) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(isSent ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wp.setMargins(0, dp(10), 0, dp(4));
        wrapper.setLayoutParams(wp);

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(6), dp(6), dp(6), dp(6));
        bubble.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        bubble.setBackgroundResource(isSent ? R.drawable.bg_ticket_chat_sent : R.drawable.bg_ticket_chat_received);
        bubble.setOnLongClickListener(v -> {
            showDeleteDialog(msgId, isSent);
            return true;
        });

        if ("image".equals(fileType)) {
            com.google.android.material.imageview.ShapeableImageView iv = new com.google.android.material.imageview.ShapeableImageView(this);
            iv.setLayoutParams(new LinearLayout.LayoutParams(dp(240), dp(180)));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setShapeAppearanceModel(iv.getShapeAppearanceModel().toBuilder().setAllCornerSizes(dp(12)).build());
            Glide.with(this).load(fileUrl).placeholder(android.R.drawable.ic_menu_gallery).into(iv);
            iv.setOnClickListener(v -> showMediaOptions(fileUrl, "image/*", fileName));
            bubble.addView(iv);
        } else if ("document".equals(fileType)) {
            LinearLayout docCard = new LinearLayout(this);
            docCard.setOrientation(LinearLayout.HORIZONTAL);
            docCard.setGravity(Gravity.CENTER_VERTICAL);
            docCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            docCard.setLayoutParams(new LinearLayout.LayoutParams(dp(240), LinearLayout.LayoutParams.WRAP_CONTENT));
            ImageView docIcon = new ImageView(this);
            docIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
            docIcon.setImageResource(R.drawable.ic_attachment);
            docIcon.setColorFilter(isSent ? 0xFFFFFFFF : 0xFF1A2340);
            TextView tvFileName = new TextView(this);
            tvFileName.setText(fileName != null ? fileName : "Document");
            tvFileName.setTextColor(isSent ? 0xFFFFFFFF : 0xFF1A2340);
            tvFileName.setTextSize(13f);
            tvFileName.setPadding(dp(10), 0, 0, 0);
            tvFileName.setMaxLines(2);
            tvFileName.setEllipsize(android.text.TextUtils.TruncateAt.END);
            try {
                tvFileName.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.dmsans_medium));
            } catch (Exception e) {
            }
            docCard.addView(docIcon);
            docCard.addView(tvFileName);
            bubble.addView(docCard);
            bubble.setOnClickListener(v -> showMediaOptions(fileUrl, "application/pdf", fileName));
        } else if ("audio".equals(fileType)) {
            LinearLayout audioCard = new LinearLayout(this);
            audioCard.setOrientation(LinearLayout.HORIZONTAL);
            audioCard.setGravity(Gravity.CENTER_VERTICAL);
            audioCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            audioCard.setLayoutParams(new LinearLayout.LayoutParams(dp(240), LinearLayout.LayoutParams.WRAP_CONTENT));
            ImageView micIcon = new ImageView(this);
            micIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(32)));
            micIcon.setImageResource(R.drawable.ic_mic);
            micIcon.setColorFilter(isSent ? 0xFFFFFFFF : 0xFF1A2340);
            TextView tvAudio = new TextView(this);
            tvAudio.setText("Audio File");
            tvAudio.setTextColor(isSent ? 0xFFFFFFFF : 0xFF1A2340);
            tvAudio.setTextSize(13f);
            tvAudio.setPadding(dp(10), 0, 0, 0);
            try {
                tvAudio.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.dmsans_medium));
            } catch (Exception e) {
            }
            audioCard.addView(micIcon);
            audioCard.addView(tvAudio);
            bubble.addView(audioCard);
            bubble.setOnClickListener(v -> showMediaOptions(fileUrl, "audio/*", fileName));
        }

        if (caption != null && !caption.isEmpty()) {
            TextView tvCaption = new TextView(this);
            tvCaption.setText(caption);
            tvCaption.setTextColor(isSent ? 0xFFFFFFFF : 0xFF1A2340);
            tvCaption.setTextSize(13f);
            tvCaption.setPadding(dp(10), dp(8), dp(10), dp(4));
            try {
                tvCaption.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.dmsans_regular));
            } catch (Exception e) {
            }
            bubble.addView(tvCaption);
        }

        TextView tvTime = new TextView(this);
        tvTime.setText(formatMessageDateTime(timestamp));
        tvTime.setTextSize(10f);
        tvTime.setTextColor(0xFF9CA3AF);
        try {
            tvTime.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.dmsans_regular));
        } catch (Exception e) {
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(isSent ? 0 : dp(4), dp(4), isSent ? dp(4) : 0, 0);
        tvTime.setLayoutParams(tp);

        wrapper.addView(bubble);
        wrapper.addView(tvTime);
        llMessages.addView(wrapper);
    }

    private void showDeleteDialog(String msgId, boolean isSent) {
        String[] options = isSent ? new String[]{"🗑  Delete for Me", "🗑  Delete for Everyone"} : new String[]{"🗑  Delete for Me"};
        new android.app.AlertDialog.Builder(this).setTitle("Delete Message").setItems(options, (dialog, which) -> {
            if (isSent && which == 1) {
                db.collection("chats").document(chatId).collection("messages").document(msgId).get().addOnSuccessListener(doc -> {
                    String fileUrl = doc.getString("fileUrl");
                    db.collection("chats").document(chatId).collection("messages").document(msgId).delete().addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Deleted for everyone ✅", Toast.LENGTH_SHORT).show();
                        if (fileUrl != null && !fileUrl.isEmpty())
                            SupabaseManager.deleteFile(fileUrl, new SupabaseManager.DeleteCallback() {
                                @Override
                                public void onSuccess() {
                                }

                                @Override
                                public void onFailure(String e) {
                                }
                            });
                    });
                });
            } else {
                db.collection("chats").document(chatId).collection("messages").document(msgId).update("deletedFor", FieldValue.arrayUnion(currentUid)).addOnSuccessListener(unused -> Toast.makeText(this, "Deleted for you ✅", Toast.LENGTH_SHORT).show());
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void sendMessage() {
        String text = etReply.getText().toString().trim();
        if (text.isEmpty()) return;
        etReply.setText("");
        long ts = System.currentTimeMillis();
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderUid", currentUid);
        msg.put("text", text);
        msg.put("timestamp", ts);
        msg.put("seen", false);
        db.collection("chats").document(chatId).collection("messages").add(msg).addOnSuccessListener(ref -> {
            updateChatMeta(text, ts);
            if (facultyUid != null) sendFcmNotification(facultyUid, text);
        });
    }

    private void updateChatMeta(String text, long ts) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("lastMessage", text);
        meta.put("lastMessageTime", ts);
        meta.put("lastSenderUid", currentUid);
        meta.put("unreadFor", Arrays.asList(facultyUid != null ? facultyUid : ""));
        db.collection("chats").document(chatId).set(meta, com.google.firebase.firestore.SetOptions.merge());
    }

    private void showAttachmentSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_attachment, null);
        sheetView.findViewById(R.id.optionDocument).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"});
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Documents"), REQ_DOCUMENT);
        });
        sheetView.findViewById(R.id.optionCamera).setOnClickListener(v -> {
            dialog.dismiss();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            } else openCamera();
        });
        sheetView.findViewById(R.id.optionGallery).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, REQ_GALLERY);
        });
        sheetView.findViewById(R.id.optionAudio).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Audio"), REQ_AUDIO);
        });
        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void openCamera() {
        try {
            File photoFile = File.createTempFile("photo_" + System.currentTimeMillis(), ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            cameraPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            startActivityForResult(intent, REQ_CAMERA);
        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        if (requestCode == REQ_CAMERA) {
            if (cameraPhotoUri != null) openPhotoPreview(cameraPhotoUri);
        } else if (requestCode == REQ_GALLERY) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++)
                        uploadFileDirectly(data.getClipData().getItemAt(i).getUri(), "image");
                } else if (data.getData() != null) openPhotoPreview(data.getData());
            }
        } else if (requestCode == REQ_PHOTO_PREVIEW) {
            if (data != null) {
                String fileUrl = data.getStringExtra("fileUrl");
                String fileType = data.getStringExtra("fileType");
                String caption = data.getStringExtra("caption");
                String fileName = data.getStringExtra("fileName");
                if (fileUrl != null) sendMediaMessage(fileUrl, fileType, caption, fileName);
            }
        } else if (requestCode == REQ_DOCUMENT) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++)
                        uploadFileDirectly(data.getClipData().getItemAt(i).getUri(), "document");
                } else if (data.getData() != null) uploadFileDirectly(data.getData(), "document");
            }
        } else if (requestCode == REQ_AUDIO) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++)
                        uploadFileDirectly(data.getClipData().getItemAt(i).getUri(), "audio");
                } else if (data.getData() != null) uploadFileDirectly(data.getData(), "audio");
            }
        }
    }

    private void openPhotoPreview(Uri uri) {
        Intent intent = new Intent(this, PhotoPreviewActivity.class);
        intent.putExtra("photoUri", uri.toString());
        intent.putExtra("chatId", chatId);
        intent.putExtra("currentUid", currentUid);
        intent.putExtra("otherUid", facultyUid);
        startActivityForResult(intent, REQ_PHOTO_PREVIEW);
    }

    private void uploadFileDirectly(Uri fileUri, String type) {
        String ext = "image".equals(type) ? ".jpg" : "audio".equals(type) ? ".mp3" : ".pdf";
        String fName = type + "_" + System.currentTimeMillis() + ext;
        SupabaseManager.uploadFile(this, fileUri, fName, new SupabaseManager.UploadCallback() {
            @Override
            public void onSuccess(String fileUrl) {
                sendMediaMessage(fileUrl, type, "", fName);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(TicketDetailActivity.this, "Upload failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMediaMessage(String fileUrl, String fileType, String caption, String fileName) {
        long ts = System.currentTimeMillis();
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderUid", currentUid);
        msg.put("text", caption != null ? caption : "");
        msg.put("fileUrl", fileUrl);
        msg.put("fileType", fileType);
        msg.put("fileName", fileName);
        msg.put("timestamp", ts);
        msg.put("seen", false);
        db.collection("chats").document(chatId).collection("messages").add(msg).addOnSuccessListener(ref -> {
            String preview = "image".equals(fileType) ? "📷 Photo" : "audio".equals(fileType) ? "🎵 Audio" : "📄 Document";
            updateChatMeta(preview, ts);
            if (facultyUid != null) sendFcmNotification(facultyUid, preview);
        });
    }

    private void sendFcmNotification(String receiverUid, String body) {
        db.collection("faculty_user").document(receiverUid).get().addOnSuccessListener(fDoc -> {
            String token = fDoc.getString("fcmToken");
            if (token != null) callFcmApi(token, currentUserName, body);
        });
    }

    private void sendTicketNotification(String receiverUid, String title, String body, String type) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("body", body);
        notif.put("type", type);
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("read", false);
        notif.put("userId", receiverUid);
        notif.put("profileImageUrl", currentUserPhotoUrl != null ? currentUserPhotoUrl : "");
        db.collection("notifications").add(notif);

        fetchReceiverToken(receiverUid, token -> {
            if (token != null && !token.isEmpty()) {
                callResolutionApi(token, title, body, type);
            }
        });
    }

    private interface TokenCallback {
        void onLoaded(String token);
    }

    private void fetchReceiverToken(String uid, TokenCallback callback) {
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String token = userDoc.getString("fcmToken");
            if (token != null && !token.isEmpty()) {
                callback.onLoaded(token);
            } else {
                db.collection("faculty_user").document(uid).get()
                        .addOnSuccessListener(facultyDoc -> callback.onLoaded(facultyDoc.getString("fcmToken")));
            }
        });
    }

    private void callFcmApi(String token, String senderName, String body) {
        new Thread(() -> {
            try {
                InputStream is = getAssets().open("service_account.json");
                com.google.auth.oauth2.GoogleCredentials creds = com.google.auth.oauth2.GoogleCredentials.fromStream(is).createScoped("https://www.googleapis.com/auth/firebase.messaging");
                creds.refreshIfExpired();
                String tokenVal = creds.getAccessToken().getTokenValue();
                JSONObject data = new JSONObject();
                data.put("senderName", senderName);
                data.put("body", body);
                data.put("chatId", chatId);
                data.put("senderUid", currentUid);
                data.put("type", "chat_reply");
                data.put("profileImageUrl", currentUserPhotoUrl != null ? currentUserPhotoUrl : "");
                JSONObject msg = new JSONObject();
                msg.put("token", token);
                msg.put("data", data);
                JSONObject payload = new JSONObject();
                payload.put("message", msg);
                URL url = new URL("https://fcm.googleapis.com/v1/projects/collegehelpdeskproject/messages:send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + tokenVal);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                Log.e("FCM", "Error: " + e.getMessage());
            }
        }).start();
    }

    private void callResolutionApi(String token, String title, String body, String type) {
        new Thread(() -> {
            try {
                InputStream is = getAssets().open("service_account.json");
                com.google.auth.oauth2.GoogleCredentials creds = com.google.auth.oauth2.GoogleCredentials.fromStream(is).createScoped("https://www.googleapis.com/auth/firebase.messaging");
                creds.refreshIfExpired();
                String tokenVal = creds.getAccessToken().getTokenValue();
                JSONObject data = new JSONObject();
                data.put("title", title);
                data.put("body", body);
                data.put("type", type);
                data.put("ticketId", ticketId);
                data.put("profileImageUrl", currentUserPhotoUrl != null ? currentUserPhotoUrl : "");
                JSONObject msg = new JSONObject();
                msg.put("token", token);
                msg.put("data", data);
                JSONObject payload = new JSONObject();
                payload.put("message", msg);
                URL url = new URL("https://fcm.googleapis.com/v1/projects/collegehelpdeskproject/messages:send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + tokenVal);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                Log.e("FCM", "Error: " + e.getMessage());
            }
        }).start();
    }

    private void showMediaOptions(String fileUrl, String mimeType, String fileName) {
        new android.app.AlertDialog.Builder(this).setTitle(fileName != null ? fileName : "File").setItems(new String[]{"📂  Open", "🔗  Copy Link"}, (dialog, which) -> {
            if (which == 0) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(fileUrl), mimeType);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(Intent.createChooser(intent, "Open with"));
            } else {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("fileUrl", fileUrl));
                Toast.makeText(this, "Link copied! ✅", Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    private void loadFacultyDetails(String facultyUid) {
        db.collection("faculty_user").document(facultyUid).get().addOnSuccessListener(fDoc -> {
            if (!fDoc.exists()) return;
            tvStaffName.setText(fDoc.getString("firstName") + " " + fDoc.getString("surname"));
            tvStaffCategory.setText(fDoc.getString("department"));
            String url = fDoc.getString("profilePhoto");
            if (url != null && !url.isEmpty()) {
                facultyPP.setPadding(0, 0, 0, 0);
                facultyPP.clearColorFilter();
                Glide.with(this).load(url).transform(new CircleCrop()).placeholder(R.drawable.ic_profile_user).into(facultyPP);
            }
        });
    }

    private void loadAttachments(List<String> urls, List<String> names) {
        if (layoutAttachments == null || urls == null) return;
        layoutAttachments.removeAllViews();
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            String name = (names != null && i < names.size()) ? names.get(i) : "File " + (i + 1);
            TextView tv = new TextView(this);
            tv.setText("📎 " + name);
            tv.setPadding(0, dp(4), 0, dp(4));
            tv.setTextColor(0xFF3B6FE8);
            tv.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
            layoutAttachments.addView(tv);
        }
    }

    private void applyPriorityStyle(String priority) {
        String safePriority = priority == null ? "Low" : priority.trim();
        int color;

        switch (safePriority.toLowerCase(Locale.ROOT)) {
            case "high":
                safePriority = "High";
                color = 0xFFEF4444;
                break;
            case "medium":
                safePriority = "Medium";
                color = 0xFF1565C0;
                break;
            default:
                safePriority = "Low";
                color = 0xFF2E7D32;
                break;
        }

        tvPriority.setText("\u25CF " + safePriority + " Priority");
        tvPriority.setTextColor(color);
    }

    private void applyStatusStyle(TextView tv, String status) {
        switch (status.toLowerCase()) {
            case "open":
                tv.setTextColor(0xFFC9A84C);
                tv.setBackgroundResource(R.drawable.bg_status_open);
                break;
            case "in progress":
                tv.setTextColor(0xFFD4720A);
                tv.setBackgroundResource(R.drawable.bg_status_progress);
                break;
            case "resolved":
                tv.setTextColor(0xFF1A6645);
                tv.setBackgroundResource(R.drawable.bg_status_resolved);
                break;
        }
    }

    private void showEmptyState() {
        TextView tv = new TextView(this);
        tv.setText("No activity yet.");
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(20), 0, 0);
        llMessages.addView(tv);
    }

    private void markCurrentChatAsRead() {
        db.collection("chats").document(chatId).update("unreadFor", FieldValue.arrayRemove(currentUid));
    }

    private String formatMessageDateTime(long timestamp) {
        return timestamp == 0 ? "" : new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(timestamp)).toLowerCase();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
        if (ticketListener != null) ticketListener.remove();
    }
}
