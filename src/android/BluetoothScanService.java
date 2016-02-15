package com.example.scandevice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Created by Slonic on 1/27/2016.
 */
public class BluetoothScanService extends Service{

    private Handler mHandler;
    private HandlerThread handlerThread;
    private final static int mMinute = 60000;
    private int count;
    private int mTimeInterval;

    public static final String TIME_STAMP = "10000";
    public static int mCount = 0;
    public DBHelper dbManager;

    BluetoothAdapter bluetoothAdapter;


    @Override
    public void onCreate(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBlueToothState();
        dbManager = new DBHelper(getApplicationContext(), "BTE.db", null, 1);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mTimeInterval = Integer.parseInt(intent.getStringExtra(TIME_STAMP));
        count = mMinute/Integer.parseInt(intent.getStringExtra(TIME_STAMP));
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        handlerThread = new HandlerThread("StartScanBTEThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mHandler.post(mRunnable);
        return START_STICKY;
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
                bluetoothAdapter.startDiscovery();
                mHandler.postDelayed(mRunnable, mTimeInterval);
                mCount++;
            }
        }
    };

    private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                dbManager.insert("insert into SCAN_LIST values(null,'" + device + "', '" + rssi + "');");
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

    private void CheckBlueToothState(){
        if (bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "Bluetooth NOT support", Toast.LENGTH_LONG).show();
        }else{
            if (!bluetoothAdapter.isEnabled()){
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
            }
        }
    }
}// class end



