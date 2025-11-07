package com.example.petzoneapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petzoneapplication.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HealthLogAdapter extends RecyclerView.Adapter<HealthLogAdapter.ViewHolder> {

    private final List<Map<String, Object>> healthLogs;

    public HealthLogAdapter(List<Map<String, Object>> healthLogs) {
        this.healthLogs = healthLogs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_health_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> log = healthLogs.get(position);
        
        // Get type from log
        String type = log.get("type") != null ? log.get("type").toString() : "Health";
        
        // Title: try several schema fields
        Object title = getFromDetails(log, "title");
        if (title == null) title = getFromDetails(log, "medicationName");
        if (title == null) title = getFromDetails(log, "vaccineName");
        if (title == null) title = getFromDetails(log, "activityType");
        if (title == null) title = getFromDetails(log, "veterinarianName");
        if (title == null) title = getFromDetails(log, "reason");

        // Notes: fallback to other descriptive fields
        Object notes = getFromDetails(log, "notes");
        if (notes == null) notes = getFromDetails(log, "diagnosis");
        if (notes == null) notes = getFromDetails(log, "treatment");
        if (notes == null) notes = "No additional notes";

        // Set icon and colors based on type
        HealthTypeInfo typeInfo = getHealthTypeInfo(type);
        holder.iconTextView.setText(typeInfo.icon);
        holder.iconTextView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(typeInfo.backgroundColor)));
        holder.typeTextView.setText(type);
        holder.typeTextView.setTextColor(android.graphics.Color.parseColor(typeInfo.textColor));
        holder.typeTextView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(typeInfo.backgroundColor)));
        
        // Set title and notes
        holder.titleTextView.setText(String.valueOf(title));
        holder.notesTextView.setText(String.valueOf(notes));
        
        // Format and set date
        Object createdAt = log.get("createdAt");
        if (createdAt != null) {
            holder.dateTextView.setText(formatDate(createdAt.toString()));
        } else {
            holder.dateTextView.setText("Recent");
        }
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return "Recent";
        }
    }

    private HealthTypeInfo getHealthTypeInfo(String type) {
        switch (type) {
            case "Medication":
                return new HealthTypeInfo("💊", "#E3F2FD", "#1976D2");
            case "VetVisit":
                return new HealthTypeInfo("🏥", "#F3E5F5", "#7B1FA2");
            case "Vaccination":
                return new HealthTypeInfo("💉", "#E8F5E9", "#388E3C");
            case "Exercise":
                return new HealthTypeInfo("🏃", "#FFF3E0", "#F57C00");
            default:
                return new HealthTypeInfo("📋", "#E0E0E0", "#616161");
        }
    }

    private static class HealthTypeInfo {
        String icon;
        String backgroundColor;
        String textColor;

        HealthTypeInfo(String icon, String backgroundColor, String textColor) {
            this.icon = icon;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
        }
    }

    @Override
    public int getItemCount() {
        return healthLogs.size();
    }

    @SuppressWarnings("unchecked")
    private Object getFromDetails(Map<String, Object> log, String key) {
        if (log == null) return null;
        Object direct = log.get(key);
        if (direct != null) return direct;
        Object details = log.get("details");
        if (details instanceof Map) {
            return ((Map<String, Object>) details).get(key);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView iconTextView, titleTextView, typeTextView, notesTextView, dateTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconTextView = itemView.findViewById(R.id.iconTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            notesTextView = itemView.findViewById(R.id.notesTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }
    }
}