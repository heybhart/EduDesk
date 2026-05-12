package com.example.myapplication;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SupabaseManager {

    private static final String SUPABASE_URL =
            "YOUR_SUPABASE_URL";

    private static final String SUPABASE_KEY =
            "YOUR_SUPABASE_ANON_KEY";

    private static final String BUCKET_NAME = "ticket-files";

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(String fileUrl);
        void onFailure(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // â”€â”€ File upload â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public static void uploadFile(Context context,
                                  Uri fileUri,
                                  String fileName,
                                  UploadCallback callback) {

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String uniqueName = System.currentTimeMillis() + "_" + fileName;
                String uploadUrl  = SUPABASE_URL + "/storage/v1/object/"
                        + BUCKET_NAME + "/" + uniqueName;

                URL url = new URL(uploadUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
                conn.setRequestProperty("apikey", SUPABASE_KEY);
                conn.setRequestProperty("Content-Type", "application/octet-stream");

                InputStream  inputStream  = context.getContentResolver().openInputStream(fileUri);
                OutputStream outputStream = conn.getOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1)
                    outputStream.write(buffer, 0, bytesRead);

                inputStream.close();
                outputStream.flush();
                outputStream.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == 200 || responseCode == 201) {
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/"
                            + BUCKET_NAME + "/" + uniqueName;
                    mainHandler.post(() -> callback.onSuccess(publicUrl));
                } else {
                    final int code = responseCode;
                    mainHandler.post(() -> callback.onFailure("Upload failed. Code: " + code));
                }

            } catch (Exception e) {
                final String errMsg = e.getMessage();
                mainHandler.post(() -> callback.onFailure("Error: " + errMsg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // â”€â”€ File delete â€” Supabase storage se â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // fileUrl = full public URL
    // e.g. https://xxx.supabase.co/storage/v1/object/public/ticket-files/image_123.jpg
    public static void deleteFile(String fileUrl, DeleteCallback callback) {

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // â”€â”€ URL se file path nikalo â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                // /storage/v1/object/public/ticket-files/fileName
                // â†’ DELETE /storage/v1/object/ticket-files/fileName
                String marker    = "/object/public/" + BUCKET_NAME + "/";
                int    idx       = fileUrl.indexOf(marker);

                if (idx == -1) {
                    mainHandler.post(() -> callback.onFailure("Invalid file URL"));
                    return;
                }

                String fileName  = fileUrl.substring(idx + marker.length());
                String deleteUrl = SUPABASE_URL + "/storage/v1/object/"
                        + BUCKET_NAME + "/" + fileName;

                URL url = new URL(deleteUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
                conn.setRequestProperty("apikey", SUPABASE_KEY);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200 || responseCode == 204) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    final int code = responseCode;
                    mainHandler.post(() -> callback.onFailure("Delete failed. Code: " + code));
                }

            } catch (Exception e) {
                final String errMsg = e.getMessage();
                mainHandler.post(() -> callback.onFailure("Error: " + errMsg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}
