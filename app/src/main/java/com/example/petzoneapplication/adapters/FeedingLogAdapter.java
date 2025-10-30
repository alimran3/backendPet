package com.example.petzoneapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petzoneapplication.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class FeedingLogAdapter extends RecyclerView.Adapter<FeedingLogAdapter.ViewHolder> {

    private final List<Map<String, Object>> feedingLogs;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final OnAddSlotClickListener listener;

    public FeedingLogAdapter(List<Map<String, Object>> feedingLogs) { this(feedingLogs, null); }

    public FeedingLogAdapter(List<Map<String, Object>> feedingLogs, OnAddSlotClickListener listener) {
        this.feedingLogs = feedingLogs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feeding_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> log = feedingLogs.get(position);
        // Prefer nested details map, fallback to top-level
        Object foodType = getFromDetails(log, "foodType");
        // Backend uses 'amount' for Feeding quantity; support legacy 'quantity' fallback
        Object quantity = getFromDetails(log, "amount");
        if (quantity == null) quantity = getFromDetails(log, "quantity");
        Object timeSlot = getFromDetails(log, "timeSlot");

        holder.foodTypeTextView.setText(String.valueOf(foodType != null ? foodType : "-"));
        holder.quantityTextView.setText(String.valueOf(quantity != null ? quantity : "-"));
        holder.timeSlotTextView.setText(timeSlot != null ? String.valueOf(timeSlot) : "");

        Object ts = log.get("timestamp");
        if (ts == null) ts = log.get("createdAt");
        if (ts == null) ts = log.get("date");
        holder.timestampTextView.setText(formatDate(ts));

        // Date header grouping by day
        String currentDay = formatDay(ts);
        String previousDay = null;
        long currentDayStart = getDayStartMillis(ts);
        if (position > 0) {
            Map<String, Object> prev = feedingLogs.get(position - 1);
            Object pts = prev.get("timestamp");
            if (pts == null) pts = prev.get("createdAt");
            if (pts == null) pts = prev.get("date");
            previousDay = formatDay(pts);
        }
        if (position == 0 || (currentDay != null && !currentDay.equals(previousDay))) {
            holder.dateHeaderTextView.setVisibility(View.VISIBLE);
            holder.dateHeaderTextView.setText(currentDay != null ? currentDay : "");
            // Show per-slot buttons for missing slots
            if (listener != null && currentDayStart > 0) {
                Set<String> present = getPresentSlotsForDay(currentDayStart);
                holder.addSlotContainer.setVisibility(View.VISIBLE);
                setupSlotButton(holder.addMorningButton, present.contains("Morning"), () -> listener.onAddSlot(currentDayStart, "Morning"));
                setupSlotButton(holder.addNoonButton, present.contains("Noon"), () -> listener.onAddSlot(currentDayStart, "Noon"));
                setupSlotButton(holder.addNightButton, present.contains("Night"), () -> listener.onAddSlot(currentDayStart, "Night"));
                if (present.contains("Morning") && present.contains("Noon") && present.contains("Night")) {
                    holder.addSlotContainer.setVisibility(View.GONE);
                }
            } else {
                holder.addSlotContainer.setVisibility(View.GONE);
                clearSlotButtons(holder);
            }
        } else {
            holder.dateHeaderTextView.setVisibility(View.GONE);
            holder.addSlotContainer.setVisibility(View.GONE);
            clearSlotButtons(holder);
        }
    }

    private void setupSlotButton(Button btn, boolean alreadyPresent, Runnable onClick) {
        if (alreadyPresent) {
            btn.setVisibility(View.GONE);
            btn.setOnClickListener(null);
        } else {
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(v -> onClick.run());
        }
    }

    private void clearSlotButtons(ViewHolder holder) {
        holder.addMorningButton.setVisibility(View.GONE);
        holder.addNoonButton.setVisibility(View.GONE);
        holder.addNightButton.setVisibility(View.GONE);
        holder.addMorningButton.setOnClickListener(null);
        holder.addNoonButton.setOnClickListener(null);
        holder.addNightButton.setOnClickListener(null);
    }

    private String formatDate(Object timestamp) {
        long ms = parseToMillis(timestamp);
        if (ms > 0) return dateFormat.format(new Date(ms));
        return "Unknown date";
    }

    private String formatDay(Object timestamp) {
        long ms = parseToMillis(timestamp);
        if (ms <= 0) return null;
        return dayFormat.format(new Date(ms));
    }

    private long getDayStartMillis(Object timestamp) {
        long t = parseToMillis(timestamp);
        if (t <= 0) return -1L;
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private boolean allSlotsPresentForDay(long dayStart) {
        long start = dayStart;
        long end = start + 24L * 60L * 60L * 1000L - 1L;
        Set<String> present = new HashSet<>();
        for (Map<String, Object> log : feedingLogs) {
            Object ts = log.get("date");
            if (ts == null) ts = log.get("timestamp");
            if (ts == null) ts = log.get("createdAt");
            long t = parseToMillis(ts);
            if (t >= start && t <= end) {
                Object slot = getFromDetails(log, "timeSlot");
                if (slot != null) present.add(String.valueOf(slot));
            }
        }
        return present.contains("Morning") && present.contains("Noon") && present.contains("Night");
    }

    private Set<String> getPresentSlotsForDay(long dayStart) {
        long start = dayStart;
        long end = start + 24L * 60L * 60L * 1000L - 1L;
        Set<String> present = new HashSet<>();
        for (Map<String, Object> log : feedingLogs) {
            Object ts = log.get("date");
            if (ts == null) ts = log.get("timestamp");
            if (ts == null) ts = log.get("createdAt");
            long t = parseToMillis(ts);
            if (t >= start && t <= end) {
                Object slot = getFromDetails(log, "timeSlot");
                if (slot != null) present.add(String.valueOf(slot));
            }
        }
        return present;
    }

    private long parseToMillis(Object timestamp) {
        if (timestamp == null) return -1L;
        if (timestamp instanceof Long) return (Long) timestamp;
        if (timestamp instanceof Number) return ((Number) timestamp).longValue();
        if (timestamp instanceof String) {
            String s = (String) timestamp;
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
            // Try common ISO formats
            String[] patterns = new String[]{
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    "yyyy-MM-dd'T'HH:mm:ssXXX"
            };
            for (String p : patterns) {
                try {
                    java.text.SimpleDateFormat iso = new java.text.SimpleDateFormat(p, java.util.Locale.US);
                    iso.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    Date d = iso.parse(s);
                    if (d != null) return d.getTime();
                } catch (Exception ignored2) {}
            }
        }
        return -1L;
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

    @Override
    public int getItemCount() {
        return feedingLogs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateHeaderTextView, foodTypeTextView, quantityTextView, timeSlotTextView, timestampTextView;
        LinearLayout addSlotContainer;
        Button addMorningButton, addNoonButton, addNightButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateHeaderTextView = itemView.findViewById(R.id.dateHeaderTextView);
            foodTypeTextView = itemView.findViewById(R.id.foodTypeTextView);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            timeSlotTextView = itemView.findViewById(R.id.timeSlotTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            addSlotContainer = itemView.findViewById(R.id.addSlotContainer);
            addMorningButton = itemView.findViewById(R.id.addMorningButton);
            addNoonButton = itemView.findViewById(R.id.addNoonButton);
            addNightButton = itemView.findViewById(R.id.addNightButton);
        }
    }

    public interface OnAddSlotClickListener {
        void onAddSlot(long dayStartMillis, String slot);
    }
}