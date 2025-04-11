package com.example.chatapp.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.adapter.ChatAdapter;
import com.example.chatapp.listeners.UserListener;
import com.example.chatapp.model.ChatItem;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements UserListener {

    private Toolbar topAppBar;
    private EditText searchEditText;
    private RecyclerView chatRecyclerView;
    private BottomNavigationView bottomNavigation;
    private ChatAdapter adapter;
    private List<ChatItem> chatList;
    private List<ChatItem> originalChatList;
    FirebaseFirestore db;
    PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferenceManager = new PreferenceManager(getApplicationContext());


        topAppBar = findViewById(R.id.topAppBar);
        searchEditText = topAppBar.findViewById(R.id.searchEditText);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        bottomNavigation = findViewById(R.id.bottomNavigation);


        getToken();

        chatList = new ArrayList<>();
       originalChatList = new ArrayList<>();
        getDataUser();

        adapter = new ChatAdapter(chatList, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);
       bottomNavigation.setSelectedItemId(R.id.nav_message);


        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_message) {
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });


        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                filterChatList(s.toString());
            }
        });
    }

    public void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection(Constants.KEY_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
            userRef.update(Constants.KEY_FCM_TOKEN, token)
                    .addOnSuccessListener(unused -> {
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);

                    });
          //  Log.d("FCM_TOKEN", "Token mới sau login: " + token);
        });

    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getDataUser() {
        db = FirebaseFirestore.getInstance();
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);


        db.collection(Constants.KEY_USERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    chatList.clear();
                    originalChatList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getId();

                        if (userId.equals(currentUserId)) {
                            continue;
                        }

                        String userName = document.getString(Constants.KEY_NAME);
                        String profileImage = document.getString(Constants.KEY_IMAGE);
                        ChatItem chatItem = new ChatItem(userId, userName, "Start chatting...", "N/A", profileImage);
                        chatList.add(chatItem);
                        originalChatList.add(chatItem);


                        listenToConversations(currentUserId, userId, chatItem);
                    }

                    if (chatList.isEmpty()) {
                        showToast("No users found");
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> showToast("Failed to load users: " + e.getMessage()));
    }

    private void listenToConversations(String currentUserId, String otherUserId, ChatItem chatItem) {

        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, currentUserId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, otherUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("DEBUG_CHAT", "lỗi conversation: " + error.getMessage());
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        QueryDocumentSnapshot conversationDoc = (QueryDocumentSnapshot) value.getDocuments().get(0);
                        String lastMessage = conversationDoc.getString(Constants.KEY_LAST_MESSAGE);
                        Date timestamp = conversationDoc.getDate(Constants.KEY_TIMESTAMP);

                        chatItem.setLastMessage(lastMessage != null ? lastMessage : "Start chatting...");
                        chatItem.setTime(formatTime(timestamp));
                        adapter.notifyDataSetChanged();
                    }
                });


        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, otherUserId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("DEBUG_CHAT", "lỗi conversion: " + error.getMessage());
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        QueryDocumentSnapshot reverseDoc = (QueryDocumentSnapshot) value.getDocuments().get(0);
                        String lastMessage = reverseDoc.getString(Constants.KEY_LAST_MESSAGE);
                        Date timestamp = reverseDoc.getDate(Constants.KEY_TIMESTAMP);

                        chatItem.setLastMessage(lastMessage != null ? lastMessage : "Start chatting...");
                        chatItem.setTime(formatTime(timestamp));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private String formatTime(Date date) {
        if (date == null) {
            return "N/A";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(date);
    }


    private void filterChatList(String query) {
        chatList.clear();
        if (query.isEmpty()) {
            chatList.addAll(originalChatList);
        } else {
            for (ChatItem chatItem : originalChatList) {
                if (chatItem.getUserName().toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) {
                    chatList.add(chatItem);
                }
            }
        }
        adapter.notifyDataSetChanged();
        if (chatList.isEmpty()) {
            showToast("Không tìm thấy User");
        }
    }

    @Override
    public void onUserClicked(ChatItem chatItem) {
        Intent i = new Intent(getApplicationContext(), ChatActivity.class);
        i.putExtra(Constants.KEY_USER, chatItem);
        startActivity(i);
        finish();
    }

}