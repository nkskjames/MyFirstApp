package com.example.njames.myfirstapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;


import java.util.HashMap;
import java.util.Map;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FirebaseIntentService extends IntentService {
    private static final String TAG = "FirebaseIntentService";

    public FirebaseIntentService() {
        super("FirebaseIntentService");
    }

    public static void startActionThingInit(Context context,String thingName) {
        Intent intent = new Intent(context, FirebaseIntentService.class);
        intent.setAction(Constants.ACTION_THING_INIT);
        intent.putExtra(Constants.EXTRA_THING_ID, thingName);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (Constants.ACTION_THING_INIT.equals(action)) {
                handleActionThingInit(intent.getStringExtra(Constants.EXTRA_THING_ID));
            }
        }
    }

    private void handleActionThingInit(String thingName) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String tokenFirebase = FirebaseInstanceId.getInstance().getToken();
        String deviceId = MyFirebaseInstanceIDService.getDeviceId(getContentResolver());
        // Write a message to the database
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        Map<String, Object> childUpdates = new HashMap<>();
        String uid = mAuth.getCurrentUser().getUid();
        childUpdates.put("/users/" + uid + "/" + thingName +"/tokenId", tokenFirebase);
        database.updateChildren(childUpdates);

        Intent intent = new Intent(Constants.ACTION_THING_INIT)
                .putExtra(Constants.EXTRA_THING_ID,thingName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
