package com.example.petzoneapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petzoneapplication.R;
import com.example.petzoneapplication.models.Memory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoriesAdapter extends RecyclerView.Adapter<MemoriesAdapter.ViewHolder> {

    private Context context;
    private List<Memory> memories;

    public MemoriesAdapter(Context context, List<Memory> memories) {
        this.context = context;
        this.memories = memories;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_memory_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Memory memory = memories.get(position);

        Glide.with(context)
                .load(memory.getImageUrl())
                .centerCrop()
                .placeholder(R.drawable.placeholder_pet)
                .into(holder.imageView);

        // Set caption
        if (memory.getCaption() != null && !memory.getCaption().isEmpty()) {
            holder.captionText.setText(memory.getCaption());
            holder.captionText.setVisibility(View.VISIBLE);
        } else {
            holder.captionText.setText("Sweet memory ❤");
            holder.captionText.setVisibility(View.VISIBLE);
        }

        // Set date
        if (memory.getCreatedAt() != null) {
            holder.dateText.setText(formatDate(memory.getCreatedAt()));
            holder.dateText.setVisibility(View.VISIBLE);
        } else {
            holder.dateText.setVisibility(View.GONE);
        }
    }

    private String formatDate(String createdAt) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(createdAt);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return "Recent";
        }
    }

    @Override
    public int getItemCount() {
        return memories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView captionText;
        TextView dateText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.memoryImage);
            captionText = itemView.findViewById(R.id.captionText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
}