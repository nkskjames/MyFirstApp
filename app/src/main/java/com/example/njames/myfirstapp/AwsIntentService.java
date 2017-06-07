package com.example.njames.myfirstapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.firebase.iid.FirebaseInstanceId;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class AwsIntentService extends IntentService {
    private static final String TAG = "AwsIntentService";

    public AwsIntentService() {
        super("AwsIntentService");
    }

    public static void resetToken(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                Constants.PREFKEY_BBQ_AUTH, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.FIREBASE_TOKEN, "");
        editor.commit();
    }
    public static void startActionAddEndpoint(Context context) {
        Intent intent = new Intent(context, AwsIntentService.class);
        intent.setAction(Constants.ACTION_ADD_ENDPOINT);
//        intent.putExtra(EXTRA_DEVICE_ID, deviceId);
        context.startService(intent);
    }
    public static void startActionAwsLogin(Context context) {
        Intent intent = new Intent(context, AwsIntentService.class);
        intent.setAction(Constants.ACTION_AWS_LOGIN);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (Constants.ACTION_ADD_ENDPOINT.equals(action)) {
                handleActionAddEndpoint();
            }
            if (Constants.ACTION_AWS_LOGIN.equals(action)) {
                //handleActionAwsLogin();
                handleActionAddEndpoint();
            }
        }
    }
    public static CognitoCachingCredentialsProvider getCredentialProvider(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                Constants.PREFKEY_BBQ_AUTH, Context.MODE_PRIVATE);

        String loginAccount = sharedPref.getString(Constants.LOGIN_ACCOUNT,"");
        String loginToken = sharedPref.getString(Constants.LOGIN_TOKEN,"");

        if (loginAccount.isEmpty() || loginToken.isEmpty()) { return null; }

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context,    /* get the context for the application */
                Constants.COGNITO_POOL_ID,    /* Identity Pool ID */
                Regions.US_WEST_2           /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );
        Map<String, String> logins = new HashMap<String, String>();
        logins.put(loginAccount, loginToken);
        credentialsProvider.setLogins(logins);
        return credentialsProvider;
    }
    /*
    private void handleActionAwsLogin() {
        Log.i(TAG,"handleActionAwsLogin");
        SharedPreferences sharedPref = this.getApplicationContext().getSharedPreferences(
                Constants.PREFKEY_BBQ_AUTH, Context.MODE_PRIVATE);

        String loginAccount = sharedPref.getString(Constants.LOGIN_ACCOUNT,"");
        String loginToken = sharedPref.getString(Constants.LOGIN_TOKEN,"");

        if (loginAccount.isEmpty() || loginToken.isEmpty()) { return; }

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                this.getApplicationContext(),
                Constants.COGNITO_POOL_ID,
                Regions.US_WEST_2
        );
        credentialsProvider.clear();
        Map<String, String> logins = new HashMap<String, String>();
        logins.put(loginAccount, loginToken);
        credentialsProvider.setLogins(logins);
        credentialsProvider.refresh();

        Log.i(TAG,credentialsProvider.getCachedIdentityId());
        Intent localIntent = new Intent(Constants.ACTION_AWS_LOGIN)
                .putExtra(Constants.EXTRA_AWS_ID,credentialsProvider.getCachedIdentityId());
        Log.i(TAG,FirebaseInstanceId.getInstance().getToken());
        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
    */

    private void handleActionAddEndpoint() {
        //AwsIntentService.resetToken(getApplicationContext());
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                Constants.PREFKEY_BBQ_AUTH, Context.MODE_PRIVATE);
        String loginAccount = sharedPref.getString(Constants.LOGIN_ACCOUNT,"");
        String loginToken = sharedPref.getString(Constants.LOGIN_TOKEN,"");
        String firebaseToken = sharedPref.getString(Constants.FIREBASE_TOKEN,"");

        if (loginAccount.isEmpty() || loginToken.isEmpty()) { return; }
        String currentToken = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG,"Firebase token: "+currentToken);
        CognitoCachingCredentialsProvider credentialsProvider = AwsIntentService.getCredentialProvider(getApplicationContext());
        /*
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                Constants.COGNITO_POOL_ID,
                Regions.US_WEST_2
        );
        Map<String, String> logins = new HashMap<String, String>();
        logins.put(loginAccount, loginToken);
        credentialsProvider.setLogins(logins);
        */
        credentialsProvider.refresh();

        AmazonDynamoDBClient dynamodb = new AmazonDynamoDBClient(credentialsProvider);
        dynamodb.setRegion(Region.getRegion(Regions.US_WEST_2));

        DynamoDBMapper mapper = new DynamoDBMapper(dynamodb);

        DBUserRegistration user = new DBUserRegistration();
        user.setUser(credentialsProvider.getIdentityId());
        DynamoDBQueryExpression<DBUserRegistration> queryExpression =
                new DynamoDBQueryExpression<DBUserRegistration>()
                        .withHashKeyValues(user);

        List<DBUserRegistration> devices = mapper.query(DBUserRegistration.class, queryExpression);
        ArrayList<String> things = new ArrayList<String>();
        Set<String> thingSet = new HashSet<String>();
        for (DBUserRegistration device : devices) {
            Log.i(TAG, "Thing: "+device.getThingId());
            things.add(device.getThingId());
            thingSet.add(device.getThingId());
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(Constants.THINGS_LIST,thingSet);
        editor.commit();

        Intent localIntent = new Intent(Constants.ACTION_GET_THINGS)
                .putStringArrayListExtra(Constants.EXTRA_THING_LIST,things);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        if (firebaseToken.equals(currentToken)) {
            Log.i(TAG,"Token has not changed");
        } else {
            String deviceId = MyFirebaseInstanceIDService.getDeviceId(getContentResolver());
            Log.i(TAG, "Token has changed; Adding Endpoint");

            HashMap<String, AttributeValue> attributes = new HashMap<String, AttributeValue>();

            AttributeValue key = new AttributeValue();
            key.setS(credentialsProvider.getIdentityId());
            attributes.put("user_id", key);

            AttributeValue dkey = new AttributeValue();
            dkey.setS(deviceId);
            attributes.put("device_id", dkey);

            AttributeValue data = new AttributeValue();
            data.setS(currentToken);
            attributes.put("token", data);

            AttributeValue tkey = new AttributeValue();
            attributes.put("thing_id", tkey);

            for (String thing : things) {
                tkey.setS(thing);
                dynamodb.putItem("CreateEndpointRequest", attributes);
            }
            editor.putString(Constants.FIREBASE_TOKEN, currentToken);
            editor.commit();
        }

        Intent awsIntent = new Intent(Constants.ACTION_AWS_LOGIN)
                .putExtra(Constants.EXTRA_AWS_ID,credentialsProvider.getCachedIdentityId());
        LocalBroadcastManager.getInstance(this).sendBroadcast(awsIntent);
    }
}
