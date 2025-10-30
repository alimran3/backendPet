package com.example.petzoneapplication.fragments.pet_tabs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.adapters.MemoriesAdapter;
import com.example.petzoneapplication.models.Memory;
import com.example.petzoneapplication.network.ApiService;
import com.example.petzoneapplication.network.RetrofitClient;
import com.example.petzoneapplication.utils.SharedPrefManager;

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

public class PetMemoriesFragment extends Fragment {

    private static final String ARG_PET_ID = "pet_id";
    private String petId;
    private ApiService apiService;
    private String token;

    private RecyclerView memoriesRecyclerView;
    private MemoriesAdapter adapter;
    private final List<Memory> memories = new ArrayList<>();

    private Button addMemoryButton;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadMemory(uri);
                }
            }
    );

    public static PetMemoriesFragment newInstance(String petId) {
        PetMemoriesFragment fragment = new PetMemoriesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            petId = getArguments().getString(ARG_PET_ID);
        }
        apiService = RetrofitClient.getApiService();
        token = SharedPrefManager.getInstance(requireContext()).getToken();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pet_memories, container, false);
        memoriesRecyclerView = view.findViewById(R.id.memoriesRecyclerView);
        addMemoryButton = view.findViewById(R.id.addMemoryButton);

        memoriesRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new MemoriesAdapter(requireContext(), memories);
        memoriesRecyclerView.setAdapter(adapter);

        addMemoryButton.setOnClickListener(v -> openGallery());

        loadMemories();
        return view;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void loadMemories() {
        apiService.getPetMemories("Bearer " + token, petId).enqueue(new Callback<List<Memory>>() {
            @Override
            public void onResponse(@NonNull Call<List<Memory>> call, @NonNull Response<List<Memory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    memories.clear();
                    memories.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Memory>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Failed to load memories: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadMemory(Uri imageUri) {
        try {
            ProgressDialog pd = new ProgressDialog(requireContext());
            pd.setMessage("Uploading...");
            pd.setCancelable(false);
            pd.show();

            // Copy to temp file
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
                            if (response.isSuccessful() && response.body() != null) {
                                Toast.makeText(requireContext(), "Memory added", Toast.LENGTH_SHORT).show();
                                loadMemories();
                            } else {
                                Toast.makeText(requireContext(), "Failed to add memory", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Memory> call, @NonNull Throwable t) {
                            pd.dismiss();
                            Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error preparing image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}