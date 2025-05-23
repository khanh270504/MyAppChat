package com.example.chatapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public void putBoolean(String key, Boolean val) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, val);
        editor.apply();
    }

    public Boolean getBoolean(String key) {
        return  sharedPreferences.getBoolean(key, false);
    }

    public void putString(String key, String val) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, val);
        editor.apply();
    }

    public String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}

