package com.example.petzoneapplication.fragments.pet_tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.adapters.HealthLogAdapter;
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

public class HealthFragment extends Fragment {

    private static final String ARG_PET_ID = "pet_id";
    private String petId;
    private ApiService apiService;
    private EditText titleEditText, notesEditText;
    private Spinner healthTypeSpinner;
    private Button addHealthLogButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView healthRecyclerView;
    private HealthLogAdapter adapter;
    private final List<Map<String, Object>> healthLogs = new ArrayList<>();
    private String token;

    public static HealthFragment newInstance(String petId) {
        HealthFragment fragment = new HealthFragment();
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
        View view = inflater.inflate(R.layout.fragment_health, container, false);

        titleEditText = view.findViewById(R.id.titleEditText);
        notesEditText = view.findViewById(R.id.notesEditText);
        healthTypeSpinner = view.findViewById(R.id.healthTypeSpinner);
        addHealthLogButton = view.findViewById(R.id.addHealthLogButton);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        healthRecyclerView = view.findViewById(R.id.healthRecyclerView);

        // Initialize spinner with health types
        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.health_types,
                android.R.layout.simple_spinner_item
        );
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        healthTypeSpinner.setAdapter(adapterSpinner);

        setupRecyclerView();
        setupListeners();
        loadHealthLogs();

        return view;
    }

    private void setupRecyclerView() {
        healthRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HealthLogAdapter(healthLogs);
        healthRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        addHealthLogButton.setOnClickListener(v -> addHealthLog());
        swipeRefreshLayout.setOnRefreshListener(this::loadHealthLogs);
    }

    private void addHealthLog() {
        String title = titleEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();
        String type = healthTypeSpinner.getSelectedItem() != null
                ? healthTypeSpinner.getSelectedItem().toString()
                : "";

        if (title.isEmpty() || type.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("petId", petId);
        // Backend expects one of: Feeding, Grooming, Exercise, Medication, VetVisit, Vaccination
        payload.put("type", type);
        Map<String, Object> details = new HashMap<>();
        details.put("notes", notes);
        if ("Medication".equals(type)) {
            details.put("medicationName", title);
        } else if ("Vaccination".equals(type)) {
            details.put("vaccineName", title);
        } else if ("VetVisit".equals(type)) {
            details.put("reason", title);
        } else if ("Exercise".equals(type)) {
            details.put("activityType", title);
        }
        payload.put("details", details);

        apiService.addCareLog("Bearer " + token, payload)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Health log added successfully", Toast.LENGTH_SHORT).show();
                            titleEditText.setText("");
                            notesEditText.setText("");
                            healthTypeSpinner.setSelection(0);
                            loadHealthLogs();
                        } else {
                            Toast.makeText(requireContext(), "Failed to add health log", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadHealthLogs() {
        swipeRefreshLayout.setRefreshing(true);
        healthLogs.clear();

        String[] types = new String[]{"Medication", "VetVisit", "Vaccination", "Exercise"};
        final int[] pending = {types.length};

        Callback<List<Map<String, Object>>> cb = new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    healthLogs.addAll(response.body());
                }
                if (--pending[0] == 0) {
                    swipeRefreshLayout.setRefreshing(false);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                if (--pending[0] == 0) {
                    swipeRefreshLayout.setRefreshing(false);
                    adapter.notifyDataSetChanged();
                }
            }
        };

        for (String t : types) {
            apiService.getCareLogs("Bearer " + token, petId, t).enqueue(cb);
        }
    }
}