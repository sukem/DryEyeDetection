package com.example.sukem.dryeyedetection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceBroadcastReceiver extends BroadcastReceiver {
    private final static String TAG = "ServiceBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent targetIntent = new Intent(context, ForegroundService.class);
        context.stopService(targetIntent);
    }
}
