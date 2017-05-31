package com.example.njames.myfirstapp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static java.security.AccessController.getContext;

/**
 * Created by njames on 5/20/17.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = "MyFirebaseIIDService";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG, "Refreshed token: " + refreshedToken);
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                Constants.PREFKEY_BBQ_AUTH, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.FIREBASE_TOKEN, refreshedToken);
        editor.commit();

        //sendRegistrationToServer(refreshedToken);
    }

    //private void sendRegistrationToServer(String token) {
    //    DynamoIntentService.startActionAddEndpoint(getApplicationContext(),getDeviceId(),token);
    //}
    public static String getDeviceId(ContentResolver content) {
        return Settings.Secure.getString(content, Settings.Secure.ANDROID_ID);
    }
}
