package com.example.petzoneapplication.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.petzoneapplication.R;
import com.example.petzoneapplication.adapters.PetDetailPagerAdapter;
import com.example.petzoneapplication.models.Pet;
import com.example.petzoneapplication.network.ApiService;
import com.example.petzoneapplication.network.RetrofitClient;
import com.example.petzoneapplication.utils.SharedPrefManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PetDetailActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES
    };

    private Toolbar toolbar;
    private ImageView petImageView;
    private TextView petNameTextView, petBreedTextView, petAgeTextView;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Pet pet;
    
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    uploadPetPhoto(selectedImage);
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null && extras.containsKey("data")) {
                        // Save the bitmap to a temporary file and upload
                        android.graphics.Bitmap imageBitmap = (android.graphics.Bitmap) extras.get("data");
                        Uri photoUri = saveBitmapToTempFile(imageBitmap);
                        if (photoUri != null) {
                            uploadPetPhoto(photoUri);
                        }
                    }
                }
            });
    private ApiService apiService;
    private String token;
    private String petId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_detail);

        if (getIntent() == null || !getIntent().hasExtra("pet_id")) {
            Toast.makeText(this, "Pet ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        petId = getIntent().getStringExtra("pet_id");

        apiService = RetrofitClient.getApiService();
        token = SharedPrefManager.getInstance(this).getToken();

        initViews();
        setupToolbar();
        loadPetDetails();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        petImageView = findViewById(R.id.petImageView);
        petNameTextView = findViewById(R.id.petNameTextView);
        petBreedTextView = findViewById(R.id.petBreedTextView);
        petAgeTextView = findViewById(R.id.petAgeTextView);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadPetDetails() {
        String authHeader = "Bearer " + token;
        apiService.getPetById(authHeader, petId).enqueue(new Callback<Pet>() {
            @Override
            public void onResponse(@NonNull Call<Pet> call, @NonNull Response<Pet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pet = response.body();
                    // Set the toolbar title only after the pet data is successfully loaded
                    getSupportActionBar().setTitle(pet.getName());
                    displayPetInfo();
                    setupTabs();
                    setupPhotoUpload();
                } else {
                    Toast.makeText(PetDetailActivity.this, "Failed to load pet details.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Pet> call, @NonNull Throwable t) {
                Toast.makeText(PetDetailActivity.this, "Network error.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayPetInfo() {
        petNameTextView.setText(pet.getName());
        petBreedTextView.setText(pet.getSpecies() + " • " + pet.getBreed());
        petAgeTextView.setText(calculateAge(pet.getDateOfBirth()));

        if (pet.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(pet.getPhotoUrl())
                    .placeholder(R.drawable.placeholder_pet)
                    .into(petImageView);
        }
    }

    private void setupTabs() {
        PetDetailPagerAdapter adapter = new PetDetailPagerAdapter(this, petId);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Feeding");
                            tab.setIcon(R.drawable.ic_food);
                            break;
                        case 1:
                            tab.setText("Grooming");
                            tab.setIcon(R.drawable.ic_grooming);
                            break;
                        case 2:
                            tab.setText("Health");
                            tab.setIcon(R.drawable.ic_health);
                            break;
                        case 3:
                            tab.setText("Memories");
                            tab.setIcon(R.drawable.ic_camera);
                            break;
                    }
                }).attach();
    }

    private String calculateAge(String dob) {
        // Calculate age from date of birth
        return "2 years old";
    }

    private void setupPhotoUpload() {
        petImageView.setOnClickListener(v -> checkPermissionAndPickImage());
    }

    private void checkPermissionAndPickImage() {
        boolean allPermissionsGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU 
                    && permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                continue; // Skip READ_EXTERNAL_STORAGE on Android 13+
            }
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        } else {
            showImageSourceDialog();
        }
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openImagePicker();
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                showImageSourceDialog();
            } else {
                Toast.makeText(this, "Permissions are required to upload photos", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void uploadPetPhoto(Uri imageUri) {
        try {
            // Show progress dialog
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Uploading photo...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            File photoFile = createTempFileFromInputStream(inputStream);

            // Compress the image
            Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageData);
            MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo",
                    "photo.jpg", requestFile);

            apiService.uploadPetPhoto("Bearer " + token, pet.getId(), photoPart)
                    .enqueue(new Callback<Pet>() {
                        @Override
                        public void onResponse(@NonNull Call<Pet> call, @NonNull Response<Pet> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful() && response.body() != null) {
                                pet = response.body();
                                displayPetInfo();
                                Toast.makeText(PetDetailActivity.this,
                                        "Photo uploaded successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                String errorBodyString = "Unknown error";
                                if (response.errorBody() != null) {
                                    try {
                                        errorBodyString = response.errorBody().string();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        errorBodyString = "Error reading response";
                                    }
                                }
                                Toast.makeText(PetDetailActivity.this,
                                        "Failed to upload photo: " + errorBodyString, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Pet> call, @NonNull Throwable t) {
                            progressDialog.dismiss();
                            Toast.makeText(PetDetailActivity.this,
                                    "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error preparing image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private File createTempFileFromInputStream(InputStream inputStream) throws Exception {
        File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
        FileOutputStream fos = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fos.close();
        inputStream.close();
        return tempFile;
    }

    private Uri saveBitmapToTempFile(android.graphics.Bitmap bitmap) {
        try {
            File tempFile = File.createTempFile("camera", ".jpg", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return android.net.Uri.fromFile(tempFile);
        } catch (Exception e) {
            Toast.makeText(this, "Error saving photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
