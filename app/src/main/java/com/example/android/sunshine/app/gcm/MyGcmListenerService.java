package com.example.android.sunshine.app.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.android.sunshine.app.MainActivity;
import com.example.android.sunshine.app.R;
import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by safwanx on 2/20/17.
 */

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_WEATHER = "weather";
    private static final String EXTRA_LOCATION = "location";

    public static final int NOTIFICATION_ID = 1;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]

    @Override
    public void onMessageReceived(String from, Bundle data) {
        super.onMessageReceived(from, data);

        // Let's un-parcel the bundle
        if (data != null && !data.isEmpty()) {
            String senderId = getString(R.string.gcm_defaultSenderId);

            if (senderId != null && senderId.length() == 0) {
                Toast.makeText(this, "Sender ID string needs to be set.", Toast.LENGTH_LONG).show();
            }

            // Message is coming from our server
            if (senderId.equals(from)) {
                // Process message and then post a notification of the received message.
                String weather = data.getString(EXTRA_WEATHER);
                String location = data.getString(EXTRA_LOCATION);

                String alert = String.format(getString(R.string.gcm_weather_alert),
                        weather,
                        location);

                sendNotification(alert);
            }

            Log.i(TAG, "Received: " + data.toString());
        }
    }

    /**
     * Put the message into a notification and post it.
     * This is just one simple example of what you might choose to do with a GCM message.
     *
     * @param message The alert message to be posted.
     */
    private void sendNotification(String message) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        // Notifications using both a large and a small icon (which yours should!) need the large
        // icon as a bitmap. So we need to create that here from the resource ID, and pass the
        // object along in our notification builder. Generally, you want to use the app icon as the
        // small icon, so that users understand what app is triggering this notification.

        Bitmap largeIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.art_storm);

        // Create the Notification using NotificationCompat.builder.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.art_clear)
                .setLargeIcon(largeIcon)
                .setContentTitle("Weather Alert!")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message)
                .setPriority(Notification.PRIORITY_HIGH);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
