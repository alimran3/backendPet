package com.example.petzoneapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.petzoneapplication.models.User;
import com.google.gson.Gson;

public class SharedPrefManager {
    private static final String SHARED_PREF_NAME = "petzone_prefs";
    private static final String KEY_USER = "key_user";
    private static final String KEY_TOKEN = "key_token";
    private static final String KEY_IS_LOGGED_IN = "key_is_logged_in";
    private static final String KEY_NOTIFICATIONS_ENABLED = "key_notifications";
    private static final String KEY_DARK_MODE = "key_dark_mode";
    private static final String KEY_COMMUNITY_SHARE = "key_community_share";

    private static SharedPrefManager instance;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final Gson gson;

    private SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        gson = new Gson();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    public void saveUser(User user) {
        String userJson = gson.toJson(user);
        editor.putString(KEY_USER, userJson);
        editor.putString(KEY_TOKEN, user.getToken());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public User getUser() {
        String userJson = sharedPreferences.getString(KEY_USER, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled);
        editor.apply();
    }

    public boolean isNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setDarkModeEnabled(boolean enabled) {
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
    }

    public boolean isDarkModeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setCommunityShareEnabled(boolean enabled) {
        editor.putBoolean(KEY_COMMUNITY_SHARE, enabled);
        editor.apply();
    }

    public boolean isCommunityShareEnabled() {
        return sharedPreferences.getBoolean(KEY_COMMUNITY_SHARE, true);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}