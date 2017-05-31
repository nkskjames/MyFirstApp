package com.example.njames.myfirstapp;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

import static android.net.wifi.WifiManager.EXTRA_WIFI_STATE;
import static android.net.wifi.WifiManager.WIFI_MODE_FULL;

public class ThingSetupActivity extends AppCompatActivity {

    private static final String TAG = "ThingSetupActivity";
    private WifiManager mWifiManager;
    private static final String SSID = "BBQTemp";
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private int netIdSave = -1;
    private int netId = -1;
    private static final String thingUrl = "http://192.168.4.1/";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private static final int WIFI_STATE_BEGIN = 0;
    private static final int WIFI_STATE_APPSSID = 1;
    private static final int WIFI_STATE_BACK = 2;
    private int state = WIFI_STATE_BEGIN;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thing_setup);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                int wifiState = intent.getIntExtra(EXTRA_WIFI_STATE,0);
                Log.i(TAG,"Wifi change: "+wifiState+","+netId);
                /*
                if (state == WIFI_STATE_BEGIN) {
                    try {
                        Log.i(TAG,sendPost());
                    } catch (Exception e) {
                        Log.i(TAG,e.toString());
                    }
                }
                */
                /*
                mWifiManager.disconnect();
                mWifiManager.disableNetwork(netId);
                if (netIdSave != -1) {
                    Log.i(TAG,"Restoring network");
                    mWifiManager.enableNetwork(netIdSave, true);
                    mWifiManager.reconnect();
                }
                */
            }
        };
        mFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver,mFilter);
/*
        mWifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);

        if (mWifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                netIdSave = wifiInfo.getNetworkId();
                Log.i(TAG, "Save network: " + netIdSave);
            } else {
                Log.i(TAG, "Not connected");
            }
        } else {
            mWifiManager.setWifiEnabled(true);
            Log.i(TAG,"Wifi not enabled, enabling");
        }
*/
        //WifiConfiguration config = new WifiConfiguration();
        //config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        //config.SSID = "\"" + SSID + "\"";
        //netId = mWifiManager.addNetwork(config);

        //mWifiManager.disconnect();
        //mWifiManager.enableNetwork(netId, true);
        //mWifiManager.reconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mFilter);
    }


    @Override
    protected void onPause() {
        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        mReceiver = null;
        super.onPause();
    }


    private String sendPost() throws IOException {

        OkHttpClient client = new OkHttpClient();

        String json = "{'username': 'test_usernanme',"
                + "'ssid':'JamesHouse',"
                + "'password': 'travl4me'"
                + "}";

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(thingUrl)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
