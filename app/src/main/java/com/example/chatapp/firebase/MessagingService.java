package com.example.chatapp.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.chatapp.activities.ChatActivity;
import com.example.chatapp.model.ChatItem;
import com.example.chatapp.utilities.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {

        super.onNewToken(token);
//        Log.d("FCM", "onNewToken called");
//        if (token != null && !token.isEmpty()) {
//            Log.d("FCM", "Token: " + token);
//        } else {
//            Log.d("FCM", "Token is null or empty!");
//        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        if (message.getData() == null || message.getData().isEmpty()) {
        //    Log.e("FCM", "Message data is null or empty");
            return;
        }


        ChatItem chatItem = new ChatItem();
        chatItem.id = message.getData().get(Constants.KEY_USER_ID);
        chatItem.userName = message.getData().get(Constants.KEY_NAME);
        chatItem.token = message.getData().get(Constants.KEY_FCM_TOKEN);


        String content = message.getData().get("message");
        if (content == null || content.isEmpty()) {
            content = "Bạn có tin nhắn mới";
        }


        int notificationId = chatItem.id != null ? chatItem.id.hashCode() : new Random().nextInt();
        String channelId = "chat_message";


        Intent intent = new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.KEY_USER, chatItem);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );


        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(chatItem.userName)
                .setContentText(content)
                .setSound(defaultSoundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Tin nhắn mới",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo khi có tin nhắn mới");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }


        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
            Log.d("FCM", "Notification displayed for user: " + chatItem.userName);
        }
    }

}
