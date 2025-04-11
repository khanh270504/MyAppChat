package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.chatapp.R;
import com.example.chatapp.adapter.MessageAdapter;
import com.example.chatapp.model.ChatItem;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.makeramen.roundedimageview.RoundedImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private ImageButton buttonBack, buttonSend;
    private RoundedImageView imageProfile;
    private TextView textName;
    private EditText inputMessage;
    private RecyclerView recyclerViewChat;
    private List<ChatMessage> chatMessages;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    String conversionId = null;
    private ChatItem chatItem;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        buttonBack = findViewById(R.id.buttonBack);
        buttonSend = findViewById(R.id.buttonSend);
        imageProfile = findViewById(R.id.imageProfile);
        textName = findViewById(R.id.textName);
        inputMessage = findViewById(R.id.inputMessage);
        recyclerViewChat = findViewById(R.id.recyclerViewChat);

        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        chatItem = (ChatItem) getIntent().getSerializableExtra(Constants.KEY_USER);
        textName.setText(chatItem.getUserName());
        String encodedImage = chatItem.getProfileImage();
        Bitmap receiverBitmap = null;
        if (encodedImage != null) {
            receiverBitmap = decodeImage(encodedImage);
            imageProfile.setImageBitmap(receiverBitmap);
        }
        messageAdapter = new MessageAdapter(chatMessages, receiverBitmap, preferenceManager.getString(Constants.KEY_USER_ID));
        recyclerViewChat.setAdapter(messageAdapter);
        db = FirebaseFirestore.getInstance();

        buttonBack.setOnClickListener(v -> {
            Intent i = new Intent(ChatActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });
        buttonSend.setOnClickListener(v -> sendMessage());

        loadMess();
        inputMessage.setOnClickListener(v -> sendMessage());
        listenerMessage();
    }

    private void loadMess() {
        textName.setText(chatItem.getUserName());
        String encodedImage = chatItem.getProfileImage();
        if (encodedImage != null) {
            Bitmap bitmap = decodeImage(encodedImage);
            imageProfile.setImageBitmap(bitmap);
        }
    }

    private Bitmap decodeImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void sendMessage() {
        if (inputMessage.getText().toString().trim().isEmpty()) {
            return;
        }

        String messageContent = inputMessage.getText().toString();
        inputMessage.setText(null);

        DocumentReference userRef = db.collection(Constants.KEY_USERS).document(chatItem.id);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            String receiverToken = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
//            Log.d("ChatActivity", "Receiver FCM Token: " + (receiverToken != null ? receiverToken : "null"));
//            if (receiverToken == null || receiverToken.isEmpty()) {
//                Log.w("ChatActivity", "Receiver token is null or empty");
//            } else {
//                Log.d("ChatActivity", "Receiver token retrieved successfully: " + receiverToken.length() + " characters");
//            }

            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_RECEIVER_ID, chatItem.id);
            message.put(Constants.KEY_MESSAGE, messageContent);
            message.put(Constants.KEY_TIMESTAMP, new Date());
            message.put(Constants.KEY_FCM_TOKEN, receiverToken);

            db.collection(Constants.KEY_COLLECTION_CHAT)
                    .add(message)
                    .addOnSuccessListener(documentReference -> {
                        if (receiverToken != null && !receiverToken.isEmpty()) {
                            sendNotification(receiverToken, messageContent);
                        }
//                        else {
//                            Log.w("TOKEN_NULL", "Người nhận chưa có token FCM");
//                        }
                    })
                    .addOnFailureListener(e -> Log.e("ChatActivity", "Failed to send message: " + e.getMessage()));

            if (conversionId != null) {
                updateConversion(messageContent);
            } else {
                HashMap<String, Object> conversion = new HashMap<>();
                conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                conversion.put(Constants.KEY_RECEIVER_ID, chatItem.id);
                conversion.put(Constants.KEY_RECEIVER_NAME, chatItem.userName);
                conversion.put(Constants.KEY_RECEIVER_IMAGE, chatItem.profileImage);
                conversion.put(Constants.KEY_LAST_MESSAGE, messageContent);
                conversion.put(Constants.KEY_TIMESTAMP, new Date());
                addConversion(conversion);
            }
        }).addOnFailureListener(e -> Log.e("ChatActivity", "Failed to get receiver token: " + e.getMessage()));
    }

    private void sendNotification(String receiverToken, String messageContent) {
        if (receiverToken == null || receiverToken.isEmpty()) {
            Log.e("ChatActivity", "Receiver FCM token is null or empty");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        String fcmUrl = "https://fcm.googleapis.com/v1/projects/chatapp-c6e64/messages:send";

        try {
            JSONObject notificationData = new JSONObject();
            JSONObject message = new JSONObject();
            JSONObject data = new JSONObject();
            JSONObject notification = new JSONObject();

            data.put(Constants.KEY_MESSAGE, messageContent);
            data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));

            notification.put("title", preferenceManager.getString(Constants.KEY_NAME));
            notification.put("body", messageContent);

            message.put("token", receiverToken);
            message.put("data", data);
            message.put("notification", notification);
            notificationData.put("message", message);

            Log.d("ChatActivity", "Notification JSON: " + notificationData.toString());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, fcmUrl, notificationData,
                    response -> Log.d("ChatActivity", "Notification sent successfully: " + response.toString()),
                    error -> {
                        Log.e("ChatActivity", "Failed to send notification: " + error.toString());
                        if (error.networkResponse != null) {
                            Log.e("ChatActivity", "Status Code: " + error.networkResponse.statusCode);
                            Log.e("ChatActivity", "Response Data: " + new String(error.networkResponse.data));
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    String accessToken = getAccessToken();
                    Log.d("ChatActivity", "Access Token: " + (accessToken != null ? accessToken.substring(0, 10) + "..." : "null"));
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + accessToken);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            queue.add(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e("ChatActivity", "Error creating notification payload: " + e.getMessage());
        }
    }

    private String getAccessToken() {
        try {
            InputStream serviceAccountStream = getAssets().open("chatapp-c6e64-firebase-adminsdk-fbsvc-f9964ccaa0.json");
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream)
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            Log.e("ChatActivity", "Failed to get access token: " + e.getMessage());
            return null;
        }
    }

    private String getReadDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, YYYY - hh:mm a", Locale.getDefault()).format(date);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int cnt = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.date = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, Comparator.comparing(obj -> obj.date));
            messageAdapter.notifyDataSetChanged();
            if (cnt == 0) {
                messageAdapter.notifyDataSetChanged();
            } else {
                messageAdapter.notifyItemRangeChanged(chatMessages.size(), chatMessages.size());
                recyclerViewChat.smoothScrollToPosition(chatMessages.size() - 1);
            }
        }
        if (conversionId == null) {
            checkForConversion();
        }
    };

    public void listenerMessage() {
        db = FirebaseFirestore.getInstance();
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        String receiverId = chatItem.id;
        db.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, userId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .addSnapshotListener(eventListener);
        db.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, userId)
                .addSnapshotListener(eventListener);
    }

    private void addConversion(HashMap<String, Object> conversion) {
        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversion() {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(preferenceManager.getString(Constants.KEY_USER_ID), chatItem.id);
            checkForConversionRemotely(chatItem.id, preferenceManager.getString(Constants.KEY_USER_ID));
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        conversionId = documentSnapshot.getId();
                    }
                });
    }
}