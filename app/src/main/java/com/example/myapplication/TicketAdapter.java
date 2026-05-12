package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private final Context context;
    private List<TicketModel> ticketList;
    private OnTicketClickListener listener;

    public interface OnTicketClickListener {
        void onTicketClick(TicketModel ticket);
    }

    public TicketAdapter(Context context, List<TicketModel> ticketList, OnTicketClickListener listener) {
        this.context = context;
        this.ticketList = ticketList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        TicketModel ticket = ticketList.get(position);

        holder.tvTicketId.setText("TKT-ID -> " + ticket.getTicketId());
        holder.tvSubject.setText("Subject -> " + ticket.getSubject());
        holder.tvCategory.setText("Category -> " + ticket.getCategory());
        String status = ticket.getStatus() != null ? ticket.getStatus() : "";
        holder.tvStatus.setText(getDisplayStatus(status));

        // --- Assigned Faculty Name Logic ---
        if (ticket.getAssignedName() != null && !ticket.getAssignedName().isEmpty()) {
            holder.tvAssignedName.setText(ticket.getAssignedName());
            holder.tvAssignedName.setTextColor(Color.parseColor("#3B6FE8"));
        } else {
            holder.tvAssignedName.setText("Not Opened");
            holder.tvAssignedName.setTextColor(Color.parseColor("#888888"));
        }

        if (ticket.getCreatedAt() != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.getDefault());
            String dateTime = sdf.format(new Date(ticket.getCreatedAt()));
            holder.tvDateTime.setText(dateTime);
        }

        switch (status.toUpperCase()) {
            case "OPEN":
                holder.tvStatus.setTextColor(Color.parseColor("#3B6FE8"));
                holder.tvStatus.getBackground().setTint(Color.parseColor("#EBF0FD"));
                break;
            case "ACTIVE":
            case "IN PROGRESS":
                holder.tvStatus.setTextColor(Color.parseColor("#C8982A"));
                holder.tvStatus.getBackground().setTint(Color.parseColor("#FEF3C7"));
                break;
            case "CLOSED":
                holder.tvStatus.setTextColor(Color.parseColor("#6B7280"));
                holder.tvStatus.getBackground().setTint(Color.parseColor("#F3F4F6"));
                break;
            case "RESOLVED":
                holder.tvStatus.setTextColor(Color.parseColor("#059669"));
                holder.tvStatus.getBackground().setTint(Color.parseColor("#D1FAE5"));
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTicketClick(ticket);
        });
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    public void filterList(List<TicketModel> filteredList) {
        this.ticketList = filteredList;
        notifyDataSetChanged();
    }

    private String getDisplayStatus(String status) {
        if ("Active".equalsIgnoreCase(status)) {
            return "In Progress";
        }
        return status;
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketId, tvSubject, tvCategory, tvStatus, tvDateTime, tvAssignedName;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketId = itemView.findViewById(R.id.tvTicketId);
            tvSubject  = itemView.findViewById(R.id.tvSubject);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStatus   = itemView.findViewById(R.id.tvStatus);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvAssignedName = itemView.findViewById(R.id.tvAssignedName);
        }
    }
}
