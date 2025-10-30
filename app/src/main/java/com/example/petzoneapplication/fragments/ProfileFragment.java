package com.example.petzoneapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.activities.LoginActivity;
import com.example.petzoneapplication.models.User;
import com.example.petzoneapplication.utils.SharedPrefManager;

public class ProfileFragment extends Fragment {

    private TextView nameTextView, emailTextView;
    private Switch notificationSwitch, darkModeSwitch, communityShareSwitch;
    private Button editProfileButton, changePasswordButton, logoutButton;
    private SharedPrefManager prefManager;

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
        nameTextView = view.findViewById(R.id.nameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        notificationSwitch = view.findViewById(R.id.notificationSwitch);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        communityShareSwitch = view.findViewById(R.id.communityShareSwitch);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        logoutButton = view.findViewById(R.id.logoutButton);

        prefManager = SharedPrefManager.getInstance(getContext());
    }

    private void loadUserData() {
        User user = prefManager.getUser();
        if (user != null) {
            nameTextView.setText(user.getName());
            emailTextView.setText(user.getEmail());
        }
    }

    private void setupListeners() {
        editProfileButton.setOnClickListener(v -> {
            // Open edit profile dialog or activity
        });

        changePasswordButton.setOnClickListener(v -> {
            // Open change password dialog
        });

        logoutButton.setOnClickListener(v -> {
            showLogoutConfirmation();
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save notification preference
        });

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Toggle dark mode
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
}