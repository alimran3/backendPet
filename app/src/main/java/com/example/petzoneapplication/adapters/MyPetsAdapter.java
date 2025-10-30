package com.example.petzoneapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petzoneapplication.R;
import com.example.petzoneapplication.models.Pet;

import java.util.List;

public class MyPetsAdapter extends RecyclerView.Adapter<MyPetsAdapter.ViewHolder> {

    private Context context;
    private List<Pet> pets;
    private OnPetClickListener listener;

    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }

    public MyPetsAdapter(Context context, List<Pet> pets, OnPetClickListener listener) {
        this.context = context;
        this.pets = pets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pet pet = pets.get(position);

        holder.petName.setText(pet.getName());
        holder.petBreed.setText(pet.getBreed());

        if (pet.getPhotoUrl() != null) {
            Glide.with(context)
                    .load(pet.getPhotoUrl())
                    .placeholder(R.drawable.placeholder_pet)
                    .centerCrop()
                    .into(holder.petImage);
        } else {
            holder.petImage.setImageResource(R.drawable.placeholder_pet);
        }

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPetClick(pet);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView petImage;
        TextView petName, petBreed;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            petImage = itemView.findViewById(R.id.petImage);
            petName = itemView.findViewById(R.id.petName);
            petBreed = itemView.findViewById(R.id.petBreed);
        }
    }
}