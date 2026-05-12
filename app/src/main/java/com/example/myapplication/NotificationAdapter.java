package com.example.myapplication;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationDeleteListener {
        void onDelete(NotificationModel notification, int position);
    }

    private final List<NotificationModel> notificationList;
    private final OnNotificationDeleteListener deleteListener;

    public NotificationAdapter(List<NotificationModel> notificationList,
                               OnNotificationDeleteListener deleteListener) {
        this.notificationList = notificationList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);
        holder.title.setText(notification.getTitle());
        holder.body.setText(notification.getBody());
        bindNotificationIcon(holder.icon, notification.getProfileImageUrl());
        
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                notification.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        holder.time.setText(timeAgo);

        if (notification.isRead()) {
            holder.unreadDot.setVisibility(View.GONE);
        } else {
            holder.unreadDot.setVisibility(View.VISIBLE);
        }

        holder.btnDeleteNotification.setOnClickListener(v -> {
            if (deleteListener != null) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    deleteListener.onDelete(notification, adapterPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    private void bindNotificationIcon(ImageView iconView, String profileImageUrl) {
        if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
            iconView.setBackgroundResource(R.drawable.bg_white_rounded);
            iconView.setBackgroundTintList(null);
            iconView.setPadding(0, 0, 0, 0);
            iconView.clearColorFilter();
            Glide.with(iconView.getContext())
                    .load(profileImageUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_profile_user)
                    .error(R.drawable.ic_profile_user)
                    .into(iconView);
        } else {
            iconView.setImageResource(R.drawable.ic_bolt);
            iconView.setBackgroundResource(R.drawable.bg_white_rounded);
            iconView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEFF6FF));
            iconView.setPadding(12, 12, 12, 12);
            iconView.setColorFilter(0xFF3B6FE8);
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, body, time;
        View unreadDot;
        ImageView btnDeleteNotification, icon;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.notificationIcon);
            title = itemView.findViewById(R.id.notificationTitle);
            body = itemView.findViewById(R.id.notificationBody);
            time = itemView.findViewById(R.id.notificationTime);
            unreadDot = itemView.findViewById(R.id.unreadDot);
            btnDeleteNotification = itemView.findViewById(R.id.btnDeleteNotification);
        }
    }
}
