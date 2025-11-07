package com.example.petzoneapplication.fragments.pet_tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.adapters.FeedingLogAdapter;
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

public class FeedingFragment extends Fragment {

    private static final String ARG_PET_ID = "pet_id";
    private String petId;
    private ApiService apiService;
    private EditText foodTypeEditText, quantityEditText;
    private Button addFeedingButton;
    private EditText dateEditText;
    private MaterialButton morningButton, noonButton, nightButton;
    private long selectedDateMillis = System.currentTimeMillis();
    private String selectedSlot = "Morning";
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView feedingRecyclerView;
    private FeedingLogAdapter adapter;
    private final List<Map<String, Object>> feedingLogs = new ArrayList<>();
    private String token;

    public static FeedingFragment newInstance(String petId) {
        FeedingFragment fragment = new FeedingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PET_ID, petId);
        fragment.setArguments(args);
        return fragment;
    }

    private void showAddSlotDialog(long dayStartMillis, String slot) {
        android.widget.EditText foodInput = new android.widget.EditText(requireContext());
        foodInput.setHint("Food type");
        android.widget.EditText amountInput = new android.widget.EditText(requireContext());
        amountInput.setHint("Amount (e.g., 1 cup)");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(foodInput);
        layout.addView(amountInput);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add " + slot + " feeding")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String food = foodInput.getText().toString().trim();
                    String amount = amountInput.getText().toString().trim();
                    if (food.isEmpty() || amount.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter food and amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addFeedingForDaySlot(food, amount, dayStartMillis, slot);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddNextDialog(long dayStartMillis) {
        android.widget.EditText foodInput = new android.widget.EditText(requireContext());
        foodInput.setHint("Food type");
        android.widget.EditText amountInput = new android.widget.EditText(requireContext());
        amountInput.setHint("Amount (e.g., 1 cup)");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(foodInput);
        layout.addView(amountInput);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add next feeding")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String food = foodInput.getText().toString().trim();
                    String amount = amountInput.getText().toString().trim();
                    if (food.isEmpty() || amount.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter food and amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addFeedingForDay(food, amount, dayStartMillis);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addFeedingForDay(String foodType, String amount, long dayStartMillis) {
        String nextSlot = getNextAvailableSlotForDay(dayStartMillis);
        if (nextSlot == null) {
            Toast.makeText(requireContext(), "All slots for this day are already logged", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("petId", petId);
        payload.put("type", "Feeding");
        Map<String, Object> details = new HashMap<>();
        details.put("foodType", foodType);
        details.put("amount", amount);
        details.put("timeSlot", nextSlot);
        payload.put("details", details);
        payload.put("date", dayStartMillis);

        apiService.addCareLog("Bearer " + token, payload)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            loadFeedingLogs();
                        } else {
                            Toast.makeText(requireContext(), "Failed to add next feeding", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(requireContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private long parseToMillis(Object... candidates) {
        for (Object ts : candidates) {
            if (ts == null) continue;
            if (ts instanceof Long) return (Long) ts;
            if (ts instanceof Number) return ((Number) ts).longValue();
            if (ts instanceof String) {
                String s = (String) ts;
                try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
                String[] patterns = new String[]{
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                        "yyyy-MM-dd'T'HH:mm:ssXXX"
                };
                for (String p : patterns) {
                    try {
                        java.text.SimpleDateFormat iso = new java.text.SimpleDateFormat(p, java.util.Locale.US);
                        iso.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        java.util.Date d = iso.parse(s);
                        if (d != null) return d.getTime();
                    } catch (Exception ignored2) {}
                }
            }
        }
        return -1L;
    }

    private void addFeedingForDaySlot(String foodType, String amount, long dayStartMillis, String slot) {
        if (foodType == null || foodType.isEmpty() || amount == null || amount.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter food and amount", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("petId", petId);
        payload.put("type", "Feeding");
        Map<String, Object> details = new HashMap<>();
        details.put("foodType", foodType);
        details.put("amount", amount);
        details.put("timeSlot", slot);
        payload.put("details", details);
        payload.put("date", dayStartMillis);

        apiService.addCareLog("Bearer " + token, payload)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            loadFeedingLogs();
                        } else {
                            Toast.makeText(requireContext(), "Failed to add feeding", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(requireContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            petId = getArguments().getString(ARG_PET_ID);
            if (petId == null || petId.isEmpty()) {
                Toast.makeText(requireContext(), "Error: Pet ID is missing", Toast.LENGTH_LONG).show();
            }
        }
        apiService = RetrofitClient.getApiService();
        token = SharedPrefManager.getInstance(requireContext()).getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Authentication token is missing", Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feeding, container, false);

        foodTypeEditText = view.findViewById(R.id.foodTypeEditText);
        quantityEditText = view.findViewById(R.id.quantityEditText);
        dateEditText = view.findViewById(R.id.dateEditText);
        morningButton = view.findViewById(R.id.morningButton);
        noonButton = view.findViewById(R.id.noonButton);
        nightButton = view.findViewById(R.id.nightButton);
        addFeedingButton = view.findViewById(R.id.addFeedingButton);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        feedingRecyclerView = view.findViewById(R.id.feedingRecyclerView);

        setupRecyclerView();
        setupListeners();
        initDateField();
        setSelectedSlot("Morning"); // Default to Morning
        loadFeedingLogs();

        return view;
    }

    private void setupRecyclerView() {
        feedingRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FeedingLogAdapter(feedingLogs);
        feedingRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        addFeedingButton.setOnClickListener(v -> addFeedingLog());
        swipeRefreshLayout.setOnRefreshListener(this::loadFeedingLogs);
        dateEditText.setOnClickListener(v -> openDatePicker());
        morningButton.setOnClickListener(v -> setSelectedSlot("Morning"));
        noonButton.setOnClickListener(v -> setSelectedSlot("Noon"));
        nightButton.setOnClickListener(v -> setSelectedSlot("Night"));
    }

    private void addFeedingLog() {
        String foodType = foodTypeEditText.getText().toString().trim();
        String quantity = quantityEditText.getText().toString().trim();

        if (foodType.isEmpty() || quantity.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the selected time slot from the buttons
        if (selectedSlot == null || selectedSlot.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a time slot (Morning, Noon, or Night)", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("petId", petId);
        payload.put("type", "Feeding");
        Map<String, Object> details = new HashMap<>();
        details.put("foodType", foodType);
        // Backend expects 'amount' for Feeding quantity
        details.put("amount", quantity);
        details.put("timeSlot", selectedSlot);
        payload.put("details", details);
        payload.put("date", selectedDateMillis);

        apiService.addCareLog("Bearer " + token, payload)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Feeding log added successfully", Toast.LENGTH_SHORT).show();
                            foodTypeEditText.setText("");
                            quantityEditText.setText("");
                            loadFeedingLogs();
                        } else {
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                                Toast.makeText(requireContext(), "Failed: " + errorBody, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Failed to add feeding log: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        t.printStackTrace(); // Log the error
                        Toast.makeText(requireContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void initDateField() {
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        dateEditText.setText(df.format(new java.util.Date(selectedDateMillis)));
    }

    private void openDatePicker() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis);
        new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(year, month, dayOfMonth, 0, 0, 0);
            c.set(java.util.Calendar.MILLISECOND, 0);
            selectedDateMillis = c.getTimeInMillis();
            initDateField();
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private void setSelectedSlot(String slot) {
        selectedSlot = slot;
        
        // Reset all buttons to normal state
        morningButton.setStrokeWidth(0);
        noonButton.setStrokeWidth(0);
        nightButton.setStrokeWidth(0);
        
        morningButton.setAlpha(0.6f);
        noonButton.setAlpha(0.6f);
        nightButton.setAlpha(0.6f);
        
        // Highlight selected button
        if ("Morning".equals(slot)) {
            morningButton.setAlpha(1.0f);
            morningButton.setStrokeWidth(4);
            morningButton.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF000000));
        } else if ("Noon".equals(slot)) {
            noonButton.setAlpha(1.0f);
            noonButton.setStrokeWidth(4);
            noonButton.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF000000));
        } else if ("Night".equals(slot)) {
            nightButton.setAlpha(1.0f);
            nightButton.setStrokeWidth(4);
            nightButton.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF000000));
        }
    }

    private String getNextAvailableSlotForDay(long dayMillis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(dayMillis);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        long startOfDay = c.getTimeInMillis();
        long endOfDay = startOfDay + 24L * 60L * 60L * 1000L - 1L;

        java.util.Set<String> present = new java.util.HashSet<>();
        for (Map<String, Object> log : feedingLogs) {
            Object ts = log.get("date");
            if (ts == null) ts = log.get("timestamp");
            if (ts == null) ts = log.get("createdAt");
            long t = -1L;
            if (ts instanceof Long) t = (Long) ts;
            else if (ts instanceof String) {
                try { t = Long.parseLong((String) ts); } catch (NumberFormatException ignored) {}
            }
            if (t >= startOfDay && t <= endOfDay) {
                Object slot = getFromDetails(log, "timeSlot");
                if (slot != null) present.add(String.valueOf(slot));
            }
        }
        if (!present.contains("Morning")) return "Morning";
        if (!present.contains("Noon")) return "Noon";
        if (!present.contains("Night")) return "Night";
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object getFromDetails(Map<String, Object> log, String key) {
        if (log == null) return null;
        Object direct = log.get(key);
        if (direct != null) return direct;
        Object details = log.get("details");
        if (details instanceof Map) {
            return ((Map<String, Object>) details).get(key);
        }
        return null;
    }

    private void loadFeedingLogs() {
        swipeRefreshLayout.setRefreshing(true);
        apiService.getCareLogs("Bearer " + token, petId, "Feeding")
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                        swipeRefreshLayout.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            feedingLogs.clear();
                            feedingLogs.addAll(response.body());
                            android.util.Log.d("FeedingFragment", "Loaded " + feedingLogs.size() + " feeding logs");
                            // Sort by date desc to ensure correct day grouping
                            java.util.Collections.sort(feedingLogs, (a, b) -> {
                                long ta = parseToMillis(a.get("date"), a.get("timestamp"), a.get("createdAt"));
                                long tb = parseToMillis(b.get("date"), b.get("timestamp"), b.get("createdAt"));
                                return Long.compare(tb, ta);
                            });
                            adapter.refreshData();
                            if (feedingLogs.isEmpty()) {
                                Toast.makeText(requireContext(), "No feeding logs yet. Add your first meal!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            android.util.Log.e("FeedingFragment", "Failed to load: " + response.code());
                            Toast.makeText(requireContext(), "Failed to load feeding logs: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(requireContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}