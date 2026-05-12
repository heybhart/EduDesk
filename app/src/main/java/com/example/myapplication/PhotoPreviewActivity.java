package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PhotoPreviewActivity extends AppCompatActivity {

    ImageView ivPreview, btnPreviewBack;
    EditText etCaption;
    View btnPreviewSend;

    Uri photoUri;
    String chatId, currentUid, otherUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_preview);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        ivPreview      = findViewById(R.id.ivPreview);
        btnPreviewBack = findViewById(R.id.btnPreviewBack);
        etCaption      = findViewById(R.id.etCaption);
        btnPreviewSend = findViewById(R.id.btnPreviewSend);

        // ── Intent se data lo ─────────────────────────────────
        String uriStr = getIntent().getStringExtra("photoUri");
        chatId        = getIntent().getStringExtra("chatId");
        currentUid    = getIntent().getStringExtra("currentUid");
        otherUid      = getIntent().getStringExtra("otherUid");

        if (uriStr != null) {
            photoUri = Uri.parse(uriStr);
            ivPreview.setImageURI(photoUri);
        }

        // ── Back ──────────────────────────────────────────────
        btnPreviewBack.setOnClickListener(v -> finish());

        // ── Send ──────────────────────────────────────────────
        btnPreviewSend.setOnClickListener(v -> uploadAndSend());
    }

    private void uploadAndSend() {
        if (photoUri == null) return;

        btnPreviewSend.setEnabled(false);
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        String fileName = "chat_" + System.currentTimeMillis() + ".jpg";
        String caption  = etCaption.getText().toString().trim();

        SupabaseManager.uploadFile(this, photoUri, fileName,
                new SupabaseManager.UploadCallback() {
                    @Override
                    public void onSuccess(String fileUrl) {
                        // ── Wapas Chat_Screen ko result bhejo ─
                        Intent result = new Intent();
                        result.putExtra("fileUrl",   fileUrl);
                        result.putExtra("fileType",  "image");
                        result.putExtra("caption",   caption);
                        result.putExtra("fileName",  fileName);
                        setResult(RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onFailure(String error) {
                        btnPreviewSend.setEnabled(true);
                        Toast.makeText(PhotoPreviewActivity.this,
                                "Upload failed: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}