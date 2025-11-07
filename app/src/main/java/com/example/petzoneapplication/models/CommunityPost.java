package com.example.petzoneapplication.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CommunityPost {
    @SerializedName("_id")
    private String id;

    @SerializedName("petName")
    private String petName;

    @SerializedName("breed")
    private String breed;

    @SerializedName("caption")
    private String caption;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("careTag")
    private String careTag;

    @SerializedName("likesCount")
    private int likesCount;

    @SerializedName("likes")
    private List<String> likes;

    @SerializedName("isLiked")
    private boolean isLiked;

    @SerializedName("createdAt")
    private String createdAt;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCareTag() {
        return careTag;
    }

    public void setCareTag(String careTag) {
        this.careTag = careTag;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public List<String> getLikes() {
        return likes;
    }

    public void setLikes(List<String> likes) {
        this.likes = likes;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}