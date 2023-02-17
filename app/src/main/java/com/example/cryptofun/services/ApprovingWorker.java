package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cryptofun.data.ApprovedToken;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.database.Kline;
import com.example.cryptofun.ui.view.GridViewElement;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ApprovingWorker extends Worker {

    private static final String TAG = "APRVWorker";

    private DBHandler databaseDB;
    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String TIME_APPROVED = "approve_time";
    private final List<String> listOfSymbols = new ArrayList<>();
    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";

    public ApprovingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private void sendMessageToActivity(List<String> list1, List<String> list2, List<String> list3,
                                       List<String> list4, List<String> list5, ArrayList<GridViewElement> gridList) {

        Intent intent = new Intent("ApprovedService");
        Log.e(TAG, "SendMessage " + Thread.currentThread() + " " + Thread.activeCount());
        Bundle bundle = new Bundle();
        // 1 - 30min, 2 - 2h, 3 - 6h
        bundle.putStringArrayList("list1", (ArrayList<String>) list1);
        bundle.putStringArrayList("list2", (ArrayList<String>) list2);
        bundle.putStringArrayList("list3", (ArrayList<String>) list3);
        bundle.putStringArrayList("list4", (ArrayList<String>) list4);
        bundle.putStringArrayList("list5", (ArrayList<String>) list5);
        bundle.putSerializable("cryptoGridViewList", (Serializable) gridList);
        intent.putExtra("bundleApprovedCrypto", bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        databaseDB.close();
    }

    private void sendInfoToActivity() {
        Intent intent = new Intent("Approve_Fragment_Prepared");
        Log.e(TAG,"Fragment updated");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public void approvingCryptos() {

        long timeToClearOldApproved = System.currentTimeMillis() - 900000;
        long sixHours = 21600000;
        long twoHour = 7200000;
        long halfHour = 1800000;
        long now = 1800000;
        databaseDB.deleteOldApproved(TABLE_NAME_APPROVED, TIME_APPROVED, timeToClearOldApproved);

        List<String> lastSixHoursTokensStat = getListOfSymbolsAccordingToProvidedTime(sixHours, twoHour);
        List<String> lastTwoHoursTokensStat = getListOfSymbolsAccordingToProvidedTime(twoHour, halfHour);
        List<String> last30MinTokensStat = getListOfSymbolsAccordingToProvidedTime(halfHour, 0);
        List<String> occurrencesOfLONGFreshApprovedTokens = new ArrayList<>();
        List<String> occurrencesOfSHORTFreshApprovedTokens = new ArrayList<>();

        if (last30MinTokensStat.size() > 0) {

            boolean nothingOnList = last30MinTokensStat.get(0).contains("Nothing");

            for (int i = 0; i < last30MinTokensStat.size(); i++) {
                String a = last30MinTokensStat.get(i);
                //Log.e("APPPROVEDEDDEDD", String.valueOf(nothingOnList));
                String result = a.substring(a.indexOf("[") + 1, a.indexOf("]"));
                String result2 = "";
                if (!nothingOnList) {
                    Log.e(TAG, a);
                    result2 = a.substring(a.indexOf("%") + 2, a.indexOf("[")-1);
                }
                int occurences = 1;
                for (int j = 0; j < lastTwoHoursTokensStat.size(); j++) {
                    if (lastTwoHoursTokensStat.get(j).contains(result)) {
                        occurences++;
                    }
                }
                for (int k = 0; k < lastSixHoursTokensStat.size(); k++) {
                    if (lastSixHoursTokensStat.get(k).contains(result)) {
                        occurences++;
                    }
                }
                if (occurences > 1 && result2.contains("LONG")) {
                    occurrencesOfLONGFreshApprovedTokens.add(result + " [" + result2 + "] on " + occurences + ".");
                } else if (occurences > 1 && result2.contains("SHORT")) {
                     occurrencesOfSHORTFreshApprovedTokens.add(result + " [" + result2 + "] on " + occurences + ".");
                }

            }
        }

        setMainParametersOnView(last30MinTokensStat,lastTwoHoursTokensStat,lastSixHoursTokensStat,
                occurrencesOfLONGFreshApprovedTokens, occurrencesOfSHORTFreshApprovedTokens);

    }

    private List<String> getListOfSymbolsAccordingToProvidedTime(long timeFrom, long timeTo) {

        @SuppressLint("SimpleDateFormat")
        DateFormat df = new SimpleDateFormat("HH:mm");
        long currentTime = System.currentTimeMillis();
        List<String> returnList = new ArrayList<>();

        Cursor data2 = databaseDB.firstAppearOfTokenInCertainTime(currentTime - timeFrom, currentTime - timeTo);

        if (data2.getCount() == 0) {
            returnList.add("[Nothing found in database.]");
        } else {
            while (data2.moveToNext()) {

                ApprovedToken tempToken = new ApprovedToken(data2.getString(0), data2.getInt(8), data2.getFloat(5), data2.getLong(7), data2.getFloat(9));
                String finalString;
                String symbol = tempToken.getSymbol();
                int longOrShort = tempToken.getLongOrShort();
                long approveTime = tempToken.getTime();
                float closePrice = tempToken.getClosePrice();
                float price = tempToken.getPriceOnTimeOfApprove();

                //Log.e("APPROVE SERVICE", symbol + " " + longOrShort + " " + approveTime + " " + (currentTime - timeFrom) + " ");

                if (approveTime > currentTime - timeFrom) {
                    Timestamp stamp = new Timestamp(approveTime);
                    String date = df.format(new Date(stamp.getTime()));

                    String longShort = "";
                    DecimalFormat dfNr = new DecimalFormat("00.##");
                    DecimalFormat dfPr = new DecimalFormat("#.######");

                    float percentOfChange = ((closePrice / price) * 100) - 100;

                    //Log.e("APPHIST", price + " " + closePrice + " " + percentOfChange);

                    if (longOrShort == 0) {
                        if (percentOfChange < -0.25){
                            longShort = "SHORT";
//                            finalString = date + " -- " + longShort + " [" + symbol + "] -- " +  dfPr.format(price)
//                                    + " (" + dfNr.format(percentOfChange) +"%)";
                            finalString = dfNr.format(percentOfChange) +"% " + longShort  + " [" + symbol + "]" + " " + date
                                    + " (" + dfPr.format(price) + ")";
                            returnList.add(finalString);
                        }
                    } else if (longOrShort == 1) {
                        if (percentOfChange > 0.25) {
                            longShort = "LONG";
//                            finalString = date + " -- " + longShort + " [" + symbol + "] -- " + dfPr.format(price)
//                                    + " (" + dfNr.format(percentOfChange) +"%)";
                            finalString = dfNr.format(percentOfChange) +"% " + longShort  + " [" + symbol + "]" + " " + date
                                    + " (" + dfPr.format(price) + ")";
                            returnList.add(finalString);
                        }
                    }
                }
            }
        }
        data2.close();


        if (returnList.size() == 0) {
            returnList.add("[Nothing passed a test.]");
        } else {
            Collections.sort(returnList,Collections.reverseOrder());
        }

        Log.e(TAG, timeFrom + "  " + returnList);

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

    public void setMainParametersOnView(List<String> tokenList1, List<String> tokenList2, List<String> tokenList3, List<String> tokenList4, List<String> tokenList5) {

        ArrayList<GridViewElement> cryptoGridViewList = new ArrayList<>();
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
            ArrayList<GridViewElement> cryptoPercentageList = new ArrayList<>();
            for (int i = 0; i < listOfSymbols.size(); i++) {
                cryptoPercentageList.add(get24hPercentChangeForCrypto(listOfSymbols.get(i)));
                //Log.e("HomeFragments", "Crypto percentage: " + cryptoPercentageList.get(i).getSymbol() + " " + cryptoPercentageList.get(i).getPercent());
                if (cryptoPercentageList.get(i) == null) {
                    cryptoPercentageList.remove(i);
                }
            }
            int under15 = 0;
            int under5 = 0;
            int under0 = 0;
            int over0 = 0;
            int over5 = 0;
            int over15 = 0;

            for (int i = 0; i < cryptoPercentageList.size(); i++) {

                if (cryptoPercentageList.get(i).getPercent() < -15) {
                    under15++;
                } else if (cryptoPercentageList.get(i).getPercent() >= -15 && cryptoPercentageList.get(i).getPercent() < -5) {
                    under5++;
                } else if (cryptoPercentageList.get(i).getPercent() >= -5 && cryptoPercentageList.get(i).getPercent() < 0) {
                    under0++;
                } else if (cryptoPercentageList.get(i).getPercent() >= 0 && cryptoPercentageList.get(i).getPercent() < 5) {
                    over0++;
                } else if (cryptoPercentageList.get(i).getPercent() >= 5 && cryptoPercentageList.get(i).getPercent() <= 15) {
                    over5++;
                } else if (cryptoPercentageList.get(i).getPercent() > 15) {
                    over15++;
                }
            }

            float percentU15 = (float) under15 / cryptoPercentageList.size() * 100;
            float percentU5 = (float) under5 / cryptoPercentageList.size() * 100;
            float percentU0 = (float) under0 / cryptoPercentageList.size() * 100;
            float percentO0 = (float) over0 / cryptoPercentageList.size() * 100;
            float percentO5 = (float) over5 / cryptoPercentageList.size() * 100;
            float percentO15 = (float) over15 / cryptoPercentageList.size() * 100;

            if (biggestNrOfTradesSymbols.size() > 1) {
                cryptoGridViewList.add(new GridViewElement("< -15%", percentU15, under15));
                cryptoGridViewList.add(new GridViewElement("-15% - -5%", percentU5, under5));
                cryptoGridViewList.add(new GridViewElement("-5% - 0%", percentU0, under0));
                cryptoGridViewList.add(new GridViewElement("0% - 5%", percentO0, over0));
                cryptoGridViewList.add(new GridViewElement("5% - 15%", percentO5, over5));
                cryptoGridViewList.add(new GridViewElement("> 15%", percentO15, over15));
                for (int i = 0; i < nrOfGridElements; i++) {
                    cryptoGridViewList.add(get24hPercentChangeForCrypto(biggestNrOfTradesSymbols.get(i)));
                }
            }
        }
        data.close();

        sendInfoToActivity();
        sendMessageToActivity(tokenList1,tokenList2,tokenList3,tokenList4,tokenList5, cryptoGridViewList);
    }

    // Calculate 24h change of price in %
    public GridViewElement get24hPercentChangeForCrypto(String symbol) {

        List<Kline> coinKlines4h = new ArrayList<>();
        Cursor data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol);

        if (data.getCount() == 0) {
            Log.e(TAG, "24hPercentCount-Table " + TABLE_NAME_KLINES_DATA + " Empty");

        } else {
            data.moveToFirst();
            while (data.moveToNext()) {

                if (data.getString(10).equals("4h")) {

                    coinKlines4h.add(new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10)));

                }
            }

            if (coinKlines4h.size() >= 3) { //7 for 24h
                //For 8h change
                long openTime = coinKlines4h.get(2).gettOpenTime();
                float closePriceYesterday = coinKlines4h.get(2).gettClosePrice();
                float percentOfChange = 0;
                long openTime2 = coinKlines4h.get(0).gettOpenTime();
                float closePriceToday = coinKlines4h.get(0).gettClosePrice();

               // if (openTime + 28800000 == openTime2)
                percentOfChange = ((closePriceToday / closePriceYesterday) * 100) - 100;
                //}

                // For 24h change:
//                long openTime = coinKlines4h.get(6).gettOpenTime();
//                float closePriceYesterday = coinKlines4h.get(6).gettClosePrice();
//                float percentOfChange = 0;
//                long openTime2 = coinKlines4h.get(0).gettOpenTime();
//                float closePriceToday = coinKlines4h.get(0).gettClosePrice();
//
//                if (openTime + 86400000 == openTime2) {
//                    percentOfChange = ((closePriceToday / closePriceYesterday) * 100) - 100;
//                }

//                Log.e(TAG, "24hPercentCount-yesterday " + openTime + ", " + closePriceYesterday);
//                Log.e(TAG, "24hPercentCount-today " + openTime2 + ", " + closePriceToday);
//                Log.e(TAG, "percent " + percentOfChange);

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


    @NonNull
    @Override
    public Result doWork() {
        databaseDB = DBHandler.getInstance(getApplicationContext());
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "START");
                        approvingCryptos();
                    }
                }
        ).start();
        return Result.success();
    }
}
