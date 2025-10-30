package com.example.petzoneapplication.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.ProgressDialog;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.adapters.MemoriesAdapter;
import com.example.petzoneapplication.models.Memory;
import com.example.petzoneapplication.models.Pet;
import com.example.petzoneapplication.network.ApiService;
import com.example.petzoneapplication.network.RetrofitClient;
import com.example.petzoneapplication.utils.SharedPrefManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MemoriesFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    private RecyclerView recyclerView;
    private MemoriesAdapter adapter;
    private TextView emptyView;
    private FloatingActionButton fabAddMemory;
    private List<Memory> memoryList = new ArrayList<>();

    private ApiService apiService;
    private String token;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memories, container, false);

        apiService = RetrofitClient.getApiService();
        token = SharedPrefManager.getInstance(requireContext()).getToken();

        initViews(view);
        setupRecyclerView();
        loadMemories();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        fabAddMemory = view.findViewById(R.id.fabAddMemory);

        fabAddMemory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerOptions();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new MemoriesAdapter(getContext(), memoryList);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);
    }

    private void loadMemories() {
        apiService.getAllMemories("Bearer " + token).enqueue(new Callback<List<Memory>>() {
            @Override
            public void onResponse(@NonNull Call<List<Memory>> call, @NonNull Response<List<Memory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    memoryList.clear();
                    memoryList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
                updateEmptyView();
            }

            @Override
            public void onFailure(@NonNull Call<List<Memory>> call, @NonNull Throwable t) {
                updateEmptyView();
            }
        });
    }

    private void updateEmptyView() {
        if (memoryList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showImagePickerOptions() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Add Memory");
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
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            Uri uri = data.getData();
            choosePetAndUpload(uri);
        }
    }

    private void choosePetAndUpload(Uri imageUri) {
        apiService.getMyPets("Bearer " + token).enqueue(new Callback<List<Pet>>() {
            @Override
            public void onResponse(@NonNull Call<List<Pet>> call, @NonNull Response<List<Pet>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Pet> pets = response.body();
                    List<String> names = new ArrayList<>();
                    for (Pet p : pets) names.add(p.getName());

                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Select Pet")
                            .setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, names), (dialog, which) -> {
                                String petId = pets.get(which).getId();
                                uploadMemory(imageUri, petId);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Pet>> call, @NonNull Throwable t) { }
        });
    }

    private void uploadMemory(Uri imageUri, String petId) {
        try {
            ProgressDialog pd = new ProgressDialog(requireContext());
            pd.setMessage("Uploading...");
            pd.setCancelable(false);
            pd.show();

            InputStream is = requireContext().getContentResolver().openInputStream(imageUri);
            File temp = File.createTempFile("memory", ".jpg", requireContext().getCacheDir());
            FileOutputStream fos = new FileOutputStream(temp);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) fos.write(buf, 0, len);
            fos.close();
            is.close();

            RequestBody petIdBody = RequestBody.create(MediaType.parse("text/plain"), petId);
            RequestBody captionBody = RequestBody.create(MediaType.parse("text/plain"), "");
            RequestBody isSharedBody = RequestBody.create(MediaType.parse("text/plain"), "false");
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), temp);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "memory.jpg", fileBody);

            apiService.addMemory("Bearer " + token, petIdBody, captionBody, isSharedBody, imagePart)
                    .enqueue(new Callback<Memory>() {
                        @Override
                        public void onResponse(@NonNull Call<Memory> call, @NonNull Response<Memory> response) {
                            pd.dismiss();
                            if (response.isSuccessful()) {
                                loadMemories();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Memory> call, @NonNull Throwable t) {
                            pd.dismiss();
                        }
                    });
        } catch (Exception ignored) { }
    }
}