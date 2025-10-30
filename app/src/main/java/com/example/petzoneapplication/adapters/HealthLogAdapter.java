package com.example.petzoneapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petzoneapplication.R;

import java.util.List;
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
        // Title: try several schema fields if custom 'title' wasn't persisted
        Object title = getFromDetails(log, "title");
        if (title == null) title = getFromDetails(log, "medicationName");
        if (title == null) title = getFromDetails(log, "vaccineName");
        if (title == null) title = getFromDetails(log, "activityType");
        if (title == null) title = getFromDetails(log, "veterinarianName");

        Object category = getFromDetails(log, "category");

        // Notes: fallback to other descriptive fields
        Object notes = getFromDetails(log, "notes");
        if (notes == null) notes = getFromDetails(log, "reason");
        if (notes == null) notes = getFromDetails(log, "diagnosis");
        if (notes == null) notes = getFromDetails(log, "treatment");

        holder.titleTextView.setText(String.valueOf(title != null ? title : "-"));
        // Show category (e.g., Vaccination, Deworming) instead of the constant top-level type "Health"
        holder.typeTextView.setText(String.valueOf(category != null ? category : "-"));
        holder.notesTextView.setText(String.valueOf(notes != null ? notes : "-"));
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
        TextView titleTextView, typeTextView, notesTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            notesTextView = itemView.findViewById(R.id.notesTextView);
        }
    }
}