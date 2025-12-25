package com.example.sleepagentapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BlobView extends View {

    private Paint paint;
    private Path path;
    private long startTime;
    private Random random = new Random();

    // The List of Active Spikes
    private List<Hill> hills = new ArrayList<>();

    // Logic to track the "Main" active hill
    private Hill currentActiveHill = null;
    private float lastAmplitude = 0f;

    // Configuration
    private static final int POINT_COUNT = 30;
    private static final float WOBBLE_SPEED = 0.002f;
    private static final float MAX_SPIKE_HEIGHT = 300f;

    public BlobView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.parseColor("#7E57C2")); // Deep Purple
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        path = new Path();
        startTime = System.currentTimeMillis();
    }

    public void updateAmplitude(float newAmplitude) {
        // Noise Gate
        if (newAmplitude < 0.05f) {
            newAmplitude = 0f;
        }

        // 1. Calculate the "Kick"
        float delta = newAmplitude - lastAmplitude;

        // 2. Decide: New Spike or Sustain?
        boolean isSharpAttack = delta > 0.15f;

        if (isSharpAttack) {
            spawnNewHill(newAmplitude);
        }
        else if (currentActiveHill != null && !currentActiveHill.isDead()) {
            // SUSTAIN: Keep the current hill moving with the voice
            currentActiveHill.targetHeight = newAmplitude * MAX_SPIKE_HEIGHT;
        }
        else if (newAmplitude > 0.05f) {
            // Start of a new sound (from silence)
            spawnNewHill(newAmplitude);
        }

        lastAmplitude = newAmplitude;
    }

    private void spawnNewHill(float amplitude) {
        // --- THE FIX: RETIRE THE OLD HILL ---
        // Before we switch to a new hill, tell the old one to go down.
        if (currentActiveHill != null) {
            currentActiveHill.targetHeight = 0f; // "Retire"
        }
        // ------------------------------------

        int randomSpot = random.nextInt(POINT_COUNT);
        float height = amplitude * MAX_SPIKE_HEIGHT;

        Hill newHill = new Hill(randomSpot, height);
        hills.add(newHill);

        currentActiveHill = newHill;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float baseRadius = Math.min(centerX, centerY) * 0.45f;

        path.reset();
        long time = System.currentTimeMillis() - startTime;
        float[] totalOffsets = new float[POINT_COUNT];

        // Physics Loop
        Iterator<Hill> iterator = hills.iterator();
        while (iterator.hasNext()) {
            Hill hill = iterator.next();
            hill.update();

            // Add hill height to the total shape
            for (int i = 0; i < POINT_COUNT; i++) {
                float distance = Math.abs(i - hill.centerIndex);
                if (distance > POINT_COUNT / 2f) {
                    distance = POINT_COUNT - distance;
                }
                if (distance < 5) {
                    float shape = (5 - distance) / 5f;
                    shape = shape * shape;
                    totalOffsets[i] += hill.currentHeight * shape;
                }
            }

            // Cleanup
            if (hill.isDead()) {
                iterator.remove();
                if (hill == currentActiveHill) {
                    currentActiveHill = null;
                }
            }
        }

        // Draw Blob
        float[] xPoints = new float[POINT_COUNT];
        float[] yPoints = new float[POINT_COUNT];

        for (int i = 0; i < POINT_COUNT; i++) {
            double angle = (2.0 * Math.PI / POINT_COUNT) * i;
            float noiseOffset = (float) Math.sin((time * WOBBLE_SPEED) + (i * 0.8)) * 15;
            float radius = baseRadius + noiseOffset + totalOffsets[i];

            xPoints[i] = centerX + (float) (Math.cos(angle) * radius);
            yPoints[i] = centerY + (float) (Math.sin(angle) * radius);
        }

        path.moveTo((xPoints[0] + xPoints[POINT_COUNT - 1]) / 2, (yPoints[0] + yPoints[POINT_COUNT - 1]) / 2);
        for (int i = 0; i < POINT_COUNT; i++) {
            float thisX = xPoints[i];
            float thisY = yPoints[i];
            int nextI = (i + 1) % POINT_COUNT;
            float midX = (thisX + xPoints[nextI]) / 2;
            float midY = (thisY + yPoints[nextI]) / 2;
            path.quadTo(thisX, thisY, midX, midY);
        }

        path.close();
        canvas.drawPath(path, paint);
        invalidate();
    }

    // --- PHYSICS CLASS ---
    private static class Hill {
        int centerIndex;
        float targetHeight;
        float currentHeight;

        Hill(int index, float target) {
            this.centerIndex = index;
            this.targetHeight = target;
            this.currentHeight = 0f;
        }

        void update() {
            if (currentHeight < targetHeight) {
                // Grow
                currentHeight += (targetHeight - currentHeight) * 0.2f;
            } else {
                // Shrink
                currentHeight += (targetHeight - currentHeight) * 0.1f;
            }
        }

        boolean isDead() {
            return currentHeight < 1f && targetHeight < 1f;
        }
    }
}