package com.example.njames.myfirstapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;

public class MonitorActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "MonitorActivity";
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Broadcast received");
            int ta[] = intent.getIntArrayExtra("t");
            int tua[] = intent.getIntArrayExtra("tu");
            int tla[] = intent.getIntArrayExtra("tl");
            String tda[] = intent.getStringArrayExtra("td");
            for (int i = 0; i < 3; i++) {
                t[i] = Integer.valueOf(ta[i]);
                tu[i] = Integer.valueOf(tua[i]);
                tl[i] = Integer.valueOf(tla[i]);
                td[i] = tda[i];
            }
            updateView();
        }
    };


    IntentFilter mIntentFilter = new IntentFilter(Constants.ACTION_RECEIVE_DATA);
    AmazonDynamoDBClient dynamodb;
    CognitoCachingCredentialsProvider credentialsProvider;
    private String thingName;
    private boolean convertToF = false;
    Integer t[] = new Integer[]{0,0,0};
    Integer tu[] = new Integer[]{0,0,0};
    Integer tl[] = new Integer[]{0,0,0};
    String td[] = new String[3];
    ArrayList<View> views = new ArrayList<View>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        thingName = this.getIntent().getStringExtra(Constants.EXTRA_THING_ID);
        findViewById(R.id.update_label_button).setOnClickListener(this);
        findViewById(R.id.units).setOnClickListener(this);
        Switch unitView = (Switch) findViewById(R.id.units);
        unitView.setOnCheckedChangeListener(this);

        views.add(findViewById(R.id.t0));
        views.add(findViewById(R.id.t1));
        views.add(findViewById(R.id.t2));
    }

    private void updateView() {
        int i = 0;
        for (View view : views) {
            ((TextView)view.findViewById(R.id.t)).setText(getTemperatureString(t[i],true));
            ((EditText)view.findViewById(R.id.tu)).setText(getTemperatureString(tu[i],false));
            ((EditText)view.findViewById(R.id.tl)).setText(getTemperatureString(tl[i],false));
            ((EditText)view.findViewById(R.id.td)).setText(td[i]);
            i++;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.update_label_button:
                int i = 0;
                Intent labelsActivity = new Intent(this, UpdateLabelsActivity.class);
                ArrayList<String> tus = new ArrayList<>();
                ArrayList<String> tls = new ArrayList<>();
                ArrayList<String> tds = new ArrayList<>();
                for (View view : views) {
                    String tu = ((EditText)view.findViewById(R.id.tu)).getText().toString();
                    String tl = ((EditText)view.findViewById(R.id.tl)).getText().toString();
                    String td = ((EditText)view.findViewById(R.id.td)).getText().toString();
                    tus.add(tu);
                    tls.add(tl);
                    tds.add(td);
                }

                labelsActivity.putExtra(Constants.EXTRA_THING_ID, thingName);
                labelsActivity.putExtra(Constants.EXTRA_THRESHOLDS_UPPER,tus);
                labelsActivity.putExtra(Constants.EXTRA_THRESHOLDS_LOWER,tls);
                labelsActivity.putExtra(Constants.EXTRA_LABELS,tds);
                startActivity(labelsActivity);
                break;
        }
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        convertToF = isChecked;
        updateView();
    }

    private String getTemperatureString(int temp, boolean addPrefix) {
        double tmpd = temp;
        String prefix = "C";
        if (convertToF) {
            tmpd = temp * 1.8 + 32;
            prefix = "F";
        }
        if (!addPrefix) {
            prefix = "";
        }
        Integer integer = Integer.valueOf((int) Math.round(tmpd));
        return String.valueOf(integer)+prefix;
    }
}
