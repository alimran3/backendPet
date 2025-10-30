package com.example.petzoneapplication.models;

import com.google.gson.annotations.SerializedName;

public class Memory {
    @SerializedName("_id")
    private String id;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("caption")
    private String caption;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("isShared")
    private boolean isShared;

    // Getters and Setters
    public String getId() { 
        return id; 
    }

    public void setId(String id) { 
        this.id = id; 
    }

    public String getImageUrl() { 
        return imageUrl; 
    }

    public void setImageUrl(String imageUrl) { 
        this.imageUrl = imageUrl; 
    }

    public String getCaption() { 
        return caption; 
    }

    public void setCaption(String caption) { 
        this.caption = caption; 
    }

    public String getCreatedAt() { 
        return createdAt; 
    }

    public void setCreatedAt(String createdAt) { 
        this.createdAt = createdAt; 
    }

    public boolean isShared() { 
        return isShared; 
    }

    public void setShared(boolean shared) { 
        isShared = shared; 
    }
}