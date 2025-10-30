package com.example.petzoneapplication.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.petzoneapplication.fragments.pet_tabs.FeedingFragment;
import com.example.petzoneapplication.fragments.pet_tabs.GroomingFragment;
import com.example.petzoneapplication.fragments.pet_tabs.HealthFragment;
import com.example.petzoneapplication.fragments.pet_tabs.PetMemoriesFragment;

public class PetDetailPagerAdapter extends FragmentStateAdapter {

    private final String petId;

    public PetDetailPagerAdapter(@NonNull FragmentActivity fragmentActivity, String petId) {
        super(fragmentActivity);
        this.petId = petId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return FeedingFragment.newInstance(petId);
            case 1:
                return GroomingFragment.newInstance(petId);
            case 2:
                return HealthFragment.newInstance(petId);
            case 3:
                return PetMemoriesFragment.newInstance(petId);
            default:
                // Should never happen, but return a safe default.
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}