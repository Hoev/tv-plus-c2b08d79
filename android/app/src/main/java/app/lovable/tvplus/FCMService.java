package app.lovable.tvplus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Firebase Cloud Messaging Service for receiving push notifications
 */
public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "tvplus_notifications";
    private static final String CHANNEL_NAME = "TV Plus Notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token refreshed: " + token);
        // Token will be sent to server via WebView JavaScript
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d(TAG, "Message received from: " + message.getFrom());

        // Check for notification payload
        if (message.getNotification() != null) {
            String title = message.getNotification().getTitle();
            String body = message.getNotification().getBody();
            Map<String, String> data = message.getData();
            
            showNotification(title, body, data);
        }
        
        // Check for data payload
        if (!message.getData().isEmpty()) {
            handleDataMessage(message.getData());
        }
    }

    private void showNotification(String title, String body, Map<String, String> data) {
        createNotificationChannel();

        // Create intent for notification tap
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Add deep link data if present
        if (data != null) {
            if (data.containsKey("action")) {
                intent.putExtra("action", data.get("action"));
            }
            if (data.containsKey("channelId")) {
                intent.putExtra("channelId", data.get("channelId"));
            }
            if (data.containsKey("url")) {
                intent.putExtra("url", data.get("url"));
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title != null ? title : "TV Plus")
            .setContentText(body != null ? body : "")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        Log.d(TAG, "Data message: " + data.toString());
        
        String action = data.get("action");
        if (action != null) {
            switch (action) {
                case "open_channel":
                    // Handle open channel action
                    String channelId = data.get("channelId");
                    Log.d(TAG, "Open channel: " + channelId);
                    break;
                    
                case "open_url":
                    // Handle open URL action
                    String url = data.get("url");
                    Log.d(TAG, "Open URL: " + url);
                    break;
                    
                default:
                    Log.d(TAG, "Unknown action: " + action);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("TV Plus app notifications");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
