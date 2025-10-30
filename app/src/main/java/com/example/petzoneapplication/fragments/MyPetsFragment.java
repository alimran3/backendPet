package com.example.petzoneapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.activities.AddPetActivity;
import com.example.petzoneapplication.activities.PetDetailActivity;
import com.example.petzoneapplication.adapters.MyPetsAdapter;
import com.example.petzoneapplication.models.Pet;
import com.example.petzoneapplication.network.ApiService;
import com.example.petzoneapplication.network.RetrofitClient;
import com.example.petzoneapplication.utils.SharedPrefManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPetsFragment extends Fragment implements MyPetsAdapter.OnPetClickListener {

    private static final String TAG = "MyPetsFragment";
    private RecyclerView recyclerView;
    private MyPetsAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FloatingActionButton fabAddPet;
    private final List<Pet> petList = new ArrayList<>();
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_pets, container, false);

        initViews(view);
        setupRecyclerView();
        loadMyPets();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        fabAddPet = view.findViewById(R.id.fabAddPet);

        apiService = RetrofitClient.getApiService();

        fabAddPet.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddPetActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new MyPetsAdapter(getContext(), petList, this);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);
    }

    public void loadMyPets() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        String token = SharedPrefManager.getInstance(getContext()).getToken();

        if (token == null || token.isEmpty()) {
            Log.w(TAG, "No auth token found. Loading sample data.");
            loadSampleData();
            return;
        }

        String authHeader = "Bearer " + token;
        Log.d(TAG, "Fetching pets with token.");

        apiService.getMyPets(authHeader).enqueue(new Callback<List<Pet>>() {
            @Override
            public void onResponse(@NonNull Call<List<Pet>> call, @NonNull Response<List<Pet>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully fetched " + response.body().size() + " pets.");
                    petList.clear();
                    petList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                } else {
                    Log.e(TAG, "Failed to fetch pets. Code: " + response.code());
                    Toast.makeText(getContext(), "Failed to load pets. Displaying sample data.", Toast.LENGTH_LONG).show();
                    loadSampleData();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Pet>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Network error while fetching pets.", t);
                Toast.makeText(getContext(), "Network error. Displaying sample data.", Toast.LENGTH_LONG).show();
                loadSampleData();
            }
        });
    }

    private void loadSampleData() {
        Log.d(TAG, "Loading sample pet data.");
        petList.clear();

        Pet pet1 = new Pet("Max", "Dog", "Labrador", "2020-01-15", "Male");
        pet1.setId("sample-1");
        pet1.setPhotoUrl("https://images.unsplash.com/photo-1587300003388-59208cc962cb");
        petList.add(pet1);

        Pet pet2 = new Pet("Luna", "Cat", "Persian", "2021-03-20", "Female");
        pet2.setId("sample-2");
        pet2.setPhotoUrl("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba");
        petList.add(pet2);

        Pet pet3 = new Pet("Charlie", "Dog", "Golden Retriever", "2019-07-10", "Male");
        pet3.setId("sample-3");
        // No photo URL for this one to test placeholder
        petList.add(pet3);

        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (petList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPetClick(Pet pet) {
        if (pet == null || pet.getId() == null) {
            Toast.makeText(getContext(), "Cannot view details for this pet.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onPetClick: Pet or Pet ID is null!");
            return;
        }
        Log.d(TAG, "Clicked on pet: " + pet.getName() + " (ID: " + pet.getId() + ")");
        Intent intent = new Intent(getActivity(), PetDetailActivity.class);
        intent.putExtra("pet_id", pet.getId());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyPets();
    }
}