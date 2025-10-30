package com.example.petzoneapplication.fragments.pet_tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.adapters.GroomingLogAdapter;
import com.example.petzoneapplication.network.ApiService;
import com.example.petzoneapplication.network.RetrofitClient;
import com.example.petzoneapplication.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroomingFragment extends Fragment {

    private static final String ARG_PET_ID = "pet_id";
    private String petId;
    private ApiService apiService;
    private EditText groomingTypeEditText, notesEditText;
    private Button addGroomingButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView groomingRecyclerView;
    private GroomingLogAdapter adapter;
    private final List<Map<String, Object>> groomingLogs = new ArrayList<>();
    private String token;

    public static GroomingFragment newInstance(String petId) {
        GroomingFragment fragment = new GroomingFragment();
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
        View view = inflater.inflate(R.layout.fragment_grooming, container, false);

        groomingTypeEditText = view.findViewById(R.id.groomingTypeEditText);
        notesEditText = view.findViewById(R.id.notesEditText);
        addGroomingButton = view.findViewById(R.id.addGroomingButton);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        groomingRecyclerView = view.findViewById(R.id.groomingRecyclerView);

        setupRecyclerView();
        setupListeners();
        loadGroomingLogs();

        return view;
    }

    private void setupRecyclerView() {
        groomingRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GroomingLogAdapter(groomingLogs);
        groomingRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        addGroomingButton.setOnClickListener(v -> addGroomingLog());
        swipeRefreshLayout.setOnRefreshListener(this::loadGroomingLogs);
    }

    private void addGroomingLog() {
        String groomingType = groomingTypeEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();

        if (groomingType.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter grooming type", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("petId", petId);
        payload.put("type", "Grooming");
        Map<String, Object> details = new HashMap<>();
        details.put("groomingType", groomingType);
        details.put("notes", notes);
        payload.put("details", details);

        apiService.addCareLog("Bearer " + token, payload)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Grooming log added successfully", Toast.LENGTH_SHORT).show();
                            groomingTypeEditText.setText("");
                            notesEditText.setText("");
                            loadGroomingLogs();
                        } else {
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                                Toast.makeText(requireContext(), "Failed: " + errorBody, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Failed to add grooming log: " + response.code(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadGroomingLogs() {
        swipeRefreshLayout.setRefreshing(true);
        apiService.getCareLogs("Bearer " + token, petId, "Grooming")
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                        swipeRefreshLayout.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            groomingLogs.clear();
                            groomingLogs.addAll(response.body());
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(requireContext(), "Failed to load grooming logs", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}