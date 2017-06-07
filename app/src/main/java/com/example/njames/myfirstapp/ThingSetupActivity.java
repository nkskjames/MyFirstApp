package com.example.njames.myfirstapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import static android.net.wifi.WifiManager.EXTRA_NETWORK_INFO;
import static android.net.wifi.WifiManager.EXTRA_WIFI_INFO;

public class ThingSetupActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = "ThingSetupActivity";
    private WifiManager mWifiManager;
    private static final String thingSSID = "BBQTemp";
    private String currentSSID = "";
    private int retryCount = 0;
    private IntentFilter mFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    private IntentFilter mFilter2 = new IntentFilter(Constants.ACTION_POST_FORM);
    private IntentFilter mFilter3 = new IntentFilter(Constants.ACTION_RECEIVE_DATA);

    private int netIdSave = -1;
    private int netId = -1;
    private static final String thingUrl = "http://192.168.4.1/ssidSelected";
    private static final int CONFIG_STATE_IDLE = 0;
    private static final int CONFIG_STATE_PENDING = 1;
    private static final int CONFIG_STATE_THING_ONWIFI = 2;
    private static final int CONFIG_STATE_ENDPOINT_REQUEST_SENT = 3;
    private static final int CONFIG_STATE_DONE = 4;
    private int configState = CONFIG_STATE_IDLE;
    private String thingName = "";
    private ConnectivityManager conn;
    private Handler handler = new Handler();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_RECEIVE_DATA)) {
                String msgThingName = intent.getStringExtra("thingname");
                Log.i(TAG,"message received: "+msgThingName);
                if (thingName.equals(msgThingName) && configState == CONFIG_STATE_ENDPOINT_REQUEST_SENT) {
                    configState = CONFIG_STATE_DONE;
                    finish();
                }
            }
            if (intent.getAction().equals(Constants.ACTION_POST_FORM)) {
                String response = intent.getStringExtra(Constants.EXTRA_POST_RESPONSE);
                String thingName = intent.getStringExtra(Constants.EXTRA_POST_THINGNAME);
                onThingSetupResponse(response,thingName);
            }
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(EXTRA_NETWORK_INFO);
                WifiInfo wifi = intent.getParcelableExtra(EXTRA_WIFI_INFO);
                if (wifi != null && info.isConnected()) {
                    String connectSSID = wifi.getSSID();
                    connectSSID = connectSSID.substring(1, connectSSID.length() - 1);
                    if (connectSSID.equals(currentSSID)) {
                        onWifiConnectInternet();
                    } else if(connectSSID.equals(thingSSID)) {
                        onWifiConnectThing();
                    }
                }
            }
        }
    };

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private void onWifiConnectInternet() {
        Log.i(TAG,"onWifiConnectInternet");
        if (configState == CONFIG_STATE_THING_ONWIFI) {
            Log.i(TAG,"onWifiConnectInternet: Config onwifi");
            AwsIntentService.resetToken(getApplicationContext());
            AwsIntentService.startActionAddEndpoint(getApplicationContext());

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (configState == CONFIG_STATE_ENDPOINT_REQUEST_SENT) {
                        Log.i(TAG,"never recieved request");
                        configState = CONFIG_STATE_IDLE;
                        Toast.makeText(getApplicationContext(),
                                "Never received response from device; perhaps password is wrong", Toast.LENGTH_LONG)
                                .show();
                    } else {
                        finish();
                    }
                }
            }, 30000);
            configState = CONFIG_STATE_ENDPOINT_REQUEST_SENT;
        }
    }
    //step 1
    private void onWifiConnectThing() {
        Log.i(TAG,"onWifiConnectThing");
        if (configState == CONFIG_STATE_IDLE) {
            doSetup();
        }
    }
    // step 2
    private void onThingSetupResponse(String response, String thingName) {
        Log.i(TAG,"Thing setup response: "+response+": "+thingName);
        if (response.equals("OK")) {
            configState = CONFIG_STATE_THING_ONWIFI;
            reconnectToInternet();
        } else {
            configState = CONFIG_STATE_IDLE;
            if (retryCount < 3) {
                Log.i(TAG, "Retrying");
                doSetup();
                retryCount++;
            } else {
                //Setup failed
                reconnectToInternet();
                Toast.makeText(getApplicationContext(),
                        "Setup Error: " + response, Toast.LENGTH_LONG)
                        .show();
            }
        }
    }
    private void reconnectToInternet() {
        mWifiManager.disconnect();
        mWifiManager.disableNetwork(netId);
        Log.i(TAG, "Restoring network");
        mWifiManager.enableNetwork(netIdSave, true);
        mWifiManager.reconnect();
    }
    //Scan all networks for find Wifi
    //Need to do this because when wifi doesn't have internet access
    //  Android will use cellular, so we have to find wifi and force to use it.
    private void doSetup() {
        Network nets[] = conn.getAllNetworks();
        for (Network n : nets) {
            NetworkInfo ninfo = conn.getNetworkInfo(n);
            if (ninfo.getType() == ConnectivityManager.TYPE_WIFI) {
                TextView ssidPassword = (TextView) findViewById(R.id.password);
                new ConfigThing().execute(
                        n,
                        ssidPassword.getText().toString(),
                        AwsIntentService.getCredentialProvider(getApplicationContext()).getCachedIdentityId());
                configState = CONFIG_STATE_PENDING;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thing_setup);

        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        conn = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        findViewById(R.id.connect_button).setOnClickListener(this);
        TextView ssidText = (TextView) findViewById(R.id.ssid);
        Button b = (Button) findViewById(R.id.connect_button);
        b.setVisibility(View.GONE);
        configState = CONFIG_STATE_IDLE;

        if (mWifiManager.isWifiEnabled()) {
            WifiInfo winfo = mWifiManager.getConnectionInfo();
            currentSSID = winfo.getSSID();
            currentSSID = currentSSID.substring(1, currentSSID.length() - 1);
            ssidText.setText(currentSSID);
            netIdSave = winfo.getNetworkId();
            b.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Must be connected to WiFi", Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "OnResume");
        registerReceiver(mReceiver, mFilter);
        registerReceiver(mReceiver, mFilter3);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mReceiver, mFilter2);
    }


    @Override
    protected void onPause() {
        Log.i(TAG, "OnPause");
        unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_button:
                hideKeyboard();
                if (configState == CONFIG_STATE_IDLE) {
                    retryCount = 0;

                    WifiConfiguration config = new WifiConfiguration();
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    config.SSID = "\"" + thingSSID + "\"";

                    netId = mWifiManager.addNetwork(config);
                    mWifiManager.disconnect();
                    mWifiManager.enableNetwork(netId, true);
                    mWifiManager.reconnect();
                }
                break;
        }
    }

    private class ConfigThing extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... data) {
            String rtn = "RESPONSE_ERROR";
            String thingName = "ERROR";
            Network network = (Network)data[0];
            conn.bindProcessToNetwork(network);
            try {
                OkHttpClient client = new OkHttpClient();
                client.setRetryOnConnectionFailure(true);
                RequestBody body = new FormEncodingBuilder()
                        .add("username", (String)data[2])
                        .add("ssid", currentSSID)
                        .add("password", (String)data[1])
                        .add("ip", "")
                        .add("gw", "")
                        .add("netmask", "")
                        .build();
                Request request = new Request.Builder()
                        .url(thingUrl)
                        .post(body)
                        .build();
                Response response = client.newCall(request).execute();
                Log.i(TAG, "Response: " + response.code());
                thingName = response.body().string();
                Log.i(TAG, "Body: " + thingName);

                if (response.code() == 200) {
                    rtn = "OK";
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                rtn = "CONNECTION_ERROR";
            }
            conn.bindProcessToNetwork(null);
            Intent localIntent = new Intent(Constants.ACTION_POST_FORM)
                    .putExtra(Constants.EXTRA_POST_RESPONSE, rtn)
                    .putExtra(Constants.EXTRA_POST_THINGNAME, thingName );

            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
            return null;
        }
    };
}
