package com.example.chatapp.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.listeners.UserListener;
import com.example.chatapp.model.ChatMessage;
import com.makeramen.roundedimageview.RoundedImageView;

public class ChatViewHolder extends RecyclerView.ViewHolder {

    RoundedImageView profileImage;
    TextView userName, lastMessage, time;

    public ChatViewHolder(@NonNull View itemView) {
        super(itemView);
        profileImage = itemView.findViewById(R.id.profileImage);
        userName = itemView.findViewById(R.id.userName);
        lastMessage = itemView.findViewById(R.id.lastMessage);
        time = itemView.findViewById(R.id.time);

    }

}
