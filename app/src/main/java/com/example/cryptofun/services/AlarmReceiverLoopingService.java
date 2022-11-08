package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class AlarmReceiverLoopingService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent in = new Intent(context, UpdatingDatabaseService.class);
        context.startService(in);
        setAlarm(context);
    }

    @SuppressLint("ShortAlarm")
    public void setAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiverLoopingService.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
        assert am != null;
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + 20L * 1000L) , pi); //Next alarm in 20s
    }
}