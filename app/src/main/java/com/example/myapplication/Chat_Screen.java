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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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
import java.util.Locale;
import java.util.Map;

public class Chat_Screen extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────
    TextView tvChatName, tvChatDept;
    EditText etChatMessage;
    View btnSend;
    ScrollView svMessages;
    LinearLayout llMessages, userdetailcontainer;
    ImageView addAttachment, chatHeaderAvatar;

    // ── Firebase ──────────────────────────────────────────────
    FirebaseFirestore db;
    String currentUid, otherUid, otherName, otherDept, chatId, ticketId;
    String currentUserName = "";
    ListenerRegistration messageListener;

    // ── Camera ────────────────────────────────────────────────
    Uri cameraPhotoUri;

    // ── Request codes ─────────────────────────────────────────
    static final int REQ_GALLERY = 101;
    static final int REQ_CAMERA = 102;
    static final int REQ_DOCUMENT = 103;
    static final int REQ_AUDIO = 104;
    static final int REQ_PHOTO_PREVIEW = 105;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_screen);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        // Bind views
        tvChatName = findViewById(R.id.tvChatName);
        tvChatDept = findViewById(R.id.tvChatDept);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSend = findViewById(R.id.btnSend);
        svMessages = findViewById(R.id.svMessages);
        llMessages = findViewById(R.id.llMessages);
        addAttachment = findViewById(R.id.addAttachment);
        userdetailcontainer = findViewById(R.id.userdetailcontainer);
        chatHeaderAvatar = findViewById(R.id.chatHeaderAvatar);

        // Get data from intent
        otherUid = getIntent().getStringExtra("facultyUid");
        otherName = getIntent().getStringExtra("facultyName");
        otherDept = getIntent().getStringExtra("facultyDepartment");
        chatId = getIntent().getStringExtra("chatId");
        ticketId = getIntent().getStringExtra("ticketId");

        // Set header text
        tvChatName.setText(otherName);
        tvChatDept.setText(otherDept != null ? otherDept.toUpperCase() : "");

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Chat logic separation
        if (ticketId != null && !ticketId.isEmpty()) {
            // If ticket-based chat, use ticketId as chatId
            chatId = "ticket_" + ticketId;
        } else if (chatId == null || chatId.isEmpty()) {
            // Normal direct chat
            String[] uids = {currentUid, otherUid};
            Arrays.sort(uids);
            chatId = uids[0] + "_" + uids[1];
        }

        // Load current user name (used in FCM notifications)
        fetchCurrentUserName();

        // Load other user profile photo in header
        loadHeaderPhoto();

        // Header click → view other user profile
        userdetailcontainer.setOnClickListener(v -> {
            Intent in = new Intent(Chat_Screen.this, ShowUserProfile.class);
            in.putExtra("otherUID", otherUid);
            startActivity(in);
        });

        // Buttons
        findViewById(R.id.btnChatBack).setOnClickListener(v -> finish());
        addAttachment.setOnClickListener(v -> showAttachmentSheet());
        btnSend.setOnClickListener(v -> sendMessage());

        // Load messages if chat exists
        db.collection("chats").document(chatId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        markCurrentChatAsRead();
                        loadMessages();
                    }
                });
    }

    private void loadHeaderPhoto() {
        if (chatHeaderAvatar == null || otherUid == null) return;
        db.collection("users").document(otherUid).get().addOnSuccessListener(doc -> {
            String photo = doc.exists() ? doc.getString("profilePhoto") : null;
            if (photo != null && !photo.isEmpty()) applyHeaderPhoto(photo);
            else {
                db.collection("faculty_user").document(otherUid).get().addOnSuccessListener(fDoc -> {
                    String fp = fDoc.exists() ? fDoc.getString("profilePhoto") : null;
                    if (fp != null && !fp.isEmpty()) applyHeaderPhoto(fp);
                });
            }
        });
    }

    private void applyHeaderPhoto(String url) {
        chatHeaderAvatar.setPadding(0, 0, 0, 0);
        chatHeaderAvatar.clearColorFilter();
        Glide.with(this).load(url).transform(new CircleCrop()).placeholder(R.drawable.ic_profile_user).into(chatHeaderAvatar);
    }

    private void fetchCurrentUserName() {
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                currentUserName = buildFullName(doc.getString("firstName"), doc.getString("surname"));
            } else {
                db.collection("faculty_user").document(currentUid).get().addOnSuccessListener(fDoc -> {
                    if (fDoc.exists()) {
                        currentUserName = buildFullName(fDoc.getString("firstName"), fDoc.getString("surname"));
                    }
                });
            }
        });
    }

    private String buildFullName(String first, String last) {
        return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
    }

    private void sendMessage() {
        String text = etChatMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etChatMessage.setText("");
        btnSend.setEnabled(false);
        long timestamp = System.currentTimeMillis();
        Map<String, Object> message = new HashMap<>();
        message.put("senderUid", currentUid);
        message.put("text", text);
        message.put("timestamp", timestamp);
        message.put("seen", false);

        createChatDocIfNeeded(() ->
                db.collection("chats").document(chatId)
                        .collection("messages").add(message)
                        .addOnSuccessListener(ref -> {
                            btnSend.setEnabled(true);
                            updateLastMessage(text, timestamp);
                            sendFcmNotification(otherUid, text);
                        })
                        .addOnFailureListener(e -> {
                            btnSend.setEnabled(true);
                            Toast.makeText(this, "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        })
        );
    }

    private void sendMediaMessage(String fileUrl, String fileType, String caption, String fileName) {
        long timestamp = System.currentTimeMillis();
        Map<String, Object> message = new HashMap<>();
        message.put("senderUid", currentUid);
        message.put("text", caption != null ? caption : "");
        message.put("fileUrl", fileUrl);
        message.put("fileType", fileType);
        message.put("fileName", fileName);
        message.put("timestamp", timestamp);
        message.put("seen", false);

        createChatDocIfNeeded(() ->
                db.collection("chats").document(chatId)
                        .collection("messages").add(message)
                        .addOnSuccessListener(ref -> {
                            String preview = "image".equals(fileType) ? "📷 Photo" : "audio".equals(fileType) ? "🎵 Audio" : "📄 Document";
                            updateLastMessage(preview, timestamp);
                            sendFcmNotification(otherUid, preview);
                        })
        );
    }

    private void updateLastMessage(String text, long timestamp) {
        Map<String, Object> update = new HashMap<>();
        update.put("lastMessage", text);
        update.put("lastMessageTime", timestamp);
        update.put("lastSenderUid", currentUid);
        update.put("unreadFor", Arrays.asList(otherUid));
        db.collection("chats").document(chatId).update(update);
    }

    private void sendFcmNotification(String receiverUid, String messageText) {
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            String photo = (doc.exists() && doc.getString("profilePhoto") != null) ? doc.getString("profilePhoto") : "";
            if (!photo.isEmpty()) fetchTokenAndSend(receiverUid, messageText, photo);
            else {
                db.collection("faculty_user").document(currentUid).get().addOnSuccessListener(fDoc -> {
                    String fp = (fDoc.exists() && fDoc.getString("profilePhoto") != null) ? fDoc.getString("profilePhoto") : "";
                    fetchTokenAndSend(receiverUid, messageText, fp);
                });
            }
        });
    }

    private void fetchTokenAndSend(String receiverUid, String messageText, String senderPhoto) {
        db.collection("users").document(receiverUid).get().addOnSuccessListener(doc -> {
            String token = doc.exists() ? doc.getString("fcmToken") : null;
            if (token != null) callFcmApi(token, messageText, senderPhoto);
            else {
                db.collection("faculty_user").document(receiverUid).get().addOnSuccessListener(fDoc -> {
                    String ft = fDoc.exists() ? fDoc.getString("fcmToken") : null;
                    if (ft != null) callFcmApi(ft, messageText, senderPhoto);
                });
            }
        });
    }

    private void callFcmApi(String receiverToken, String messageText, String senderPhoto) {
        String senderName = currentUserName.isEmpty() ? "New Message" : currentUserName;
        String body = messageText.length() > 80 ? messageText.substring(0, 80) + "..." : messageText;
        new Thread(() -> {
            try {
                InputStream is = getAssets().open("service_account.json");
                com.google.auth.oauth2.GoogleCredentials credentials = com.google.auth.oauth2.GoogleCredentials.fromStream(is).createScoped("https://www.googleapis.com/auth/firebase.messaging");
                credentials.refreshIfExpired();
                String accessToken = credentials.getAccessToken().getTokenValue();
                JSONObject data = new JSONObject();
                data.put("senderName", senderName);
                data.put("body", body);
                data.put("chatId", chatId);
                data.put("senderUid", currentUid);
                data.put("senderPhoto", senderPhoto != null ? senderPhoto : "");
                JSONObject androidConfig = new JSONObject();
                androidConfig.put("priority", "high");
                JSONObject messageObj = new JSONObject();
                messageObj.put("token", receiverToken);
                messageObj.put("data", data);
                messageObj.put("android", androidConfig);
                JSONObject payload = new JSONObject();
                payload.put("message", messageObj);
                URL url = new URL("https://fcm.googleapis.com/v1/projects/collegehelpdeskproject/messages:send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                android.util.Log.e("FCM_DEBUG", "Exception: " + e.getMessage(), e);
            }
        }).start();
    }

    private void loadMessages() {
        messageListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    markCurrentChatAsRead();
                    llMessages.removeAllViews();
                    if (snapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String msgId = doc.getId();
                        String senderUid = doc.getString("senderUid");
                        String text = doc.getString("text");
                        String fileUrl = doc.getString("fileUrl");
                        String fileType = doc.getString("fileType");
                        String fileName = doc.getString("fileName");
                        Long timestamp = doc.getLong("timestamp");
                        boolean isSent = currentUid.equals(senderUid);
                        java.util.List<String> deletedFor = (java.util.List<String>) doc.get("deletedFor");
                        if (deletedFor != null && deletedFor.contains(currentUid)) continue;
                        if (fileUrl != null && !fileUrl.isEmpty()) {
                            addMediaBubble(msgId, fileUrl, fileType, text, fileName, timestamp != null ? timestamp : 0L, isSent);
                        } else {
                            addMessageBubble(msgId, text != null ? text : "", timestamp != null ? timestamp : 0L, isSent);
                        }
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
        tvMessage.setLineSpacing(0, 1.3f);
        tvMessage.setPadding(dp(14), dp(14), dp(14), dp(14));
        tvMessage.setMaxWidth(dp(280));
        tvMessage.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvMessage.setBackgroundResource(isSent ? R.drawable.bg_bubble_sent : R.drawable.bg_bubble_received);
        tvMessage.setTextColor(isSent ? 0xFFFFFFFF : 0xFF1A2340);
        tvMessage.setOnLongClickListener(v -> {
            showDeleteDialog(msgId, isSent);
            return true;
        });

        TextView tvTime = new TextView(this);
        tvTime.setText(formatMessageDateTime(timestamp));
        tvTime.setTextSize(11f);
        tvTime.setTextColor(0xFFAAAAAA);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(0, dp(4), 0, 0);
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
        bubble.setPadding(dp(8), dp(8), dp(8), dp(8));
        bubble.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        bubble.setBackgroundResource(isSent ? R.drawable.bg_bubble_sent : R.drawable.bg_bubble_received);
        bubble.setOnLongClickListener(v -> {
            showDeleteDialog(msgId, isSent);
            return true;
        });

        if ("image".equals(fileType)) {
            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new LinearLayout.LayoutParams(dp(200), dp(200)));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(0xFF333333);
            Glide.with(this).load(fileUrl).placeholder(android.R.drawable.ic_menu_gallery).into(iv);
            iv.setOnClickListener(v -> showMediaOptions(fileUrl, "image/*", fileName));
            bubble.addView(iv);
        } else if ("document".equals(fileType)) {
            LinearLayout docCard = new LinearLayout(this);
            docCard.setOrientation(LinearLayout.HORIZONTAL);
            docCard.setGravity(Gravity.CENTER_VERTICAL);
            docCard.setPadding(dp(8), dp(8), dp(8), dp(8));
            docCard.setLayoutParams(new LinearLayout.LayoutParams(dp(220), LinearLayout.LayoutParams.WRAP_CONTENT));
            ImageView docIcon = new ImageView(this);
            docIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
            docIcon.setImageResource(R.drawable.ic_attachment);
            docIcon.setColorFilter(isSent ? 0xFFFFFFFF : 0xFF1A2340);
            TextView tvFileName = new TextView(this);
            tvFileName.setText(fileName != null ? fileName : "Document");
            tvFileName.setTextColor(isSent ? 0xFFFFFFFF : 0xFF1A2340);
            tvFileName.setTextSize(13f);
            tvFileName.setPadding(dp(8), 0, 0, 0);
            tvFileName.setMaxLines(2);
            tvFileName.setEllipsize(android.text.TextUtils.TruncateAt.END);
            docCard.addView(docIcon);
            docCard.addView(tvFileName);
            bubble.addView(docCard);
            bubble.setOnClickListener(v -> showMediaOptions(fileUrl, "application/pdf", fileName));
        } else if ("audio".equals(fileType)) {
            LinearLayout audioCard = new LinearLayout(this);
            audioCard.setOrientation(LinearLayout.HORIZONTAL);
            audioCard.setGravity(Gravity.CENTER_VERTICAL);
            audioCard.setPadding(dp(8), dp(8), dp(8), dp(8));
            audioCard.setLayoutParams(new LinearLayout.LayoutParams(dp(220), LinearLayout.LayoutParams.WRAP_CONTENT));
            ImageView micIcon = new ImageView(this);
            micIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
            micIcon.setImageResource(R.drawable.ic_mic);
            micIcon.setColorFilter(isSent ? 0xFFFFFFFF : 0xFF1A2340);
            TextView tvAudio = new TextView(this);
            tvAudio.setText("🎵  " + (fileName != null ? fileName : "Audio"));
            tvAudio.setTextColor(isSent ? 0xFFFFFFFF : 0xFF1A2340);
            tvAudio.setTextSize(13f);
            tvAudio.setPadding(dp(8), 0, 0, 0);
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
            tvCaption.setPadding(dp(4), dp(6), dp(4), 0);
            bubble.addView(tvCaption);
        }

        TextView tvTime = new TextView(this);
        tvTime.setText(formatMessageDateTime(timestamp));
        tvTime.setTextSize(11f);
        tvTime.setTextColor(0xFFAAAAAA);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(0, dp(4), 0, 0);
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
                Toast.makeText(Chat_Screen.this, "Upload failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openPhotoPreview(Uri uri) {
        Intent intent = new Intent(this, PhotoPreviewActivity.class);
        intent.putExtra("photoUri", uri.toString());
        intent.putExtra("chatId", chatId);
        intent.putExtra("currentUid", currentUid);
        intent.putExtra("otherUid", otherUid);
        startActivityForResult(intent, REQ_PHOTO_PREVIEW);
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

    private void createChatDocIfNeeded(Runnable onReady) {
        db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) onReady.run();
            else {
                db.collection("users").document(currentUid).get().addOnSuccessListener(myDoc -> {
                    String myName = "User", myDept = "";
                    if (myDoc.exists()) {
                        myName = buildFullName(myDoc.getString("firstName"), myDoc.getString("surname"));
                        myDept = myDoc.getString("course") != null ? myDoc.getString("course") : "";
                    }
                    Map<String, Object> chat = new HashMap<>();
                    chat.put("participants", Arrays.asList(currentUid, otherUid));
                    chat.put("user1Uid", currentUid);
                    chat.put("user1Name", myName);
                    chat.put("user1Dept", myDept);
                    chat.put("user2Uid", otherUid);
                    chat.put("user2Name", otherName != null ? otherName : "User");
                    chat.put("user2Dept", otherDept != null ? otherDept : "");
                    chat.put("lastMessage", "");
                    chat.put("lastMessageTime", System.currentTimeMillis());
                    chat.put("lastSenderUid", "");
                    chat.put("unreadFor", Arrays.asList());
                    if (ticketId != null) chat.put("ticketId", ticketId);

                    db.collection("chats").document(chatId).set(chat).addOnSuccessListener(unused -> {
                        loadMessages();
                        onReady.run();
                    }).addOnFailureListener(e -> onReady.run());
                });
            }
        });
    }

    private void showEmptyState() {
        TextView tv = new TextView(this);
        tv.setText("Say hello! 👋\nStart the conversation.");
        tv.setTextColor(0xFFAAAAAA);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(40), 0, 0);
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        llMessages.addView(tv);
    }

    private void markCurrentChatAsRead() {
        db.collection("chats").document(chatId).update("unreadFor", FieldValue.arrayRemove(currentUid));
    }

    private String formatMessageDateTime(long timestamp) {
        return timestamp == 0 ? "" : new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(timestamp));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
}