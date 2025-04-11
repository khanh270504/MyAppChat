package com.example.chatapp.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.listeners.UserListener;
import com.example.chatapp.model.ChatItem;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatViewHolder> {
    private List<ChatItem> chatList;

    private final UserListener userListener;


    public ChatAdapter(List<ChatItem> chatList, UserListener userListener) {
        this.chatList = chatList;
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem chatItem = chatList.get(position);
        holder.userName.setText(chatItem.getUserName());
        holder.lastMessage.setText(chatItem.getLastMessage());
        holder.time.setText(chatItem.getTime());

        if (chatItem.getProfileImage() != null) {
            byte[] decodedImage = Base64.decode(chatItem.getProfileImage(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);
            holder.profileImage.setImageBitmap(bitmap);
        } else {
            holder.profileImage.setImageResource(R.drawable.logo);
        }
        holder.itemView.setOnClickListener(v -> {
            userListener.onUserClicked(chatItem);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }
}
