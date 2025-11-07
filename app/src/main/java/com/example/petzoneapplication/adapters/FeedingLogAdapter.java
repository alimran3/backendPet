package com.example.petzoneapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petzoneapplication.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeedingLogAdapter extends RecyclerView.Adapter<FeedingLogAdapter.ViewHolder> {

    private List<DayFeeding> dayFeedings;
    private final List<Map<String, Object>> rawLogs;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public FeedingLogAdapter(List<Map<String, Object>> feedingLogs) {
        this.rawLogs = feedingLogs;
        this.dayFeedings = groupByDate(feedingLogs);
    }
    
    public void refreshData() {
        this.dayFeedings = groupByDate(rawLogs);
        android.util.Log.d("FeedingLogAdapter", "Grouped into " + dayFeedings.size() + " days from " + rawLogs.size() + " logs");
        notifyDataSetChanged();
    }
    
    private List<DayFeeding> groupByDate(List<Map<String, Object>> logs) {
        Map<String, DayFeeding> grouped = new LinkedHashMap<>();
        
        for (Map<String, Object> log : logs) {
            Object ts = log.get("timestamp");
            if (ts == null) ts = log.get("createdAt");
            if (ts == null) ts = log.get("date");
            
            String date = formatDate(ts);
            Object timeSlot = getFromDetails(log, "timeSlot");
            Object foodType = getFromDetails(log, "foodType");
            Object amount = getFromDetails(log, "amount");
            if (amount == null) amount = getFromDetails(log, "quantity");
            
            String slot = timeSlot != null ? timeSlot.toString() : "Morning";
            String food = foodType != null ? foodType.toString() : "Pet Food";
            String qty = amount != null ? amount.toString() : "-";
            
            android.util.Log.d("FeedingLogAdapter", "Processing: date=" + date + ", slot=" + slot + ", food=" + food + ", qty=" + qty);
            
            if (!grouped.containsKey(date)) {
                grouped.put(date, new DayFeeding(date, food));
            }
            
            DayFeeding dayFeeding = grouped.get(date);
            if (slot.equalsIgnoreCase("Morning")) {
                dayFeeding.morningQty = qty;
            } else if (slot.equalsIgnoreCase("Noon")) {
                dayFeeding.noonQty = qty;
            } else if (slot.equalsIgnoreCase("Night")) {
                dayFeeding.nightQty = qty;
            }
        }
        
        return new ArrayList<>(grouped.values());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feeding_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayFeeding dayFeeding = dayFeedings.get(position);
        
        android.util.Log.d("FeedingLogAdapter", "Binding: date=" + dayFeeding.date + ", food=" + dayFeeding.foodType);
        
        holder.dateTextView.setText(dayFeeding.date);
        holder.foodTypeTextView.setText(dayFeeding.foodType);
        
        // Set food type and quantity for each slot
        holder.morningFood.setText(dayFeeding.foodType);
        holder.morningQuantity.setText(dayFeeding.morningQty);
        
        holder.noonFood.setText(dayFeeding.foodType);
        holder.noonQuantity.setText(dayFeeding.noonQty);
        
        holder.nightFood.setText(dayFeeding.foodType);
        holder.nightQuantity.setText(dayFeeding.nightQty);
        
        // Calculate total
        double total = parseQuantity(dayFeeding.morningQty) + 
                       parseQuantity(dayFeeding.noonQty) + 
                       parseQuantity(dayFeeding.nightQty);
        holder.totalQuantity.setText(total > 0 ? String.format(Locale.getDefault(), "%.1f cups", total) : "-");
    }
    
    private double parseQuantity(Object qty) {
        if (qty == null) return 0;
        String str = qty.toString().replaceAll("[^0-9.]", "");
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatDate(Object timestamp) {
        try {
            if (timestamp == null) return "Recent";
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(timestamp.toString());
            return dateFormat.format(date);
        } catch (Exception e) {
            return "Recent";
        }
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
        return dayFeedings.size();
    }
    
    private static class DayFeeding {
        String date;
        String foodType;
        String morningQty = "-";
        String noonQty = "-";
        String nightQty = "-";
        
        DayFeeding(String date, String foodType) {
            this.date = date;
            this.foodType = foodType;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView foodTypeTextView, dateTextView;
        TextView morningFood, morningQuantity;
        TextView noonFood, noonQuantity;
        TextView nightFood, nightQuantity;
        TextView totalQuantity;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            foodTypeTextView = itemView.findViewById(R.id.foodTypeTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            
            morningFood = itemView.findViewById(R.id.morningFood);
            morningQuantity = itemView.findViewById(R.id.morningQuantity);
            
            noonFood = itemView.findViewById(R.id.noonFood);
            noonQuantity = itemView.findViewById(R.id.noonQuantity);
            
            nightFood = itemView.findViewById(R.id.nightFood);
            nightQuantity = itemView.findViewById(R.id.nightQuantity);
            
            totalQuantity = itemView.findViewById(R.id.totalQuantity);
        }
    }
}