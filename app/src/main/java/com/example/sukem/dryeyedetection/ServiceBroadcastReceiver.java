package com.example.sukem.dryeyedetection;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceBroadcastReceiver extends BroadcastReceiver {
    private final static String TAG = "ServiceBroadcastReceiver";

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent targetIntent = new Intent(context, ForegroundService.class);
        context.stopService(targetIntent);

        Log.d(TAG, "context.stopService called");
    }
}
