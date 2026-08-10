package com.studenthub.app.adapter;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.studenthub.app.R;
import com.studenthub.app.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that displays chat messages in a RecyclerView, showing the
 * current device's own messages on the right (blue bubble) and every
 * other message — including ones sent from the Web App — on the left
 * (white bubble).
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messageList = new ArrayList<>();
    private final String currentSenderId;

    public MessageAdapter(String currentSenderId) {
        this.currentSenderId = currentSenderId;
    }

    /** Replaces the full message list (called every time Firebase fires a new snapshot). */
    public void setMessages(List<ChatMessage> newMessages) {
        messageList.clear();
        messageList.addAll(newMessages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        boolean isMine = currentSenderId != null && currentSenderId.equals(message.getSenderId());
        return isMine ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        String time = message.getTimestamp() != null
                ? DateFormat.format("hh:mm a", message.getTimestamp()).toString()
                : "";

        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;
            h.body.setText(message.getText());
            h.time.setText(time);
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            h.body.setText(message.getText());
            h.time.setText(time);
            h.sender.setText(message.getSenderName() != null ? message.getSenderName() : "Unknown");
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView body, time;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.textMessageBody);
            time = itemView.findViewById(R.id.textTimestamp);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView body, time, sender;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            body = itemView.findViewById(R.id.textMessageBody);
            time = itemView.findViewById(R.id.textTimestamp);
            sender = itemView.findViewById(R.id.textSenderName);
        }
    }
}
