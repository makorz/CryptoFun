package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.data.KlineRequest;
import com.example.cryptofun.data.StatusOfKline;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.database.Kline;
import com.example.cryptofun.database.rawTable_Kline;
import com.example.cryptofun.retrofit.RetrofitClient;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UpdatingDatabaseService extends Service {

    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";

    private static final String TABLE_NAME_CONFIG = "config";
    private static final String DESCRIPTION = "description";
    private static final String VALUE = "value";
    private static final String ID = "id";


    private List<String> listOfApprovedCryptos = new ArrayList<>();
    private DBHandler databaseDB;

    // This method run only one time. At the first time of service created and running
    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceUpdateDB", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        databaseDB = new DBHandler(this);
        checkDBLastTimeOfUpdate();
        startCountingAndReturnResult();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessageToActivity(String date) {
        Intent intent = new Intent("DB_updated");
        intent.putExtra("updateDate", date);
        Log.e("UPDService", "SendMessage1");
        LocalBroadcastManager.getInstance(UpdatingDatabaseService.this).sendBroadcast(intent);
    }

    private void sendMessageToActivity2(List<String> list) {
        Intent intent = new Intent("DB_updated");
        Bundle bundle = new Bundle();
        bundle.putStringArrayList("approvedCrypto", (ArrayList<String>) list);
        intent.putExtra("bundleApprovedCrypto", bundle);
        Log.e("UPDService", "SendMessage2");
        LocalBroadcastManager.getInstance(UpdatingDatabaseService.this).sendBroadcast(intent);
    }

    private void updateDBtoCurrentValues(long timeDifference, long closeTimeOfLast3mKline) {
        Log.e("UPDService", "UpdateDBSTart");
        int maxNrOfKlines = 20;
        List<String> listOfSymbols = new ArrayList<>();
        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        if (data.getCount() == 0) {
            Log.e("UPDService", "Table " + TABLE_SYMBOL_AVG + " is empty.");
        } else {
            while (data.moveToNext()) {
                listOfSymbols.add(data.getString(0));
            }
        }
        data.close();

        int nrOf3mKlinesFromLastDBUpdate = (int) (-timeDifference / 180000);
        data = databaseDB.nrOfKlinesForSymbolInInterval("ETHUSDT","3m");
        data.moveToFirst();
        int nrOf3mKlinesInDB = data.getInt(0);
        data.close();

        if(nrOf3mKlinesFromLastDBUpdate > 0) {
            //Delete not finshed kline from last time
            Log.e("UPDService", "HOW OFTEN");
            for (int i = 0; i < listOfSymbols.size(); i++){
                databaseDB.deleteLastKlinesForSymbolInterval("3m",listOfSymbols.get(i),1);
            }
            //Download last Klines
            observableStart(listOfSymbols, nrOf3mKlinesFromLastDBUpdate, "3m");
        }


        long nrOf15mKlinesFromLastDBUpdate = (timeDifference / 900000);
        long nrOf4hKlinesFromLastDBUpdate = (timeDifference / 14400000);
        long nrOf1dKlinesFromLastDBUpdate = (timeDifference / 86400000);

        Log.e("UPDService", "Last Close Time " + closeTimeOfLast3mKline);
        Log.e("UPDService", "How many 3minutes klines " + nrOf3mKlinesFromLastDBUpdate);
        Log.e("UPDService", "How many 15minutes klines " + nrOf15mKlinesFromLastDBUpdate);
        Log.e("UPDService", "How many 4hours klines " + nrOf4hKlinesFromLastDBUpdate);
        Log.e("UPDService", "How many 1day klines " + nrOf1dKlinesFromLastDBUpdate);

    }

        private void observableStart(List<String> listOfSymbols, int nrOfKlinesToDownload, String interval) {

            List<rawTable_Kline> klinesDataList = new ArrayList<>();
            List<KlineRequest> request = new ArrayList<>();
            List<Observable<?>> observableRequestList = new ArrayList<>();

            for (int i = 0; i < listOfSymbols.size(); i++) {
                request.add(new KlineRequest(RetrofitClient.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), nrOfKlinesToDownload + 1, interval),
                        listOfSymbols.get(i), "3m"));
            }

            for (int i = 0; i < request.size(); i++) {
                observableRequestList.add(request.get(i).getRequest());
            }

            Log.e("OBSERVABLE UPDATEDB", "request size: " + request.size());
            Log.e("OBSERVABLE UPDATEDB", "listOfCrypto size: " + listOfSymbols.size());

            Observable.zip(
                            observableRequestList,
                            new Function<Object[], Object>() {
                                @Override
                                public Object apply(Object[] objects) throws Exception {
                                    // Objects[] is an array of combined results of completed requests
                                    Log.e("OBSERVABLE", " HOW MANY? --> " + objects.length);
                                    for (int i = 0; i < objects.length; i++) {
                                        request.get(i).setDataOfSymbolInterval((String[][]) objects[i]);
                                    }
                                    // do something with those results and emit new event
                                    return new Object();
                                }
                            })
                    // After all requests had been performed the next observer will receive the Object, returned from Function
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            // Will be triggered if all requests will end successfully (4xx and 5xx also are successful requests too)
                            new Consumer<Object>() {
                                @Override
                                public void accept(Object o) throws Exception {
                                    //Do something on successful completion of all requests
                                    Log.e("OBSERVABLE UPDATEDB", " PERFECT");

                                    for (int i = 0; i < request.size(); i++) {
                                        String[][] aaa = request.get(i).getDataOfSymbolInterval();
                                        String symbol = request.get(i).getSymbol();
                                        String interval = request.get(i).getInterval();

                                        for (int j = 0; j < aaa.length; j++) {
                                            rawTable_Kline temp = new rawTable_Kline(symbol,
                                                    Long.parseLong(aaa[j][0]),
                                                    Float.parseFloat(aaa[j][1]),
                                                    Float.parseFloat(aaa[j][2]),
                                                    Float.parseFloat(aaa[j][3]),
                                                    Float.parseFloat(aaa[j][4]),
                                                    Float.parseFloat(aaa[j][5]),
                                                    Long.parseLong(aaa[j][6]),
                                                    Long.parseLong(aaa[j][8]),
                                                    interval);
                                            klinesDataList.add(temp);
                                        }
                                    }
                                    for (int i = 0; i < klinesDataList.size(); i++) {
                                        if (klinesDataList.get(i).getTokenSymbol() == null) {
                                            klinesDataList.remove(i);
                                        }
                                    }
                                    //Add everything to DB and refresh View
                                    if (databaseDB.addNewKlineData(klinesDataList) > 50) {
                                        klinesDataList.clear();
                                    }
                                }
                            },

                            // Will be triggered if any error during requests will happen
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable e) throws Exception {
                                    //Do something on error completion of requests
                                    Log.e("OBSERVABLE", " IS FUCKED" + e.toString());
                                }
                            }
                    );
    }


    private void startCountingAndReturnResult() {
        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        List<String> listOfSymbols = new ArrayList<>();
        if (data.getCount() == 0) {
            Log.e("UPDService", "Table " + TABLE_SYMBOL_AVG + " is empty.");
        } else {
            listOfApprovedCryptos.clear();
            while (data.moveToNext()) {
                listOfSymbols.add(data.getString(0));
            }
            for (int i = 0; i < listOfSymbols.size();i++) {
                countBestCryptoToBuy(listOfSymbols.get(i));
            }
            if (listOfApprovedCryptos.size() <= 1) {
                listOfApprovedCryptos.add("No Crypto is worth buying right now!");
            }
            Log.e("UPDService", listOfApprovedCryptos.toString());
            List<String> sortedCryptoList = listOfApprovedCryptos.stream().sorted().collect(Collectors.toList());
            sendMessageToActivity2(sortedCryptoList);

        }
        data.close();
    }

    private void checkDBLastTimeOfUpdate(){
        Cursor data = databaseDB.retrieveLastCloseTime(TABLE_NAME_KLINES_DATA, "3m");
        if (data.getCount() == 0) {
            Log.e("UPDService", "Table " + TABLE_NAME_KLINES_DATA + " is empty.");
        } else {
            data.moveToFirst();
            Kline tempKline = new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                    data.getFloat(5), data.getFloat(6),data.getFloat(7), data.getLong(8), data.getLong(9),data.getString(10));

            Timestamp stamp = new Timestamp(System.currentTimeMillis());
            Timestamp stamp2 = new Timestamp(tempKline.gettCloseTime());
            @SuppressLint("SimpleDateFormat")
            DateFormat df = new SimpleDateFormat("d MMM yyyy HH:mm:ss Z");
            String date = df.format(new Date(stamp.getTime()));
            String date2 = df.format(new Date(stamp2.getTime()));

            //if more than 3 minutes passed since last closeTimeOfKline --> UpdateDB
            long time = (tempKline.gettCloseTime() - System.currentTimeMillis());

            if(time < 0) {

                Cursor data2 = databaseDB.retrieveParam(1);
                if (data2.getCount() == 0) {
                    databaseDB.addParam(1, "Last Update Time", date);
                } else {
                    if (data2.getCount() >= 2) {
                        databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID,1);
                        databaseDB.addParam(1, "Last Update Time", date);
                    }
                    databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG,VALUE, date, ID,"1");
                   // updateDBtoCurrentValues(time, tempKline.gettCloseTime());
                }
                Log.e("UPDService", "DB is not actual" + tempKline.gettCloseTime() + " " + time + " " + System.currentTimeMillis() + " " + date + " " + date2);
                sendMessageToActivity(date);
            } else {
                Log.e("UPDService", "DB is actual" + tempKline.gettCloseTime() + " " + time + " " + System.currentTimeMillis() + " " + date + " " + date2);
            }
        }
        data.close();
    }

    // Function checks what crypto is going to run in green
    private void countBestCryptoToBuy(String symbol) {

        List<StatusOfKline> status = new ArrayList<>();

        List<Kline> coinKlines3m = new ArrayList<>();
        List<Kline> coinKlines15m = new ArrayList<>();
        List<Kline> coinKlines1d = new ArrayList<>();
        List<Kline> coinKlines4h = new ArrayList<>();

        Cursor data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol);

        if (data.getCount() >= 0) {

            while (data.moveToNext()) {

                Kline tempKline = new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                        data.getFloat(5), data.getFloat(6),data.getFloat(7), data.getLong(8), data.getLong(9),data.getString(10));

                String interval = tempKline.gettKlineInterval();
                switch (interval) {
                    case "3m":
                        coinKlines3m.add(tempKline);
                        break;
                    case "15m":
                        coinKlines15m.add(tempKline);
                        break;
                    case "1d":
                        coinKlines1d.add(tempKline);
                        break;
                    case "4h":
                        coinKlines4h.add(tempKline);
                        break;
                    default:
                        break;
                }

                float klinePlusMinus = tempKline.gettOpenPrice() - tempKline.gettClosePrice();
                StatusOfKline tempStatus;
                if (klinePlusMinus < 0) {
                    tempStatus = new StatusOfKline(tempKline.gettKlineInterval(), 1);
                } else {
                    tempStatus = new StatusOfKline(tempKline.gettKlineInterval(), 0);
                }
                status.add(tempStatus);

            }
        }

        if (isKlineAcceptable(status)) {
            listOfApprovedCryptos.add(symbol + "  APPROVED  ");
        }

        data.close();
        status.clear();

    }

    // Function check if proviced result from function countBestCryptoToBuy matches required status
    public boolean isKlineAcceptable(List<StatusOfKline> status) {

        int nrOfGreenKlines3m = 4;
        int nrOfGreenKlines15m = 3;
        int nrOfGreenKlines1d = 2;
        int sumOf3m = 0;
        int sumOf15m = 0;
        int sumOf1d = 0;

        for (int i = 0; i < status.size(); i++) {

            if (status.get(i).getInterval().equals("3m")) {
                sumOf3m += status.get(i).getResult();
            } else if (status.get(i).getInterval().equals("15m")) {
                sumOf15m += status.get(i).getResult();
            } else {
                sumOf1d += status.get(i).getResult();
            }

        }

        return sumOf3m == nrOfGreenKlines3m && sumOf15m == nrOfGreenKlines15m;
    }



}