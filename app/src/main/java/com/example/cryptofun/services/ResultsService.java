package com.example.cryptofun.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.R;
import com.example.cryptofun.data.AccountBalance;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.ui.retrofit.RetrofitClientSecretTestnet;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultsService extends Service {

    private static final String TAG = "RESService";

    private DBHandler databaseDB;
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String VALUE_REAL = "value_real";
    private static final String ID = "id";

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        databaseDB = DBHandler.getInstance(getApplicationContext());

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "START");

                        // It's for foreground services, because in newest Android, background are not working. Foreground need to inform user that it is running
                        Notification notification = createNotification(intent);
                        // Notification ID cannot be 0.
                        startForeground(4, notification);
                        getTestAccountBalance();

                    }
                }
        ).start();

        return START_STICKY;
    }

    private void getTestAccountBalance() {

        String testBalance;
        ArrayList<String> automaticBalance = new ArrayList<>();

        Cursor data2 = databaseDB.retrieveParam(2);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 2");
            databaseDB.addParam(2, "Test account balance", "", 0, 100);
            testBalance = "100.00";
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 2);
            databaseDB.addParam(2, "Last Update Time", "", 0, 100);
            testBalance = "100.00";
        } else {
            data2.moveToFirst();
            float balance = data2.getFloat(4);
            DecimalFormat dfNr = new DecimalFormat("0.00");
            testBalance = dfNr.format(balance);
        }
        data2.close();


        for (int i = 6; i < 11; i++) {
            Cursor data = databaseDB.retrieveParam(i);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param for test account " + (i - 5) );
                databaseDB.addParam(i, "Test account nr " + (i-5) + " balance", "", 0, 100);
                automaticBalance.add("100.00");
            } else if (data.getCount() >= 2) {
                databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, i);
                databaseDB.addParam(i, "Test account nr " + (i-5) + " balance", "", 0, 100);
                automaticBalance.add("100.00");
            } else {
                data.moveToFirst();
                float balance = data.getFloat(4);
                DecimalFormat dfNr = new DecimalFormat("0.00");
                automaticBalance.add(dfNr.format(balance));
            }
            data.close();
        }

        ServiceFunctions.getRealAccountBalance(getApplicationContext(),null);

       // sendMessageToActivity(testBalance, automaticBalance);

    //    getRealAccountBalance(testBalance, automaticBalance);
    }

    private void getRealAccountBalance(String testBalance, ArrayList<String> automaticBalance) {


        Call<List<AccountBalance>> call = RetrofitClientSecretTestnet.getInstance(getApplicationContext(), 0,  "", 0, "", "", "", "", "0", "0", "",
                        0,"", "0", 0, 0)
                .getMyApi().getAccountBalance();


        // For real account
       // Call<List<AccountBalance>> call = RetrofitClientSecret.getInstance(getApplicationContext()).getMyApi().getAccountBalance();
        Log.e(TAG, call.toString());
        call.enqueue(new Callback<List<AccountBalance>>() {
            @Override
            public void onResponse(@NonNull Call<List<AccountBalance>> call, @NonNull Response<List<AccountBalance>> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        List<AccountBalance> balanceList = response.body();
                        for (int i = 0; i < balanceList.size(); i++) {
                            Log.e(TAG, balanceList.get(i).getAsset());
                            if (balanceList.get(i).getAsset().contains("USDT")) {
                                DecimalFormat dfNr = new DecimalFormat("0.00");
                                String realBalance = dfNr.format(balanceList.get(i).getAvailableBalance());
                                Cursor data2 = databaseDB.retrieveParam(3);
                                if (data2.getCount() == 0) {
                                    Log.e(TAG, "There is no param nr 3");
                                    databaseDB.addParam(3, "Real account balance", "", 0, balanceList.get(i).getBalance());
                                } else if (data2.getCount() >= 2) {
                                    databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 3);
                                    databaseDB.addParam(3, "Real Update Time", "", 0, balanceList.get(i).getBalance());
                                } else {
                                    databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, balanceList.get(i).getBalance(), ID, "3");
                                }
                                data2.close();
                                sendMessageToActivity(testBalance, realBalance, automaticBalance);
                            }
                        }
                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = response.errorBody().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e(TAG, "Error response: " + errorBody);
                    sendMessageToActivity(testBalance, "No data.", automaticBalance);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AccountBalance>> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e(TAG, String.valueOf(t));
            }

        });

    }


    private Notification createNotification(Intent intent) {

        String CHANNEL_ID = "cryptoFun";
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, intent,
                        PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel chan = new NotificationChannel(
                CHANNEL_ID,
                TAG,
                NotificationManager.IMPORTANCE_LOW);
        chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        // Create a notification to indicate that the service is running.
        // You can customize the notification to display the information you want.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CryptoFun")
                .setContentText("Results check.")
                .setContentIntent(pendingIntent)
                .setChannelId(CHANNEL_ID)
                .setSmallIcon(R.drawable.crypto_fun_logo);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void sendMessageToActivity(String testBalance, String realBalance, ArrayList<String> automaticList) {

        Intent intent = new Intent("ResultsStatus");
        Log.e(TAG, "SendMessage " + Thread.currentThread() + " " + Thread.activeCount());
        Bundle bundle = new Bundle();
        // 1 - 30min, 2 - 2h, 3 - 6h
        bundle.putString("testBalance", testBalance);
        bundle.putString("realBalance", realBalance);
        bundle.putString("autoBalance1", automaticList.get(0));
        bundle.putString("autoBalance2", automaticList.get(1));
        bundle.putString("autoBalance3", automaticList.get(2));
        bundle.putString("autoBalance4", automaticList.get(3));
        bundle.putString("autoBalance5", automaticList.get(4));

        intent.putExtra("bundleResultsStatus", bundle);
        LocalBroadcastManager.getInstance(ResultsService.this).sendBroadcast(intent);
        stopForeground(true);
        stopSelf();
    }

}
