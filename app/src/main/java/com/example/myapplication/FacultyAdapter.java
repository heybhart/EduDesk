package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class FacultyAdapter extends RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder> {

    private final Context context;
    private List<Map<String, Object>> facultyList;
    private final OnFacultyClickListener listener;

    // ── Click listener interface ──────────────────────────────
    public interface OnFacultyClickListener {
        void onFacultyClick(String uid, String fullName, String department);
    }

    public FacultyAdapter(Context context,
                          List<Map<String, Object>> facultyList,
                          OnFacultyClickListener listener) {
        this.context     = context;
        this.facultyList = facultyList;
        this.listener    = listener;
    }

    // ── List update karo — filter ke baad ────────────────────
    public void updateList(List<Map<String, Object>> newList) {
        this.facultyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FacultyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_faculty_card, parent, false);
        return new FacultyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacultyViewHolder holder, int position) {
        Map<String, Object> user = facultyList.get(position);

        String firstName  = getStr(user, "firstName");
        String surname    = getStr(user, "surname");
        String fullName   = (firstName + " " + surname).trim();
        String college    = getStr(user, "college");
        String department = getStr(user, "department");
        String uid        = getStr(user, "uid");
        String userType   = getStr(user, "userType"); // "Faculty" or "Student"

        // Fill text fields
        holder.tvName.setText(fullName);
        holder.tvDepartment.setText(department);
        holder.tvCollege.setText("🎓  " + college);

        // Reset avatar to default before loading
        holder.ivAvatar.setPadding(0, 0, 0, 0);
        holder.ivAvatar.clearColorFilter();
        holder.ivAvatar.setImageResource(R.drawable.ic_profile_user);

        // Load profile photo
        loadProfilePhoto(holder.ivAvatar, uid, userType);

        // Click handler
        holder.itemView.setOnClickListener(v ->
                listener.onFacultyClick(uid, fullName, department));
    }

    // ── Load profile photo from Firestore ────────────────────
    private void loadProfilePhoto(ImageView avatar, String uid, String userType) {
        if (uid == null || uid.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Pick collection based on user type
        String collection = "Faculty".equals(userType) ? "faculty_user" : "users";

        db.collection(collection).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String photo = doc.exists() ? doc.getString("profilePhoto") : null;
                    if (photo != null && !photo.isEmpty()) {
                        applyPhoto(avatar, photo);
                    }
                    // No photo → default icon stays
                });
    }

    // Apply photo using Glide with circle crop
    private void applyPhoto(ImageView avatar, String url) {
        Glide.with(context)
                .load(url)
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_profile_user)
                .error(R.drawable.ic_profile_user)
                .into(avatar);
    }

    @Override
    public int getItemCount() {
        return facultyList != null ? facultyList.size() : 0;
    }

    // ── ViewHolder ────────────────────────────────────────────
    public static class FacultyViewHolder extends RecyclerView.ViewHolder {
        TextView  tvName, tvDepartment, tvCollege;
        ImageView ivAvatar;

        public FacultyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName       = itemView.findViewById(R.id.tvName);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvCollege    = itemView.findViewById(R.id.tvCollege);
            ivAvatar     = itemView.findViewById(R.id.ivAvatar);
        }
    }

    // ── Null safe string ──────────────────────────────────────
    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}