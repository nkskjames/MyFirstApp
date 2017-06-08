package com.example.njames.myfirstapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

import static android.content.ContentValues.TAG;

public class MyMessagingService extends FirebaseMessagingService {
    public MyMessagingService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // ...
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG,"To: "+refreshedToken);
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message: " + remoteMessage.getData());
            try {
                //TODO: make sure message is formatted properly
                String jsonStr = remoteMessage.getData().toString();
                jsonStr = jsonStr.replace('=',':');
                JSONObject json = new JSONObject(jsonStr);
                String command = json.getString("command");
                if (command.equals("signup_done")) {
                    Intent intent = new Intent(Constants.ACTION_SIGNUP_DONE);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                } else if(command.equals("data")){
                    JSONArray ta = json.getJSONArray("t");
                    JSONArray tda = json.getJSONArray("td");
                    JSONArray tla = json.getJSONArray("tl");
                    JSONArray tua = json.getJSONArray("tu");

                    int t[] = new int[3];
                    int tu[] = new int[3];
                    int tl[] = new int[3];
                    String td[] = new String[3];
                    for (int i = 0; i < 3; i++) {
                        t[i] = ta.getInt(i);
                        tu[i] = tua.getInt(i);
                        tl[i] = tla.getInt(i);
                        td[i] = tda.getString(i);
                    }
                    Intent intent = new Intent(Constants.ACTION_RECEIVE_DATA);
                    intent.putExtra("t", t);
                    intent.putExtra("td", td);
                    intent.putExtra("tu", tu);
                    intent.putExtra("tl", tl);
                    intent.putExtra("thingName", json.getString("thingName"));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                } else {
                    Log.e(TAG,"Unknown command: "+command);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG,e.getMessage());
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    private void sendNotification(String messageTitle,String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long[] pattern = {500, 500, 500, 500, 500};

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setVibrate(pattern)
                .setLights(Color.BLUE, 1, 1)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
