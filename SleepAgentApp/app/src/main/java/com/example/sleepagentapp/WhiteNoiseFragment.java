package com.example.sleepagentapp;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WhiteNoiseFragment extends Fragment {

    private MediaPlayer mediaPlayer;
    private MediaRecorder mediaRecorder;
    private AudioManager audioManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = false;

    // ALARM & AUTO-STOP VARIABLES
    private Calendar alarmTime = Calendar.getInstance();
    private boolean isAlarmSet = false;

    // UI Elements
    private Button btnPlay;
    private TextView txtDecibel, txtStatus, txtSoundName, txtAlarmTime;
    private ProgressBar volumeBar;
    private CardView soundCapsule;
    private View alarmContainer;

    // --- SOUND LIST ---
    private static class SoundOption {
        String name;
        int resId;
        SoundOption(String name, int resId) { this.name = name; this.resId = resId; }
    }
    private List<SoundOption> soundList = new ArrayList<>();
    private SoundOption currentSound = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_white_noise, container, false);

        // Bind UI
        btnPlay = view.findViewById(R.id.btn_play_noise);
        txtDecibel = view.findViewById(R.id.txt_db_level);
        txtStatus = view.findViewById(R.id.txt_status);
        txtSoundName = view.findViewById(R.id.text_sound_name);
        soundCapsule = view.findViewById(R.id.sound_capsule);
        volumeBar = view.findViewById(R.id.progress_volume);

        // Alarm UI
        txtAlarmTime = view.findViewById(R.id.alarm_time_display);
        alarmContainer = view.findViewById(R.id.alarm_container);

        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

        // 1. SETUP ALARM PICKER (New Feature)
        // Default to tomorrow 7 AM
        alarmTime.add(Calendar.DAY_OF_YEAR, 1);
        alarmTime.set(Calendar.HOUR_OF_DAY, 7);
        alarmTime.set(Calendar.MINUTE, 0);
        alarmTime.set(Calendar.SECOND, 0);

        alarmContainer.setOnClickListener(v -> showTimePicker());
        txtAlarmTime.setOnClickListener(v -> showTimePicker());

        // 2. LOAD SOUNDS
        loadSounds();
        if (!soundList.isEmpty()) {
            currentSound = soundList.get(0);
            txtSoundName.setText(currentSound.name);
        }

        // 3. LISTENERS
        soundCapsule.setOnClickListener(v -> showSoundMenu());
        btnPlay.setOnClickListener(v -> togglePlayback());

        return view;
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog picker = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {

            // Set Alarm Logic
            alarmTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            alarmTime.set(Calendar.MINUTE, minute);
            alarmTime.set(Calendar.SECOND, 0);

            // If user picks a time that passed today, assume they mean tomorrow
            if (alarmTime.before(now)) {
                alarmTime.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Update Text
            String amPm = (hourOfDay >= 12) ? "PM" : "AM";
            int hour12 = (hourOfDay > 12) ? hourOfDay - 12 : hourOfDay;
            if (hour12 == 0) hour12 = 12;
            String minStr = (minute < 10) ? "0" + minute : "" + minute;

            txtAlarmTime.setText(String.format(Locale.getDefault(), "%02d:%s %s", hour12, minStr, amPm));
            isAlarmSet = true;
            Toast.makeText(getContext(), "Noise will stop automatically at " + hour12 + ":" + minStr + " " + amPm, Toast.LENGTH_SHORT).show();

        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);

        picker.show();
    }

    private void loadSounds() {
        soundList.clear();
        try {
            soundList.add(new SoundOption("Ocean Waves", R.raw.ocean));
            soundList.add(new SoundOption("Heavy Rain", R.raw.rain));
            soundList.add(new SoundOption("Forest Stream", R.raw.stream));
            soundList.add(new SoundOption("Waterfall", R.raw.waterfall));
        } catch (Exception e) {}

        if (soundList.isEmpty()) soundList.add(new SoundOption("Default", R.raw.ocean));
    }

    private void showSoundMenu() {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), soundCapsule);
        for (int i = 0; i < soundList.size(); i++) popup.getMenu().add(0, i, i, soundList.get(i).name);

        popup.setOnMenuItemClickListener(item -> {
            currentSound = soundList.get(item.getItemId());
            txtSoundName.setText(currentSound.name);
            if (isPlaying) { stopNoise(); startNoise(); }
            return true;
        });
        popup.show();
    }

    private void togglePlayback() {
        if (isPlaying) {
            stopNoise();
        } else {
            // Try to get Mic Permission, but DON'T stop playback if denied
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                // We proceed to play anyway (non-adaptive mode)
            }
            startNoise();
        }
    }

    private void startNoise() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(getContext(), currentSound.resId);
                mediaPlayer.setLooping(true);
            }
            mediaPlayer.start();
            isPlaying = true;
            btnPlay.setText("STOP");
            btnPlay.setBackgroundColor(0xFFFF5252); // Red

            // Start the "Brain" loop
            startSmartLoop();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Playback Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopNoise() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            isPlaying = false;
            btnPlay.setText("PLAY SMART NOISE");
            btnPlay.setBackgroundColor(0xFF6200EE);

            stopSmartLoop();
            txtStatus.setText("Status: Idle");
            txtDecibel.setText("0 dB");
            volumeBar.setProgress(0);
        } catch (Exception e) {}
    }

    // --- SMART LOOP: Handles Alarm Check AND Adaptive Volume ---
    private void startSmartLoop() {
        // Try to start mic
        if (mediaRecorder == null) {
            try {
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.setOutputFile("/dev/null");
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (Exception e) {
                // FALLBACK: If mic fails (emulator), just log it and continue without mic
                txtStatus.setText("Status: Manual Mode (Mic Unavailable)");
                mediaRecorder = null;
            }
        }
        handler.post(smartRunnable);
    }

    private void stopSmartLoop() {
        handler.removeCallbacks(smartRunnable);
        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); mediaRecorder.release(); } catch (Exception e) {}
            mediaRecorder = null;
        }
    }

    private Runnable smartRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying) return;

            // 1. CHECK ALARM (Auto-Stop Feature)
            if (isAlarmSet) {
                Calendar now = Calendar.getInstance();
                if (now.after(alarmTime)) {
                    stopNoise();
                    Toast.makeText(getContext(), "⏰ Alarm time reached. Noise stopped.", Toast.LENGTH_LONG).show();
                    isAlarmSet = false; // Reset
                    return; // Stop the loop
                }
            }

            // 2. CHECK MIC (Adaptive Volume Feature)
            if (mediaRecorder != null) {
                try {
                    double amplitude = mediaRecorder.getMaxAmplitude();
                    if (amplitude > 0) {
                        double db = 20 * Math.log10(amplitude);

                        // Adaptive Logic
                        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int targetVol;
                        if (db < 40) targetVol = (int)(maxVol * 0.3);
                        else if (db < 60) targetVol = (int)(maxVol * 0.6);
                        else targetVol = maxVol;

                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0);

                        // UI Updates
                        txtDecibel.setText(String.format("%.0f dB", db));
                        txtStatus.setText(db > 60 ? "Status: 🛡️ Blocking Noise" : "Status: 🌙 Gentle Masking");
                        volumeBar.setProgress((int)db);
                    }
                } catch (Exception e) {
                    // Ignore transient errors
                }
            }

            handler.postDelayed(this, 1000); // Check every 1 second
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopNoise();
    }
}