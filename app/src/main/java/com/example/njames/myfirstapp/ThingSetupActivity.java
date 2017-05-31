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

import static android.net.wifi.WifiManager.WIFI_MODE_FULL;

public class ThingSetupActivity extends AppCompatActivity {

    private static final String TAG = "ThingSetupActivity";
    private WifiManager mWifiManager;
    private static final String SSID = "Boingo Hotspot";
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private int netIdSave = -1;
    private int netId = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thing_setup);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG,"Wifi change:");

                mWifiManager.disconnect();
                mWifiManager.disableNetwork(netId);
                if (netIdSave != -1) {
                    Log.i(TAG,"Restoring network");
                    mWifiManager.enableNetwork(netIdSave, true);
                    mWifiManager.reconnect();
                }
            }
        };
        mFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver,mFilter);
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

        WifiConfiguration config = new WifiConfiguration();
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.SSID = "\"" + SSID + "\"";
        netId = mWifiManager.addNetwork(config);

        mWifiManager.disconnect();
        mWifiManager.enableNetwork(netId, true);
        mWifiManager.reconnect();
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

}
