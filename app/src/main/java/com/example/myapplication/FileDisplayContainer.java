package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FileDisplayContainer {

    public interface OnRemoveListener {
        void onRemove();
    }

    public static void addFileView(Context context, LinearLayout container,
                                   String filetype, String filename,
                                   String filesize, String status,
                                   OnRemoveListener onRemove) {

        // ── Root Layout ───────────────────────────────────────
        LinearLayout root = new LinearLayout(context);
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 180);
        rootParams.setMargins(0, 0, 0, 28);
        root.setLayoutParams(rootParams);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(32, 0, 32, 0);
        root.setBackgroundResource(R.drawable.bg_input_ticket);

        // ── Icon Frame ────────────────────────────────────────
        FrameLayout iconFrame = new FrameLayout(context);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(100, 100);
        iconFrame.setLayoutParams(frameParams);

        ImageView imageIcon = new ImageView(context);
        FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        imgParams.gravity = Gravity.CENTER;
        imageIcon.setLayoutParams(imgParams);
        imageIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageIcon.setPadding(8, 8, 8, 8);

        // ── Icon set — PNG original colors ────────────────────
        String type = filetype.toLowerCase();
        if (type.contains("pdf")) {
            imageIcon.setImageResource(R.drawable.icons_pdf);
        } else if (type.contains("image") || type.contains("jpg")
                || type.contains("png") || type.contains("jpeg")) {
            imageIcon.setImageResource(R.drawable.icons_image);
        } else if (type.contains("doc")) {
            imageIcon.setImageResource(R.drawable.icons_word);
        } else if (type.contains("xls") || type.contains("csv")) {
            imageIcon.setImageResource(R.drawable.icons_excel);
        } else if (type.contains("zip") || type.contains("rar")) {
            imageIcon.setImageResource(R.drawable.icons_zip);
        } else {
            imageIcon.setImageResource(R.drawable.icons_unknown_file);
        }

        iconFrame.addView(imageIcon);

        // ── Text Container ────────────────────────────────────
        LinearLayout textContainer = new LinearLayout(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(24, 0, 0, 0);
        textContainer.setLayoutParams(textParams);
        textContainer.setOrientation(LinearLayout.VERTICAL);

        TextView tvFileName = new TextView(context);
        tvFileName.setText(filename);
        tvFileName.setTextSize(13);
        tvFileName.setMaxLines(1);
        tvFileName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvFileName.setTextColor(Color.parseColor("#1A2340"));

        TextView tvFileInfo = new TextView(context);
        if ("Pending".equals(status)) {
            tvFileInfo.setText(filesize + " · Ready to upload");
            tvFileInfo.setTextColor(Color.parseColor("#C9A84C"));
        } else {
            tvFileInfo.setText(filesize + " · Uploaded ✓");
            tvFileInfo.setTextColor(Color.parseColor("#4CAF50"));
        }
        tvFileInfo.setTextSize(12);

        textContainer.addView(tvFileName);
        textContainer.addView(tvFileInfo);

        root.addView(iconFrame);
        root.addView(textContainer);

        // ── Remove Button — sirf tab jab onRemove != null ─────
        if (onRemove != null) {
            ImageView removeBtn = new ImageView(context);
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(50, 50);
            removeBtn.setLayoutParams(removeParams);
            removeBtn.setImageResource(R.drawable.ic_close);
            removeBtn.setColorFilter(Color.parseColor("#AAAAAA"));
            removeBtn.setOnClickListener(v -> {
                container.removeView(root);
                onRemove.onRemove();
            });
            root.addView(removeBtn);
        }

        container.addView(root);
    }
}