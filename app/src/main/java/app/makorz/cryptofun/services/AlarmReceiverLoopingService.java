package app.makorz.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class AlarmReceiverLoopingService extends BroadcastReceiver {

    private static final String TAG = "AlarmService";

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent in = new Intent(context, UpdatingDatabaseService.class);

        Log.e(TAG, "isServiceRunning APRV: " + isMyServiceRunning(ApprovingService.class, context) + " UPDT: "
                + isMyServiceRunning(UpdatingDatabaseService.class, context));

        if (!isMyServiceRunning(UpdatingDatabaseService.class, context)) {
            Log.e(TAG, "Start service UPD");
            context.startForegroundService(in);
        } else {
            if (!isMyServiceRunning(ApprovingService.class, context)) {
                Log.e(TAG, "Start service APRV");
                Intent serviceIntent = new Intent(context, ApprovingService.class);
                context.startForegroundService(serviceIntent);
            }
        }
        setAlarm(context, 1);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("ShortAlarm")
    public void setAlarm(Context context, int howManyMinutes) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiverLoopingService.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
        assert am != null;
        Log.e(TAG, "Time: " + howManyMinutes + "m");
        am.setAlarmClock(new AlarmManager.AlarmClockInfo((System.currentTimeMillis() + howManyMinutes * 60 * 1000L), pi), pi);
    }

    public void stopAlarm(Context context) {
        Log.e(TAG, "Canceled");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiverLoopingService.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }
}