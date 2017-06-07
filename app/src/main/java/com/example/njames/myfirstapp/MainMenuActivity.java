package com.example.njames.myfirstapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.util.ArrayList;
import java.util.Set;

public class MainMenuActivity extends AppCompatActivity implements
        View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainMenuActivity";
    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private Spinner list;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //ArrayList<String> things =
            //        intent.getStringArrayListExtra(Constants.EXTRA_THING_LIST);

            //ArrayAdapter<String> thingAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,things);
            //list.setAdapter(thingAdapter);
            updateList();
            Log.i(TAG,"updating thing list");
        }
    };
    IntentFilter mIntentFilter=new IntentFilter(Constants.ACTION_GET_THINGS);

    private void updateList() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                Constants.PREFKEY_BBQ_AUTH, Context.MODE_PRIVATE);
        Set<String> thingSet = sharedPref.getStringSet(Constants.THINGS_LIST,null);
        ArrayList<String> things = new ArrayList<String>();
        if (thingSet != null) {
            for (String thing: thingSet) {
                things.add(thing);
            }
        }
        ArrayAdapter<String> thingAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,things);
        list.setAdapter(thingAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        findViewById(R.id.device_setup_button).setOnClickListener(this);
        findViewById(R.id.refresh_button).setOnClickListener(this);
        findViewById(R.id.monitor_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);

        list = (Spinner)findViewById(R.id.thing_list);
        updateList();
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("786224253129-skol2unf18uccd44st372lv8uro77ci7.apps.googleusercontent.com")
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG,"selected back");
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG,"selected");
        if (item.getItemId() == android.R.id.home) {
            Log.i(TAG,"go home");
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            finish();
                        }
                    });
        }
        //return super.onOptionsItemSelected(item);
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.device_setup_button:
                Intent setupActivity = new Intent(this, ThingSetupActivity.class);
                startActivity(setupActivity);
                break;
            case R.id.refresh_button:
                AwsIntentService.startActionAddEndpoint(getApplicationContext());
                break;
            case R.id.monitor_button:
                String thing = (String) list.getSelectedItem();
                if (thing != null) {
                    Intent monitorActivity = new Intent(this, MonitorActivity.class);
                    monitorActivity.putExtra(Constants.EXTRA_THING_ID, (String) list.getSelectedItem());
                    startActivity(monitorActivity);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please select a device", Toast.LENGTH_LONG)
                            .show();
                }
                break;
            case R.id.sign_out_button:
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                            finish();
                            }
                        });
                break;
        }
    }
    @Override
    public void finish() {
        Log.i(TAG,"finished");
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),    /* get the context for the application */
                "us-west-2:96107a1e-261a-4a63-8b28-776d91dd44d7",    /* Identity Pool ID */
                Regions.US_WEST_2           /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );
        credentialsProvider.clear();
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);

        super.finish();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mReceiver,
                mIntentFilter);

    }


    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onPause();
    }

}
