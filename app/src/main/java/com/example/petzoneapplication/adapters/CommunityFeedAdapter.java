package com.example.petzoneapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.petzoneapplication.R;
import com.example.petzoneapplication.models.CommunityPost;

import java.util.List;

public class CommunityFeedAdapter extends RecyclerView.Adapter<CommunityFeedAdapter.ViewHolder> {

    private Context context;
    private List<CommunityPost> posts;

    public CommunityFeedAdapter(Context context, List<CommunityPost> posts) {
        this.context = context;
        this.posts = posts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityPost post = posts.get(position);

        holder.petInfo.setText(post.getPetName() + " • " + post.getBreed());
        holder.caption.setText(post.getCaption());
        holder.careTag.setText(getCareTagEmoji(post.getCareTag()) + " " + post.getCareTag());
        holder.likeCount.setText(String.valueOf(post.getLikes()));

        if (post.getImageUrl() != null) {
            Glide.with(context)
                    .load(post.getImageUrl())
                    .placeholder(R.drawable.placeholder_pet)
                    .into(holder.postImage);
        }

        holder.likeButton.setImageResource(
                post.isLiked() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline
        );

        holder.likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                post.setLiked(!post.isLiked());
                if (post.isLiked()) {
                    post.setLikes(post.getLikes() + 1);
                } else {
                    post.setLikes(post.getLikes() - 1);
                }
                notifyItemChanged(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    private String getCareTagEmoji(String careTag) {
        if (careTag == null) return "📝";
        switch (careTag.toLowerCase()) {
            case "feeding":
                return "🍽️";
            case "grooming":
                return "🛁";
            case "exercise":
                return "🏃";
            case "memory":
                return "📸";
            default:
                return "📝";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView petInfo, caption, careTag, likeCount;
        ImageView postImage;
        ImageButton likeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            petInfo = itemView.findViewById(R.id.petInfo);
            caption = itemView.findViewById(R.id.caption);
            careTag = itemView.findViewById(R.id.careTag);
            likeCount = itemView.findViewById(R.id.likeCount);
            postImage = itemView.findViewById(R.id.postImage);
            likeButton = itemView.findViewById(R.id.likeButton);
        }
    }
}