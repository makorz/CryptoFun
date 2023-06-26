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

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.R;
import com.example.cryptofun.data.PositionRisk;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.ui.orders.OrderListViewElement;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class OrdersService extends Service implements CallbackButton {

    private static final String TAG = "ORDService";

    private DBHandler databaseDB;
    private static final String TABLE_NAME_ORDERS = "current_orders";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String VALUE_REAL = "value_real";
    private static final String ID = "id";
    private static final String CURRENT_PRICE = "current_price";
    private static final String STOP_LIMIT = "stop_limit_price";

    ArrayList<OrderListViewElement> returnList = new ArrayList<>();

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


    private void sendMessageToActivity(ArrayList<OrderListViewElement> listOrders, String test,  ArrayList<String> automatic) { //String real,

        Intent intent = new Intent("OrdersStatus");
        Log.e(TAG, "SendMessage " + Thread.currentThread() + " " + Thread.activeCount());
        Bundle bundle = new Bundle();
        // 1 - 30min, 2 - 2h, 3 - 6h
        bundle.putSerializable("ordersList", (Serializable) listOrders);
        bundle.putString("testBalance", test);
//        bundle.putString("realBalance", real);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroy");
    }

    private void updatingCurrentOrders() {

        Cursor data = databaseDB.retrieveAllFromTable(TABLE_NAME_ORDERS);

        if (data.getCount() == 0) {
            Log.e(TAG, "No active orders");
        } else {
            while (data.moveToNext()) {

                OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getInt(13), data.getString(14), data.getFloat(15));
                returnList.add(tempToken);

            }
        }
        data.close();

        ServiceFunctions.getAllOrders(System.currentTimeMillis(), getApplicationContext());

        Collections.sort(returnList, new Comparator<OrderListViewElement>() {
            public int compare(OrderListViewElement o1, OrderListViewElement o2) {
                return Long.compare(o1.getTimeWhenPlaced(), o2.getTimeWhenPlaced());
            }
        });

        for (int i = 0; i < returnList.size(); i++) {

            /*
            TODO:
                    Może dorzucę markprice tutaj? hmmmm
             */

            float price = 0;
            Cursor data2 = databaseDB.retrieveLastClosePrice(returnList.get(i).getSymbol());
            if (data2.getCount() == 0) {
                Log.e(TAG, "There is no crypto with that symbol");
            } else {
                data2.moveToFirst();
                price = data2.getFloat(6);

                float percentPrevious = returnList.get(i).getPercentOfPriceChange();
                databaseDB.updatePricesOfCryptoInOrder(returnList.get(i).getSymbol(), CURRENT_PRICE, price, returnList.get(i).getTimeWhenPlaced());
                returnList.get(i).setCurrentPrice(price);
                float percentNow = returnList.get(i).getPercentOfPriceChange();
                String symbol = returnList.get(i).getSymbol();

                long currentTime = System.currentTimeMillis();
                long orderTime = returnList.get(i).getTimeWhenPlaced();
                long oneHour = 3600000;
                long sixHours = 21600000;
                long twoHours = 7200000;
                long threeAndHalfHours = 12600000;
                long fourHours = 14400000;
                long eightHours = 25200000;
                long tenHours = 36000000;
                long minutes45 = 2700000;
                long minutes15 = 900000;
                long halfHour = 1800000;
                long minutes8 = 480000;

                String infoOfOrder = " SYMBOL: " + returnList.get(i).getSymbol() + " Exit$# " + returnList.get(i).getCurrentAmount() + " CP# " + price + " PP# " + returnList.get(i).getCurrentPrice() + " SL# " + returnList.get(i).getStopLimitPrice() + " TP# " + returnList.get(i).getTakeProfitPrice() + " isItSHORT# " + returnList.get(i).getIsItShort()  + " Percent Now# "
                        + percentNow + " Percent previous# " + percentPrevious + " Time when placed# " + orderTime + " Time + 10h#  " + (orderTime + tenHours) + "current time# " + currentTime;

                if (returnList.get(i).getIsItShort() == 1) {

                    //We don't take stop limit price when closing test order - we take amount when we check prices, so it can be lower if big move was made, stop limit would prevent of that
                    if (price > returnList.get(i).getStopLimitPrice()) {
                        returnList.get(i).setCurrentPrice(returnList.get(i).getStopLimitPrice());

                        Log.e(TAG,  "LEVEL6 STOP (SHORT) " + infoOfOrder);
                        ServiceFunctions.writeToFile("LEVEL6 STOP (SHORT) " + infoOfOrder, getApplicationContext(), "result");
                        ServiceFunctions.writeToFile("LEVEL6 STOP (SHORT) " + infoOfOrder, getApplicationContext(), "orders");
                        closeOrder(i,  returnList);

                    }
                    if (price < returnList.get(i).getTakeProfitPrice()) {
                        returnList.get(i).setCurrentPrice(returnList.get(i).getTakeProfitPrice());

                        Log.e(TAG,  "LEVEL6 TAKE (SHORT) " + infoOfOrder);
                        ServiceFunctions.writeToFile("LEVEL6 TAKE (SHORT) " + infoOfOrder, getApplicationContext(),"result");
                        ServiceFunctions.writeToFile("LEVEL6 TAKE (SHORT) " + infoOfOrder, getApplicationContext(),"orders");
                        closeOrder(i, returnList);

                    }
                } else {
                    if (price < returnList.get(i).getStopLimitPrice()) {
                        returnList.get(i).setCurrentPrice(returnList.get(i).getStopLimitPrice());

                        Log.e(TAG,  "LEVEL6 STOP (LONG) " + infoOfOrder);
                        ServiceFunctions.writeToFile("LEVEL6 STOP (LONG) " + infoOfOrder, getApplicationContext(), "result");
                        ServiceFunctions.writeToFile("LEVEL6 STOP (LONG)" + infoOfOrder, getApplicationContext(), "orders");
                        closeOrder(i,  returnList);

                    }
                    if (price > returnList.get(i).getTakeProfitPrice()) {
                        returnList.get(i).setCurrentPrice(returnList.get(i).getTakeProfitPrice());

                        Log.e(TAG,  "LEVEL6 TAKE (LONG)" + infoOfOrder);
                        ServiceFunctions.writeToFile("LEVEL6 TAKE (LONG) " + infoOfOrder, getApplicationContext(),"result");
                        ServiceFunctions.writeToFile("LEVEL6 TAKE (LONG) " + infoOfOrder, getApplicationContext(),"orders");
                        closeOrder(i,  returnList);


                    }
                }

                Log.e(TAG, "LEVEL6 --> " + returnList.get(i).getSymbol() + " " + returnList.get(i).getOrderType() + " " + returnList.get(i).getIsItReal() + " " + returnList.get(i).getStopLimitPrice());

                /*
                TODO:
                    Aktualizuje stary stop limit na nowszy z mniej korzystna cena  - do sprawdzenia
                 */

                if (percentNow > 0.75 && percentNow > percentPrevious && returnList.get(i).getIsItShort() == 0 && returnList.get(i).getOrderType().equals("MARKET")) {

                    Log.e(TAG, "LEVEL6(Previous SL - LONG): " + returnList.get(i).getStopLimitPrice());
                    float stopLimitPrice = returnList.get(i).getCurrentPrice() * 0.99f; // 0.995f

                    if (returnList.get(i).getIsItReal() == 0 && stopLimitPrice > returnList.get(i).getStopLimitPrice()) {

                        returnList.get(i).setStopLimitPrice(stopLimitPrice);
                        databaseDB.updatePricesOfCryptoInOrder(returnList.get(i).getSymbol(), STOP_LIMIT, stopLimitPrice, returnList.get(i).getTimeWhenPlaced());
                        Log.e(TAG, "LEVEL6(New SL - LONG - TEST): " + returnList.get(i).getStopLimitPrice());

                    }

                    if (returnList.get(i).getIsItReal() == 1 && stopLimitPrice > returnList.get(i).getStopLimitPrice()) {

                        returnList.get(i).setStopLimitPrice(stopLimitPrice);
                        databaseDB.updatePricesOfCryptoInOrder(returnList.get(i).getSymbol(), STOP_LIMIT, stopLimitPrice, returnList.get(i).getTimeWhenPlaced());
                        Log.e(TAG, "LEVEL6(New SL - LONG - REAL): " + returnList.get(i).getStopLimitPrice());

                        boolean isThereStopLimitForThatSymbol = false;
                        long stopLimitOrderId = 0;
                        long timeOfStopLimitOrderPlacement = 0;

                        for (int j = 0; j < returnList.size(); j++) {


                            if (returnList.get(j).getSymbol().equals(symbol) && returnList.get(j).getOrderType().equals("STOP_MARKET") && returnList.get(j).getIsItReal() == 1) {
                                isThereStopLimitForThatSymbol = true;
                                stopLimitOrderId = returnList.get(j).getOrderID();
                                timeOfStopLimitOrderPlacement = returnList.get(j).getTimeWhenPlaced();
                            }

                        }

                        if (isThereStopLimitForThatSymbol) {
                            Log.e(TAG,"LEVEL6.5 SL - NEW - START " + stopLimitPrice);
                            ServiceFunctions.updateStopLimitForOrder(returnList.get(i).getSymbol(), stopLimitOrderId , "SELL","STOP_MARKET","RESULT" , stopLimitPrice , "true", timeOfStopLimitOrderPlacement, System.currentTimeMillis(), 0, getApplicationContext(), returnList.get(i) , null );

                        } else {
                            Log.e(TAG,"LEVEL6.6 SL - NEW - START " + stopLimitPrice);
                            ServiceFunctions.setStopLimitOrTakeProfitMarket(returnList.get(i).getSymbol(), "SELL", "STOP_MARKET", "RESULT" , stopLimitPrice , "true", System.currentTimeMillis(), 0, returnList.get(i), getApplicationContext(),  null);

                        }

                    }
                } else if (percentNow < -0.75 && percentPrevious > percentNow && returnList.get(i).getIsItShort() == 1 && returnList.get(i).getOrderType().equals("MARKET")) {

                    Log.e(TAG, "LEVEL6(Previous SL - SHORT):" + returnList.get(i).getStopLimitPrice());
                    float stopLimitPrice = returnList.get(i).getCurrentPrice() * 1.01f; // 1.005f

                    if (returnList.get(i).getIsItReal() == 0 && stopLimitPrice < returnList.get(i).getStopLimitPrice() ) {

                        returnList.get(i).setStopLimitPrice(stopLimitPrice);
                        databaseDB.updatePricesOfCryptoInOrder(returnList.get(i).getSymbol(), STOP_LIMIT, stopLimitPrice, returnList.get(i).getTimeWhenPlaced());
                        Log.e(TAG, "LEVEL6(New SL - SHORT - TEST): " + returnList.get(i).getStopLimitPrice());

                    }

                    if (returnList.get(i).getIsItReal() == 1 && stopLimitPrice < returnList.get(i).getStopLimitPrice()) {

                        returnList.get(i).setStopLimitPrice(stopLimitPrice);
                        Log.e(TAG, "LEVEL6(New SL - SHORT - REAL): " + stopLimitPrice);

                        boolean isThereStopLimitForThatSymbol = false;
                        long stopLimitOrderId = 0;
                        long timeOfStopLimitOrderPlacement = 0;

                        for (int j = 0; j < returnList.size(); j++) {

                            if (returnList.get(j).getSymbol().equals(symbol) && returnList.get(j).getOrderType().equals("STOP_MARKET") && returnList.get(j).getIsItReal() == 1) {
                                isThereStopLimitForThatSymbol = true;
                                stopLimitOrderId = returnList.get(j).getOrderID();
                                timeOfStopLimitOrderPlacement = returnList.get(j).getTimeWhenPlaced();
                            }

                        }

                        if (isThereStopLimitForThatSymbol) {
                            Log.e(TAG,"LEVEL6.5 SL - NEW - START " + stopLimitPrice);
                            ServiceFunctions.updateStopLimitForOrder(returnList.get(i).getSymbol(), stopLimitOrderId , "BUY","STOP_MARKET","RESULT" , stopLimitPrice , "true", timeOfStopLimitOrderPlacement, System.currentTimeMillis(), 0, getApplicationContext(), returnList.get(i) , null );

                        } else {
                            Log.e(TAG,"LEVEL6.6 SL - NEW - START " + stopLimitPrice);
                            ServiceFunctions.setStopLimitOrTakeProfitMarket(returnList.get(i).getSymbol(), "BUY", "STOP_MARKET", "RESULT" , stopLimitPrice , "true", System.currentTimeMillis(), 0, returnList.get(i), getApplicationContext(),  null);

                        }
                    }
                }

                else if (((orderTime + eightHours) < currentTime)
                        && returnList.get(i).getIsItShort() == 0
                        && returnList.get(i).getEntryPrice() * 0.995  > returnList.get(i).getCurrentPrice()
                ) {
                    Log.e(TAG,  "TIME PASSED(LONG) " +  infoOfOrder);
                    ServiceFunctions.writeToFile("TIME PASSED(LONG) " +  infoOfOrder, getApplicationContext(),"result");
                    ServiceFunctions.writeToFile("TIME PASSED(LONG) " +  infoOfOrder, getApplicationContext(),"orders");
                    closeOrder(i,  returnList);
                } else if (((orderTime + eightHours) < currentTime)
                        && returnList.get(i).getIsItShort() == 1
                        && returnList.get(i).getEntryPrice() * 1.005 < returnList.get(i).getCurrentPrice()
                ) {
                    Log.e(TAG,  "TIME PASSED(SHORT) " +  infoOfOrder);
                    ServiceFunctions.writeToFile("TIME PASSED(SHORT) " +  infoOfOrder, getApplicationContext(),"result");
                    ServiceFunctions.writeToFile("TIME PASSED(SHORT) " +  infoOfOrder, getApplicationContext(),"orders");
                    closeOrder(i, returnList);
                }

            }
            data2.close();
        }
        getTestAccountBalance();

    }

    /* TODO:
        Trzeba sprawdzać jakie zlecenia mamy dla danego symbolu realnego, jeśli nie udało się zrobić stop limita przy tworzeniu zlecenia, to trzeba próbowac i aktualziować co chwilę
        Sprawdzać trzeba wszystkie pozycje dla symboli z naszej bazy. Bo jeśli zakończy się samoistnie, to pozostanie w naszej bazie do momentu kiedy zrealizujemy cele zamknięcia (np. teraz delkatnie musnął stop limit i wszystko się zamkneło na koncie binance natomiast u nas nadal wisiały ordery (bo aktualizowane są w większym interwale niz realne działanie binance.

            Trzeba sprawdzić dlaczego zmiana stop limit wraca w drugą stronę - screeny A i B

     */

    private void closeOrder(int position, ArrayList<OrderListViewElement> returnList) {

        //(String symbol, long time, float currentAmount, int isItReal, int isItShort, int margin, int accountNumber, String orderType, float quantity, long orderId, ArrayList<OrderListViewElement> returnList)

        //(returnList.get(i).getSymbol(), returnList.get(i).getTimeWhenPlaced(), returnList.get(i).getCurrentAmount(), returnList.get(i).getIsItReal(), returnList.get(i).getIsItShort(),
        //                            returnList.get(i).getMargin(), returnList.get(i).getAccountNumber(), returnList.get(i).getOrderType(), returnList.get(i).getQunatity(), returnList.get(i).getOrderID(),  returnList);

        Log.e(TAG, "Order was closed for: " + returnList.get(position).getSymbol() + " on account nr" + returnList.get(position).getAccountNumber() + ". Is it short: " + returnList.get(position).getIsItShort()
                + ", margin: " + returnList.get(position).getMargin() + ", amount of USDT from order:" + returnList.get(position).getCurrentAmount());

//        databaseDB.deleteOrder(symbol, time, isItReal, isItShort, margin);
//        float balance = 0;

        if (returnList.get(position).getIsItReal() == 1) {
            Log.e(TAG, "Real not yet");

            if (returnList.get(position).getOrderType().equals("TAKE_PROFIT_MARKET") || returnList.get(position).getOrderType().equals("STOP_MARKET")) {

                boolean areStopOrTakeRelevant = false;

                for (int i = 0; i < returnList.size(); i++) {

                    if (returnList.get(i).getSymbol().equals(returnList.get(position).getSymbol()) && returnList.get(i).getOrderType().equals("MARKET") && returnList.get(i).getIsItReal() == 1) {
                        areStopOrTakeRelevant = true;
                    }

                }
                if (!areStopOrTakeRelevant) {
                    ServiceFunctions.deleteOrder(returnList.get(position).getSymbol(), returnList.get(position).getOrderID(), System.currentTimeMillis(), getApplicationContext(), null);
                }


            } else if (returnList.get(position).getOrderType().equals("MARKET")) {

                ServiceFunctions.getPositions(returnList.get(position).getSymbol(), System.currentTimeMillis(),getApplicationContext(), returnList.get(position), null);

            }

        } else {

            float balance = 0;

            databaseDB.deleteOrder(returnList.get(position).getSymbol(), returnList.get(position).getTimeWhenPlaced(), returnList.get(position).getIsItReal(),
                    returnList.get(position).getIsItShort(), returnList.get(position).getMargin());

            Cursor data = databaseDB.retrieveParam(returnList.get(position).getAccountNumber());
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr " + returnList.get(position).getAccountNumber());
            } else {
                data.moveToFirst();
                balance = data.getFloat(4);
            }
            data.close();

            String nrOfParameterForAccount = String.valueOf(returnList.get(position).getAccountNumber());
            databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, balance + returnList.get(position).getCurrentAmount(), ID, nrOfParameterForAccount);

        }
//        String nrOfParameterForAccount = String.valueOf(returnList.get(position).getAccountNumber());
//        databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, balance + returnList.get(position).getCurrentAmount(), ID, nrOfParameterForAccount);

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
        sendMessageToActivity(returnList, testBalance, automaticBalance);
        ServiceFunctions.getRealAccountBalance(getApplicationContext(),null);
    }

    @Override
    public void onSuccess() {

    }

    @Override
    public void onError() {

    }
}
