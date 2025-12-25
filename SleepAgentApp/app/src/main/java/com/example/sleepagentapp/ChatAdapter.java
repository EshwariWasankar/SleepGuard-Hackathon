package com.example.sleepagentapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_BOT = 2;
    private final List<Message> messages = new ArrayList<>();

    // Helper class for data
    static class Message {
        String text;
        boolean isUser;
        Message(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    public void addMessage(String text, boolean isUser) {
        messages.add(new Message(text, isUser));
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_user, parent, false);
            return new UserHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_bot, parent, false);
            return new BotHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        if (holder instanceof UserHolder) {
            ((UserHolder) holder).text.setText(msg.text);
        } else {
            ((BotHolder) holder).text.setText(msg.text);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class UserHolder extends RecyclerView.ViewHolder {
        TextView text;
        UserHolder(View v) { super(v); text = v.findViewById(R.id.text_message_body); }
    }
    static class BotHolder extends RecyclerView.ViewHolder {
        TextView text;
        BotHolder(View v) { super(v); text = v.findViewById(R.id.text_message_body); }
    }
}