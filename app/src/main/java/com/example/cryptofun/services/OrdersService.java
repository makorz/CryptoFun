package com.example.cryptofun.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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

import com.example.cryptofun.MainActivity;
import com.example.cryptofun.R;
import com.example.cryptofun.data.AccountBalance;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.retrofit.RetrofitClientSecret;
import com.example.cryptofun.ui.view.OrderListViewElement;

import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersService extends Service {

    private static final String TAG = "ORDService";

    private DBHandler databaseDB;
    private static final String TABLE_NAME_ORDERS = "current_orders";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String VALUE_REAL = "value_real";
    private static final String ID = "id";
    private static final String IS_IT_REAL = "isReal";
    private static final String ENTRY_AMOUNT = "entry_amount";
    private static final String ENTRY_PRICE = "entry_price";
    private static final String CURRENT_PRICE = "current_price";
    private static final String STOP_LIMIT = "stop_limit_price";
    private static final String TAKE_PROFIT = "take_profit_price";
    private static final String MARGIN = "margin";
    private static final String TIME_WHEN_PLACED = "time_when_placed";
    private static final String IS_IT_CROSSED = "isCrossed";
    private static final String IS_IT_SHORT = "isShort";
    private static final String WHAT_ACCOUNT = "account_nr";

    ArrayList<OrderListViewElement> returnList = new ArrayList<>();

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
                        Notification notification = createNotification();
                        // Notification ID cannot be 0.
                        startForeground(1, notification);
                        updatingCurrentOrders();

                    }
                }
        ).start();

        return START_STICKY;
    }


    private Notification createNotification() {

        String CHANNEL_ID = "cryptoFun";
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
                .setContentText("Orders verification.")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.crypto_fun_logo);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void sendMessageToActivity(ArrayList<OrderListViewElement> listOrders, String test, String real, ArrayList<String> automatic) {

        Intent intent = new Intent("OrdersStatus");
        Log.e(TAG, "SendMessage " + Thread.currentThread() + " " + Thread.activeCount());
        Bundle bundle = new Bundle();
        // 1 - 30min, 2 - 2h, 3 - 6h
        bundle.putSerializable("ordersList", (Serializable) listOrders);
        bundle.putString("testBalance", test);
        bundle.putString("realBalance", real);
        bundle.putString("autoBalance1", automatic.get(0));
        bundle.putString("autoBalance2", automatic.get(1));
        bundle.putString("autoBalance3", automatic.get(2));
        bundle.putString("autoBalance4", automatic.get(3));
        bundle.putString("autoBalance5", automatic.get(4));
        intent.putExtra("bundleOrdersStatus", bundle);
        LocalBroadcastManager.getInstance(OrdersService.this).sendBroadcast(intent);
        stopForeground(true);
        stopSelf();
    }


    private void updatingCurrentOrders() {

        Cursor data = databaseDB.retrieveAllFromTable(TABLE_NAME_ORDERS);

        if (data.getCount() == 0) {
            Log.e(TAG, "No active orders");
        } else {
            while (data.moveToNext()) {

                OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12));
                returnList.add(tempToken);

            }
        }
        data.close();

        Collections.sort(returnList, new Comparator<OrderListViewElement>() {
            public int compare(OrderListViewElement o1, OrderListViewElement o2) {
                return Long.compare(o1.getTimeWhenPlaced(), o2.getTimeWhenPlaced());
            }
        });

        for (int i = 0; i < returnList.size(); i++) {

            float price = 0;
            Cursor data2 = databaseDB.retrieveLastClosePrice(returnList.get(i).getSymbol());
            if (data2.getCount() == 0) {
                Log.e(TAG, "There is no crypto with that symbol");
            } else {
                data2.moveToFirst();
                price = data2.getFloat(6);

                Log.e(TAG, "Is it close time? SYMBOL: " + returnList.get(i).getSymbol() + " CP: " + price + " PP: " + returnList.get(i).getCurrentPrice());

                float percentPrevious = returnList.get(i).getPercentOfPriceChange();
                databaseDB.updateCurrentPriceOfCryptoInOrders(returnList.get(i).getSymbol(), CURRENT_PRICE, price, returnList.get(i).getTimeWhenPlaced());
                returnList.get(i).setCurrentPrice(price);
                float percentNow = returnList.get(i).getPercentOfPriceChange();


                Log.e(TAG, "Is it close time? SYMBOL: " + returnList.get(i).getSymbol() + " SL: " + returnList.get(i).getStopLimitPrice() + " TP: "
                        + returnList.get(i).getTakeProfitPrice() + " SHORT?: " + returnList.get(i).getIsItShort() + " CA: " + returnList.get(i).getCurrentAmount() + " Percent Now: "
                        + percentNow + " Percent previous: " + percentPrevious);

                long currentTime = System.currentTimeMillis();
                long orderTime = returnList.get(i).getTimeWhenPlaced();
                long oneHour = 3600000;
                long twoHours = 7200000;
                long minutes45 = 2700000;
                long halfHour = 1800000;
                long minutes3 = 180000;

                if (returnList.get(i).getIsItShort() == 1) {

                    if (price > returnList.get(i).getStopLimitPrice()) {
                        closeOrder(returnList.get(i).getSymbol(), returnList.get(i).getTimeWhenPlaced(), returnList.get(i).getCurrentAmount(), returnList.get(i).getIsItReal(), returnList.get(i).getIsItShort(),
                                returnList.get(i).getMargin(), returnList.get(i).getAccountNumber());
                    }
                    if (price < returnList.get(i).getTakeProfitPrice()) {
                        closeOrder(returnList.get(i).getSymbol(), returnList.get(i).getTimeWhenPlaced(), returnList.get(i).getCurrentAmount(), returnList.get(i).getIsItReal(), returnList.get(i).getIsItShort(),
                                returnList.get(i).getMargin(), returnList.get(i).getAccountNumber());
                    }
                } else {
                    if (price < returnList.get(i).getStopLimitPrice()) {
                        closeOrder(returnList.get(i).getSymbol(), returnList.get(i).getTimeWhenPlaced(), returnList.get(i).getCurrentAmount(), returnList.get(i).getIsItReal(), returnList.get(i).getIsItShort(),
                                returnList.get(i).getMargin(), returnList.get(i).getAccountNumber());
                    }
                    if (price > returnList.get(i).getTakeProfitPrice()) {
                        closeOrder(returnList.get(i).getSymbol(), returnList.get(i).getTimeWhenPlaced(), returnList.get(i).getCurrentAmount(), returnList.get(i).getIsItReal(), returnList.get(i).getIsItShort(),
                                returnList.get(i).getMargin(), returnList.get(i).getAccountNumber());
                    }
                }

                if (percentNow > 1 && percentNow > percentPrevious && returnList.get(i).getIsItShort() == 0) {
                    Log.e(TAG, "Previous SL: " + returnList.get(i).getStopLimitPrice());
                    float stopLimitPrice = returnList.get(i).getCurrentPrice() * 0.99f;
                    returnList.get(i).setStopLimitPrice(stopLimitPrice);
                    databaseDB.updateCurrentPriceOfCryptoInOrders(returnList.get(i).getSymbol(), STOP_LIMIT, stopLimitPrice, returnList.get(i).getTimeWhenPlaced());
                    Log.e(TAG, "New SL: " + returnList.get(i).getStopLimitPrice());
                } else if (percentNow < -1 && percentPrevious > percentNow && returnList.get(i).getIsItShort() == 1) {
                    Log.e(TAG, "Previous SL: " + returnList.get(i).getStopLimitPrice());
                    float stopLimitPrice = returnList.get(i).getCurrentPrice() * 1.01f;
                    returnList.get(i).setStopLimitPrice(stopLimitPrice);
                    databaseDB.updateCurrentPriceOfCryptoInOrders(returnList.get(i).getSymbol(), STOP_LIMIT, stopLimitPrice, returnList.get(i).getTimeWhenPlaced());
                    Log.e(TAG, "New SL: " + returnList.get(i).getStopLimitPrice());
                } else if (((orderTime + halfHour) < currentTime) && returnList.get(i).getIsItShort() == 0 && returnList.get(i).getEntryPrice() * 0.995 > returnList.get(i).getCurrentPrice()) {

                    closeOrder(returnList.get(i).getSymbol(), returnList.get(i).getTimeWhenPlaced(), returnList.get(i).getCurrentAmount(), returnList.get(i).getIsItReal(), returnList.get(i).getIsItShort(),
                            returnList.get(i).getMargin(), returnList.get(i).getAccountNumber());
                } else if (((orderTime + halfHour) < currentTime) && returnList.get(i).getIsItShort() == 1 && returnList.get(i).getEntryPrice() * 1.005 < returnList.get(i).getCurrentPrice()) {

                    closeOrder(returnList.get(i).getSymbol(), returnList.get(i).getTimeWhenPlaced(), returnList.get(i).getCurrentAmount(), returnList.get(i).getIsItReal(), returnList.get(i).getIsItShort(),
                            returnList.get(i).getMargin(), returnList.get(i).getAccountNumber());
                }

            }
            data2.close();
        }
        getTestAccountBalance();

    }

    public void closeOrder(String symbol, long time, float currentAmount, int isItReal, int isItShort, int margin, int accountNumber) {

        Log.e(TAG, "Order was closed for: " + symbol + " on account nr" + accountNumber + ". Is it short: " + isItShort + ", margin: " + margin + ", amount of USDT from order:" + currentAmount);

        databaseDB.deleteOrder(symbol, time, isItReal, isItShort, margin);
        float balance = 0;
        if (isItReal == 1) {
            Log.e(TAG, "Real not yet");
        } else {
            Cursor data = databaseDB.retrieveParam(accountNumber);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr " + accountNumber);
            } else {
                data.moveToFirst();
                balance = data.getFloat(4);
            }
            data.close();
        }
        String nrOfParameterForAccount = String.valueOf(accountNumber);
        databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, balance + currentAmount, ID, nrOfParameterForAccount);

    }

    private void getTestAccountBalance() {

        String testBalance = "";
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
                Log.e(TAG, "There is no param for test account " + (i - 5));
                databaseDB.addParam(i, "Test account nr " + (i - 5) + " balance", "", 0, 100);
                automaticBalance.add("100.00");
            } else if (data.getCount() >= 2) {
                databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, i);
                databaseDB.addParam(i, "Test account nr " + (i - 5) + " balance", "", 0, 100);
                automaticBalance.add("100.00");
            } else {
                data.moveToFirst();
                float balance = data.getFloat(4);
                DecimalFormat dfNr = new DecimalFormat("0.00");
                automaticBalance.add(dfNr.format(balance));
            }
            data.close();
        }

        getRealAccountBalance(testBalance, automaticBalance);
    }

    private void getRealAccountBalance(String testBalance, ArrayList<String> automaticBalance) {
        Call<List<AccountBalance>> call = RetrofitClientSecret.getInstance(getApplicationContext()).getMyApi().getAccountBalance();
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
                                String realBalance = dfNr.format(balanceList.get(i).getBalance());
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
                                sendMessageToActivity(returnList, testBalance, realBalance, automaticBalance);
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
                    sendMessageToActivity(returnList, testBalance, "No data.", automaticBalance);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AccountBalance>> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e(TAG, String.valueOf(t));
                sendMessageToActivity(returnList, testBalance, "No data.", automaticBalance);
            }

        });

    }


}
