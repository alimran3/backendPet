package com.example.petzoneapplication.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.models.Pet;
import com.example.petzoneapplication.network.ApiService;
import com.example.petzoneapplication.network.RetrofitClient;
import com.example.petzoneapplication.utils.SharedPrefManager;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPetActivity extends AppCompatActivity {
    private static final String TAG = "AddPetActivity";

    private EditText petNameEditText, breedEditText, dobEditText;
    private Spinner speciesSpinner;
    private RadioGroup genderRadioGroup;
    private ImageView petImageView;
    private Button selectImageButton, savePetButton;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private final Calendar selectedDate = Calendar.getInstance();
    private Uri selectedImageUri = null;
    private ApiService apiService;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        petImageView.setImageBitmap(bitmap);
                        petImageView.setVisibility(View.VISIBLE);
                        selectedImageUri = imageUri;
                    } catch (IOException e) {
                        Log.e(TAG, "Error getting bitmap from gallery", e);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        petImageView.setImageBitmap(imageBitmap);
                        petImageView.setVisibility(View.VISIBLE);
                        // Persist to temp file and store Uri so we can upload later
                        selectedImageUri = saveBitmapToTempFile(imageBitmap);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);

        initViews();
        setupToolbar();
        setupSpinner();
        setupListeners();

        apiService = RetrofitClient.getApiService();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        petNameEditText = findViewById(R.id.petNameEditText);
        speciesSpinner = findViewById(R.id.speciesSpinner);
        breedEditText = findViewById(R.id.breedEditText);
        dobEditText = findViewById(R.id.dobEditText);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);
        petImageView = findViewById(R.id.petImageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        savePetButton = findViewById(R.id.savePetButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add New Pet");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        String[] species = {"Dog", "Cat", "Bird", "Rabbit", "Hamster", "Fish", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, species);
        speciesSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        dobEditText.setOnClickListener(v -> showDatePicker());
        selectImageButton.setOnClickListener(v -> showImagePickerOptions());
        savePetButton.setOnClickListener(v -> savePet());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    dobEditText.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showImagePickerOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    openCamera();
                    break;
                case 1:
                    openGallery();
                    break;
                case 2:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void savePet() {
        String name = petNameEditText.getText().toString().trim();
        String species = speciesSpinner.getSelectedItem().toString();
        String breed = breedEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            petNameEditText.setError("Pet name is required");
            return;
        }

        if (TextUtils.isEmpty(breed)) {
            breedEditText.setError("Breed is required");
            return;
        }

        if (TextUtils.isEmpty(dob)) {
            dobEditText.setError("Date of birth is required");
            return;
        }

        String gender = genderRadioGroup.getCheckedRadioButtonId() == R.id.maleRadio ? "Male" : "Female";

        Pet pet = new Pet(name, species, breed, dob, gender);

        showLoading(true);

        String token = SharedPrefManager.getInstance(this).getToken();

        apiService.addPet("Bearer " + token, pet).enqueue(new Callback<Pet>() {
            @Override
            public void onResponse(@NonNull Call<Pet> call, @NonNull Response<Pet> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(AddPetActivity.this, "Pet added successfully!", Toast.LENGTH_SHORT).show();
                    Pet created = response.body();
                    if (selectedImageUri != null) {
                        // Upload photo for the newly created pet
                        uploadPetPhoto(created.getId(), selectedImageUri);
                    } else {
                        setResult(RESULT_OK);
                        finish();
                    }
                } else {
                    Toast.makeText(AddPetActivity.this, "Failed to add pet", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Pet> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(AddPetActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        savePetButton.setEnabled(!show);
    }

    private void uploadPetPhoto(String petId, Uri imageUri) {
        try {
            // Reuse logic similar to PetDetailActivity: compress and upload
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            java.io.File tempFile = java.io.File.createTempFile("new_pet", ".jpg", getCacheDir());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            inputStream.close();

            Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(tempFile.getAbsolutePath());
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageData = baos.toByteArray();

            okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), imageData);
            okhttp3.MultipartBody.Part photoPart = okhttp3.MultipartBody.Part.createFormData("photo", "photo.jpg", requestFile);

            String token = SharedPrefManager.getInstance(this).getToken();
            apiService.uploadPetPhoto("Bearer " + token, petId, photoPart)
                    .enqueue(new retrofit2.Callback<Pet>() {
                        @Override
                        public void onResponse(@NonNull retrofit2.Call<Pet> call, @NonNull retrofit2.Response<Pet> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AddPetActivity.this, "Photo uploaded", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AddPetActivity.this, "Photo upload failed", Toast.LENGTH_SHORT).show();
                            }
                            setResult(RESULT_OK);
                            finish();
                        }

                        @Override
                        public void onFailure(@NonNull retrofit2.Call<Pet> call, @NonNull Throwable t) {
                            Toast.makeText(AddPetActivity.this, "Photo upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error preparing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }

    private Uri saveBitmapToTempFile(Bitmap bitmap) {
        try {
            java.io.File tempFile = java.io.File.createTempFile("camera_add_pet", ".jpg", getCacheDir());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return android.net.Uri.fromFile(tempFile);
        } catch (Exception e) {
            Toast.makeText(this, "Error saving photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
