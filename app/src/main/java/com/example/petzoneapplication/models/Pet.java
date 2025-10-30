package com.example.petzoneapplication.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Pet implements Parcelable {
    @SerializedName("_id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("species")
    private String species;

    @SerializedName("breed")
    private String breed;

    @SerializedName("dateOfBirth")
    private String dateOfBirth;

    @SerializedName("gender")
    private String gender;

    @SerializedName("photoUrl")
    private String photoUrl;

    @SerializedName("userId")
    private String userId;

    // Constructors
    public Pet() {}

    public Pet(String name, String species, String breed, String dateOfBirth, String gender) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
    }

    protected Pet(Parcel in) {
        id = in.readString();
        name = in.readString();
        species = in.readString();
        breed = in.readString();
        dateOfBirth = in.readString();
        gender = in.readString();
        photoUrl = in.readString();
        userId = in.readString();
    }

    public static final Creator<Pet> CREATOR = new Creator<Pet>() {
        @Override
        public Pet createFromParcel(Parcel in) {
            return new Pet(in);
        }

        @Override
        public Pet[] newArray(int size) {
            return new Pet[size];
        }
    };

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(species);
        dest.writeString(breed);
        dest.writeString(dateOfBirth);
        dest.writeString(gender);
        dest.writeString(photoUrl);
        dest.writeString(userId);
    }
}