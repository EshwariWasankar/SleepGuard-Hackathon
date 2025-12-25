package com.example.sleepagentapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private CardView globalHeader;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        globalHeader = findViewById(R.id.global_header);
        bottomNav = findViewById(R.id.bottom_navigation);
        ImageView playBtn = findViewById(R.id.btn_quick_play);

        // Set the listener for tab clicks
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Quick Play Button logic: Jump to White Noise Tab
        playBtn.setOnClickListener(v -> {
            bottomNav.setSelectedItemId(R.id.nav_white_noise);
        });

        // Load default fragment (Agent) if first time opening
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new AgentFragment()).commit();
        }
    }

    // Helper to hide tabs for Focus Mode (Called by WhiteNoiseFragment)
    public void setFocusMode(boolean enabled) {
        if (enabled) {
            bottomNav.setVisibility(View.GONE);
            globalHeader.setVisibility(View.GONE); // Hide top island too
        } else {
            bottomNav.setVisibility(View.VISIBLE);
            // We don't force globalHeader visible here because navListener handles it
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    int itemId = item.getItemId();

                    // LOGIC: Hide Header ONLY on White Noise Tab
                    if (itemId == R.id.nav_white_noise) {
                        globalHeader.setVisibility(View.GONE); // Hide it
                        selectedFragment = new WhiteNoiseFragment();
                    }
                    else {
                        globalHeader.setVisibility(View.VISIBLE); // Show it for all others

                        if (itemId == R.id.nav_agent) {
                            selectedFragment = new AgentFragment();
                        } else if (itemId == R.id.nav_data) {
                            selectedFragment = new DataFragment();
                        } else if (itemId == R.id.nav_schedule) { // --- NEW SCHEDULE TAB ---
                            selectedFragment = new ScheduleFragment();
                        } else if (itemId == R.id.nav_profile) {
                            selectedFragment = new ProfileFragment();
                        }
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                    }
                    return true;
                }
            };
}