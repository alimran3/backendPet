package com.example.petzoneapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.activities.AddPetActivity;
import com.example.petzoneapplication.adapters.CommunityFeedAdapter;
import com.example.petzoneapplication.models.CommunityPost;
import com.example.petzoneapplication.network.ApiService;
import com.example.petzoneapplication.network.RetrofitClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class CommunityFragment extends Fragment {

    private RecyclerView recyclerView;
    private CommunityFeedAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddPet;
    private TextView emptyView;
    private ApiService apiService;
    private List<CommunityPost> postList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        initViews(view);
        setupRecyclerView();
        loadCommunityFeed();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        fabAddPet = view.findViewById(R.id.fabAddPet);
        emptyView = view.findViewById(R.id.emptyView);

        apiService = RetrofitClient.getApiService();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadCommunityFeed();
            }
        });

        fabAddPet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), AddPetActivity.class));
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new CommunityFeedAdapter(getContext(), postList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadCommunityFeed() {
        swipeRefreshLayout.setRefreshing(true);
        Log.d("CommunityFragment", "Loading community feed...");
        
        try {
            apiService.getCommunityFeed().enqueue(new retrofit2.Callback<List<CommunityPost>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<List<CommunityPost>> call, @NonNull retrofit2.Response<List<CommunityPost>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    postList.clear();
                    postList.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    Log.d("CommunityFragment", "Loaded " + postList.size() + " posts");

                    if (postList.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e("CommunityFragment", "Response not successful: " + response.code());
                    Toast.makeText(getContext(), "Failed to load community posts", Toast.LENGTH_SHORT).show();
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CommunityPost>> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e("CommunityFragment", "Network error: " + t.getMessage());
                t.printStackTrace();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
        } catch (Exception e) {
            Log.e("CommunityFragment", "Error creating request: " + e.getMessage());
            e.printStackTrace();
            swipeRefreshLayout.setRefreshing(false);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}