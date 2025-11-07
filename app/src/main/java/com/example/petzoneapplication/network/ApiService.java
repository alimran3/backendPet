package com.example.petzoneapplication.network;

import com.example.petzoneapplication.models.CommunityPost;
import com.example.petzoneapplication.models.Pet;
import com.example.petzoneapplication.models.User;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // Authentication
    @POST("auth/login")
    Call<User> login(@Body Map<String, String> credentials);

    @POST("auth/signup")
    Call<User> signUp(@Body Map<String, String> userData);

    // Community Feed
    @GET("community/feed")
    Call<List<CommunityPost>> getCommunityFeed();

    @POST("community/posts/{postId}/like")
    Call<Void> likePost(@Path("postId") String postId);

    // Pets
    @GET("pets")
    Call<List<Pet>> getMyPets(@Header("Authorization") String token);

    @GET("pets/{petId}")
    Call<Pet> getPetById(@Header("Authorization") String token, @Path("petId") String petId);

    @POST("pets")
    Call<Pet> addPet(@Header("Authorization") String token, @Body Pet pet);

    @Multipart
    @POST("pets/{petId}/photo")
    Call<Pet> uploadPetPhoto(
            @Header("Authorization") String token,
            @Path("petId") String petId,
            @Part MultipartBody.Part photo
    );

    // Care Logs (aligned with backend /care-logs)
    @GET("care-logs/pet/{petId}")
    Call<List<Map<String, Object>>> getCareLogs(
            @Header("Authorization") String token,
            @Path("petId") String petId,
            @Query("type") String type
    );

    @POST("care-logs")
    Call<Void> addCareLog(
            @Header("Authorization") String token,
            @Body Map<String, Object> careLog
    );

    // Memories
    @GET("memories/all")
    Call<List<com.example.petzoneapplication.models.Memory>> getAllMemories(
            @Header("Authorization") String token
    );

    @GET("memories/pet/{petId}")
    Call<List<com.example.petzoneapplication.models.Memory>> getPetMemories(
            @Header("Authorization") String token,
            @Path("petId") String petId
    );

    @Multipart
    @POST("memories")
    Call<com.example.petzoneapplication.models.Memory> addMemory(
            @Header("Authorization") String token,
            @Part("petId") RequestBody petId,
            @Part("caption") RequestBody caption,
            @Part("isShared") RequestBody isShared,
            @Part MultipartBody.Part image
    );

    // Profile
    @Multipart
    @POST("auth/profile/photo")
    Call<User> uploadProfilePhoto(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image
    );

    @PUT("auth/profile")
    Call<User> updateProfile(
            @Header("Authorization") String token,
            @Body Map<String, Object> updates
    );

    @POST("auth/change-password")
    Call<Map<String, String>> changePassword(
            @Header("Authorization") String token,
            @Body Map<String, String> passwords
    );
}