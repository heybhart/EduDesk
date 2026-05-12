package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;


public class SessionManager {

    private static final String PREF_NAME   = "EduDeskPrefs";
    private static final String IS_Student_LOGGED_IN = "isStudentLoggedIn";
    private static final String IS_Faculty_LOGGED_IN = "isFacultyLoggedIn";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void save_session_of_student()
    {
        editor.putBoolean(IS_Student_LOGGED_IN, true);
        editor.apply();
    }

    public boolean isStudentSessionValid() {
        return prefs.getBoolean(IS_Student_LOGGED_IN, false);
    }

    // Logout pe call karo
    public void clearstudentSession() {
        editor.remove(IS_Student_LOGGED_IN).apply();
    }

//   ========================================= Faculty Session Cheking =======================

    public void save_session_of_faculty()
    {
        editor.putBoolean(IS_Faculty_LOGGED_IN, true);
        editor.apply();
    }

    public boolean is_faculty_session_valid() {
        return prefs.getBoolean(IS_Faculty_LOGGED_IN, false);
    }

    // Logout pe call karo
    public void clearfacultySession() {
        editor.remove(IS_Faculty_LOGGED_IN).apply();
    }

    // Pura session clear karne ke liye (Optional)
    public void logoutAll() {
        editor.clear().apply();
    }
}
