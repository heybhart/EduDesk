package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends AppCompatActivity {

    ViewPager2 viewPager;
    LinearLayout navHome, navTickets, navLibrary, navProfile;
    ImageView icHome, icTickets, icLibrary, icProfile;
    TextView tvHome, tvTickets, tvLibrary, tvProfile;

    // =========================================================
    // onCreate
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        // Bind views
        viewPager  = findViewById(R.id.viewPager);
        navHome    = findViewById(R.id.navHome);
        navTickets = findViewById(R.id.navTickets);
        navLibrary = findViewById(R.id.navChat);
        navProfile = findViewById(R.id.navProfile);
        icHome     = findViewById(R.id.icHome);
        icTickets  = findViewById(R.id.icTickets);
        icLibrary  = findViewById(R.id.icChat);
        icProfile  = findViewById(R.id.icProfile);
        tvHome     = navHome.findViewWithTag("tv");
        tvTickets  = navTickets.findViewWithTag("tv");
        tvLibrary  = navLibrary.findViewWithTag("tv");
        tvProfile  = navProfile.findViewWithTag("tv");

        // Notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        // ViewPager adapter
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 4; }
            @Override public Fragment createFragment(int pos) {
                switch (pos) {
                    case 1:  return new Ticket();
                    case 2:  return new ChatActivity();
                    case 3:  return new Profile();
                    default: return new Home();
                }
            }
        });

        // Sync bottom nav with swipe
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateNavUI(position);
            }
        });

        // Bottom nav clicks
        navHome.setOnClickListener(v    -> viewPager.setCurrentItem(0, true));
        navTickets.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        navLibrary.setOnClickListener(v -> viewPager.setCurrentItem(2, true));
        navProfile.setOnClickListener(v -> viewPager.setCurrentItem(3, true));

        // Default tab
        int openTab = getIntent().getIntExtra("openTab", 0);
        viewPager.setCurrentItem(openTab, false);
        updateNavUI(openTab);

        // Double back press to exit
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    private boolean doubleBackPressed = false;

                    @Override
                    public void handleOnBackPressed() {
                        if (viewPager.getCurrentItem() != 0) {
                            viewPager.setCurrentItem(0, true);
                        } else {
                            if (doubleBackPressed) {
                                setEnabled(false);
                                getOnBackPressedDispatcher().onBackPressed();
                            } else {
                                doubleBackPressed = true;
                                Toast.makeText(MainActivity.this,
                                        "Press back again to exit",
                                        Toast.LENGTH_SHORT).show();
                                new android.os.Handler().postDelayed(
                                        () -> doubleBackPressed = false, 2000);
                            }
                        }
                    }
                });
    }

    // =========================================================
    // onNewIntent — called when MainActivity is already running
    // e.g. coming from ViewAllTicket with openTab extra
    // =========================================================

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int openTab = intent.getIntExtra("openTab", -1);
        if (openTab != -1) {
            viewPager.setCurrentItem(openTab, false);
            updateNavUI(openTab);
        }
    }

    // =========================================================
    // Update bottom nav icon colors
    // =========================================================

    private void updateNavUI(int selected) {
        int navy = 0xFF1A2340;
        int gray = 0xFFAAAAAA;

        icHome.setColorFilter(gray);
        icTickets.setColorFilter(gray);
        icLibrary.setColorFilter(gray);
        icProfile.setColorFilter(gray);

        switch (selected) {
            case 0: icHome.setColorFilter(navy);    break;
            case 1: icTickets.setColorFilter(navy); break;
            case 2: icLibrary.setColorFilter(navy); break;
            case 3: icProfile.setColorFilter(navy); break;
        }
    }
}