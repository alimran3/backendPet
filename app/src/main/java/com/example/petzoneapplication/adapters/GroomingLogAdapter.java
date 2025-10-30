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
        holder.groomingTypeTextView.setText(String.valueOf(groomingType != null ? groomingType : "-"));
        holder.notesTextView.setText(String.valueOf(notes != null ? notes : "-"));
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
        TextView groomingTypeTextView, notesTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            groomingTypeTextView = itemView.findViewById(R.id.groomingTypeTextView);
            notesTextView = itemView.findViewById(R.id.notesTextView);
        }
    }
}