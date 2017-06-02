package com.example.njames.myfirstapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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
    private static final String SSID = "BBQTemp";
    private String currentSSID = "";
    private int retryCount = 0;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.ACTION_POST_FORM)) {
                String response = intent.getStringExtra(Constants.EXTRA_POST_RESPONSE);
                Log.i(TAG,"Config response: "+response);
                if (!response.equals("OK") && retryCount < 3) {
                    Log.i(TAG,"Retrying");
                    doSetup();
                    retryCount++;
                } else {
                    //Reconnect to previous Wifi
                    mWifiManager.disconnect();
                    mWifiManager.disableNetwork(netId);
                    if (netIdSave != -1) {
                        Log.i(TAG, "Restoring network");
                        mWifiManager.enableNetwork(netIdSave, true);
                        mWifiManager.reconnect();
                    }
                    if (response.equals("OK")) {
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Setup Error: " + response, Toast.LENGTH_LONG)
                                .show();
                    }
                }
            }
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(EXTRA_NETWORK_INFO);
                WifiInfo wifi = intent.getParcelableExtra(EXTRA_WIFI_INFO);
                if (wifi != null && info.isConnected()) {
                    Log.i(TAG, "connected: " + wifi.getSSID());
                    if (wifi.getSSID().equals("\"" + SSID + "\"")) {
                        if (connectCount == 1 && !configDone) {
                            doSetup();
                        }
                        connectCount++;
                    }
                }
            }
        }
        //Scan all networks for find Wifi
        //Need to do this because when wifi doesn't have internet access
        //  Android will use cellular, so we have to find wifi and force to use it.
        private void doSetup() {
            Network nets[] = conn.getAllNetworks();
            for (Network n : nets) {
                NetworkInfo ninfo = conn.getNetworkInfo(n);
                if (ninfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    //conn.bindProcessToNetwork(n);
                    TextView ssidPassword = (TextView)findViewById(R.id.password);
                    new ConfigThing().execute(
                            n,
                            ssidPassword.getText().toString(),
                            DynamoIntentService.getCredentialProviderFactory(getApplicationContext()).getCachedIdentityId());
                    configDone = true;
                }
            }
        }
    };


    private IntentFilter mFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    private IntentFilter mFilter2 = new IntentFilter(Constants.ACTION_POST_FORM);
    ;
    private int netIdSave = -1;
    private int netId = -1;
    private static final String thingUrl = "http://192.168.4.1/ssidSelected";
    private static final String rebootUrl = "http://192.168.4.1/reboot";
    private boolean configDone = false;
    private int connectCount = 0;
    private ConnectivityManager conn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thing_setup);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        conn = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        findViewById(R.id.connect_button).setOnClickListener(this);
        TextView ssidText = (TextView) findViewById(R.id.ssid);
        Button b = (Button) findViewById(R.id.connect_button);
        b.setVisibility(View.GONE);

        if (mWifiManager.isWifiEnabled()) {
            WifiInfo winfo = mWifiManager.getConnectionInfo();
            currentSSID = winfo.getSSID();
            currentSSID = currentSSID.substring(1, currentSSID.length() - 1);
            ssidText.setText(currentSSID);
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
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(mReceiver, mFilter2);
    }


    @Override
    protected void onPause() {
        Log.i(TAG, "OnPause");
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        }
        mReceiver = null;
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_button:
                configDone = true;
                connectCount = 0;
                    WifiConfiguration config = new WifiConfiguration();
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    config.SSID = "\"" + SSID + "\"";
                    netId = mWifiManager.addNetwork(config);
                    mWifiManager.disconnect();
                    mWifiManager.enableNetwork(netId, true);
                    configDone = false;
                    connectCount = 0;
                    retryCount = 0;
                    mWifiManager.reconnect();
                break;
        }
    }

    private class ConfigThing extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... data) {
            String rtn = "RESPONSE_ERROR";
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
                if (response.code() == 200) {
                    rtn = "OK";
                    client.setRetryOnConnectionFailure(false);
                    Request rebootRequest = new Request.Builder()
                            .url(rebootUrl)
                            .build();
                    client.newCall(rebootRequest).execute();
                    client.
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                rtn = "CONNECTION_ERROR";
            }
            conn.bindProcessToNetwork(null);
            Intent localIntent = new Intent(Constants.ACTION_POST_FORM)
                    .putExtra(Constants.EXTRA_POST_RESPONSE, rtn);

            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
            return null;
        }
    };
}
