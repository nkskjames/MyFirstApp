package com.example.njames.myfirstapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

public class MonitorActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MonitorActivity";
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    AmazonDynamoDBClient dynamodb;
    CognitoCachingCredentialsProvider credentialsProvider;
    String thingName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        thingName = this.getIntent().getStringExtra(Constants.EXTRA_THING_ID);
        findViewById(R.id.update_label_button).setOnClickListener(this);

        findViewById(R.id.units).setOnClickListener(this);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Broadcast received");
                TextView t0 = (TextView) findViewById(R.id.t0_value);
                TextView t1 = (TextView) findViewById(R.id.t1_value);
                TextView t2 = (TextView) findViewById(R.id.t2_value);
                EditText t0l = (EditText) findViewById(R.id.t0_label);
                EditText t1l = (EditText) findViewById(R.id.t1_label);
                EditText t2l = (EditText) findViewById(R.id.t2_label);
                t0.setText(intent.getStringExtra("t0"));
                t1.setText(intent.getStringExtra("t1"));
                t2.setText(intent.getStringExtra("t2"));
                t0l.setText(intent.getStringExtra("d0"));
                t1l.setText(intent.getStringExtra("d1"));
                t2l.setText(intent.getStringExtra("d2"));
            }
        };

        mIntentFilter = new IntentFilter(Constants.ACTION_RECEIVE_DATA);
        registerReceiver(mReceiver, mIntentFilter);
    }

    private void updateLabels() {
        Log.i(TAG, "Updating labels");
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),    /* get the context for the application */
                "us-west-2:96107a1e-261a-4a63-8b28-776d91dd44d7",    /* Identity Pool ID */
                Regions.US_WEST_2           /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );
        Log.i(TAG, credentialsProvider.getCachedIdentityId());
        EditText t0l = (EditText) findViewById(R.id.t0_label);
        EditText t1l = (EditText) findViewById(R.id.t1_label);
        EditText t2l = (EditText) findViewById(R.id.t2_label);
        RequestClass request = new RequestClass(
                thingName,
                t0l.getText().toString(),
                t1l.getText().toString(),
                t2l.getText().toString());

        LambdaInvokerFactory factory = new LambdaInvokerFactory(this.getApplicationContext(),
                Regions.US_WEST_2, credentialsProvider);

        final UpdateThingDescInterface myInterface = factory.build(UpdateThingDescInterface.class);

        new AsyncTask<RequestClass, Void, Void>() {
            @Override
            protected Void doInBackground(RequestClass... params) {
                // invoke "echo" method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    myInterface.UpdateThingDesc(params[0]);
                    return null;
                } catch (LambdaFunctionException lfe) {
                    Log.e("Tag", "Failed to invoke echo", lfe);
                    return null;
                }
            }
        }.execute(request);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.update_label_button:
                updateLabels();
                break;
            case R.id.units:
                Switch units = (Switch) findViewById(R.id.units);
                if (units.isActivated()) {

                } else {

                }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }


    @Override
    protected void onPause() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        mReceiver = null;
        super.onPause();
    }


    private class UpdateShadowTask extends AsyncTask<Void, Void, Void> {

        private String thingName;
        private String updateState;

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }

        /*
        @Override
        protected void onPostExecute() {
            if (result.getError() == null) {
                Log.i(UpdateShadowTask.class.getCanonicalName(), result.getResult());
            } else {
                Log.e(UpdateShadowTask.class.getCanonicalName(), "Error in Update Shadow",
                        result.getError());
            }
        }*/
    }

    ;
}
