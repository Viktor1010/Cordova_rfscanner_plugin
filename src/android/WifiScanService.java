package com.example.scandevice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import java.util.List;
/**
 * Created by Slonic on 1/27/2016.
 */
public class WifiScanService extends Service {

    private Handler mHandler;
    private HandlerThread handlerThread;
    private final static int mMinute = 60000;
    private int count;
    private int mTimeInterval;

    public static final String TIME_STAMP = "10000";
    public static int mCount = 0;
    public DBHelper dbManager;

    WifiManager mainWifi;

    @Override
    public void onCreate(){
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        dbManager = new DBHelper(getApplicationContext(), "WiFi.db", null, 1);
        CheckWiFiState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mTimeInterval = Integer.parseInt(intent.getStringExtra(TIME_STAMP));
        count = mMinute/Integer.parseInt(intent.getStringExtra(TIME_STAMP));
        registerReceiver(new WifiReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        handlerThread = new HandlerThread("StartScanWiFiThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mHandler.post(mRunnable);
        return START_STICKY;
    }

    private void CheckWiFiState(){
        if(mainWifi.isWifiEnabled())
            mainWifi.setWifiEnabled(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }


    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if(mHandler != null) {
                if (mCount == count) {
                    stop(); return;
                }
                mainWifi.startScan();
                mHandler.postDelayed(mRunnable, mTimeInterval);
                mCount++;
            }
        }
    };



    private void stop(){
        mCount = 0;
        mHandler.removeCallbacks(mRunnable);
        handlerThread.quit();
        handlerThread.interrupt();
        handlerThread = null;
        mHandler = null;
        Thread.interrupted();
    }
    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            int maxLevel = 5;
            List<ScanResult> wifiList;
            wifiList = mainWifi.getScanResults();
            for (ScanResult result : wifiList) {
                {
                    int level = WifiManager.calculateSignalLevel(result.level, maxLevel);
                    String SSID = result.SSID;
                    String capabilities = result.capabilities;
                    dbManager.insert("insert into SCAN_LIST values(null,'" + SSID + "', '" + level + "');");
                }
            }
        }
    }
}// class end