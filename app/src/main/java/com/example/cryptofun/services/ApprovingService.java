package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.R;
import com.example.cryptofun.data.AccountBalance;
import com.example.cryptofun.data.ApprovedToken;
import com.example.cryptofun.data.MarkPrice;
import com.example.cryptofun.data.PercentagesOfChanges;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.database.Kline;
import com.example.cryptofun.databinding.FragmentLoadingBinding;
import com.example.cryptofun.retrofit.RetrofitClientFutures;
import com.example.cryptofun.retrofit.RetrofitClientSecret;
import com.example.cryptofun.ui.view.GridViewElement;
import com.example.cryptofun.ui.view.ListViewElement;
import com.example.cryptofun.ui.view.OrderListViewElement;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Array;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApprovingService extends Service {

    private static final String TAG = "APRVService";
    
    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String TIME_APPROVED = "approve_time";
    private static final String TABLE_HISTORIC_PERCENTAGES = "history_percentages";
    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private static final String ID = "id";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String DESCRIPTION = "description";
    private static final String VALUE_STRING = "value_string";
    private static final String VALUE_INT = "value_int";
    private static final String VALUE_REAL = "value_real";
    private static final String TABLE_NAME_ORDERS = "current_orders";
    int howManyNeedsToDo = 6;
    int serviceFinishedEverything = 0;

    private DBHandler databaseDB;
    private final List<String> listOfSymbols = new ArrayList<>();
    
    ArrayList<ListViewElement> lastSixHoursTokensStat = new ArrayList<>();
    ArrayList<ListViewElement> lastTwoHoursTokensStat = new ArrayList<>();
    ArrayList<ListViewElement> last30MinTokensStat = new ArrayList<>();
    ArrayList<ListViewElement> occurrencesOfLONGFreshApprovedTokens = new ArrayList<>();
    ArrayList<ListViewElement> occurrencesOfSHORTFreshApprovedTokens = new ArrayList<>();
    ArrayList<GridViewElement> cryptoGridViewList = new ArrayList<>();
    ArrayList<ListViewElement> cryptoForLONGOrders = new ArrayList<>();
    ArrayList<ListViewElement> cryptoForSHORTOrders = new ArrayList<>();

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
                        approvingCryptos();

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
                .setContentText("Calculating what's best.")
                .setSmallIcon(R.drawable.crypto_fun_logo)
                .setPriority(NotificationCompat.PRIORITY_MIN);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessageToActivity() {
        Log.e(TAG, "RESULT: " + serviceFinishedEverything + " HOW: " + howManyNeedsToDo);
        if (serviceFinishedEverything >= howManyNeedsToDo) {
            Intent intent = new Intent("ApprovedService");
            Log.e(TAG, "SendMessage " + Thread.currentThread() + " " + Thread.activeCount());
            Bundle bundle = new Bundle();
            // 1 - 30min, 2 - 2h, 3 - 6h
            bundle.putSerializable("list1", (Serializable) last30MinTokensStat);
            bundle.putSerializable("list2", (Serializable) lastTwoHoursTokensStat);
            bundle.putSerializable("list3", (Serializable) lastSixHoursTokensStat);
            bundle.putSerializable("list4", (Serializable) occurrencesOfLONGFreshApprovedTokens);
            bundle.putSerializable("list5", (Serializable) occurrencesOfSHORTFreshApprovedTokens);
            bundle.putSerializable("cryptoGridViewList", (Serializable) cryptoGridViewList);
            intent.putExtra("bundleApprovedCrypto", bundle);
            LocalBroadcastManager.getInstance(ApprovingService.this).sendBroadcast(intent);
            stopForeground(true);
            stopSelf();
        }
    }


    private void sendInfoToActivity() {
        Intent intent = new Intent("Approve_Fragment_Prepared");
        Log.e(TAG, "Fragment updated");
        Bundle bundle = new Bundle();
        bundle.putSerializable("list1", (Serializable) last30MinTokensStat);
        bundle.putSerializable("list2", (Serializable) lastTwoHoursTokensStat);
        bundle.putSerializable("list3", (Serializable) lastSixHoursTokensStat);
        bundle.putSerializable("list4", (Serializable) occurrencesOfLONGFreshApprovedTokens);
        bundle.putSerializable("list5", (Serializable) occurrencesOfSHORTFreshApprovedTokens);
        bundle.putSerializable("cryptoGridViewList", (Serializable) cryptoGridViewList);
        intent.putExtra("bundleApprovedCrypto", bundle);
        LocalBroadcastManager.getInstance(ApprovingService.this).sendBroadcast(intent);
    }

    public void approvingCryptos() {
        long timeToClearOldApproved = System.currentTimeMillis() - 900000;
        long sixHours = 21600000;
        long twoHour = 7200000;
        long halfHour = 1800000;
        long twentytwoMinutes = 1320000;
        long tenMinutes = 600000;
        long now = 1800000;
        databaseDB.deleteOldApproved(TABLE_NAME_APPROVED, TIME_APPROVED, timeToClearOldApproved);

        lastSixHoursTokensStat = getListOfSymbolsAccordingToProvidedTime(sixHours, twoHour);
        lastTwoHoursTokensStat = getListOfSymbolsAccordingToProvidedTime(twoHour, twentytwoMinutes);
        last30MinTokensStat = getListOfSymbolsAccordingToProvidedTime(twentytwoMinutes, 0);

        if (last30MinTokensStat.size() > 0) {

            for (int i = 0; i < last30MinTokensStat.size(); i++) {
                String result = last30MinTokensStat.get(i).getText();
                int occurrencesLong = 1;
                int occurrencesShort = 1;
                for (int j = 0; j < lastTwoHoursTokensStat.size(); j++) {
                    if (lastTwoHoursTokensStat.get(j).getText().contains(result) && last30MinTokensStat.get(i).isItLONG()) {
                        occurrencesLong++;
                    }
                    if (lastTwoHoursTokensStat.get(j).getText().contains(result) && !last30MinTokensStat.get(i).isItLONG()) {
                        occurrencesShort++;
                    }
                }
                for (int k = 0; k < lastSixHoursTokensStat.size(); k++) {
                    if (lastSixHoursTokensStat.get(k).getText().contains(result) && last30MinTokensStat.get(i).isItLONG()) {
                        occurrencesLong++;
                    }
                    if (lastSixHoursTokensStat.get(k).getText().contains(result) && !last30MinTokensStat.get(i).isItLONG()) {
                        occurrencesShort++;
                    }
                }
                String finalResultLong = result + " on " + occurrencesLong + " lists.";
                String finalResultShort = result + " on " + occurrencesShort + " lists.";

                if (last30MinTokensStat.get(i).isItLONG()) {
                    if (occurrencesLong > 1) {
                        cryptoForLONGOrders.add(last30MinTokensStat.get(i));
                        Log.e(TAG, finalResultLong);
                        occurrencesOfLONGFreshApprovedTokens.add(new ListViewElement(finalResultLong));
                    }
                } else {
                    if (occurrencesShort > 1) {
                        cryptoForSHORTOrders.add(last30MinTokensStat.get(i));
                        Log.e(TAG, finalResultShort);
                        occurrencesOfSHORTFreshApprovedTokens.add(new ListViewElement(finalResultShort));
                    }
                }
            }
        }

        setMainParametersOnView();

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
                        if (percentOfChange < -0.25) {
                            returnList.add(new ListViewElement(symbol, percentOfChange, price, date, false));
                        }
                    } else if (longOrShort == 1) {
                        if (percentOfChange > 0.25) {
                            returnList.add(new ListViewElement(symbol, percentOfChange, price, date, true));
                        }
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

//    private void writeToFile(String data, Context context) {
//        try {
//            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("result.txt", Context.MODE_APPEND));
//            outputStreamWriter.write(data);
//            outputStreamWriter.close();
//        } catch (IOException e) {
//            Log.e("Exception", "File write failed: " + e.toString());
//        }
//    }

    public void setMainParametersOnView() {

        // First array list for grid view, other two need to calculate changes in percents hour after hour
        ArrayList<GridViewElement> cryptoPercentageListGrid3hours = new ArrayList<>();
//        ArrayList<GridViewElement> cryptoPercentageList2hours = new ArrayList<>();
//        ArrayList<GridViewElement> cryptoPercentageList1hour = new ArrayList<>();
//        List<Float> hours3Ago = new ArrayList<>();
//        List<Float> hours2Ago = new ArrayList<>();
//        List<Float> hours1Ago = new ArrayList<>();

        float percentU5 = 0;
        float percentU2 = 0;
        float percentU0 = 0;
        float percentO0 = 0;
        float percentO2 = 0;
        float percentO5 = 0;

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
                cryptoPercentageListGrid3hours.add(getTimePercentChangeForCrypto(listOfSymbols.get(i), 12));
//                cryptoPercentageList2hours.add(getTimePercentChangeForCrypto(listOfSymbols.get(i), 6));
//                cryptoPercentageList1hour.add(getTimePercentChangeForCrypto(listOfSymbols.get(i), 4));

                if (cryptoPercentageListGrid3hours.get(i) == null) {
                    cryptoPercentageListGrid3hours.remove(i);
                }
//                if (cryptoPercentageList2hours.get(i) == null) {
//                    cryptoPercentageList2hours.remove(i);
//                }
//                if (cryptoPercentageList1hour.get(i) == null) {
//                    cryptoPercentageList1hour.remove(i);
//                }
            }

//            hours3Ago = percentsFromHour(cryptoPercentageListGrid3hours);
//            hours2Ago = percentsFromHour(cryptoPercentageList2hours);
//            hours1Ago = percentsFromHour(cryptoPercentageList1hour);

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


            int under5 = 0;
            int under2 = 0;
            int under0 = 0;
            int over0 = 0;
            int over2 = 0;
            int over5 = 0;

            for (int i = 0; i < cryptoPercentageListGrid3hours.size(); i++) {

                if (cryptoPercentageListGrid3hours.get(i).getPercent() < -5) {
                    under5++;
                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= -5 && cryptoPercentageListGrid3hours.get(i).getPercent() < -2) {
                    under2++;
                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= -2 && cryptoPercentageListGrid3hours.get(i).getPercent() < 0) {
                    under0++;
                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= 0 && cryptoPercentageListGrid3hours.get(i).getPercent() < 2) {
                    over0++;
                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() >= 2 && cryptoPercentageListGrid3hours.get(i).getPercent() <= 5) {
                    over2++;
                } else if (cryptoPercentageListGrid3hours.get(i).getPercent() > 5) {
                    over5++;
                }
            }

            percentU5 = (float) under5 / cryptoPercentageListGrid3hours.size() * 100;
            percentU2 = (float) under2 / cryptoPercentageListGrid3hours.size() * 100;
            percentU0 = (float) under0 / cryptoPercentageListGrid3hours.size() * 100;
            percentO0 = (float) over0 / cryptoPercentageListGrid3hours.size() * 100;
            percentO2 = (float) over2 / cryptoPercentageListGrid3hours.size() * 100;
            percentO5 = (float) over5 / cryptoPercentageListGrid3hours.size() * 100;

            PercentagesOfChanges changes = new PercentagesOfChanges(percentU5, percentU2,percentU0,percentO0,percentO2,percentO5, System.currentTimeMillis());
            databaseDB.addPercentages(changes);

            if (biggestNrOfTradesSymbols.size() > 1) {
                cryptoGridViewList.add(new GridViewElement("< -5%", percentU5, under5));
                cryptoGridViewList.add(new GridViewElement("-5% - -2%", percentU2, under2));
                cryptoGridViewList.add(new GridViewElement("-2% - 0%", percentU0, under0));
                cryptoGridViewList.add(new GridViewElement("0% - 2%", percentO0, over0));
                cryptoGridViewList.add(new GridViewElement("2% - 5%", percentO2, over2));
                cryptoGridViewList.add(new GridViewElement("> 5%", percentO5, over5));
                for (int i = 0; i < nrOfGridElements; i++) {
                    cryptoGridViewList.add(getTimePercentChangeForCrypto(biggestNrOfTradesSymbols.get(i),3));
                }
            }


        }
        data.close();

//        @SuppressWarnings("unchecked")
//        List<Float>[] listsArray = new List[]{hours3Ago, hours2Ago};
//        Log.e(TAG,"Percents of movement: " + Arrays.toString(listsArray));

//        int u5 = checkBehavior(listsArray,0);
//        int u2 = checkBehavior(listsArray,1);
//        int u0 = checkBehavior(listsArray,2);
//        int o0 = checkBehavior(listsArray,3);
//        int o2 = checkBehavior(listsArray,4);
//        int o5 = checkBehavior(listsArray,5);

//        Log.e(TAG, "u5: " + u5 + " u2: " + u2 + " u0: " + u0 + " o0: " + o0 + " o2: " + o2 + " o5: " + o5);

        ArrayList<PercentagesOfChanges> percentages = new ArrayList<>();
        
        long now = System.currentTimeMillis();
        long twentyMinutes = 1200000;
        long halfhour = 1800000;
        data = databaseDB.retrievePercentages(now - twentyMinutes , now);
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_HISTORIC_PERCENTAGES + " is empty");
        } else {
            while (data.moveToNext()) {
                percentages.add(new PercentagesOfChanges(data.getFloat(1),data.getFloat(2),data.getFloat(3),data.getFloat(4),data.getFloat(5),data.getFloat(6),data.getLong(7)));
            }
        }

        float underPercentage = percentU0 + percentU2 + percentU5;
        float overPercentage = percentO0 + percentO2 + percentO5;
        boolean isItGoodForShort = false;
        boolean isItGoodForLong = false;

        if (percentages.size() > 6 ) {
            isItGoodForLong = isPercentageInFavor(percentages,0);
            isItGoodForShort = isPercentageInFavor(percentages,1);
            databaseDB.clearHistoricPercentages(now - halfhour);
        }


        if (cryptoForSHORTOrders.size() > 0 && percentU5 < 20 && percentU2 > 5 && underPercentage > overPercentage && isItGoodForShort ) {
            serviceFinishedEverything++;
            automaticOrdersFunction(cryptoForSHORTOrders);
        } else if (cryptoForLONGOrders.size() > 0 && percentO5 < 20 && percentO2 > 5 && underPercentage < overPercentage && isItGoodForLong ) {
            serviceFinishedEverything++;
            automaticOrdersFunction(cryptoForLONGOrders);
        } else if (cryptoForLONGOrders.size() > 0 && percentU5 + percentU2 > 85) {
            serviceFinishedEverything++;
            automaticOrdersFunction(cryptoForLONGOrders);
        } else if (cryptoForSHORTOrders.size() > 0 && percentO5 + percentO2 > 85) {
            serviceFinishedEverything++;
            automaticOrdersFunction(cryptoForSHORTOrders);
        } else {
            serviceFinishedEverything = 6;
            sendMessageToActivity();
        }


//
//        float underPercentageSmall = percentU0 + percentU2;
//        float overPercentageSmall = percentO0 + percentO2;
//
//        float underPercentageBig = percentU2 + percentU5;
//        float overPercentageBig = percentO2 + percentO5;

//        if (overPercentage > 80) {
//            if (cryptoForSHORTOrders.size() > 0) {
//                serviceFinishedEverything++;
//                automaticOrdersFunction(cryptoForSHORTOrders);
//            } else {
//                serviceFinishedEverything = 6;
//                sendMessageToActivity();
//            }
//        } else if (underPercentage > 80) {
//            if (cryptoForLONGOrders.size() > 0) {
//                serviceFinishedEverything++;
//                automaticOrdersFunction(cryptoForLONGOrders);
//            } else {
//                serviceFinishedEverything = 6;
//                sendMessageToActivity();
//            }
//        } else if (underPercentage > 60 && (percentU3 + percentU8) > (percentO3 + percentO8)) {
//            if (cryptoForLONGOrders.size() > 0) {
//                serviceFinishedEverything++;
//                automaticOrdersFunction(cryptoForLONGOrders);
//            } else {
//                serviceFinishedEverything = 6;
//                sendMessageToActivity();
//            }
//        } else if (overPercentage > 60 && (percentO3 + percentO8) > (percentU3 + percentU8)) {
//            if (cryptoForSHORTOrders.size() > 0) {
//                serviceFinishedEverything++;
//                automaticOrdersFunction(cryptoForSHORTOrders);
//            } else {
//                serviceFinishedEverything = 6;
//                sendMessageToActivity();
//            }
//        } else {
//            serviceFinishedEverything = 6;
//            sendMessageToActivity();
//        }

        // Ciekawa opcja poniżej
//        if (cryptoForSHORTOrders.size() > 1 && cryptoForSHORTOrders.size() > cryptoForLONGOrders.size()) {
//            serviceFinishedEverything++;
//            automaticOrdersFunction(cryptoForSHORTOrders);
//        } else if (cryptoForLONGOrders.size() > 1 && cryptoForLONGOrders.size() > cryptoForSHORTOrders.size()) {
//            serviceFinishedEverything++;
//            automaticOrdersFunction(cryptoForLONGOrders);
//        } else {
//            serviceFinishedEverything = 6;
//            sendMessageToActivity();


    }

    public boolean isPercentageInFavor(ArrayList<PercentagesOfChanges> percents, int isItShort) {

        int howManyTendencies = percents.size() / 2;
        boolean acceppted = false;
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

            Log.e(TAG, "Value: " + currentNumber + " previous number: " + prevNumber + " index: " + i);
            if (currentNumber > prevNumber) {
                temp++;
            } else if (currentNumber < prevNumber) {

            } else {
                temp++;
            }

            if (temp >= howManyTendencies) {
                acceppted = true;
                break;
            }
            prevNumber = currentNumber;
        }

        return acceppted;
    }

//
//
//
//    public static int checkBehavior(List<Float>[] lists, int index) {
//
//        int whatIsHappening = 0;
//
//        float prevNumber = lists[0].get(index);
//        Log.e(TAG, "Value: " + prevNumber + " index: " + index);
//
//        for (int i = 1; i < lists.length; i++) {
//            float currentNumber = lists[i].get(index);
//            Log.e(TAG, "Value: " + currentNumber + " index: " + index);
//            if (currentNumber * 1.005 > prevNumber) {
//                whatIsHappening = 1;
//            } else if (currentNumber * 1.005 < prevNumber) {
//                whatIsHappening = -1;
//            } else {
//                whatIsHappening = 0;
//            }
//            prevNumber = currentNumber;
//        }
//
//        if (whatIsHappening == 1) {
//            return 1;
//        } else if (whatIsHappening == -1) {
//            return -1;
//        } else {
//            return 0;
//        }
//    }
//
//    public static List<Float> percentsFromHour(ArrayList<GridViewElement> array) {
//
//        int under5 = 0;
//        int under2 = 0;
//        int under0 = 0;
//        int over0 = 0;
//        int over2 = 0;
//        int over5 = 0;
//
//        List<Float> finalListStartingFromLowest = new ArrayList<>();
//
//        for (int i = 0; i < array.size(); i++) {
//
//            if (array.get(i).getPercent() < -5) {
//                under5++;
//            } else if (array.get(i).getPercent() >= -5 && array.get(i).getPercent() < -2) {
//                under2++;
//            } else if (array.get(i).getPercent() >= -2 && array.get(i).getPercent() < 0) {
//                under0++;
//            } else if (array.get(i).getPercent() >= 0 && array.get(i).getPercent() < 2) {
//                over0++;
//            } else if (array.get(i).getPercent() >= 2 && array.get(i).getPercent() <= 5) {
//                over2++;
//            } else if (array.get(i).getPercent() > 5) {
//                over5++;
//            }
//        }
//
//        finalListStartingFromLowest.add((float) under5 / array.size() * 100);
//        finalListStartingFromLowest.add((float) under2 / array.size() * 100);
//        finalListStartingFromLowest.add((float) under0 / array.size() * 100);
//        finalListStartingFromLowest.add((float) over0 / array.size() * 100);
//        finalListStartingFromLowest.add((float) over2 / array.size() * 100);
//        finalListStartingFromLowest.add((float) over5 / array.size() * 100);
//
//        return finalListStartingFromLowest;
//    }


    public void automaticOrdersFunction(ArrayList<ListViewElement> listOfCryptosToTry) {

        // Retrieve test accounts balances to prepare for making orders
        ArrayList<Float> testAccountBalances = new ArrayList<>();
        int margin;
        int stopLimit;
        int takeProfit;

        for (int i = 6; i < 11; i++) {
            Cursor data = databaseDB.retrieveParam(i);
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

        Cursor data = databaseDB.retrieveAllFromTable(TABLE_NAME_ORDERS);
        ArrayList<OrderListViewElement> currentOrders = new ArrayList<>();
        if (data.getCount() == 0) {
            Log.e(TAG, "No active orders");
        } else {
            while (data.moveToNext()) {

                OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12));
                currentOrders.add(tempToken);

            }
        }
        data.close();

        Log.e(TAG, "AUTOMATIC: " + testAccountBalances);
        Log.e(TAG, "List of crypto to try:" + listOfCryptosToTry.toString());

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
            databaseDB.addParam(12, "SL automatic", "", 1, 0);
            stopLimit = 1;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 12);
            databaseDB.addParam(12, "SL automatic", "", 1, 0);
            stopLimit = 1;
        } else {
            data.moveToFirst();
            stopLimit = data.getInt(3);
        }
        data.close();

        data = databaseDB.retrieveParam(13);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param 13");
            databaseDB.addParam(13, "TP automatic", "", 2, 0);
            takeProfit = 2;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 13);
            databaseDB.addParam(13, "TP automatic", "", 2, 0);
            takeProfit = 2;
        } else {
            data.moveToFirst();
            takeProfit = data.getInt(3);
        }
        data.close();

        boolean isItReal = false;
        boolean isItCrossed = false;

        ArrayList<String> currentMadeOrders = new ArrayList<>();
        //For each test account try to make order if balance is good
        for (int i = 0; i < 5; i++) {
            Log.e(TAG, "Checking account nr " + (i + 5) + " " + testAccountBalances.size());

            if (testAccountBalances.get(i) > 50) {

                Random random = new Random();
                // Generate a random index that has not been used before
                int index = random.nextInt(listOfCryptosToTry.size());
                Log.e(TAG, "Random index: " + index);

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
                    int entryAmount = (int) (testAccountBalances.get(i) * 0.95);
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
                                    Log.e(TAG, markPrice.toString());
                                    Log.e(TAG, "Automatic Order Made for " + randomElement.getText() + " on automatic test account " + (finalI + 1) + ".");

                                    makeOrder(isItReal, randomElement.getText(), entryAmount, stopLimit, takeProfit, margin, markPrice.getMarkPrice(), isItCrossed, isItShort, System.currentTimeMillis(), testAccountBalances.get(finalI), (finalI + 6));
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
            serviceFinishedEverything++;
        }
        sendMessageToActivity();
    }

    private void makeOrder(boolean isItReal, String symbol, int entryAmount, int stopLimit, int takeProfit, int margin, float currentPrice, boolean isItCrossed, boolean isItShort, long time, float balance, int nrOfParameterToUpdate) {

        if (isItReal) {
            Log.e(TAG, "REAL");
        } else {
            Log.e(TAG, "TEST");
            Log.e(TAG, isItCrossed + " " + isItShort);
            float stopLimitPrice = currentPrice * (1 - (float) stopLimit / 100);
            float takeProfitPrice = currentPrice * (1 + (float) takeProfit / 100);
            int isItShortValue = 0;
            int isItRealValue = 0;
            int isItCrossedValue = 0;
            if (isItShort) {
                stopLimitPrice = currentPrice * (1 + (float) stopLimit / 100);
                takeProfitPrice = currentPrice * (1 - (float) takeProfit / 100);
                isItShortValue = 1;
            }
            if (isItCrossed) {
                isItCrossedValue = 1;
            }

            Log.e(TAG, isItCrossedValue + " " + isItShortValue);
            OrderListViewElement toDB = new OrderListViewElement(symbol, isItRealValue, (float) entryAmount, currentPrice, currentPrice, stopLimitPrice, takeProfitPrice, time, margin, isItShortValue, isItCrossedValue, nrOfParameterToUpdate);
            DBHandler databaseDBforRetrofit = DBHandler.getInstance(getApplicationContext());
            String nrOfParameter = String.valueOf(nrOfParameterToUpdate);
            databaseDBforRetrofit.addNewOrder(toDB);
            databaseDBforRetrofit.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, balance - entryAmount, ID, nrOfParameter);

        }
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

                float closePriceYesterday = coinKlines15m.get(nrOf15mKlinesBack-1).gettClosePrice();
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

}
