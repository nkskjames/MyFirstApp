package com.example.njames.myfirstapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;


import java.util.ArrayList;
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
    public static void startActionGetThings(Context context) {
        Intent intent = new Intent(context, FirebaseIntentService.class);
        intent.setAction(Constants.ACTION_GET_THINGS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (Constants.ACTION_THING_INIT.equals(action)) {
                handleActionThingInit(intent.getStringExtra(Constants.EXTRA_THING_ID));
            }
            if (Constants.ACTION_GET_THINGS.equals(action)) {
                handleActionGetThings();
            }
        }
    }
    private void handleActionThingInit(String thingName) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String tokenFirebase = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG,"Adding token: "+tokenFirebase);
        String deviceId = MyFirebaseInstanceIDService.getDeviceId(getContentResolver());
        // Write a message to the database
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        Map<String, Object> childUpdates = new HashMap<>();
        String uid = mAuth.getCurrentUser().getUid();
        ArrayList<Double> t = new ArrayList<Double>();
        t.add(new Double(0));
        t.add(new Double(0));
        t.add(new Double(0));

        ArrayList<String> td = new ArrayList<String>();
        td.add("Temp 1");
        td.add("Temp 2");
        td.add("Temp 3");
        ArrayList<Double> tu = new ArrayList<Double>();
        tu.add(new Double(1000));
        tu.add(new Double(1000));
        tu.add(new Double(1000));
        ArrayList<Double> tl = new ArrayList<Double>();
        tl.add(new Double(-10));
        tl.add(new Double(-10));
        tl.add(new Double(-10));

        childUpdates.put("/users/" + uid + "/" + thingName +"/tokenId", tokenFirebase);
        childUpdates.put("/users/" + uid + "/" + thingName +"/t", t);
        childUpdates.put("/users/" + uid + "/" + thingName +"/td", td);
        childUpdates.put("/users/" + uid + "/" + thingName +"/tu", tu);
        childUpdates.put("/users/" + uid + "/" + thingName +"/tl", tl);
        database.updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Intent intent = new Intent(Constants.ACTION_THING_INIT);
                //.putExtra(Constants.EXTRA_THING_ID,thingName);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        });
    }
    private void handleActionGetThings() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String uid = mAuth.getCurrentUser().getUid();
        String tokenFirebase = FirebaseInstanceId.getInstance().getToken();
        String deviceId = MyFirebaseInstanceIDService.getDeviceId(getContentResolver());
        // Write a message to the database
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        Query query = database.child("users").child(uid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot thing : dataSnapshot.getChildren()) {
                    Log.i(TAG,thing.getKey().toString());
                }
                Intent intent = new Intent(Constants.ACTION_GET_THINGS);
                //  .putExtra(Constants.EXTRA_THING_ID,thingName);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //Map<String, Object> childUpdates = new HashMap<>();
        //String uid = mAuth.getCurrentUser().getUid();
        //childUpdates.put("/users/" + uid + "/" + thingName +"/tokenId", tokenFirebase);
        //database.updateChildren(childUpdates);


    }
}
