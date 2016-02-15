/*
    Copyright 2013-2014 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */

package com.example.scandevice;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ScanDevice extends CordovaPlugin {

    // Event types for callbacks
    private enum Event {
        ACTIVATE, DEACTIVATE, FAILURE
    }

    // Flag indicates if the service is bind
    private boolean isBind = false;

    // Default settings for the notification
    private static JSONObject defaultSettings = new JSONObject();

    // Tmp config settings for the notification
    private static JSONObject updateSettings;
    private String interval="1000";
    private String serverurl;
    private Boolean stopped = true;
    private MainActivity mainActivity;
    private final long m_TimerInterval = 14 * 60000;
    private final long m_tm = 60000;


    Activity context;
    // Used to (un)bind the service to with the activity
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            // Nothing to do here
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Nothing to do here
        }
    };
    private final ServiceConnection connection2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            // Nothing to do here
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Nothing to do here
        }
    };

    /**
     * Executes the request.
     *
     * @param action   The action to execute.
     * @param args     The exec() arguments.
     * @param callback The callback context used when
     *                 calling back into JavaScript.
     *
     * @return
     *      Returning false results in a "MethodNotFound" error.
     *
     * @throws JSONException
     */
    @Override
    public boolean execute (String action, JSONArray args,
                            CallbackContext callback) throws JSONException {

        if (action.equalsIgnoreCase("configure")) {
            JSONObject settings = args.getJSONObject(0);
            boolean update = args.getBoolean(1);

            if (update) {
                setUpdateSettings(settings);
                updateNotifcation();
            } else {
                setDefaultSettings(settings);
            }

            return true;
        }

        if (action.equalsIgnoreCase("start")) {
            interval= args.getString(0);
            serverurl = args.getString(1);
            stopped = false;
            mainActivity = new MainActivity();
            startrun();
            return true;
        }

        if (action.equalsIgnoreCase("stop")) {
            stopped = true;
            return true;
        }
        return false;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking
     *      Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking
     *      Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
    }

    /**
     * Called when the activity will be destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Enable the background mode.
     */
    private void startrun() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (!stopped){
                    synchronized (this){
                        try {
                            startService();
                            Thread.sleep(m_TimerInterval);
                            postServer(serverurl);
                            Thread.sleep(m_tm);
                        }catch (Exception e){}
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Disable the background mode.
     */
    private void disableMode() {
        stopService();
    }

    private void postServer(String url){
        mainActivity.postJSONData(url);
    }

    /**
     * Update the default settings for the notification.
     *
     * @param settings
     *      The new default settings
     */
    private void setDefaultSettings(JSONObject settings) {
        defaultSettings = settings;
    }

    /**
     * Update the config settings for the notification.
     *
     * @param settings
     *      The tmp config settings
     */
    private void setUpdateSettings(JSONObject settings) {
        updateSettings = settings;
    }

    /**
     * The settings for the new/updated notification.
     *
     * @return
     *      updateSettings if set or default settings
     */
    protected static JSONObject getSettings() {
        if (updateSettings != null)
            return updateSettings;

        return defaultSettings;
    }

    /**
     * Called by  to delete the update settings.
     */
    protected static void deleteUpdateSettings() {
        updateSettings = null;
    }

    /**
     * Update the notification.
     */
    private void updateNotifcation() {
        if (isBind) {
//            stopService();
//            startService();
        }
    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void startService() {


        context = cordova.getActivity();

        Intent wifiIntent = new Intent(context, WifiScanService.class);
        wifiIntent.putExtra(WifiScanService.TIME_STAMP,interval);

        Intent bluetooth = new Intent(context,BluetoothScanService.class);
        bluetooth.putExtra(BluetoothScanService.TIME_STAMP,interval);

        try {

            context.bindService(wifiIntent, connection, Context.BIND_AUTO_CREATE);
            context.startService(wifiIntent);

            context.bindService(bluetooth, connection2, Context.BIND_AUTO_CREATE);
            context.startService(bluetooth);

        } catch (Exception e) {
            e.printStackTrace();
        }
        isBind = true;
    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void stopService() {
        Activity context = cordova.getActivity();

        Intent wifiIntent = new Intent(
                context, WifiScanService.class);
        Intent bluetooth = new Intent(
                context, BluetoothScanService.class);

        if (!isBind)
            return;
        context.unbindService(connection);
        context.stopService(wifiIntent);
        context.unbindService(connection2);
        context.stopService(bluetooth);
        isBind = false;
    }


}
