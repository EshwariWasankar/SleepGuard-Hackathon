package com.example.sleepagentapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentFragment extends Fragment {

    private RecyclerView chatRecycler;
    private ChatAdapter adapter;
    private EditText chatInput;
    private Button sendButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agent, container, false);

        // 1. SETUP UI
        chatRecycler = view.findViewById(R.id.recycler_chat);
        chatInput = view.findViewById(R.id.edit_chat_input);
        sendButton = view.findViewById(R.id.btn_send_chat);

        adapter = new ChatAdapter();
        chatRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecycler.setAdapter(adapter);

        // Initial Greeting
        adapter.addMessage("Hello! I'm SleepGuard. I see your latest data. How can I help?", false);

        // 2. START PYTHON ENGINE
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(getContext()));
        }

        sendButton.setOnClickListener(v -> sendMessage());

        return view;
    }

    private void sendMessage() {
        String msg = chatInput.getText().toString().trim();
        if (msg.isEmpty()) return;

        // Add User Message to Screen
        adapter.addMessage(msg, true);
        chatInput.setText("");
        scrollToBottom();

        // 3. RUN THE AGENT (Background Thread)
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {

            String replyText;

            try {
                // A. GET DATA (From the Shared Notebook)
                SharedPreferences prefs = getContext().getSharedPreferences("SleepData", Context.MODE_PRIVATE);

                // Get values (defaulting to 0 if empty)
                double caffeine = prefs.getFloat("caffeine_mg", 0);
                double sleepDebt = prefs.getFloat("sleep_debt", 0);

                // Normalize sliders (0-10 -> 0.0-1.0) for the AI
                double noise = prefs.getFloat("noise_level", 0) / 10.0;
                double light = prefs.getFloat("light_level", 0) / 10.0;

                // B. PREPARE PYTHON ENGINE
                Python py = Python.getInstance();
                PyObject launcherModule = py.getModule("launcher");
                PyObject builtins = py.getModule("builtins");

                // --- THE FIX: MANUAL DICTIONARY CONSTRUCTION ---
                // Instead of passing a Java Map, we create a pure Python Dictionary directly.
                PyObject riskBundle = builtins.callAttr("dict");

                // We use Python's "__setitem__" to safely put data inside.
                // This guarantees the result is a Python Dict, not a Java HashMap.
                riskBundle.callAttr("__setitem__", "remaining_caffeine_mg", caffeine);
                riskBundle.callAttr("__setitem__", "sleep_debt_hours", sleepDebt);
                riskBundle.callAttr("__setitem__", "noise_disruption_risk", noise);
                riskBundle.callAttr("__setitem__", "melatonin_suppression_risk", light);
                riskBundle.callAttr("__setitem__", "user_query", msg);

                // C. CALL THE AGENT (Now passing a real Python Object)
                PyObject result = launcherModule.callAttr("run_agent_bridge", riskBundle);

                // D. PROCESS RESPONSE
                if (result != null) {
                    PyObject actionObj = result.callAttr("get", "action");
                    PyObject reasonObj = result.callAttr("get", "reasoning_summary");

                    String action = (actionObj != null) ? actionObj.toString() : "Analyzing...";
                    String reason = (reasonObj != null) ? reasonObj.toString() : "";

                    replyText = "Action: " + action + "\n\n" + reason;
                } else {
                    replyText = "The agent returned no data.";
                }

            } catch (Exception e) {
                e.printStackTrace();
                replyText = "Error: " + e.getMessage();
            }

            // E. UPDATE UI (Main Thread)
            String finalReply = replyText;
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.addMessage(finalReply, false);
                scrollToBottom();
            });
        });
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            chatRecycler.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }
}