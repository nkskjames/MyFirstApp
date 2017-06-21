package com.example.njames.myfirstapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
        CONFIG_STATE_THING_ADDED_DB,
        CONFIG_STATE_RESPONSE_RECEIVED,
        CONFIG_STATE_DONE,
        CONFIG_STATE_FAIL,
        CONFIG_STATE_ANY,
    }

    private ConfigState configState = ConfigState.CONFIG_STATE_IDLE;
    private String thing_id = "";
    private ConnectivityManager conn;
    private Handler handler = new Handler();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "receiver action: " + intent.getAction());
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(EXTRA_NETWORK_INFO);
                WifiInfo wifi = intent.getParcelableExtra(EXTRA_WIFI_INFO);
                if (wifi != null && info.isConnected()) {
                    String connectSSID = wifi.getSSID();
                    connectSSID = connectSSID.substring(1, connectSSID.length() - 1);
                    if (connectSSID.equals(currentSSID)) {
                        onWifiConnectInternet();
                    } else if (connectSSID.equals(thingSSID)) {
                        onWifiConnectThing();
                    }
                }
            }
            if (intent.getAction().equals(Constants.ACTION_POST_FORM)) {
                String response = intent.getStringExtra(Constants.EXTRA_POST_RESPONSE);
                String thing_id = intent.getStringExtra(Constants.EXTRA_POST_THINGNAME);
                handleConfigResponse(response, thing_id);
            }
            if (intent.getAction().equals(Constants.ACTION_THING_INIT)) {
                //configState = ConfigState.CONFIG_STATE_THING_ADDED_DB;
                //configStateManager(configState);
            }
            if (intent.getAction().equals(Constants.ACTION_RECEIVE_DATA)) {
                String msgthing_id = intent.getStringExtra("thing_id");
                Log.i(TAG, msgthing_id + "," + thing_id);
                if (configState == ConfigState.CONFIG_STATE_RECONNECT) {
                    if (thing_id.equals(msgthing_id)) {
                        configState = ConfigState.CONFIG_STATE_DONE;
                        configStateManager(configState);
                    }
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
            setMessage(R.string.wifi_prompt_password, false);
        } else {
            setMessage(R.string.wifi_not_connected, true);
        }
    }

    private void onWifiConnectInternet() {
        Log.i(TAG, "onWifiConnectInternet");
        configStateManager(ConfigState.CONFIG_STATE_RECONNECT);
    }

    //step 1
    private void onWifiConnectThing() {
        Log.i(TAG, "onWifiConnectThing: " + configState);
        if (configState == ConfigState.CONFIG_STATE_THING_CONNECTING) {
            configState = ConfigState.CONFIG_STATE_THING_CONNECTED;
            configStateManager(ConfigState.CONFIG_STATE_ANY);
        }
    }

    // step 2
    private void handleConfigResponse(String response, String thing_id) {
        this.thing_id = thing_id;
        Log.i(TAG, "Thing setup response: " + response + ": " + thing_id);
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
        boolean rtn = mWifiManager.disconnect();
        Log.i(TAG, "reconnectToInternet: " + rtn + "," + netId + "," + netIdSave);
        if (!rtn) {
            configState = ConfigState.CONFIG_STATE_FAIL;
            setMessage(R.string.thing_reconnecting_error, true);
            return;
        }
        rtn = mWifiManager.disableNetwork(netId);
        Log.i(TAG, "Restoring network: " + rtn);
        if (!rtn) {
            configState = ConfigState.CONFIG_STATE_FAIL;
            setMessage(R.string.thing_reconnecting_error, true);
            return;
        }

        rtn = mWifiManager.enableNetwork(netIdSave, true);
        if (!rtn) {
            configState = ConfigState.CONFIG_STATE_FAIL;
            setMessage(R.string.thing_reconnecting_error, true);
            return;
        }
        mWifiManager.reconnect();
    }

    private void configStateManager(ConfigState stateFilter) {
        if (configState != stateFilter && stateFilter != ConfigState.CONFIG_STATE_ANY) {
            Log.i(TAG, "State Manager Filter:" + stateFilter.toString());
            return;
        }
        Log.i(TAG, "State Manager: " + configState.toString());
        switch (configState) {
            case CONFIG_STATE_IDLE:
                configState = ConfigState.CONFIG_STATE_THING_CONNECTING;
                setMessage(R.string.thing_connecting, false);
                connectToThing();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setMessage(R.string.thing_not_connected, false);
                        configState = ConfigState.CONFIG_STATE_FAIL;
                    }
                }, 10000);
                break;
            case CONFIG_STATE_THING_CONNECTED:
                handler.removeCallbacksAndMessages(null);
                configState = ConfigState.CONFIG_STATE_THING_SETUP;
                setMessage(R.string.thing_configuring, false);
                doSetup();
                break;
            case CONFIG_STATE_THING_OK:
                configState = ConfigState.CONFIG_STATE_RECONNECT;
                setMessage(R.string.thing_reconnecting, false);
                reconnectToInternet();
                break;
            case CONFIG_STATE_RECONNECT:
                //Now that we know thing_id, add to database
                FirebaseIntentService.startActionThingInit(getApplicationContext(),thing_id);
                setMessage(R.string.thing_waiting_response, false);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setMessage(R.string.thing_never_responded, false);
                        configState = ConfigState.CONFIG_STATE_FAIL;
                    }
                }, 30000);
                break;
            case CONFIG_STATE_RESPONSE_RECEIVED:
                setMessage(R.string.thing_response_received, false);
                handler.removeCallbacksAndMessages(null);
                //FirebaseIntentService.resetToken(getApplicationContext());
                //FirebaseIntentService.startActionAddEndpoint(getApplicationContext());
                break;
            case CONFIG_STATE_DONE:
                setMessage(R.string.thing_setup_complete, false);
                Intent monitorActivity = new Intent(this, MonitorActivity.class);
                monitorActivity.putExtra(Constants.EXTRA_THING_ID, thing_id);
                startActivity(monitorActivity);
                break;
        }
    }

    private void setMessage(int msgid, boolean error) {
        TextView messages = (TextView) findViewById(R.id.messages);
        messages.setText(getString(msgid));
        if (error) {
            Log.e(TAG, getString(msgid));
        } else {
            Log.i(TAG, getString(msgid));
        }
    }

    private void setMessage(String msg) {
        TextView messages = (TextView) findViewById(R.id.messages);
        messages.setText(msg);
        Log.i(TAG, msg);
    }

    private void connectToThing() {
        retryCount = 0;
        WifiConfiguration config = new WifiConfiguration();
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.SSID = "\"" + thingSSID + "\"";

        netId = mWifiManager.addNetwork(config);
        if (netId == -1) {
            setMessage(R.string.thing_not_connected, false);
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
        Network nets[] = conn.getAllNetworks();
        for (Network n : nets) {
            NetworkInfo ninfo = conn.getNetworkInfo(n);
            if (ninfo != null) {
                if (ninfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    TextView ssidPassword = (TextView) findViewById(R.id.password);
                    Log.i(TAG, "WIFI Found, running setup: " + ninfo.getExtraInfo());

                    new ConfigThing().execute(
                            n,
                            ssidPassword.getText().toString());

                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_button:
                hideKeyboard();
                //configState = ConfigState.CONFIG_STATE_IDLE;
                this.thing_id = "BBQTemp_240AC405D0D4";
                configState = ConfigState.CONFIG_STATE_RECONNECT;
                configStateManager(ConfigState.CONFIG_STATE_ANY);
                break;
        }
    }

    private class ConfigThing extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... data) {
            String rtn = "RESPONSE_ERROR";
            String thing_id = "ERROR";
            Network network = (Network) data[0];
            conn.bindProcessToNetwork(network);
            try {
                OkHttpClient client = new OkHttpClient();
                client.setRetryOnConnectionFailure(true);
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        Constants.PREFKEY_BBQ_AUTH, Context.MODE_PRIVATE);

                RequestBody body = new FormEncodingBuilder()
                        .add("email", sharedPref.getString(Constants.EMAIL,""))
                        .add("password", sharedPref.getString(Constants.PASSWORD,""))
                        .add("password", sharedPref.getString(Constants.USERID,""))
                        .add("ssid", currentSSID)
                        .add("ssid_password", (String) data[1])
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
                thing_id = response.body().string();
                Log.i(TAG, "Body: " + thing_id);

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
                    .putExtra(Constants.EXTRA_POST_THINGNAME, thing_id);

            // Broadcasts the Intent to receivers in this app.
            Log.i(TAG, ">>>>>>>>>>>>>>>> broadcasting POST_FORM");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
            return null;
        }
    }

    ;


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "OnResume");
        registerReceiver(mReceiver, mFilter);

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mReceiver, mFilter2);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mReceiver, mFilter3);
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
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
