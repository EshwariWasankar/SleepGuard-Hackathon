package com.example.sleepagentapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class ScheduleFragment extends Fragment {

    private ListView listView;
    private CardView alertCard;
    private TextView alertText;
    private EditText editTaskName, editTaskTime;
    private Button btnAddTask;
    private ArrayAdapter<String> adapter;

    public static ScheduleFragment instance;

    // Static so data survives tab switches
    private static List<Task> myTasks = new ArrayList<>();
    private static boolean isShifted = false;

    private List<String> displayList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        instance = this;

        listView = view.findViewById(R.id.list_tasks);
        alertCard = view.findViewById(R.id.card_alert);
        alertText = view.findViewById(R.id.text_alert_message);

        editTaskName = view.findViewById(R.id.edit_task_name);
        editTaskTime = view.findViewById(R.id.edit_task_time);
        btnAddTask = view.findViewById(R.id.btn_add_task);

        // 1. ADD TASK LOGIC
        btnAddTask.setOnClickListener(v -> {
            String name = editTaskName.getText().toString();
            String time = editTaskTime.getText().toString();

            if (!name.isEmpty() && !time.isEmpty()) {
                Task newTask = new Task(name, time);

                // If currently shifted, automatically shift new tasks too
                if (isShifted) {
                    modifyTaskTime(newTask, 30);
                    Toast.makeText(getContext(), "Auto-shifted (+30m)", Toast.LENGTH_SHORT).show();
                }

                myTasks.add(newTask);
                refreshList();
                editTaskName.setText("");
                editTaskTime.setText("");
            }
        });

        // 2. DELETE TASK LOGIC
        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            myTasks.remove(position);
            refreshList();
            return true;
        });

        // 3. CHECK STATE
        checkCaffeineAndSync();
        refreshList();

        return view;
    }

    private void checkCaffeineAndSync() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("SleepData", Context.MODE_PRIVATE);
        float caffeine = prefs.getFloat("caffeine_mg", 0);

        // Case A: Caffeine High, but NOT shifted yet -> SHIFT IT
        if (caffeine > 350 && !isShifted) {
            shiftAllTasks(30);
            isShifted = true;
        }
        // Case B: Caffeine Low, but IS shifted -> RESET IT
        else if (caffeine <= 350 && isShifted) {
            shiftAllTasks(-30); // Subtract 30 mins
            isShifted = false;
        }

        updateAlertVisibility();
    }

    private void updateAlertVisibility() {
        if (isShifted) {
            alertCard.setVisibility(View.VISIBLE);
            alertText.setText("High Caffeine: Schedule +30m added for recovery.");
        } else {
            alertCard.setVisibility(View.GONE);
        }
    }

    private void refreshList() {
        if (getContext() == null) return;

        displayList.clear();
        for (Task t : myTasks) {
            displayList.add(t.time + "   -   " + t.name);
        }

        if (adapter == null) {
            adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, displayList);
            listView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    // Accessible method for DataFragment to trigger High Caffeine
    public void triggerHighCaffeineShift() {
        if (!isShifted) {
            shiftAllTasks(30);
            isShifted = true;
            updateAlertVisibility();
        }
    }

    // Accessible method for DataFragment to trigger Reset
    public void triggerReset() {
        if (isShifted) {
            shiftAllTasks(-30);
            isShifted = false;
            updateAlertVisibility();
        }
    }

    private void shiftAllTasks(int minutesToAdd) {
        if (getContext() == null) return;
        for (Task t : myTasks) {
            modifyTaskTime(t, minutesToAdd);
        }
        refreshList();
    }

    private boolean modifyTaskTime(Task t, int minutesToAdd) {
        try {
            String timeClean = t.time.toLowerCase().replace("am", "").replace("pm", "").trim();
            String[] parts;
            if (timeClean.contains(":")) parts = timeClean.split(":");
            else if (timeClean.contains(".")) parts = timeClean.split("\\.");
            else return false;

            int hour = Integer.parseInt(parts[0].trim());
            int min = Integer.parseInt(parts[1].trim());

            // MATH
            min += minutesToAdd;

            // Handle overflow (adding time)
            while (min >= 60) {
                min -= 60;
                hour += 1;
            }
            // Handle underflow (subtracting time)
            while (min < 0) {
                min += 60;
                hour -= 1;
            }

            // Handle 12-hour wrap
            if (hour > 12) hour -= 12;
            if (hour <= 0) hour += 12;

            // Reconstruct
            String newMin = (min < 10) ? "0" + min : "" + min;
            String newHour = (hour < 10) ? "0" + hour : "" + hour;
            String suffix = t.time.toLowerCase().contains("pm") ? " PM" : " AM";

            t.time = newHour + ":" + newMin + suffix;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}