package com.example.sleepagentapp;

import java.util.List;

public class CaffeineMath {
    private static final double HALF_LIFE_HOURS = 5.7;

    public static float calculateActiveCaffeine(List<CaffeineEntry> history) {
        float totalActive = 0;
        long now = System.currentTimeMillis();

        for (CaffeineEntry entry : history) {
            double hoursElapsed = (now - entry.timestamp) / (1000.0 * 60 * 60);
            double remaining = entry.mg * Math.pow(0.5, (hoursElapsed / HALF_LIFE_HOURS));
            if (remaining > 1.0) {
                totalActive += remaining;
            }
        }
        return totalActive;
    }
}