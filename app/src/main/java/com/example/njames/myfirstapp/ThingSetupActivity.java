package com.example.njames.myfirstapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
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

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunctionException;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.amazonaws.regions.Regions;
import com.google.firebase.iid.FirebaseInstanceId;
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
    private IntentFilter mFilter4 = new IntentFilter(Constants.ACTION_SIGNUP_DONE);

    private int netIdSave = -1;
    private int netId = -1;
    private static final String thingUrl = "http://192.168.4.1/ssidSelected";
    public enum ConfigState {
        CONFIG_STATE_IDLE,
        CONFIG_STATE_THING_CONNECTING,
        CONFIG_STATE_THING_CONNECTED,
        CONFIG_STATE_THING_SETUP,
        CONFIG_STATE_THING_OK,
        CONFIG_STATE_RECONNECT,
        CONFIG_STATE_RESPONSE_RECEIVED,
        CONFIG_STATE_DONE,
        CONFIG_STATE_FAIL,
        CONFIG_STATE_ANY,
    }
    private ConfigState configState = ConfigState.CONFIG_STATE_IDLE;
    private String thingName = "";
    private ConnectivityManager conn;
    private Handler handler = new Handler();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "receiver action: "+intent.getAction());
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
            if (intent.getAction().equals(Constants.ACTION_SIGNUP_DONE)) {
                configState = ConfigState.CONFIG_STATE_RESPONSE_RECEIVED;
                configStateManager(ConfigState.CONFIG_STATE_ANY);
            }
            if (intent.getAction().equals(Constants.ACTION_POST_FORM)) {
                String response = intent.getStringExtra(Constants.EXTRA_POST_RESPONSE);
                String thingName = intent.getStringExtra(Constants.EXTRA_POST_THINGNAME);
                handleConfigResponse(response,thingName);
            }
            if (intent.getAction().equals(Constants.ACTION_RECEIVE_DATA)) {
                String msgThingName = intent.getStringExtra("thingname");
                if (thingName.equals(msgThingName)) {
                    configState = ConfigState.CONFIG_STATE_DONE;
                    configStateManager(configState);
                    finish();
                }
            }
        }
    };

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
        configState = ConfigState.CONFIG_STATE_IDLE;

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
    private void onWifiConnectInternet() {
        Log.i(TAG,"onWifiConnectInternet");
        configStateManager(ConfigState.CONFIG_STATE_RECONNECT);
    }
    //step 1
    private void onWifiConnectThing() {
        Log.i(TAG,"onWifiConnectThing");
        configState  = ConfigState.CONFIG_STATE_THING_CONNECTED;
        configStateManager(ConfigState.CONFIG_STATE_ANY);
    }
    // step 2
    private void handleConfigResponse(String response, String thingName) {
        Log.i(TAG,"Thing setup response: "+response+": "+thingName);
        if (response.equals("OK")) {
            configState = ConfigState.CONFIG_STATE_THING_OK;
            configStateManager(ConfigState.CONFIG_STATE_ANY);
        } else {

            if (retryCount < 3) {
                configState = ConfigState.CONFIG_STATE_THING_CONNECTED;
                Log.i(TAG, "Retrying");
                configStateManager(ConfigState.CONFIG_STATE_ANY);
                retryCount++;
            } else {
                //Setup failed
                configState = ConfigState.CONFIG_STATE_FAIL;
                reconnectToInternet();
                Toast.makeText(getApplicationContext(),
                        "Setup Error: " + response, Toast.LENGTH_LONG)
                        .show();
            }
        }
    }
    private void reconnectToInternet() {
        Log.i(TAG, "reconnectToInternet");
        mWifiManager.disconnect();
        mWifiManager.disableNetwork(netId);
        Log.i(TAG, "Restoring network");
        mWifiManager.enableNetwork(netIdSave, true);
        mWifiManager.reconnect();
    }

    private void configStateManager(ConfigState stateFilter) {
        if (configState != stateFilter && stateFilter != ConfigState.CONFIG_STATE_ANY) {
            Log.i(TAG,"State Manager Filter:"+stateFilter.toString());
            return;
        }
        Log.i(TAG,"State Manager: "+configState.toString());
        switch(configState) {
            case CONFIG_STATE_IDLE:
                configState = ConfigState.CONFIG_STATE_THING_CONNECTING;
                connectToThing();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG,"never connected");
                        Toast.makeText(getApplicationContext(),
                                "Unable to connect to device.  Is it on?", Toast.LENGTH_LONG)
                                .show();
                    }
                }, 10000);
                break;
            case CONFIG_STATE_THING_CONNECTED:
                handler.removeCallbacksAndMessages(null);
                configState = ConfigState.CONFIG_STATE_THING_SETUP;
                doSetup();
                break;
            case CONFIG_STATE_THING_OK:
                configState = ConfigState.CONFIG_STATE_RECONNECT;
                reconnectToInternet();
                break;
            case CONFIG_STATE_RECONNECT:
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG,"Thing never responded");
                        Toast.makeText(getApplicationContext(),
                                "Thing never responded", Toast.LENGTH_LONG)
                                .show();
                        configState = ConfigState.CONFIG_STATE_FAIL;
                    }
                }, 30000);
                break;
            case CONFIG_STATE_RESPONSE_RECEIVED:
                handler.removeCallbacksAndMessages(null);
                AwsIntentService.resetToken(getApplicationContext());
                AwsIntentService.startActionAddEndpoint(getApplicationContext());
                break;

        }
    }
    private void connectToThing() {
        retryCount = 0;
        Log.i(TAG,"Connecting to: "+thingSSID);
        WifiConfiguration config = new WifiConfiguration();
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.SSID = "\"" + thingSSID + "\"";

        netId = mWifiManager.addNetwork(config);
        if (netId == -1) {
            Toast.makeText(getApplicationContext(),
                    "BBQTemp Wifi not found", Toast.LENGTH_LONG)
                    .show();

        } else {
            mWifiManager.disconnect();
            mWifiManager.enableNetwork(netId, true);
            mWifiManager.reconnect();
        }
    }

    //Scan all networks for find Wifi
    //Need to do this because when wifi doesn't have internet access
    //  Android will use cellular, so we have to find wifi and force to use it.
    private void doSetup() {
        Log.i(TAG, "doSetup");
        Network nets[] = conn.getAllNetworks();
        for (Network n : nets) {
            NetworkInfo ninfo = conn.getNetworkInfo(n);
            if (ninfo.getType() == ConnectivityManager.TYPE_WIFI) {
                TextView ssidPassword = (TextView) findViewById(R.id.password);
                new ConfigThing().execute(
                        n,
                        ssidPassword.getText().toString(),
                        AwsIntentService.getCredentialProvider(getApplicationContext()).getCachedIdentityId());

            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_button:
                hideKeyboard();
                configStateManager(ConfigState.CONFIG_STATE_ANY);
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
                        .add("token", FirebaseInstanceId.getInstance().getToken())
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



    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "OnResume");
        registerReceiver(mReceiver, mFilter);
        registerReceiver(mReceiver, mFilter3);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mReceiver, mFilter2);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mReceiver, mFilter4);

    }

    @Override
    protected void onPause() {
        Log.i(TAG, "OnPause");
        unregisterReceiver(mReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        super.onPause();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
