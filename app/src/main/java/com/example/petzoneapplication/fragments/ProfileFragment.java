package com.example.petzoneapplication.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.petzoneapplication.R;
import com.example.petzoneapplication.activities.LoginActivity;
import com.example.petzoneapplication.models.User;
import com.example.petzoneapplication.network.ApiService;
import com.example.petzoneapplication.network.RetrofitClient;
import com.example.petzoneapplication.utils.SharedPrefManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private ImageView profileImageView;
    private TextView nameTextView, emailTextView;
    private Switch notificationSwitch, darkModeSwitch, communityShareSwitch;
    private Button editProfileButton, changePasswordButton, logoutButton;
    private SharedPrefManager prefManager;
    private ApiService apiService;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    uploadProfilePhoto(selectedImage);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        loadUserData();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        profileImageView = view.findViewById(R.id.profileImageView);
        nameTextView = view.findViewById(R.id.nameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        notificationSwitch = view.findViewById(R.id.notificationSwitch);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        communityShareSwitch = view.findViewById(R.id.communityShareSwitch);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        logoutButton = view.findViewById(R.id.logoutButton);

        prefManager = SharedPrefManager.getInstance(getContext());
        apiService = RetrofitClient.getApiService();
    }

    private void loadUserData() {
        User user = prefManager.getUser();
        if (user != null) {
            nameTextView.setText(user.getName());
            emailTextView.setText(user.getEmail());

            // Load profile picture
            if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfilePicture())
                        .placeholder(R.drawable.ic_profile)
                        .circleCrop()
                        .into(profileImageView);
            }
        }
    }

    private void setupListeners() {
        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        editProfileButton.setOnClickListener(v -> {
            showEditProfileDialog();
        });

        changePasswordButton.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        logoutButton.setOnClickListener(v -> {
            showLogoutConfirmation();
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save notification preference
        });

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleDarkMode(isChecked);
        });

        communityShareSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save community sharing preference
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    prefManager.logout();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void uploadProfilePhoto(Uri imageUri) {
        try {
            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Uploading photo...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            File tempFile = File.createTempFile("profile", ".jpg", requireContext().getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            inputStream.close();

            Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(tempFile.getAbsolutePath());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageData);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "profile.jpg", requestFile);

            apiService.uploadProfilePhoto("Bearer " + prefManager.getToken(), imagePart)
                    .enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful() && response.body() != null) {
                                User updatedUser = response.body();
                                updatedUser.setToken(prefManager.getToken());
                                prefManager.saveUser(updatedUser);
                                loadUserData();
                                Toast.makeText(getContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Failed to upload photo", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error preparing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        
        android.widget.EditText nameInput = dialogView.findViewById(R.id.nameEditText);
        User user = prefManager.getUser();
        if (user != null) {
            nameInput.setText(user.getName());
        }

        builder.setView(dialogView)
                .setTitle("Edit Profile")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateProfile(newName);
                    } else {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateProfile(String name) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("name", name);

        apiService.updateProfile("Bearer " + prefManager.getToken(), updates)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            User updatedUser = response.body();
                            updatedUser.setToken(prefManager.getToken());
                            prefManager.saveUser(updatedUser);
                            loadUserData();
                            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        
        android.widget.EditText currentPasswordInput = dialogView.findViewById(R.id.currentPasswordEditText);
        android.widget.EditText newPasswordInput = dialogView.findViewById(R.id.newPasswordEditText);
        android.widget.EditText confirmPasswordInput = dialogView.findViewById(R.id.confirmPasswordEditText);

        builder.setView(dialogView)
                .setTitle("Change Password")
                .setPositiveButton("Change", (dialog, which) -> {
                    String currentPassword = currentPasswordInput.getText().toString();
                    String newPassword = newPasswordInput.getText().toString();
                    String confirmPassword = confirmPasswordInput.getText().toString();

                    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newPassword.length() < 6) {
                        Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    changePassword(currentPassword, newPassword);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        java.util.Map<String, String> passwords = new java.util.HashMap<>();
        passwords.put("currentPassword", currentPassword);
        passwords.put("newPassword", newPassword);

        apiService.changePassword("Bearer " + prefManager.getToken(), passwords)
                .enqueue(new Callback<java.util.Map<String, String>>() {
                    @Override
                    public void onResponse(@NonNull Call<java.util.Map<String, String>> call, @NonNull Response<java.util.Map<String, String>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<java.util.Map<String, String>> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void toggleDarkMode(boolean isEnabled) {
        // Save preference
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("AppPreferences", android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", isEnabled).apply();

        // Apply theme
        if (isEnabled) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        Toast.makeText(getContext(), "Dark mode " + (isEnabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
    }
}