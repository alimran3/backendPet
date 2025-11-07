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

public class GroomingLogAdapter extends RecyclerView.Adapter<GroomingLogAdapter.ViewHolder> {

    private final List<Map<String, Object>> groomingLogs;

    public GroomingLogAdapter(List<Map<String, Object>> groomingLogs) {
        this.groomingLogs = groomingLogs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grooming_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> log = groomingLogs.get(position);
        Object groomingType = getFromDetails(log, "groomingType");
        Object notes = getFromDetails(log, "notes");
        
        String type = groomingType != null ? groomingType.toString() : "Grooming";
        String note = notes != null ? notes.toString() : "No additional notes";
        
        // Set grooming type and icon
        holder.groomingTypeTextView.setText(type);
        holder.iconTextView.setText(getGroomingIcon(type));
        
        // Set notes
        holder.notesTextView.setText(note);
        
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

    private String getGroomingIcon(String type) {
        if (type.toLowerCase().contains("bath")) return "🛁";
        if (type.toLowerCase().contains("nail")) return "✂️";
        if (type.toLowerCase().contains("brush")) return "💈";
        if (type.toLowerCase().contains("trim") || type.toLowerCase().contains("cut")) return "✂️";
        return "🐾"; // Default paw icon
    }

    @Override
    public int getItemCount() {
        return groomingLogs.size();
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
        TextView iconTextView, groomingTypeTextView, notesTextView, dateTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconTextView = itemView.findViewById(R.id.iconTextView);
            groomingTypeTextView = itemView.findViewById(R.id.groomingTypeTextView);
            notesTextView = itemView.findViewById(R.id.notesTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }
    }
}