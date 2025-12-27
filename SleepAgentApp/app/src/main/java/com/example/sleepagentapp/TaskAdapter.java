package com.example.sleepagentapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private List<Task> tasks;

    public TaskAdapter(List<Task> tasks) { this.tasks = tasks; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Using a standard two-line layout for clarity
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);

        // Fix for faint text: Set to solid black and medium size
        holder.text1.setText(task.time + " — " + task.name);
        holder.text1.setTextColor(Color.BLACK);
        holder.text1.setTypeface(null, Typeface.BOLD);
        holder.text1.setTextSize(18f);

        // Subtext showing the category and autonomous adjustment status
        String info = "Category: " + task.category + (task.isAffectedByCaffeine ? " (⚡ Adjusted by Agent)" : "");
        holder.text2.setText(info);
        holder.text2.setTextColor(task.isAffectedByCaffeine ? Color.parseColor("#6200EE") : Color.DKGRAY);
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        ViewHolder(View v) {
            super(v);
            text1 = v.findViewById(android.R.id.text1);
            text2 = v.findViewById(android.R.id.text2);
        }
    }
}