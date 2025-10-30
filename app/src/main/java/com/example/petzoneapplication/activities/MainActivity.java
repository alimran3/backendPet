package com.example.petzoneapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.petzoneapplication.R;
import com.example.petzoneapplication.fragments.CommunityFragment;
import com.example.petzoneapplication.fragments.MemoriesFragment;
import com.example.petzoneapplication.fragments.MyPetsFragment;
import com.example.petzoneapplication.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int ADD_PET_REQUEST = 100;
    private BottomNavigationView bottomNavigationView;
    private MyPetsFragment myPetsFragment; // Keep reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Initialize MyPetsFragment once
        myPetsFragment = new MyPetsFragment();

        // Load Community Fragment first
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new CommunityFragment())
                    .commit();
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_community) {
                    selectedFragment = new CommunityFragment();
                } else if (itemId == R.id.nav_my_pets) {
                    selectedFragment = myPetsFragment; // Use existing instance
                } else if (itemId == R.id.nav_memories) {
                    selectedFragment = new MemoriesFragment();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, selectedFragment)
                            .commit();
                    return true;
                }

                return false;
            }
        });
    }

    // ✅ Handle result from AddPetActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_PET_REQUEST && resultCode == RESULT_OK) {
            // Refresh MyPetsFragment if it's active or cached
            if (myPetsFragment != null) {
                myPetsFragment.loadMyPets(); // Must have this public method
            }
        }
    }

    // ✅ Helper method to open AddPetActivity
    public void openAddPetActivity() {
        startActivityForResult(new Intent(this, AddPetActivity.class), ADD_PET_REQUEST);
    }
}