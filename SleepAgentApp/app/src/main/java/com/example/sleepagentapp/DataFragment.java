package com.example.sleepagentapp;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import java.util.Calendar;
import java.util.List;

public class DataFragment extends Fragment {

    private CardView cardCaffeine, cardDebt, cardNoise, cardLight;
    private TextView valCaffeine, valDebt, valNoise, valLight;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data, container, false);

        cardCaffeine = view.findViewById(R.id.card_caffeine);
        cardDebt = view.findViewById(R.id.card_debt);
        cardNoise = view.findViewById(R.id.card_noise);
        cardLight = view.findViewById(R.id.card_light);

        valCaffeine = view.findViewById(R.id.txt_caffeine_val);
        valDebt = view.findViewById(R.id.txt_debt_val);
        valNoise = view.findViewById(R.id.txt_noise_val);
        valLight = view.findViewById(R.id.txt_light_val);

        // Set Listeners
        cardCaffeine.setOnClickListener(v -> showInputDialog("Caffeine Consumed (mg)", "caffeine_mg", valCaffeine, " mg"));
        cardDebt.setOnClickListener(v -> showInputDialog("Sleep Debt (hours)", "sleep_debt", valDebt, " hrs"));
        cardNoise.setOnClickListener(v -> showInputDialog("Noise Level (1-10)", "noise_level", valNoise, "/10"));
        cardLight.setOnClickListener(v -> handleScreenTimeSync());

        updateUI();
        return view;
    }

    private void showInputDialog(String title, String prefKey, TextView displayView, String suffix) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter value...");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String text = input.getText().toString();
            if (!text.isEmpty()) {
                float value = Float.parseFloat(text);
                saveToPrefs(prefKey, value);
                displayView.setText(value + suffix);

                // --- SMART AGENTIC TRIGGER ---
                // If Caffeine is updated, check if we need to Shift OR Reset the schedule
                if (prefKey.equals("caffeine_mg")) {

                    if (ScheduleFragment.instance != null && ScheduleFragment.instance.isAdded()) {

                        if (value > 350) {
                            // High Risk -> Shift Forward (New Method Name)
                            ScheduleFragment.instance.triggerHighCaffeineShift();
                            Toast.makeText(getContext(), "⚠️ High Caffeine! Schedule +30m.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Low Risk -> Reset / Undo (New Method Name)
                            ScheduleFragment.instance.triggerReset();
                            Toast.makeText(getContext(), "✅ Levels Safe. Schedule reset.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Fallback if tab is closed (The ScheduleFragment checks this itself on load)
                        if (value > 350) {
                            Toast.makeText(getContext(), "⚠️ High Caffeine! Schedule will adapt.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                // -----------------------------
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void handleScreenTimeSync() {
        if (!hasUsageStatsPermission()) {
            Toast.makeText(getContext(), "Grant 'Usage Access' to sync screen time.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } else {
            float screenTimeHours = calculateScreenTime();
            float blueLightRisk = Math.min(10, (screenTimeHours / 6.0f) * 10);
            saveToPrefs("light_level", blueLightRisk);
            valLight.setText(String.format("%.1f hrs (Synced)", screenTimeHours));
            Toast.makeText(getContext(), "Screen Time Synced!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasUsageStatsPermission() {
        if (getContext() == null) return false;
        AppOpsManager appOps = (AppOpsManager) getContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private float calculateScreenTime() {
        if (getContext() == null) return 0;
        UsageStatsManager usageStatsManager = (UsageStatsManager) getContext().getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        long startTime = calendar.getTimeInMillis();

        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        long totalTimeInMillis = 0;
        if (stats != null) {
            for (UsageStats usage : stats) {
                totalTimeInMillis += usage.getTotalTimeInForeground();
            }
        }
        return totalTimeInMillis / 1000f / 3600f;
    }

    private void saveToPrefs(String key, float value) {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("SleepData", Context.MODE_PRIVATE);
        prefs.edit().putFloat(key, value).apply();
    }

    private void updateUI() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("SleepData", Context.MODE_PRIVATE);
        valCaffeine.setText(prefs.getFloat("caffeine_mg", 0) + " mg");
        valDebt.setText(prefs.getFloat("sleep_debt", 0) + " hrs");
        valNoise.setText(prefs.getFloat("noise_level", 0) + "/10");
        if (hasUsageStatsPermission()) valLight.setText(String.format("%.1f hrs", calculateScreenTime()));
    }
}