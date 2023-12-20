package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.cryptofun.data.MarkPrice;
import com.example.cryptofun.data.PercentagesOfChanges;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.data.database.Kline;
import com.example.cryptofun.retrofit.RetrofitClientFutures;
import com.example.cryptofun.ui.home.GridViewElement;
import com.example.cryptofun.ui.home.ListViewElement;
import com.example.cryptofun.ui.orders.OrderListViewElement;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ApprovingService extends Service {

    private static final String TAG = "APRVService";

    private static final String TABLE_HISTORIC_PERCENTAGES = "history_percentages";
    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private static final String ID = "id";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String TABLE_NAME_ORDERS = "current_orders";
    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String TABLE_NAME_APPROVED_HISTORIC = "historic_approved_tokens";

    int howManyNeedsToDo = 6;
    int serviceFinishedEverything = 0;
    int isAutomaticForTestEnabled = 0;
    int isAutomaticForRealEnabled = 0;
    private DBHandler databaseDB;
    private Handler handler;
    private final List<String> listOfSymbols = new ArrayList<>();
    ArrayList<ListViewElement> last14HoursTokensStat = new ArrayList<>();
    ArrayList<ListViewElement> last3MinutesTokensStat = new ArrayList<>();
    ArrayList<GridViewElement> cryptoGridViewList = new ArrayList<>();
    ArrayList<ListViewElement> cryptoForLONGOrders = new ArrayList<>();
    ArrayList<ListViewElement> cryptoForSHORTOrders = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        databaseDB = DBHandler.getInstance(getApplicationContext());

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        Log.e(TAG, "START of Service Thread");
                        // It's for foreground services, because in newest Android, background are not working. Foreground need to inform user that it is running
                        Notification notification = ServiceFunctionsOther.createNotificationSimple("Calculating what's best.", TAG, getApplicationContext());
                        // Notification ID cannot be 0.
                        startForeground(1, notification);
                        approvingCryptos();

                    }
                }
        ).start();

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "DESTROY");
    }

    private void sendMessageToActivity() {
        Log.e(TAG, "RESULT: " + serviceFinishedEverything + " HOW: " + howManyNeedsToDo);

        if (isAutomaticForRealEnabled == 1) {

            //Verification of missed orders remote and local
            ArrayList<OrderListViewElement> returnList = new ArrayList<>();
            Cursor data = databaseDB.retrieveAllFromTable(TABLE_NAME_ORDERS);
            data.moveToFirst();
            if (data.getCount() > 0) {
                do {
                    OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getLong(13), data.getString(14), data.getFloat(15));
                    returnList.add(tempToken);
                } while (data.moveToNext());
            }
            data.close();
            ServiceFunctionsAPI.getAllOrders(System.currentTimeMillis(), getApplicationContext(), returnList);
        }

        if (serviceFinishedEverything >= howManyNeedsToDo) {
            Intent intent = new Intent("ApprovedService");
            Log.e(TAG, "BROADCAST - Approve Finished " + Thread.currentThread() + " " + Thread.activeCount());
            Bundle bundle = new Bundle();
            // 1 - 30min, 2 - 2h, 3 - 6h
            bundle.putSerializable("list1", (Serializable) last3MinutesTokensStat);
            bundle.putSerializable("list3", (Serializable) last14HoursTokensStat);
            bundle.putSerializable("cryptoGridViewList", (Serializable) cryptoGridViewList);
            intent.putExtra("bundleApprovedCrypto", bundle);
            LocalBroadcastManager.getInstance(ApprovingService.this).sendBroadcast(intent);

            //Wait 3 second before stopping service, error:  Process: com.example.cryptofun, PID: 6921 android.app.ForegroundServiceDidNotStartInTimeException: Context.startForegroundService() did not then cal Service.startForeground():
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopForeground(true);
                    stopSelf();
                }
            }, 2000);

        }
    }

    public void approvingCryptos() {

        long fourteenHours = 50400000;
        //Wait two minutes before entering
        long halfMinute = 30000;
        long fourMinutes = 240000;
        long twoMinutes = 120000;

        last14HoursTokensStat = getListOfSymbolsAccordingToProvidedTime(fourteenHours, 0, TABLE_NAME_APPROVED_HISTORIC); //oneMinute
        last3MinutesTokensStat = getListOfSymbolsAccordingToProvidedTime(twoMinutes, 0, TABLE_NAME_APPROVED); //oneMinute

        Log.e(TAG, "list3mTokensSize: " + last3MinutesTokensStat.size());

        if (last3MinutesTokensStat.size() > 0) {
            for (int i = 0; i < last3MinutesTokensStat.size(); i++) {
                if (last3MinutesTokensStat.get(i).isItLONG()) {
                    cryptoForLONGOrders.add(last3MinutesTokensStat.get(i));
                } else {
                    cryptoForSHORTOrders.add(last3MinutesTokensStat.get(i));
                }
            }
        }
        setMainParametersOnView();

    }

    public void setMainParametersOnView() {

        // First array list for grid view, other two need to calculate changes in percents hour after hour
        // ArrayList<GridViewElement> cryptoPercentageListGrid3hours = new ArrayList<>();
        ArrayList<GridViewElement> cryptoPercentageList = new ArrayList<>();
        float percentU2 = 0;
        float percentU1 = 0;
        float percentU0 = 0;
        float percentO0 = 0;
        float percentO1 = 0;
        float percentO2 = 0;

        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_SYMBOL_AVG + " is empty");
        } else {
            listOfSymbols.clear();
            do {
                listOfSymbols.add(data.getString(0));
            } while (data.moveToNext());
            int nrOfGridElements = 30;
            List<String> biggestNrOfTradesSymbols = getBiggestNrOfTradesSymbols("4h", nrOfGridElements);

            for (int i = 0; i < listOfSymbols.size(); i++) {
                cryptoPercentageList.add(getTimePercentChangeForCrypto(listOfSymbols.get(i), 16));
                if (cryptoPercentageList.get(i) == null) {
                    cryptoPercentageList.remove(i);
                }
            }

            int under2 = 0;
            int under1 = 0;
            int under0 = 0;
            int over0 = 0;
            int over1 = 0;
            int over2 = 0;

            for (int i = 0; i < cryptoPercentageList.size(); i++) {

                if (cryptoPercentageList.get(i).getPercent() < -2) {
                    under2++;
                } else if (cryptoPercentageList.get(i).getPercent() >= -2 && cryptoPercentageList.get(i).getPercent() < -1) {
                    under1++;
                } else if (cryptoPercentageList.get(i).getPercent() >= -1 && cryptoPercentageList.get(i).getPercent() < 0) {
                    under0++;
                } else if (cryptoPercentageList.get(i).getPercent() >= 0 && cryptoPercentageList.get(i).getPercent() < 1) {
                    over0++;
                } else if (cryptoPercentageList.get(i).getPercent() >= 1 && cryptoPercentageList.get(i).getPercent() <= 2) {
                    over1++;
                } else if (cryptoPercentageList.get(i).getPercent() > 2) {
                    over2++;
                }
            }

            percentU2 = (float) under2 / cryptoPercentageList.size() * 100;
            percentU1 = (float) under1 / cryptoPercentageList.size() * 100;
            percentU0 = (float) under0 / cryptoPercentageList.size() * 100;
            percentO0 = (float) over0 / cryptoPercentageList.size() * 100;
            percentO1 = (float) over1 / cryptoPercentageList.size() * 100;
            percentO2 = (float) over2 / cryptoPercentageList.size() * 100;

            PercentagesOfChanges changes = new PercentagesOfChanges(percentU2, percentU1, percentU0, percentO0, percentO1, percentO2, System.currentTimeMillis());
            databaseDB.addPercentages(changes);

            if (biggestNrOfTradesSymbols.size() > 1) {
                cryptoGridViewList.add(new GridViewElement("< -2%", percentU2, under2));
                cryptoGridViewList.add(new GridViewElement("-2% - -1%", percentU1, under1));
                cryptoGridViewList.add(new GridViewElement("-1% - 0%", percentU0, under0));
                cryptoGridViewList.add(new GridViewElement("0% - 1%", percentO0, over0));
                cryptoGridViewList.add(new GridViewElement("1% - 2%", percentO1, over1));
                cryptoGridViewList.add(new GridViewElement("> 2%", percentO2, over2));

                if (biggestNrOfTradesSymbols.size() < nrOfGridElements) {
                    nrOfGridElements = biggestNrOfTradesSymbols.size();
                }

                for (int i = 0; i < nrOfGridElements; i++) {
                    cryptoGridViewList.add(getTimePercentChangeForCrypto(biggestNrOfTradesSymbols.get(i), 3));
                }
            }


        }
        data.close();

        // Check if AUTOMATIC is turned on.
        data = databaseDB.retrieveParam(16);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 16");
        } else {
            data.moveToFirst();
            isAutomaticForTestEnabled = data.getInt(3);
        }
        data.close();

        data = databaseDB.retrieveParam(15);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 15");
        } else {
            data.moveToFirst();
            isAutomaticForRealEnabled = data.getInt(3);
        }
        data.close();

        //If it is on => go on
        if (isAutomaticForTestEnabled == 1 || isAutomaticForRealEnabled == 1) {

            ArrayList<PercentagesOfChanges> percentages = new ArrayList<>();
            long now = System.currentTimeMillis();
            long tenMinutes = 600000;
            long halfHour = 1800000;

            data = databaseDB.retrievePercentages(now - tenMinutes, now);
            data.moveToFirst();
            if (data.getCount() == 0) {
                Log.e(TAG, "Table " + TABLE_HISTORIC_PERCENTAGES + " is empty");
            } else {
                do {
                    percentages.add(new PercentagesOfChanges(data.getFloat(1), data.getFloat(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getLong(7)));
                } while (data.moveToNext());
            }
            data.close();

            boolean isItGoodForShort = false;
            boolean isItGoodForLong = false;
            if (percentages.size() > 6) {
                isItGoodForLong = isPercentageInFavor(percentages, 0);
                isItGoodForShort = isPercentageInFavor(percentages, 1);
                databaseDB.clearHistoricPercentages(now - halfHour);
            }

            String infoOfOrder = "LEVEL 3 AutomaticTest: " + isAutomaticForTestEnabled + " AutomaticReal: " + isAutomaticForRealEnabled + " Percentage Favor(long, short): " + isItGoodForLong + " " + isItGoodForShort + " ListSize(Long, Short): " + cryptoForLONGOrders.size() + " " + cryptoForSHORTOrders.size() + " Under: " + percentU0 + "% " + percentU1 + "% " + percentU2 + " Over: " + percentO0 + "% " + percentO1 + "% " + percentO2 + "%";
            Log.e(TAG, infoOfOrder);
            ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "result");

            if (cryptoForSHORTOrders.size() > cryptoForLONGOrders.size()){ //&& isItGoodForShort) { // && isItGoodForShort       && percentU5 < 20 && percentU2 > 5 && underPercentage > overPercentage + 10  && percentU2 < 10 && underPercentage2 > 58 //cryptoForLONGOrders.size() + 1
                serviceFinishedEverything++;
                automaticOrdersFunction(cryptoForSHORTOrders, isAutomaticForTestEnabled, isAutomaticForRealEnabled);


            } else if (cryptoForLONGOrders.size() > cryptoForSHORTOrders.size()){ //&& isItGoodForLong) { //   // && isItGoodForLong      && percentO5 < 20 && percentO2 > 5 && underPercentage + 10 < overPercentage && percentO2 < 10 && overPercentage2 > 58
                serviceFinishedEverything++;
                automaticOrdersFunction(cryptoForLONGOrders, isAutomaticForTestEnabled, isAutomaticForRealEnabled);

            } else {
                serviceFinishedEverything = 6;
                sendMessageToActivity();
            }

        } else {
            serviceFinishedEverything = 6;
            sendMessageToActivity();
        }

    }

    private ArrayList<ListViewElement> getListOfSymbolsAccordingToProvidedTime(long timeFrom, long timeTo, String tableName) {

        long currentTime = System.currentTimeMillis();
        ArrayList<ListViewElement> returnList = new ArrayList<>();
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("HH:mm:ss - EEE, dd");
        @SuppressLint("SimpleDateFormat") DateFormat df2 = new SimpleDateFormat("HH:mm");

        Cursor data = databaseDB.firstAppearOfTokenInCertainTimeV1(currentTime - timeFrom, currentTime - timeTo, tableName);
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "Nothing in [historic_approved_tokens 1]");
        } else {
            do {
                int isItLongInt = data.getInt(1);
                boolean isItLong;
                if (isItLongInt == 1) {
                    isItLong = true;
                } else {
                    isItLong = false;
                }
                String symbol = data.getString(0);
                long approveTime = data.getLong(2);
                float approvedPrice = data.getFloat(3);

                //We are searching for highest/lowest price in certain time for that symbol and then look for opposite high/low between entry and opposite
                float maxMinPrice = 0;
                float maxMinPrice2 = 0;
                long closeTimeMaxMin = 0;

                Log.e(TAG, "1 " + symbol + " IsItLong: " + isItLong + " TimeApproved: " + df.format(approveTime) + " TimeFrom: " + df.format((currentTime - timeFrom)) + " TimeTo: "
                        + df.format((currentTime - timeTo)) + " ApprovedPrice: " + approvedPrice + " MaxMinPrice: " + maxMinPrice + " MaxMinPrice2: " + maxMinPrice2 + " CloseTimeMaxMin: " + df.format(closeTimeMaxMin) + " CurrentTime - ApprovedTime: " + (currentTime-approveTime));

                long twoHours = 3600000;

                // Because we are taking open_time ant interval 15 m, we need to take at least one Kline for start from approvetime
                Cursor data2;
                if (currentTime - approveTime < twoHours) { //900000
                    data2 = databaseDB.maxOrMinPriceForSymbolInCertainTimeAndInterval(symbol, approveTime, currentTime - timeTo, isItLong, "3m"); //approveTime - 300000
                } else {
                    data2 = databaseDB.maxOrMinPriceForSymbolInCertainTimeAndInterval(symbol, approveTime + 900000, currentTime - timeTo, isItLong, "15m"); //Without + 900000
                }

                data2.moveToFirst();
                if (data2.getCount() == 0) {
                    Log.e(TAG, "Nothing in [historic_approved_tokens 2]");
                } else {
                    maxMinPrice = data2.getFloat(1);
                    closeTimeMaxMin = data2.getLong(2);
                }
                data2.close();

                Log.e(TAG, "2 " + symbol + " IsItLong: " + isItLong + " TimeApproved: " + df.format(approveTime) + " TimeFrom: " + df.format((currentTime - timeFrom)) + " TimeTo: "
                        + df.format((currentTime - timeTo)) + " ApprovedPrice: " + approvedPrice + " MaxMinPrice: " + maxMinPrice + " MaxMinPrice2: " + maxMinPrice2 + " CloseTimeMaxMin: " + df.format(closeTimeMaxMin));

                if (currentTime - approveTime < twoHours) {
                    data2 = databaseDB.maxOrMinPriceForSymbolInCertainTimeAndInterval(symbol, approveTime, closeTimeMaxMin, !isItLong, "3m");
                } else {
                    data2 = databaseDB.maxOrMinPriceForSymbolInCertainTimeAndInterval(symbol, approveTime + 900000, closeTimeMaxMin, !isItLong, "15m"); //Without + 900000
                }

                data2.moveToFirst();
                if (data2.getCount() == 0) {
                    Log.e(TAG, "Nothing in [historic_approved_tokens 3]");
                } else {
                    maxMinPrice2 = data2.getFloat(1);
                }
                data2.close();

                if (approveTime > currentTime - timeFrom) {
                    Timestamp stamp = new Timestamp(approveTime);
                    String date = df2.format(new Date(stamp.getTime()));
                    float percentOfChange = ((maxMinPrice / approvedPrice) * 100) - 100;
                    float percentOfChange2 = ((maxMinPrice2 / approvedPrice) * 100) - 100;

                    //symbol = symbol.replace("USDT", "");

                    if (isItLong) {
                        returnList.add(new ListViewElement(symbol, percentOfChange, percentOfChange2, approvedPrice, date, isItLong));
                    } else {
                        returnList.add(new ListViewElement(symbol, percentOfChange, percentOfChange2, approvedPrice, date, isItLong));
                    }

                    Log.e(TAG, "3 " + symbol + " IsItLong: " + isItLong + " TimeApproved: " + df.format(approveTime) + " TimeFrom: " + df.format((currentTime - timeFrom)) + " TimeTo: "
                            + df.format((currentTime - timeTo)) + " ApprovedPrice: " + approvedPrice + " MaxMinPrice: " + maxMinPrice + " MaxMinPrice2: " + maxMinPrice2 + " CloseTimeMaxMin: " + df.format(closeTimeMaxMin));
                }
            } while (data.moveToNext());
        }
        data.close();

        if (returnList.size() == 0) {
            Log.e(TAG, "Nothing good in DB");
            //returnList.add(new ListViewElement("Nothing good", 0, 0, "", true));
        } else {
            returnList.sort(new Comparator<ListViewElement>() {
                public int compare(ListViewElement o1, ListViewElement o2) {
                    return o1.getTime().compareTo(o2.getTime());
                    //return Float.compare(o1.getPercentChange(), o2.getPercentChange());
                }
            });
        }
        Log.e(TAG, "Return list size: " + returnList.size());
        return returnList;
    }


    public boolean isPercentageInFavor(ArrayList<PercentagesOfChanges> percents, int isItShort) {

        int howManyTendencies = (int) (percents.size() * 0.8);
        boolean accepted = false;
        int temp = 0;

        float prevNumber = 0;
        if (isItShort == 1) {
            prevNumber = percents.get(0).getUnder1() + percents.get(0).getUnder2() + percents.get(0).getUnder3();
        } else {
            prevNumber = percents.get(0).getOver1() + percents.get(0).getOver2() + percents.get(0).getOver3();
        }

        for (int i = 1; i < percents.size(); i++) {
            float currentNumber = 0;
            if (isItShort == 1) {
                currentNumber = percents.get(i).getUnder1() + percents.get(i).getUnder2() + percents.get(i).getUnder3();
            } else {
                currentNumber = percents.get(i).getOver1() + percents.get(i).getOver2() + percents.get(i).getOver3();
            }

            if (currentNumber > prevNumber + 2) {
                temp++;
            } else if (currentNumber < prevNumber - 2) {
                temp--;
            } else {
                temp++;
            }

            //Log.e(TAG, "Value: " + currentNumber + " previous number: " + prevNumber + " index: " + i + " howMany to reach: " + howManyTendencies + " result: " + temp);

            if (temp >= howManyTendencies) {
                accepted = true;
                break;
            }
            prevNumber = currentNumber;
        }

        return accepted;
    }

    public void automaticOrdersFunction(ArrayList<ListViewElement> listOfCryptosToTry, int isAutomaticTestEnabled, int isAutomaticRealEnabled) {

        // Retrieve test accounts balances to prepare for making orders
        ArrayList<Float> testAccountBalances = new ArrayList<>();
        int margin;
        float stopLimit;
        float takeProfit;
        int realAccountNr = 3;
        float realAccountBalance;

        Cursor data;
        for (int i = 6; i < 11; i++) {
            data = databaseDB.retrieveParam(i);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param for test account " + (i - 5));
                databaseDB.addParam(i, "Test account nr " + (i - 5) + " balance", "", 0, 100);
                testAccountBalances.add(100f);
            } else if (data.getCount() >= 2) {
                databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, i);
                databaseDB.addParam(i, "Test account nr " + (i - 5) + " balance", "", 0, 100);
                testAccountBalances.add(100f);
            } else {
                data.moveToFirst();
                testAccountBalances.add(data.getFloat(4));
            }
            data.close();
        }

        data = databaseDB.retrieveParam(3);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 3");
            realAccountBalance = 0;
        } else {
            data.moveToFirst();
            realAccountBalance = data.getFloat(4);
        }
        data.close();

        data = databaseDB.retrieveAllFromTable(TABLE_NAME_ORDERS);
        ArrayList<OrderListViewElement> currentOrders = new ArrayList<>();
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "No active orders");
        } else {
            do {
                OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getInt(13), data.getString(14), data.getFloat(15));
                currentOrders.add(tempToken);
                Log.e(TAG, tempToken.toString());
            } while (data.moveToNext());
        }
        data.close();

        //Default params for my order
        data = databaseDB.retrieveParam(11);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param 11");
            databaseDB.addParam(11, "Margin automatic", "", 1, 0);
            margin = 1;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 11);
            databaseDB.addParam(11, "Margin automatic", "", 1, 0);
            margin = 1;
        } else {
            data.moveToFirst();
            margin = data.getInt(3);
        }
        data.close();

        data = databaseDB.retrieveParam(12);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param 12");
            databaseDB.addParam(12, "SL automatic", "", 0, 1);
            stopLimit = 1;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 12);
            databaseDB.addParam(12, "SL automatic", "", 0, 1);
            stopLimit = 1;
        } else {
            data.moveToFirst();
            stopLimit = data.getFloat(4);
        }
        data.close();

        data = databaseDB.retrieveParam(13);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param 13");
            databaseDB.addParam(13, "TP automatic", "", 0, 2);
            takeProfit = 2;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 13);
            databaseDB.addParam(13, "TP automatic", "", 0, 2);
            takeProfit = 2;
        } else {
            data.moveToFirst();
            takeProfit = data.getFloat(4);
        }
        data.close();
        boolean isItCrossed = false;

        ArrayList<String> currentMadeOrders = new ArrayList<>();
        //For each test account try to make order if balance is good
        for (int i = 0; i < 5; i++) {
            //Log.e(TAG, listOfCryptosToTry.toString());
            data = databaseDB.retrieveActiveOrdersOnAccount(i + 6, "MARKET", 0);
            if (data.getCount() == 0 && isAutomaticTestEnabled == 1) {

                Log.e(TAG, "No order for account nr " + (i + 6));

                if (testAccountBalances.get(i) > 30) {

                    Random random = new Random();
                    // Generate a random index that has not been used before
                    int index = random.nextInt(listOfCryptosToTry.size());
//                    String indexText = "Random test index: " + index + " size: " + listOfCryptosToTry.size();
//                    Log.e(TAG, indexText);
//                    ServiceFunctionsOther.writeToFile(indexText, getApplicationContext(), "orders");

                    // Get the element at the random index
                    ListViewElement randomElement = listOfCryptosToTry.get(index);

                    boolean thisSymbolIsAlreadyOrdered = false;

                    for (OrderListViewElement obj : currentOrders) {
                        // Check if the field value equals the search string
                        if (obj.getSymbol().equals(randomElement.getText())) {
                            thisSymbolIsAlreadyOrdered = true;
                        }
                    }

                    for (String obj2 : currentMadeOrders) {
                        // Check if the field value equals the search string
                        if (obj2.equals(randomElement.getText())) {
                            thisSymbolIsAlreadyOrdered = true;
                        }
                    }

                    if (!thisSymbolIsAlreadyOrdered) {

                        int entryAmount = (int) (testAccountBalances.get(i) * 0.98);
                        currentMadeOrders.add(randomElement.getText());

                        boolean isItShort;

                        if (randomElement.isItLONG()) {
                            isItShort = false;
                        } else {
                            isItShort = true;
                        }

                        int finalI = i;

                        getMarkPrice(randomElement.getText())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<MarkPrice>() {
                                    @Override
                                    public void onSubscribe(@NonNull Disposable d) {
                                        // do something when the subscription is made
                                    }

                                    @Override
                                    public void onNext(@NonNull MarkPrice markPrice) {

                                        // handle the MarkPrice object returned by the API
                                        String infoOfOrder = "LEVEL 4 [" + randomElement.getText() + "] " + " MarkPrice: " + markPrice.getMarkPrice() + " EntryAmount$: " + entryAmount + " AccountNr: " + (finalI + 1) + " Leverage: " + margin + " isItShort: " + isItShort ;
                                        Log.e(TAG, infoOfOrder);
                                        ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "result");
                                        ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "orders");
                                        ServiceFunctionsOther.createNotificationWithText(infoOfOrder, TAG, getApplicationContext());

                                        ServiceFunctionsAPI.makeOrderFunction(false, randomElement.getText(), entryAmount, stopLimit, takeProfit, margin, markPrice.getMarkPrice(), isItCrossed, isItShort, System.currentTimeMillis(), testAccountBalances.get(finalI), (finalI + 6), getApplicationContext(), null);

                                    }

                                    @Override
                                    public void onError(@NonNull Throwable e) {
                                        // handle any errors that occur
                                        Log.e(TAG, "An error has occurred: " + e.getMessage());
                                    }

                                    @Override
                                    public void onComplete() {
                                        // do something when the observable completes
                                    }
                                });

                    }

                }
            } else {
                Log.e(TAG, "There are active orders on account " + (i + 6));
            }
            serviceFinishedEverything++;
            data.close();
        }


        //Check how many orders can Ybe according to account balance
        int nrOfOrders = 3;
        float multiplierOfAccountBalance = 0.65f;
        if (realAccountBalance > 21 && realAccountBalance <= 100) {
            nrOfOrders = 4;
            multiplierOfAccountBalance = 0.5f;
        } else if (realAccountBalance > 100 && realAccountBalance <= 750) {
            nrOfOrders = 5;
            multiplierOfAccountBalance = 0.4f;
        } else if (realAccountBalance > 750) {
            nrOfOrders = 6;
            multiplierOfAccountBalance = 0.3f;
        }

        data = databaseDB.retrieveActiveOrdersOnAccount(1, "MARKET", 1);
        if (data.getCount() <= nrOfOrders && isAutomaticRealEnabled == 1) { ///!!!!!!!
            //Log.e(TAG, "No order for REAL account.");
            Log.e(TAG, currentOrders.toString());

            if (realAccountBalance > 5) {

                //Generate a random index that has not been used before
                Random random = new Random();
                int index = random.nextInt(listOfCryptosToTry.size());
//                String indexText = "Random real index: " + index + " size: " + listOfCryptosToTry.size();
//                Log.e(TAG, indexText);
//                ServiceFunctionsOther.writeToFile(indexText, getApplicationContext(), "orders");

                // Get the element at the random index
                ListViewElement randomElement = listOfCryptosToTry.get(index);
                boolean isThereSuchOrderForRandomizedSymbolOnRealAccount = false;

                for (int i = 0; i < currentOrders.size(); i++) {

                    if (currentOrders.get(i).getIsItReal() == 1 && currentOrders.get(i).getSymbol().equals(randomElement.getText())) {
                        isThereSuchOrderForRandomizedSymbolOnRealAccount = true;
                        Log.e(TAG, "Random symbol: " + randomElement.getText() + " Current orders symbol: " + currentOrders.get(i).getSymbol() + " is it real: " + currentOrders.get(i).getIsItReal());
                    }

                }

                int entryAmount = (int) (realAccountBalance * (multiplierOfAccountBalance * 1 + (data.getCount()/10)));
                boolean isItShort;
                isItShort = !randomElement.isItLONG();

                if (!isThereSuchOrderForRandomizedSymbolOnRealAccount) {

                    getMarkPrice(randomElement.getText())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<MarkPrice>() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {
                                    // do something when the subscription is made
                                }

                                @Override
                                public void onNext(@NonNull MarkPrice markPrice) {
                                    // handle the MarkPrice object returned by the API
                                    String infoOfOrder = "LEVEL 4 [" + randomElement.getText() + "] " + " MarkPrice: " + markPrice.getMarkPrice() + " EntryAmount$: " + entryAmount + " AccountNr: REAL Leverage: " + margin + " isItShort: " + isItShort ;
                                    Log.e(TAG, infoOfOrder);
                                    ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "result");
                                    ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "orders");
                                    ServiceFunctionsOther.createNotificationWithText(infoOfOrder, TAG, getApplicationContext());

                                    ServiceFunctionsAPI.makeOrderFunction(true, randomElement.getText(), entryAmount, stopLimit, takeProfit, margin, markPrice.getMarkPrice(), isItCrossed, isItShort, System.currentTimeMillis(), realAccountBalance, realAccountNr, getApplicationContext(), null);

                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    // handle any errors that occur
                                    Log.e(TAG, "An error has occurred: " + e.getMessage());
                                }

                                @Override
                                public void onComplete() {
                                    // do something when the observable completes
                                }
                            });
                }
            }
        } else {
            Log.e(TAG, "There is active order on REAL account ");
        }
        serviceFinishedEverything++;
        data.close();
        sendMessageToActivity();

    }

    public Observable<MarkPrice> getMarkPrice(String symbol) {
        return RetrofitClientFutures.getInstance()
                .getMyApi()
                .getMarkPrice(symbol)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // Calculate 3h change of price in %
    public GridViewElement getTimePercentChangeForCrypto(String symbol, int nrOf15mKlinesBack) {

        List<Kline> coinKlines15m = new ArrayList<>();
        Cursor data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol);
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "24hPercentCount-Table " + TABLE_NAME_KLINES_DATA + " Empty");
        } else {
            do {
                if (data.getString(10).equals("15m")) {

                    coinKlines15m.add(new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10)));

                }
            } while (data.moveToNext());

            if (coinKlines15m.size() >= nrOf15mKlinesBack) {

                float closePriceYesterday = coinKlines15m.get(nrOf15mKlinesBack - 1).gettClosePrice();
                float percentOfChange = 0;
                float closePriceToday = coinKlines15m.get(0).gettClosePrice();
                percentOfChange = ((closePriceToday / closePriceYesterday) * 100) - 100;
                data.close();
                return new GridViewElement(symbol, percentOfChange, closePriceToday);

            }
        }
        data.close();
        return new GridViewElement(symbol, 0, 0);

    }

    public List<String> getBiggestNrOfTradesSymbols(String interval, int nrOfResults) {

        List<String> bigVolume = new ArrayList<>();
        Cursor data = databaseDB.checkVolumeOfKlineInterval(interval);
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "BiggestNrOfTrades-Table " + TABLE_NAME_KLINES_DATA + " Empty");
        } else {
            int all = data.getCount();
            if (nrOfResults < all) {
                for (int i = 0; i < nrOfResults; i++) {
                    if (!data.getString(0).contains("BUSDUSDT")) {
                        bigVolume.add(data.getString(0));
                    }
                    data.moveToNext();
                }

            } else {
                do {
                    if (!data.getString(0).contains("BUSDUSDT")) {
                        bigVolume.add(data.getString(0));
                    }
                } while (data.moveToNext());
            }
        }
        data.close();
        return bigVolume;
    }


}
