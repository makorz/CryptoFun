package com.example.cryptofun.services;

import android.annotation.SuppressLint;
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

import com.example.cryptofun.R;
import com.example.cryptofun.data.ApprovedToken;
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

    int howManyNeedsToDo = 6;
    int serviceFinishedEverything = 0;
    int isAutomaticForTestEnabled = 0;
    int isAutomaticForRealEnabled = 0;
    private DBHandler databaseDB;
    private final List<String> listOfSymbols = new ArrayList<>();
    ArrayList<ListViewElement> lastSixHoursTokensStat = new ArrayList<>();
    ArrayList<ListViewElement> lastTwoHoursTokensStat = new ArrayList<>();
    ArrayList<ListViewElement> last10MinTokensStat = new ArrayList<>();
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
                        Notification notification = createNotification();
                        // Notification ID cannot be 0.
                        startForeground(1, notification);
                        approvingCryptos();

                    }
                }
        ).start();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                .setContentText("Calculating what's best.")
                .setSmallIcon(R.drawable.crypto_fun_logo)
                .setPriority(NotificationCompat.PRIORITY_MIN);

        return builder.build();
    }


    private void sendMessageToActivity() {
        Log.e(TAG, "RESULT: " + serviceFinishedEverything + " HOW: " + howManyNeedsToDo);

        if (isAutomaticForRealEnabled == 1) {

            //Verification of missed orders remote and local
            ArrayList<OrderListViewElement> returnList = new ArrayList<>();
            Cursor data = databaseDB.retrieveAllFromTable(TABLE_NAME_ORDERS);
            if (data.getCount() >= 0) {
                while (data.moveToNext()) {
                    OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getLong(13), data.getString(14), data.getFloat(15));
                    returnList.add(tempToken);
                }
            }
            data.close();
            ServiceFunctions.getAllOrders(System.currentTimeMillis(), getApplicationContext(), returnList);
        }

        if (serviceFinishedEverything >= howManyNeedsToDo) {
            Intent intent = new Intent("ApprovedService");
            Log.e(TAG, "BROADCAST - Approve Finished " + Thread.currentThread() + " " + Thread.activeCount());
            Bundle bundle = new Bundle();
            // 1 - 30min, 2 - 2h, 3 - 6h
            bundle.putSerializable("list1", (Serializable) last10MinTokensStat);
            bundle.putSerializable("list3", (Serializable) lastSixHoursTokensStat);
            bundle.putSerializable("cryptoGridViewList", (Serializable) cryptoGridViewList);
            intent.putExtra("bundleApprovedCrypto", bundle);
            LocalBroadcastManager.getInstance(ApprovingService.this).sendBroadcast(intent);
            stopForeground(true);
            stopSelf();
        }
    }

    public void approvingCryptos() {

        long sixHours = 21600000;
        long tenMinutes = 600000;
        long sixMinutes = 360000;
        long threeMinutes = 180000;

        lastSixHoursTokensStat = getListOfSymbolsAccordingToProvidedTime(sixHours, threeMinutes);
        last10MinTokensStat = getListOfSymbolsAccordingToProvidedTime(threeMinutes, 0);

        if (last10MinTokensStat.size() > 0) {
            for (int i = 0; i < last10MinTokensStat.size(); i++) {
                if (last10MinTokensStat.get(i).isItLONG()) {
                    cryptoForLONGOrders.add(last10MinTokensStat.get(i));
                } else {
                    cryptoForSHORTOrders.add(last10MinTokensStat.get(i));
                }
            }
        }
        setMainParametersOnView();

    }

    public void setMainParametersOnView() {

        // First array list for grid view, other two need to calculate changes in percents hour after hour
        // ArrayList<GridViewElement> cryptoPercentageListGrid3hours = new ArrayList<>();
        ArrayList<GridViewElement> cryptoPercentageListFourHours = new ArrayList<>();
        float percentU2 = 0;
        float percentU1 = 0;
        float percentU0 = 0;
        float percentO0 = 0;
        float percentO1 = 0;
        float percentO2 = 0;

        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_SYMBOL_AVG + " is empty");
        } else {
            listOfSymbols.clear();
            while (data.moveToNext()) {
                listOfSymbols.add(data.getString(0));
            }
            int nrOfGridElements = 36;
            List<String> biggestNrOfTradesSymbols = getBiggestNrOfTradesSymbols("4h", nrOfGridElements);

            for (int i = 0; i < listOfSymbols.size(); i++) {
                //cryptoPercentageListGrid3hours.add(getTimePercentChangeForCrypto(listOfSymbols.get(i), 12));
                cryptoPercentageListFourHours.add(getTimePercentChangeForCrypto(listOfSymbols.get(i), 12));
                if (cryptoPercentageListFourHours.get(i) == null) {
                    cryptoPercentageListFourHours.remove(i);
                }
            }

//            int under15 = 0;
//            int under5 = 0;
//            int under0 = 0;
//            int over0 = 0;
//            int over5 = 0;
//            int over15 = 0;
//
//            for (int i = 0; i < cryptoPercentageListGrid3hours.size(); i++) {
//
//                if (cryptoPercentageListGrid3hours.get(i).getPercent() < -15) {
//                    under15++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= -15 && cryptoPercentageListGrid3hours.get(i).getPercent() < -5) {
//                    under5++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= -5 && cryptoPercentageListGrid3hours.get(i).getPercent() < 0) {
//                    under0++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= 0 && cryptoPercentageListGrid3hours.get(i).getPercent() < 5) {
//                    over0++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= 5 && cryptoPercentageListGrid3hours.get(i).getPercent() <= 15) {
//                    over5++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() > 15) {
//                    over15++;
//                }
//            }
//
//            percentU15 = (float) under15 / cryptoPercentageListGrid3hours.size() * 100;
//            percentU5 = (float) under5 / cryptoPercentageListGrid3hours.size() * 100;
//            percentU0 = (float) under0 / cryptoPercentageListGrid3hours.size() * 100;
//            percentO0 = (float) over0 / cryptoPercentageListGrid3hours.size() * 100;
//            percentO5 = (float) over5 / cryptoPercentageListGrid3hours.size() * 100;
//            percentO15 = (float) over15 / cryptoPercentageListGrid3hours.size() * 100;
//
//            if (biggestNrOfTradesSymbols.size() > 1) {
//                cryptoGridViewList.add(new GridViewElement("< -15%", percentU15, under15));
//                cryptoGridViewList.add(new GridViewElement("-15% - -5%", percentU5, under5));
//                cryptoGridViewList.add(new GridViewElement("-5% - 0%", percentU0, under0));
//                cryptoGridViewList.add(new GridViewElement("0% - 5%", percentO0, over0));
//                cryptoGridViewList.add(new GridViewElement("5% - 15%", percentO5, over5));
//                cryptoGridViewList.add(new GridViewElement("> 15%", percentO15, over15));
//                for (int i = 0; i < nrOfGridElements; i++) {
//                    cryptoGridViewList.add(getTimePercentChangeForCrypto(biggestNrOfTradesSymbols.get(i),3));
//                }
//            }

//            float percentU5 = 0;
//            float percentU2 = 0;
//            float percentU0 = 0;
//            float percentO0 = 0;
//            float percentO2 = 0;
//            float percentO5 = 0;
//
//            int under5 = 0;
//            int under2 = 0;
//            int under0 = 0;
//            int over0 = 0;
//            int over2 = 0;
//            int over5 = 0;
//
//            for (int i = 0; i < cryptoPercentageListGrid3hours.size(); i++) {
//
//                if (cryptoPercentageListGrid3hours.get(i).getPercent() < -5) {
//                    under5++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= -5 && cryptoPercentageListGrid3hours.get(i).getPercent() < -2) {
//                    under2++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= -2 && cryptoPercentageListGrid3hours.get(i).getPercent() < 0) {
//                    under0++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= 0 && cryptoPercentageListGrid3hours.get(i).getPercent() < 2) {
//                    over0++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= 2 && cryptoPercentageListGrid3hours.get(i).getPercent() <= 5) {
//                    over2++;
//                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() > 5) {
//                    over5++;
//                }
//            }
//
//            percentU5 = (float) under5 / cryptoPercentageListGrid3hours.size() * 100;
//            percentU2 = (float) under2 / cryptoPercentageListGrid3hours.size() * 100;
//            percentU0 = (float) under0 / cryptoPercentageListGrid3hours.size() * 100;
//            percentO0 = (float) over0 / cryptoPercentageListGrid3hours.size() * 100;
//            percentO2 = (float) over2 / cryptoPercentageListGrid3hours.size() * 100;
//            percentO5 = (float) over5 / cryptoPercentageListGrid3hours.size() * 100;
//
//            PercentagesOfChanges changes = new PercentagesOfChanges(percentU5, percentU2, percentU0, percentO0, percentO2, percentO5, System.currentTimeMillis());
//            databaseDB.addPercentages(changes);
//
//            if (biggestNrOfTradesSymbols.size() > 1) {
//                cryptoGridViewList.add(new GridViewElement("< -5%", percentU5, under5));
//                cryptoGridViewList.add(new GridViewElement("-5% - -2%", percentU2, under2));
//                cryptoGridViewList.add(new GridViewElement("-2% - 0%", percentU0, under0));
//                cryptoGridViewList.add(new GridViewElement("0% - 2%", percentO0, over0));
//                cryptoGridViewList.add(new GridViewElement("2% - 5%", percentO2, over2));
//                cryptoGridViewList.add(new GridViewElement("> 5%", percentO5, over5));
//                for (int i = 0; i < nrOfGridElements; i++) {
//                    cryptoGridViewList.add(getTimePercentChangeForCrypto(biggestNrOfTradesSymbols.get(i), 3));
//                }
//            }
//
//
//        }
//        data.close();


            int under2 = 0;
            int under1 = 0;
            int under0 = 0;
            int over0 = 0;
            int over1 = 0;
            int over2 = 0;

            for (int i = 0; i < cryptoPercentageListFourHours.size(); i++) {

                if (cryptoPercentageListFourHours.get(i).getPercent() < -2) {
                    under2++;
                } else if (cryptoPercentageListFourHours.get(i).getPercent() >= -2 && cryptoPercentageListFourHours.get(i).getPercent() < -1) {
                    under1++;
                } else if (cryptoPercentageListFourHours.get(i).getPercent() >= -1 && cryptoPercentageListFourHours.get(i).getPercent() < 0) {
                    under0++;
                } else if (cryptoPercentageListFourHours.get(i).getPercent() >= 0 && cryptoPercentageListFourHours.get(i).getPercent() < 1) {
                    over0++;
                } else if (cryptoPercentageListFourHours.get(i).getPercent() >= 1 && cryptoPercentageListFourHours.get(i).getPercent() <= 2) {
                    over1++;
                } else if (cryptoPercentageListFourHours.get(i).getPercent() > 2) {
                    over2++;
                }
            }

            percentU2 = (float) under2 / cryptoPercentageListFourHours.size() * 100;
            percentU1 = (float) under1 / cryptoPercentageListFourHours.size() * 100;
            percentU0 = (float) under0 / cryptoPercentageListFourHours.size() * 100;
            percentO0 = (float) over0 / cryptoPercentageListFourHours.size() * 100;
            percentO1 = (float) over1 / cryptoPercentageListFourHours.size() * 100;
            percentO2 = (float) over2 / cryptoPercentageListFourHours.size() * 100;

            PercentagesOfChanges changes = new PercentagesOfChanges(percentU2, percentU1, percentU0, percentO0, percentO1, percentO2, System.currentTimeMillis());
            databaseDB.addPercentages(changes);

            if (biggestNrOfTradesSymbols.size() > 1) {
                cryptoGridViewList.add(new GridViewElement("< -2%", percentU2, under2));
                cryptoGridViewList.add(new GridViewElement("-2% - -1%", percentU1, under1));
                cryptoGridViewList.add(new GridViewElement("-1% - 0%", percentU0, under0));
                cryptoGridViewList.add(new GridViewElement("0% - 1%", percentO0, over0));
                cryptoGridViewList.add(new GridViewElement("1% - 2%", percentO1, over1));
                cryptoGridViewList.add(new GridViewElement("> 2%", percentO2, over2));

                if( biggestNrOfTradesSymbols.size() < nrOfGridElements) {
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
            if (data.getCount() == 0) {
                Log.e(TAG, "Table " + TABLE_HISTORIC_PERCENTAGES + " is empty");
            } else {
                while (data.moveToNext()) {
                    percentages.add(new PercentagesOfChanges(data.getFloat(1), data.getFloat(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getLong(7)));
                }
            }

//            float underPercentage = percentU0 + percentU1 + percentU2;
//            float underPercentage2 = percentU0 + percentU1;
//            float overPercentage = percentO0 + percentO1 + percentO2;
//            float overPercentage2 = percentO0 + percentO2;
//            boolean isItGoodForShort = false;
//            boolean isItGoodForLong = false;
//
//            if (percentages.size() > 6) {
//                isItGoodForLong = isPercentageInFavor(percentages, 0);
//                isItGoodForShort = isPercentageInFavor(percentages, 1);
//                String infoOfOrder = "LEVEL4: AutomaticTest: " + isAutomaticForTestEnabled + " AutomaticReal: " + isAutomaticForRealEnabled + " Percentage Favor: " + isItGoodForLong + " for long " + isItGoodForShort + " for short";
//                Log.e(TAG, infoOfOrder);
//                ServiceFunctions.writeToFile(infoOfOrder, getApplicationContext(), "result");
//                databaseDB.clearHistoricPercentages(now - halfHour);
//            }

            Log.e(TAG, "LEVEL3 " + cryptoForSHORTOrders.size() + " " + cryptoForLONGOrders.size());

            if (cryptoForSHORTOrders.size() > cryptoForLONGOrders.size() ) { // && isItGoodForShort       && percentU5 < 20 && percentU2 > 5 && underPercentage > overPercentage + 10  && percentU2 < 10 && underPercentage2 > 58 //cryptoForLONGOrders.size() + 1
                serviceFinishedEverything++;
                automaticOrdersFunction(cryptoForSHORTOrders, isAutomaticForTestEnabled, isAutomaticForRealEnabled);


            } else if (cryptoForLONGOrders.size() > cryptoForSHORTOrders.size()) { //  && isItGoodForLong        && percentO5 < 20 && percentO2 > 5 && underPercentage + 10 < overPercentage && percentO2 < 10 && overPercentage2 > 58
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

    private ArrayList<ListViewElement> getListOfSymbolsAccordingToProvidedTime(long timeFrom, long timeTo) {

        @SuppressLint("SimpleDateFormat")
        DateFormat df = new SimpleDateFormat("HH:mm");
        long currentTime = System.currentTimeMillis();
        ArrayList<ListViewElement> returnList = new ArrayList<ListViewElement>();

        Cursor data2 = databaseDB.firstAppearOfTokenInCertainTime(currentTime - timeFrom, currentTime - timeTo);

        if (data2.getCount() == 0) {
            //returnList.add(new ListViewElement("Nothing in DB"));
            Log.e(TAG, "Nothing in DB");
        } else {
            while (data2.moveToNext()) {

                ApprovedToken tempToken = new ApprovedToken(data2.getString(0), data2.getInt(8), data2.getFloat(5), data2.getLong(7), data2.getFloat(9));
                String symbol = tempToken.getSymbol();
                int longOrShort = tempToken.getLongOrShort();
                long approveTime = tempToken.getTime();
                float closePrice = tempToken.getClosePrice();
                float price = tempToken.getPriceOnTimeOfApprove();
                //Log.e("APPROVE SERVICE", symbol + " " + longOrShort + " " + approveTime + " " + (currentTime - timeFrom) + " ");

                if (approveTime > currentTime - timeFrom) {
                    Timestamp stamp = new Timestamp(approveTime);
                    String date = df.format(new Date(stamp.getTime()));
                    float percentOfChange = ((closePrice / price) * 100) - 100;

                    if (longOrShort == 0) {
//                        if (percentOfChange < -0.1) {
//                            returnList.add(new ListViewElement(symbol, percentOfChange, price, date, false));
//                        }
                        returnList.add(new ListViewElement(symbol, percentOfChange, price, date, false));

                    } else if (longOrShort == 1) {
//                        if (percentOfChange > 0.1) {
//                            returnList.add(new ListViewElement(symbol, percentOfChange, price, date, true));
//                        }
                        returnList.add(new ListViewElement(symbol, percentOfChange, price, date, true));
                    }
                }
            }
        }
        data2.close();

        if (returnList.size() == 0) {
            Log.e(TAG, "Nothing good in DB");
            //returnList.add(new ListViewElement("Nothing good", 0, 0, "", true));
        } else {
            Collections.sort(returnList, new Comparator<ListViewElement>() {
                public int compare(ListViewElement o1, ListViewElement o2) {
                    return Float.compare(o2.getPercentChange(), o1.getPercentChange());
                }
            });
        }
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
        if (data.getCount() == 0) {
            Log.e(TAG, "No active orders");
        } else {
            while (data.moveToNext()) {

                OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getInt(13), data.getString(14), data.getFloat(15));
                currentOrders.add(tempToken);
                Log.e(TAG, tempToken.toString());
            }
        }
        data.close();

        Log.e(TAG, "LEVEL4 AUTOMATIC: " + testAccountBalances + " REAL: " + realAccountBalance);
        //Log.e(TAG, "List of crypto to try:" + listOfCryptosToTry.toString());

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
                    Log.e(TAG, "Random from list: " + index);

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
                                        String infoOfOrder = "LEVEL5 (ORDER): Symbol# " + randomElement.getText() + " Entry$$$# " + entryAmount + " TestAccount# " + (finalI + 1) + " Margin# " + margin + " isItShort# "
                                                + isItShort + " MarkPrice# " + markPrice.getMarkPrice();
                                        Log.e(TAG, infoOfOrder);
                                        ServiceFunctions.writeToFile(infoOfOrder, getApplicationContext(), "result");
                                        ServiceFunctions.writeToFile(infoOfOrder, getApplicationContext(), "orders");

                                        ServiceFunctions.makeOrderFunction(false, randomElement.getText(), entryAmount, stopLimit, takeProfit, margin, markPrice.getMarkPrice(), isItCrossed, isItShort, System.currentTimeMillis(), testAccountBalances.get(finalI), (finalI + 6), getApplicationContext(), null);

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
                Log.e(TAG, "There are active orders on account " + (i+6));
            }
            serviceFinishedEverything++;
            data.close();
        }


        data = databaseDB.retrieveActiveOrdersOnAccount(1, "MARKET", 1);
        if (data.getCount() < 1  && isAutomaticRealEnabled == 1) { ///!!!!!!!
            Log.e(TAG, "No order for REAL account.");
            Log.e(TAG, currentOrders.toString());

            if (realAccountBalance > 6) {

                Random random = new Random();
                // Generate a random index that has not been used before
                int index = random.nextInt(listOfCryptosToTry.size());
                Log.e(TAG, "Random index: " + index);

                // Get the element at the random index
                ListViewElement randomElement = listOfCryptosToTry.get(index);

                boolean isThereSuchOrderForRandomizedSymbolOnRealAccount = false;

                for (int i = 0; i < currentOrders.size(); i++) {

                    if (currentOrders.get(i).getIsItReal() == 1 && currentOrders.get(i).getSymbol().equals(randomElement.getText())) {
                        isThereSuchOrderForRandomizedSymbolOnRealAccount = true;
                        Log.e(TAG, "Random symbol: " + randomElement.getText() + " Current orders symbol: " + currentOrders.get(i).getSymbol() + " is it real: " + currentOrders.get(i).getIsItReal() );
                    }

                }

                int entryAmount = (int) (realAccountBalance * 0.98);
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
                                    String infoOfOrder = "LEVEL5 (ORDER) " + randomElement.getText() + ", Entry$: " + entryAmount + ", REALAccount: True, Margin: " + margin + ", isItShort: "
                                            + isItShort + ", MarkPrice: " + markPrice.getMarkPrice();

                                    Log.e(TAG, infoOfOrder);
                                    ServiceFunctions.writeToFile(infoOfOrder, getApplicationContext(), "result");
                                    ServiceFunctions.writeToFile(infoOfOrder, getApplicationContext(), "orders");

                                    ServiceFunctions.makeOrderFunction(true, randomElement.getText(), entryAmount, stopLimit, takeProfit, margin, markPrice.getMarkPrice(), isItCrossed, isItShort, System.currentTimeMillis(), realAccountBalance, realAccountNr, getApplicationContext(), null);

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


        if (data.getCount() == 0) {
            Log.e(TAG, "24hPercentCount-Table " + TABLE_NAME_KLINES_DATA + " Empty");

        } else {
            data.moveToFirst();
            while (data.moveToNext()) {

                if (data.getString(10).equals("15m")) {

                    coinKlines15m.add(new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10)));

                }
            }

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
        // List<Kline> klinesVolume = new ArrayList<>();
        Cursor data = databaseDB.checkVolumeOfKlineInterval(interval);

        if (data.getCount() == 0) {
            Log.e(TAG, "BiggestNrOfTrades-Table " + TABLE_NAME_KLINES_DATA + " Empty");
        } else {
            data.moveToFirst();
            int all = data.getCount();
            if (nrOfResults < all) {
                for (int i = 0; i < nrOfResults; i++) {
                    if (!data.getString(0).contains("BUSDUSDT")) {
                        bigVolume.add(data.getString(0));
                    }
                    data.moveToNext();
                }

            } else {
                while (data.moveToNext()) {
                    if (!data.getString(0).contains("BUSDUSDT")) {
                        bigVolume.add(data.getString(0));
                    }
                }
            }
        }
        data.close();
        return bigVolume;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "DESTROY");
    }

}
