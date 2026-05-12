package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class Faculty_main_activity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout navHome, navAnnouncements, navAnalytics, navChat, navProfile;
    private ImageView icHome, icAnnouncements, icAnalytics, icChat, icProfile;
    private String name, surname, email, department, subject, college, uid, gender, phone, role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        viewPager = findViewById(R.id.viewPager);

        navHome = findViewById(R.id.navHome);
        navAnnouncements = findViewById(R.id.navAnnouncements);
        navAnalytics = findViewById(R.id.navAnalytics);
        navChat = findViewById(R.id.navChat);
        navProfile = findViewById(R.id.navProfile);

        icHome = findViewById(R.id.icHome);
        icAnnouncements = findViewById(R.id.icAnnouncements);
        icAnalytics = findViewById(R.id.icAnalytics);
        icChat = findViewById(R.id.icChat);
        icProfile = findViewById(R.id.icProfile);

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        surname = intent.getStringExtra("surname");
        email = intent.getStringExtra("email");
        department = intent.getStringExtra("department");
        subject = intent.getStringExtra("subject");
        college = intent.getStringExtra("college");
        uid = intent.getStringExtra("uid");
        gender = intent.getStringExtra("gender");
        phone = intent.getStringExtra("phone");
        role = intent.getStringExtra("role");

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 5;
            }

            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 1:
                        return new Announcements_Faculty_fragment();
                    case 2:
                        return new Analytics_Faculty_fragment();
                    case 3:
                        return new ChatActivity();
                    case 4:
                        return new Profile_Faculty_fragment();
                    default:
                        return new Home_Faculty_fragment();
                }
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateNavUI(position);
            }
        });

        navHome.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        navAnnouncements.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        navAnalytics.setOnClickListener(v -> viewPager.setCurrentItem(2, true));
        navChat.setOnClickListener(v -> viewPager.setCurrentItem(3, true));
        navProfile.setOnClickListener(v -> viewPager.setCurrentItem(4, true));

        updateNavUI(0);
    }

    private void updateNavUI(int selected) {
        int navy = 0xFF1A2340;
        int gray = 0xFFAAAAAA;

        icHome.setColorFilter(gray);
        icAnnouncements.setColorFilter(gray);
        icAnalytics.setColorFilter(gray);
        icChat.setColorFilter(gray);
        icProfile.setColorFilter(gray);

        switch (selected) {
            case 0:
                icHome.setColorFilter(navy);
                break;
            case 1:
                icAnnouncements.setColorFilter(navy);
                break;
            case 2:
                icAnalytics.setColorFilter(navy);
                break;
            case 3:
                icChat.setColorFilter(navy);
                break;
            case 4:
                icProfile.setColorFilter(navy);
                break;
        }
    }
}
