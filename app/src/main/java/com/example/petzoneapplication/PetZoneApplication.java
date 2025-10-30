package com.example.petzoneapplication;

import android.app.Application;
import android.content.Context;

public class PetZoneApplication extends Application {
    private static PetZoneApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}