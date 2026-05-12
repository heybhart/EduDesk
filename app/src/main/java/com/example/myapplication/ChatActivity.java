package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends Fragment {

    // ── Views ─────────────────────────────────────────────────
    EditText etSearch;
    LinearLayout chatListContainer;

    // ── Firebase ──────────────────────────────────────────────
    FirebaseFirestore db;
    String currentUid;
    ListenerRegistration chatListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch          = view.findViewById(R.id.etSearch);
        chatListContainer = view.findViewById(R.id.chatListContainer);

        db         = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        view.findViewById(R.id.searchContainer).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SearchChat.class)));

        loadChatList();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (chatListContainer != null) loadChatList();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListening();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListening();
    }

    private void stopListening() {
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
    }

    private void loadChatList() {
        stopListening();

        chatListener = db.collection("chats")
                .whereArrayContains("participants", currentUid)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        showEmptyState();
                        return;
                    }

                    chatListContainer.removeAllViews();
                    boolean hasNormalChats = false;

                    if (snapshots.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshots) {
                        // Ticket-based chats skip karo (unka ID 'ticket_' se start hota hai)
                        if (doc.getId().startsWith("ticket_")) {
                            continue;
                        }

                        hasNormalChats = true;
                        String user1Uid  = doc.getString("user1Uid");
                        String user2Uid  = doc.getString("user2Uid");
                        String user1Name = doc.getString("user1Name");
                        String user2Name = doc.getString("user2Name");
                        String user1Dept = doc.getString("user1Dept");
                        String user2Dept = doc.getString("user2Dept");

                        String otherUid, otherName, otherDept;
                        if (currentUid.equals(user1Uid)) {
                            otherUid  = user2Uid  != null ? user2Uid  : "";
                            otherName = user2Name != null ? user2Name : "User";
                            otherDept = user2Dept != null ? user2Dept : "";
                        } else {
                            otherUid  = user1Uid  != null ? user1Uid  : "";
                            otherName = user1Name != null ? user1Name : "User";
                            otherDept = user1Dept != null ? user1Dept : "";
                        }

                        String lastMessage     = doc.getString("lastMessage");
                        Long   lastMessageTime = doc.getLong("lastMessageTime");
                        List<String> unreadFor = (List<String>) doc.get("unreadFor");

                        addChatCard(
                                otherUid,
                                otherName,
                                otherDept,
                                lastMessage     != null ? lastMessage     : "No messages yet",
                                lastMessageTime != null ? lastMessageTime : 0L,
                                unreadFor != null && unreadFor.contains(currentUid),
                                doc.getId()
                        );
                    }
                    
                    if (!hasNormalChats) {
                        showEmptyState();
                    }
                });
    }

    private void addChatCard(String otherUid, String otherName, String otherDept,
                             String lastMessage, long lastMessageTime,
                             boolean hasUnread, String chatId) {

        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.user_chat_card, chatListContainer, false);

        TextView tvName = card.findViewById(R.id.faculty_username);
        TextView tvDept = card.findViewById(R.id.faculty_dept_name);
        TextView tvMsg  = card.findViewById(R.id.faculty_last_message);
        TextView tvTime = card.findViewById(R.id.faculty_last_message_time);
        View unreadDot  = card.findViewById(R.id.viewUnreadDot);

        tvName.setText(otherName);
        tvDept.setText(otherDept);
        tvMsg.setText(lastMessage);
        tvTime.setText(formatTime(lastMessageTime));
        unreadDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);

        ImageView avatar = card.findViewById(R.id.userAvatar);
        loadProfilePhoto(avatar, otherUid);

        card.findViewById(R.id.chatMiller).setOnClickListener(v -> {
            unreadDot.setVisibility(View.GONE);
            db.collection("chats").document(chatId)
                    .update("unreadFor", FieldValue.arrayRemove(currentUid));

            Intent intent = new Intent(requireContext(), Chat_Screen.class);
            intent.putExtra("facultyUid",        otherUid);
            intent.putExtra("facultyName",       otherName);
            intent.putExtra("facultyDepartment", otherDept);
            intent.putExtra("chatId",            chatId);
            startActivity(intent);
        });

        chatListContainer.addView(card);

        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFEEEEEE);
        chatListContainer.addView(divider);
    }

    private void loadProfilePhoto(ImageView avatar, String uid) {
        if (avatar == null || uid == null || uid.isEmpty()) return;
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String photo = doc.exists() ? doc.getString("profilePhoto") : null;
            if (photo != null && !photo.isEmpty()) applyPhoto(avatar, photo);
            else {
                db.collection("faculty_user").document(uid).get().addOnSuccessListener(fDoc -> {
                    String fp = fDoc.exists() ? fDoc.getString("profilePhoto") : null;
                    if (fp != null && !fp.isEmpty()) applyPhoto(avatar, fp);
                });
            }
        });
    }

    private void applyPhoto(ImageView avatar, String url) {
        avatar.setPadding(0, 0, 0, 0);
        avatar.clearColorFilter();
        Glide.with(this).load(url).transform(new CircleCrop()).placeholder(R.drawable.ic_profile_user).into(avatar);
    }

    private void showEmptyState() {
        chatListContainer.removeAllViews();
        TextView tv = new TextView(requireContext());
        tv.setText("No chats yet!\nSearch someone to start chatting.");
        tv.setTextColor(0xFFAAAAAA);
        tv.setTextSize(14f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(20, 80, 20, 20);
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        chatListContainer.addView(tv);
    }

    private String formatTime(long timestamp) {
        if (timestamp == 0) return "";
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 24 * 60 * 60 * 1000) return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestamp));
        else if (diff < 48 * 60 * 60 * 1000) return "Yesterday";
        else return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(timestamp));
    }
}