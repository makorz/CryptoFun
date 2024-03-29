package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.data.ApprovedToken;
import com.example.cryptofun.data.KlineRequest;
import com.example.cryptofun.data.ObservableModel;
import com.example.cryptofun.data.StrategyParameters;
import com.example.cryptofun.data.StrategyResult;
import com.example.cryptofun.data.StrategyResultV2;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.data.database.Kline;
import com.example.cryptofun.data.database.rawTable_Kline;
import com.example.cryptofun.retrofit.RetrofitClientFutures;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UpdatingDatabaseService extends Service {

    private static final String TAG = "UPDTService";

    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String TABLE_NAME_APPROVED_HISTORIC = "historic_approved_tokens";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String VALUE_STRING = "value_string";
    private static final String ID = "id";
    private DBHandler databaseDB;
    private Handler handler;
//    private PowerManager.WakeLock mWakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        databaseDB = DBHandler.getInstance(getApplicationContext());
                        Log.e(TAG, "START of Service Thread");
//                        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//                        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyService:WakeLockTag");
//                        mWakeLock.acquire(5*60*1000L );

                        // It's for foreground services, because in newest Android, background are not working. Foreground need to inform user that it is running
                        Notification notification = ServiceFunctionsOther.createNotificationSimple("Updating crypto data.", TAG, getApplicationContext());
                        // Notification ID cannot be 0.
                        startForeground(1, notification);
                        checkDBLastTimeOfUpdate();
                    }
                }
        ).start();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "DESTROY");
    }

    private void sendMessageToActivity(String date, boolean updateStart) {
        Intent intent = new Intent("DB_updated");
        intent.putExtra("updateDate", date);
        intent.putExtra("updateStarted", updateStart);
        Log.e(TAG, "BROADCAST - UpdateDB Started in Background - " + updateStart);
        LocalBroadcastManager.getInstance(UpdatingDatabaseService.this).sendBroadcast(intent);

        //Wait 3 second before stopping service, error:  Process: com.example.cryptofun, PID: 6921 android.app.ForegroundServiceDidNotStartInTimeException: Context.startForegroundService() did not then cal Service.startForeground():
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopForeground(true);
                stopSelf();
            }
        }, 2000);


    }

    private void sendInfoToActivity() {
        Intent intent = new Intent("DB_update_start");
        Log.e(TAG, "BROADCAST - Loading icon START");
        LocalBroadcastManager.getInstance(UpdatingDatabaseService.this).sendBroadcast(intent);
    }

    private void checkDBLastTimeOfUpdate() {

        Cursor data = databaseDB.retrieveLastCloseTime("3m");
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_NAME_KLINES_DATA + " is empty. [checkDBLastTimeOfUpdate]");
            Timestamp stamp = new Timestamp(System.currentTimeMillis());
            @SuppressLint("SimpleDateFormat")
            DateFormat df = new SimpleDateFormat("HH:mm - EEE, dd");
            String date = "Update time:  " + df.format(new Date(stamp.getTime()));

            Cursor data2 = databaseDB.retrieveParam(1);
            if (data2.getCount() == 0) {
                databaseDB.addParam(1, "Last Update Time", date, 0, 0);
            } else {
                if (data2.getCount() >= 2) {
                    databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 1);
                    databaseDB.addParam(1, "Last Update Time", date, 0, 0);
                }
                databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_STRING, date, ID, "1");

            }
            data2.close();
            updateDBtoCurrentValues(System.currentTimeMillis());
            sendMessageToActivity(date, true);


        } else {
            Kline tempKline = new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                    data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10));

            Timestamp stamp = new Timestamp(System.currentTimeMillis());
            Timestamp stamp2 = new Timestamp(tempKline.gettCloseTime());
            @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("HH:mm:ss - EEE, dd");
            @SuppressLint("SimpleDateFormat") DateFormat df2 = new SimpleDateFormat("HH:mm:ss");
            String date = "Update time:  " + df.format(new Date(stamp.getTime()));
            String date2 = df.format(new Date(stamp2.getTime()));

            // if more than 1 minute passed since last closeTimeOfKline --> UpdateDB
            // Need to prevent updating kline data at 50s before last closeTime of kline (observable is prepared and when it reaches API binance it takes next kline data
            long timeSinceLastUpdate = 50000; //55000
            long time = (tempKline.gettCloseTime() - timeSinceLastUpdate - System.currentTimeMillis());
            long time2 = (tempKline.gettCloseTime() - System.currentTimeMillis());

            if (time > 0 || time2 < 0) {
                Log.e(TAG, "[DB IS NOT ACTUAL] LastKlineCLoseTime: " + tempKline.gettCloseTime() + " " + df2.format(tempKline.gettCloseTime()) + " --> difference: " + timeSinceLastUpdate + " --> currentTime: " + System.currentTimeMillis() + " " + df2.format(System.currentTimeMillis()) + " --> result: " + time + " (" + date + ") (" + date2 + ")");
                Cursor data2 = databaseDB.retrieveParam(1);
                if (data2.getCount() == 0) {
                    databaseDB.addParam(1, "Last Update Time", date, 0, 0);
                } else {
                    if (data2.getCount() >= 2) {
                        databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 1);
                        databaseDB.addParam(1, "Last Update Time", date, 0, 0);
                    }
                    databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_STRING, date, ID, "1");

                }
                data2.close();
                updateDBtoCurrentValues(System.currentTimeMillis());
                sendMessageToActivity(date, true);
            } else {
                Log.e(TAG, "[DB IS ACTUAL] LastKlineCLoseTime: " + tempKline.gettCloseTime() + " " + df2.format(tempKline.gettCloseTime()) + " --> difference: " + timeSinceLastUpdate + " --> currentTime: " + System.currentTimeMillis() + " " + df2.format(System.currentTimeMillis()) + " --> result: " + time + " (" + date + ") (" + date2 + ")");
                Cursor data3 = databaseDB.howManyRows(TABLE_NAME_KLINES_DATA);//
                data3.moveToFirst();
                if (data3.getLong(0) > 10) {
                    Log.e(TAG, "Table " + TABLE_NAME_KLINES_DATA + " is not empty. [onStartCommand]");
                    //startCountingAndReturnResult(true);

                }
                data3.close();
                sendMessageToActivity(date, false);
            }
        }
        data.close();
    }

    private void updateDBtoCurrentValues(long timeOfUpdate) {
        sendInfoToActivity();
        Log.e(TAG, "StartOfUpdate " + Thread.currentThread() + " " + Thread.activeCount());
        List<String> listOfSymbols = new ArrayList<>();
        List<ObservableModel> observableList = new ArrayList<>();

        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_SYMBOL_AVG + " is empty. updateDBtoCurrentValues");
        } else {
            do {
                listOfSymbols.add(data.getString(0));
            } while (data.moveToNext());
        }
        data.close();

        for (int i = 0; i < listOfSymbols.size(); i++) {
            observableList.add(updateIntervalOfDB(listOfSymbols.get(i), "3m", timeOfUpdate));
            observableList.add(updateIntervalOfDB(listOfSymbols.get(i), "15m", timeOfUpdate));
            observableList.add(updateIntervalOfDB(listOfSymbols.get(i), "4h", timeOfUpdate));
        }

        Iterator<ObservableModel> it = observableList.iterator();
        while (it.hasNext()) {
            ObservableModel iteratorItem = it.next();
            if (iteratorItem.getNrOfKlinesToDownload() == 0) {
                it.remove();
            }
        }

        observableStart(observableList);
    }

    public ObservableModel updateIntervalOfDB(String symbol, String interval, long timeCurrent) {

        Cursor data, data2, data3;
        int nrOfKlines;
        long closeTime, differenceTime;
        ObservableModel model = new ObservableModel(symbol, 0, "3m", 0, 0);
        int nrOfKlinesFromLastDBUpdate = 1;
        int maxNrOfKlines3 = 99;
        int maxNrOfKlines15 = 99;
        int maxNrOfKlines4 = 99;
        long minutes3 = 180000;
        long minutes15 = 900000;
        long hours4 = 14400000;
        String intervalInSwitch;

        switch (interval) {
            case "3m":
                intervalInSwitch = "3m";
                data = databaseDB.retrieveLastCloseTime(intervalInSwitch);
                data2 = databaseDB.nrOfKlinesForSymbolInInterval(symbol, intervalInSwitch);
                data3 = databaseDB.checkIfThereAreDuplicates(symbol, intervalInSwitch);
                data.moveToFirst();
                data2.moveToFirst();
                data3.moveToFirst();
                // First check if table is empty ora has duplicates
                if (data.getCount() == 0 || data2.getCount() == 0 || data3.getInt(1) > 1) {
//                    Log.e("UpdatingExistingDB", "Table " + TABLE_NAME_KLINES_DATA + " is empty. [updateIntervalOfDB 3m] + data3 = " + data3.getInt(1));
                    model = new ObservableModel(symbol, maxNrOfKlines3, intervalInSwitch, 1, 0); //20
                } else {
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    differenceTime = closeTime - timeCurrent;
                    nrOfKlinesFromLastDBUpdate += (-1 * differenceTime) / minutes3;
                    if (nrOfKlines != maxNrOfKlines3) {
                        model = new ObservableModel(symbol, maxNrOfKlines3, intervalInSwitch, 1, 0);
                        // Log.e(TAG, "A " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / minutes3 + " " + nrOfKlines + " " + maxNrOfKlines3);
                    } else {
                        if (nrOfKlinesFromLastDBUpdate == 1) {
                            // Only update last row to new values
                            if (differenceTime < 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate + 1, intervalInSwitch, 2, 1);
                                // Log.e(TAG, "B " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / minutes3 + " " + nrOfKlines + " " + (nrOfKlinesFromLastDBUpdate+1));
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 2, 0);
                                // Log.e(TAG, "C " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / minutes3 + " " + nrOfKlines + " " + nrOfKlinesFromLastDBUpdate);
                            }
                        } else {
                            // Delete oldest rows, add newest rows
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate + 1, intervalInSwitch, 2, nrOfKlinesFromLastDBUpdate);
                            // Log.e(TAG, "D " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / minutes3 + " " + nrOfKlines + " " + (nrOfKlinesFromLastDBUpdate+1));
                        }
                    }

                }
                data.close();
                data2.close();
                break;
            case "15m":
                intervalInSwitch = "15m";
                data = databaseDB.retrieveLastCloseTime(intervalInSwitch);
                data2 = databaseDB.nrOfKlinesForSymbolInInterval(symbol, intervalInSwitch);
                data3 = databaseDB.checkIfThereAreDuplicates(symbol, intervalInSwitch);
                data.moveToFirst();
                data2.moveToFirst();
                data3.moveToFirst();
                if (data.getCount() == 0 || data2.getCount() == 0 || data3.getInt(1) > 1) {
//                    Log.e(TAG, "UpdateExistingDB - Table " + TABLE_NAME_KLINES_DATA + " is empty. [updateIntervalOfDB 15m]");
                    model = new ObservableModel(symbol, maxNrOfKlines15, intervalInSwitch, 1, 0);
                } else {
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    differenceTime = closeTime - timeCurrent;
                    nrOfKlinesFromLastDBUpdate += (-1 * differenceTime) / minutes15;
                    if (nrOfKlines != maxNrOfKlines15) {
                        model = new ObservableModel(symbol, maxNrOfKlines15, intervalInSwitch, 1, 0);
                        // Log.e(TAG, "A " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / minutes15 + " " + nrOfKlines + " " + maxNrOfKlines15);
                    } else {
                        if (nrOfKlinesFromLastDBUpdate == 1) {
                            // Only update last row to new values
                            if (differenceTime < 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate + 1, intervalInSwitch, 2, 1);
                                // Log.e(TAG, "B " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / minutes15 + " " + nrOfKlines + " " + (nrOfKlinesFromLastDBUpdate+1));
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 2, 0);
                                //  Log.e(TAG, "C " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / minutes15 + " " + nrOfKlines + " " + nrOfKlinesFromLastDBUpdate);
                            }
                        } else {
                            // Delete oldest rows, add newest rows
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate + 1, intervalInSwitch, 2, nrOfKlinesFromLastDBUpdate);
                            // Log.e(TAG, "D " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / minutes15 + " " + nrOfKlines + " " + (nrOfKlinesFromLastDBUpdate+1));
                        }
                    }
                }
                data.close();
                data2.close();
                break;
            case "4h":
                intervalInSwitch = "4h";
                data = databaseDB.retrieveLastCloseTime(intervalInSwitch);
                data2 = databaseDB.nrOfKlinesForSymbolInInterval(symbol, intervalInSwitch);
                data3 = databaseDB.checkIfThereAreDuplicates(symbol, intervalInSwitch);
                data.moveToFirst();
                data2.moveToFirst();
                data3.moveToFirst();
                if (data.getCount() == 0 || data2.getCount() == 0 || data3.getInt(1) > 1) {
//                    Log.e(TAG, "UpdateExistingDB - Table " + TABLE_NAME_KLINES_DATA + " is empty. [updateIntervalOfDB 4h]");
                    model = new ObservableModel(symbol, maxNrOfKlines4, intervalInSwitch, 1, 0); //maxNrOfKlines4
                } else {
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    differenceTime = closeTime - timeCurrent;
                    nrOfKlinesFromLastDBUpdate += (-1 * differenceTime) / hours4;
                    if (nrOfKlines != maxNrOfKlines4) {
                        model = new ObservableModel(symbol, maxNrOfKlines4, intervalInSwitch, 1, 0);
                        // Log.e(TAG, "A " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / hours4 + " " + nrOfKlines + " " + maxNrOfKlines4);
                    } else {
                        if (nrOfKlinesFromLastDBUpdate == 1) {
                            // Only update last row to new values
                            if (differenceTime < 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate + 1, intervalInSwitch, 2, 1);
                                // Log.e(TAG, "B " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / hours4 + " " + nrOfKlines + " " + (nrOfKlinesFromLastDBUpdate+1));
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 2, 0);
                                // Log.e(TAG, "C " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / hours4 + " " + nrOfKlines + " " + nrOfKlinesFromLastDBUpdate);
                            }
                        } else {
                            // Delete oldest rows, add newest rows
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate + 1, intervalInSwitch, 2, nrOfKlinesFromLastDBUpdate);
                            //  Log.e(TAG, "D " + closeTime + " " + timeCurrent + " " + (closeTime - timeCurrent) + " " + -(closeTime - timeCurrent) / hours4 + " " + nrOfKlines + " " + (nrOfKlinesFromLastDBUpdate+1));
                        }
                    }
                }
                data.close();
                data2.close();
                break;
            default:
                break;
        }
        return model;
    }

    @SuppressLint("CheckResult")
    private void observableStart(List<ObservableModel> list) {

        Log.e(TAG, "Observable size: " + list.size());

        List<rawTable_Kline> klinesDataList = new ArrayList<>();
        List<KlineRequest> request = new ArrayList<>();
        List<Observable<?>> observableRequestList = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            request.add(new KlineRequest(RetrofitClientFutures.getInstance().getMyApi().getKlinesData(list.get(i).getSymbol(), list.get(i).getNrOfKlinesToDownload(), list.get(i).getInterval()),
                    list.get(i).getSymbol(), list.get(i).getInterval(), list.get(i).getWhatToDoWithDB(), list.get(i).getHowManyOldOnesToDelete()));
//            Log.e(TAG, "Request [" + list.get(i).getSymbol() + "] NrOfKlines: " + list.get(i).getNrOfKlinesToDownload() + " Interval: " + list.get(i).getInterval() + " WhatToDo: " + list.get(i).getWhatToDoWithDB() + " HowManyOldOnesToDelete: " + list.get(i).getHowManyOldOnesToDelete());
        }

        for (int i = 0; i < request.size(); i++) {
            observableRequestList.add(request.get(i).getRequest());
        }

        Log.e(TAG, "Request size: " + request.size());

        Observable.zip(
                        observableRequestList,
                        new Function<Object[], Object>() {
                            @Override
                            public Object apply(Object[] objects) throws Exception {
                                // Objects[] is an array of combined results of completed requests
                                Log.e(TAG, "Observable ZIP size: " + objects.length);
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
                                Log.e(TAG, "Observable UpdateDB - PERFECT");
                                new Thread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                databaseDB = DBHandler.getInstance(getApplicationContext());
                                                for (int i = 0; i < request.size(); i++) {
                                                    String[][] aaa = request.get(i).getDataOfSymbolInterval();
                                                    String symbol = request.get(i).getSymbol();
                                                    String interval = request.get(i).getInterval();
                                                    int whatToDoInDB = request.get(i).getWhatToDoInDB();
                                                    int howManyOldOnes = request.get(i).getHowManyOldOnesToDelete();

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

                                                    //Get rid of nulls
                                                    klinesDataList.removeIf(iteratorItem -> iteratorItem.getTokenSymbol() == null);

                                                    switch (whatToDoInDB) {
                                                        case 1:
                                                            databaseDB.deleteAllKlinesForSymbolInterval(interval, symbol, klinesDataList);
                                                            klinesDataList.clear();
                                                            break;
                                                        case 2:
                                                            databaseDB.deleteKlinesAndInsertNewKlinesForSymbolInterval(interval, symbol, 1, howManyOldOnes, klinesDataList);
                                                            klinesDataList.clear();
                                                            break;
                                                        default:
                                                            break;
                                                    }
                                                }
                                                startCountingAndReturnResult(false);

                                            }
                                        }
                                ).start();

                            }
                        },

                        // Will be triggered if any error during requests will happen
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) throws Exception {
                                Log.e(TAG, "Observable UpdateDB - FUCKED " + e);
                                Toast.makeText(getApplicationContext(), "Error: " + e, Toast.LENGTH_LONG).show();
                                startCountingAndReturnResult(true);
                            }
                        }

                );
    }

    private void startCountingAndReturnResult(boolean wasThereError) {

        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        List<String> listOfSymbols = new ArrayList<>();
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_SYMBOL_AVG + " is empty. [startCountingAndReturnResult]");
        } else {
            do {
                listOfSymbols.add(data.getString(0));
            } while (data.moveToNext());
            //databaseDB.clearTable(TABLE_NAME_APPROVED);
            for (int i = 0; i < listOfSymbols.size(); i++) {
                countBestCryptoToBuy(listOfSymbols.get(i));
            }
            Timestamp stamp = new Timestamp(System.currentTimeMillis());
            //long twentyFourHours = 86400000;
            //long olderThan = System.currentTimeMillis() - twentyFourHours;
            @SuppressLint("SimpleDateFormat")
            DateFormat df = new SimpleDateFormat("HH:mm - EEE, dd");
            String date;
            if (wasThereError) {
                Log.e(TAG, "Error from Observable/Or DB is actual.");
                Cursor data2 = databaseDB.retrieveParam(1);
                if (data2.getCount() == 0) {
                    Log.e(TAG, "No param");
                    date = "No update time";
                } else {
                    data2.moveToFirst();
                    date = data2.getString(2);
                }
                data2.close();
            } else {
                date = "Update time:  " + df.format(new Date(stamp.getTime()));
            }
            //databaseDB.clearHistoricApproved(olderThan);
            sendMessageToActivity(date, false);
        }

//        if (mWakeLock != null) {
//            mWakeLock.release();
//            mWakeLock = null;
//        }

        data.close();
    }

    private void countBestCryptoToBuy(String symbol) {

        List<Kline> coinKlines3m = new ArrayList<>();
        List<Kline> coinKlines15m = new ArrayList<>();
        List<Kline> coinKlines4h = new ArrayList<>();

        Cursor data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol);
        data.moveToFirst();
        if (data.getCount() > 0) {

            do {
                Kline tempKline = new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                        data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10));

                String interval = tempKline.gettKlineInterval();
                switch (interval) {
                    case "3m":
                        coinKlines3m.add(tempKline);
                        break;
                    case "15m":
                        coinKlines15m.add(tempKline);
                        break;
                    case "4h":
                        coinKlines4h.add(tempKline);
                        break;
                    default:
                        break;
                }
            } while (data.moveToNext());
        }
        data.close();

        /* TODO:
               Przywrócić wartości jak wejdzie test na produkcji. Również wartości na kwoty zleceń i ilość zleceń w Approving service oraz w RetrfitClientSecrettEstnet pobieranie api z bazy dnaych
         */

        int activeStrategy;

        data = databaseDB.retrieveParam(17);
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 17");
            databaseDB.addParam(17, "Active Strategy Nr:", "", 1, 0);
            activeStrategy = 1;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 17);
            databaseDB.addParam(17, "Active Strategy Nr:", "", 1, 0);
            activeStrategy = 1;
        } else {
            activeStrategy = data.getInt(3);
        }
        data.close();

        switch (activeStrategy) {
            case 2:
                // Do usunięcia - fatalne wyniki
                strategyNr2_WT_ATR_T4AJStrategy(symbol, coinKlines3m, coinKlines15m, coinKlines4h);
                break;
            case 3:
                strategyNr3_RSI_EMA_15m(symbol, coinKlines3m, coinKlines15m, coinKlines4h);
                break;
            case 4:
                strategyNr4_RSI_WT_4h_15m(symbol, coinKlines3m, coinKlines15m, coinKlines4h);
                break;
            case 5:
                // Inny stop loss co w 6
                strategyNr6_EMA_4h_WT_ADX_EMA_15m(symbol, coinKlines3m, coinKlines15m, coinKlines4h);
                //strategyNr5_WT_PPO_ADX(symbol, coinKlines3m, coinKlines15m, coinKlines4h, statusOf3mKlines, statusOf15mKlines, statusOf4hKlines);
                break;
            case 6:
                strategyNr6_EMA_4h_WT_ADX_EMA_15m(symbol, coinKlines3m, coinKlines15m, coinKlines4h);
                break;
            default:
                //strategyDefaultNr1_SERIES(symbol, coinKlines3m, coinKlines15m, coinKlines4h);
                break;
        }
    }

//    private void strategyDefaultNr1_SERIES(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h) {
//
//             /*
//                            Version1: (priceDirection3 == 0 && priceDirection15 == 0 && waveTrendPredict15 == 0) --> 4,5/10
//                            Version2: (isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0) && priceDirection3 == 0 && priceDirection15 == 0) --> 6,5/10
//                            Version3: isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0)  && sumDirection >= -2  --> 3/10
//                            Version4: isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 1) && (priceDirection3 +  predict3) >= 1) --> 3/10
//                            Version5 - currently API 30 : isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0) && priceDirection3 == -1 && priceDirection15 == -1 && predict3 == -1
//                            Version6 - currently API 27 :  priceDirection3 == -1 && priceDirection15 == 1 && priceDirection4 == -1
//                            !!!! Combined last two: 5 - used when (last15mPriceChange > 0.5 || last15mPriceChange < -0.5) --- 6 - used when (last15mPriceChange > 3 || last15mPriceChange < -3) --- 6 first before 5 !!!!
//                                                 *This combined quite well works if market is going down or up, not when it's stabilised
//
//                             API 27 (01.07) --> ((waveTrendPredict15 + waveTrendPredict4 ) == -2 || (waveTrendPredict15 + waveTrendPredict4 ) == 2) && (priceDirection4 + predict15 == -2)
//                             API 30 (01.07) -->  isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 0)  && priceDirection3 == -1 && priceDirection4 == 1 &&  ((waveTrendPredict15 + waveTrendPredict4 + waveTrendPredict3) == 0)
//
//
//              */
//
//        int acceptableVolume = 1000000;
//        float volumeOfLast15mKlines;
//
//        if (coinKlines15m.size() > 2) {
//            volumeOfLast15mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 2);
//        } else {
//            volumeOfLast15mKlines = acceptableVolume;
//        }
//
//        int acceptablePercentOfVolumeRise = 30;
//        float percentOfRiseOfNumberOfVolumeInLast15min;
//
//        if (coinKlines3m.size() >= 2) {
//            percentOfRiseOfNumberOfVolumeInLast15min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines3m, 10);
//        } else {
//            percentOfRiseOfNumberOfVolumeInLast15min = 0;
//        }
//
//        if (volumeOfLast15mKlines >= acceptableVolume && percentOfRiseOfNumberOfVolumeInLast15min > acceptablePercentOfVolumeRise && !symbol.contains("BUSDUSDT") && coinKlines15m.size() >= 58 && coinKlines3m.size() >= 38 && coinKlines4h.size() >= 18) {
//
////            List<Integer> statusListOf3mToCheck;
////            List<Integer> statusListOf15mToCheck;
////            statusListOf15mToCheck = statusOf15mKlines.subList(0, 3);
////            statusListOf3mToCheck = statusOf3mKlines.subList(0, 10);
//            int nrOfTradesLast15mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 4);
//
//            DecimalFormat df2 = new DecimalFormat("0.00");
//
//            ArrayList<Double> PriceChangePercents = new ArrayList<>();
//            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 2))); //6m
//            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 5))); //15m
//            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 10))); //30m
//            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines15m.subList(0, 8))); //2h
//            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines15m.subList(0, 16))); //4h
//            String PriceLogValue = "";
//            for (Double number : PriceChangePercents) {
//                PriceLogValue += df2.format(number) + "% ";
//            }
//
//            int priceDirection3 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines3m.subList(0, 15));
//            int priceDirection15 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines15m.subList(0, 16));
//            int priceDirection4 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines4h.subList(0, 8));
//
//            int predict3 = ServiceFunctionsStrategyDefault.predict(coinKlines3m);
//            int predict15 = ServiceFunctionsStrategyDefault.predict(coinKlines15m);
//            int predict4 = ServiceFunctionsStrategyDefault.predict(coinKlines4h);
//
//            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 1 Volume30: " + volumeOfLast15mKlines + " %OfVolumeRise15: " + percentOfRiseOfNumberOfVolumeInLast15min + "  Predicted(3,15,4): " + predict3 + " " + predict15 + " " + predict4 + " PriceDirection(3,15,4): " + priceDirection3 + " " + priceDirection15 + " " + priceDirection4 + " LastPriceChange(6,15,30,2,4): " + PriceLogValue;
//            Log.e(TAG, info);
//            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
//            @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("HH:mm:ss");
//
//            if (ServiceFunctionsStrategyDefault.isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 1, 8, 3)) {
//
//                if (PriceChangePercents.get(2) < -4) {
//
//                    if (priceDirection3 == 1 && priceDirection4 == 1) {
//                        info = "LEVEL 2 [" + symbol + " SHORT] approved path price < -4, price directions up, approved at:  " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
//                        Log.e(TAG, info);
//                        ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
//
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//                    }
//                } else if (PriceChangePercents.get(2) > 0.2 && PriceChangePercents.get(2) < 2) {
//
//                    if (priceDirection3 == 1 && priceDirection15 == 1 && predict15 == 1 && priceDirection4 == 1) {
//                        info = "LEVEL 2 [" + symbol + " LONG] approved path price 0.2 - 2, price directions up, predict15 up, approved at: " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
//                        Log.e(TAG, info);
//                        ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
//
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//                    }
//                }
//            } else if (ServiceFunctionsStrategyDefault.isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 0, 8, 3)) {
//
//                if (PriceChangePercents.get(1) > 4) {
//
//                    if (priceDirection3 == -1 && priceDirection4 == -1) {
//                        info = "LEVEL 2 [" + symbol + " LONG] approved path price > 4, price directions down, approved at: " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
//                        Log.e(TAG, info);
//                        ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
//
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//                    }
//
//                } else if (PriceChangePercents.get(2) < -0.2 && PriceChangePercents.get(2) > -2) {
//
//                    if (priceDirection3 == -1 && priceDirection15 == -1 && predict15 == -1 && priceDirection4 == -1) {
//                        info = "LEVEL 2 [" + symbol + " SHORT] approved path price -0.2 - -2, price directions down, predict15 down, approved at: " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
//                        Log.e(TAG, info);
//                        ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
//
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//                    }
//                }
//            }
//
//        }
//    }

    private void strategyNr2_WT_ATR_T4AJStrategy(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h) {

        float volumeOfLast15mKlines;
        int acceptableVolume = 1000000; //1000000


        if (coinKlines15m.size() > 2) {
            volumeOfLast15mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 2);
        } else {
            volumeOfLast15mKlines = acceptableVolume;
        }

        float percentOfRiseOfNumberOfVolumeInLast15min;
        int acceptablePercentOfVolumeRise = 30;

        if (coinKlines3m.size() >= 2) {
            percentOfRiseOfNumberOfVolumeInLast15min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines3m, 10);
        } else {
            percentOfRiseOfNumberOfVolumeInLast15min = 0;
        }

        if (volumeOfLast15mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT") && coinKlines15m.size() >= 58 && coinKlines3m.size() >= 38 && coinKlines4h.size() >= 15) {

//            List<Integer> statusListOf3mToCheck;
//            List<Integer> statusListOf15mToCheck;
//            statusListOf15mToCheck = statusOf15mKlines.subList(0, 3);
//            statusListOf3mToCheck = statusOf3mKlines.subList(0, 10);

            DecimalFormat df = new DecimalFormat("0.000000");
            DecimalFormat df2 = new DecimalFormat("0.00");

            int predict3, predict15, predict4, priceDirection3, priceDirection15, priceDirection4, waveTrendPredict3, waveTrendPredict15, waveTrendPredict4;
            int nrOfTradesLast15mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 2);

            ArrayList<Double> PriceChangePercents = new ArrayList<>();
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 2))); //6m
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 5))); //15m
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 10))); //30m
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines15m.subList(0, 8))); //2h
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines15m.subList(0, 16))); //4h
            String PriceLogValue = "";

            for (Double number : PriceChangePercents) {
                PriceLogValue += df2.format(number) + "% ";
            }

            predict3 = ServiceFunctionsStrategyDefault.predict(coinKlines3m);
            priceDirection3 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines3m.subList(0, 12));

            StrategyResult strategy3m = ServiceFunctionsStrategyTa4J.strategyTa4J_nr2(coinKlines3m, getApplicationContext());
            String strategyResult3m = "Strategy3m: [" + strategy3m.getBestStrategy() + "] Positions(M,A): " + strategy3m.getNrOfPositions().get(0).toString() + " " + strategy3m.getNrOfPositions().get(1).toString() + " vsBuyAndHold(M,A): "
                    + strategy3m.getVsBuyAndHoldProfit().get(0).toString() + " " + strategy3m.getVsBuyAndHoldProfit().get(1).toString() + " total(M,A): " + strategy3m.getTotalProfit().get(0).toString() + " " + strategy3m.getTotalProfit().get(1).toString();

            if (strategy3m.getNrOfPositions().get(0) == 0 && strategy3m.getNrOfPositions().get(1) == 0) {
                strategyResult3m = "Strategy3m: NO POSITIONS";
            }
            int finalWaveTrend3m = strategy3m.getWaveTrendIndicator();

            predict15 = ServiceFunctionsStrategyDefault.predict(coinKlines15m);
            priceDirection15 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines15m.subList(0, 12));

            StrategyResult strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J_nr2(coinKlines15m, getApplicationContext());
            String strategyResult15m = "Strategy15m: [" + strategy15m.getBestStrategy() + "] Positions(M,A): " + strategy15m.getNrOfPositions().get(0).toString() + " " + strategy15m.getNrOfPositions().get(1).toString() + " vsBuyAndHold(M,A): "
                    + strategy15m.getVsBuyAndHoldProfit().get(0).toString() + " " + strategy15m.getVsBuyAndHoldProfit().get(1).toString() + " total(M,A): " + strategy15m.getTotalProfit().get(0).toString() + " " + strategy15m.getTotalProfit().get(1).toString();

            if (strategy15m.getNrOfPositions().get(0) == 0 && strategy15m.getNrOfPositions().get(1) == 0) {
                strategyResult15m = "Strategy15m: NO POSITIONS";
            }
            int finalWaveTrend15m = strategy15m.getWaveTrendIndicator();

            predict4 = ServiceFunctionsStrategyDefault.predict(coinKlines4h);
            priceDirection4 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines4h.subList(0, 15));

            int start = 0;
            int add = 2;
            double sumATR15 = 0;
            double avgATR15 = 0;
            ArrayList<Double> ATR_ChangeFromAVG15 = new ArrayList<>();

            List<Kline> klinesList15 = coinKlines15m.subList(0, 18);
            String ATRValues15 = "";
            for (int i = 0; i < (klinesList15.size() / add); i++) {
                ArrayList<Kline> klinesListInterval = new ArrayList<>(klinesList15.subList(start + add * i, add + add * i));
                double temp = ServiceFunctionsStrategyDefault.calculateATR(klinesListInterval, add);
                sumATR15 += temp;
                ATR_ChangeFromAVG15.add(temp);
            }
            avgATR15 = sumATR15 / ((double) (klinesList15.size() / add));

            ArrayList<Double> percentATR15 = new ArrayList<>();
            for (Double number : ATR_ChangeFromAVG15) {
                number = ((number * 100) / avgATR15) - 100;
                percentATR15.add(number);
                ATRValues15 += df2.format(number) + "% ";
            }

            double sumATR3 = 0;
            double avgATR3 = 0;
            ArrayList<Double> ATR_ChangeFromAVG3 = new ArrayList<>();

            List<Kline> klinesList3 = coinKlines3m.subList(0, 20);
            String ATRValues3 = "";
            for (int i = 0; i < (klinesList3.size() / add); i++) {
                ArrayList<Kline> klinesListInterval = new ArrayList<>(klinesList3.subList(start + add * i, add + add * i));
                double temp = ServiceFunctionsStrategyDefault.calculateATR(klinesListInterval, add);
                sumATR3 += temp;
                ATR_ChangeFromAVG3.add(temp);
            }
            avgATR3 = sumATR3 / ((double) (klinesList3.size() / add));

            ArrayList<Double> percentATR3 = new ArrayList<>();
            for (Double number : ATR_ChangeFromAVG3) {
                number = ((number * 100) / avgATR3) - 100;
                percentATR3.add(number);
                ATRValues3 += df2.format(number) + "% ";
            }

            String info = "LEVEL 1 [" + symbol + "] Volume30: " + volumeOfLast15mKlines + " %OfVolumeRise15: " + percentOfRiseOfNumberOfVolumeInLast15min + " WaveTrendStrategy(3,15): " + finalWaveTrend3m + " " + finalWaveTrend15m + " AvgATR15: " + df.format(avgATR15) + " ATR%15: " + ATRValues15 + " " + " AvgATR3: " + df.format(avgATR3) + " ATR%3: " + ATRValues3 + " " + strategyResult3m + " " + strategyResult15m + " " + coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice + " Predicted(3, 15, 4): " + predict3 + " " + predict15 + " " + predict4 + " PriceDirection: " + priceDirection3 + " " + priceDirection15 + " " + priceDirection4 + " LastPriceChange(6,15,30,2,4): " + PriceLogValue;
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");

            @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("HH:mm:ss");

            if (finalWaveTrend3m == 1 && finalWaveTrend15m == 1 && percentATR15.get(0) < 70 && percentATR15.get(1) < 70 && percentATR15.get(0) > -25 && percentATR15.get(1) > -25 && PriceChangePercents.get(4) > -5) {
                info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                Log.e(TAG, info);
                ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);

            } else if (finalWaveTrend3m == -1 && finalWaveTrend15m == -1 && percentATR15.get(0) < 70 && percentATR15.get(1) < 70 && percentATR15.get(0) > -25 && percentATR15.get(1) > -25 && PriceChangePercents.get(4) < 5) {
                info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                Log.e(TAG, info);
                ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
            }

        }

    }


    private void strategyNr4_RSI_WT_4h_15m(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h) {

        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 1000000; //1000000
        int acceptablePercentOfVolumeRise = 30;
        int acceptableNrOfTrades = 5000;

        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
        }

        if (coinKlines3m.size() > 15) {
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines3m, 15);
        } else {
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }

        if (nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && percentOfRiseOfNumberOfVolumeInLast45min > acceptablePercentOfVolumeRise && !symbol.contains("BUSDUSDT") && coinKlines15m.size() >= 59 && coinKlines3m.size() >= 39 && coinKlines4h.size() >= 19) {

            DecimalFormat df = new DecimalFormat("0.000000");
            DecimalFormat df2 = new DecimalFormat("0.00");
            @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("HH:mm:ss");

            ArrayList<Double> PriceChangePercents = new ArrayList<>();
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 2))); //6m
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 5))); //15m
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 10))); //30m
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines15m.subList(0, 8))); //2h
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines15m.subList(0, 16))); //4h
            String PriceLogValue = "";
            for (Double number : PriceChangePercents) {
                PriceLogValue += df2.format(number) + "% ";
            }

            int start = 0;
            int add = 2;
            double sumATR15 = 0;
            double avgATR15 = 0;
            ArrayList<Double> ATR_ChangeFromAVG15 = new ArrayList<>();

            List<Kline> klinesList15 = coinKlines15m.subList(0, 18);
            String ATRValues15 = "";
            for (int i = 0; i < (klinesList15.size() / add); i++) {
                ArrayList<Kline> klinesListInterval = new ArrayList<>(klinesList15.subList(start + add * i, add + add * i));
                double temp = ServiceFunctionsStrategyDefault.calculateATR(klinesListInterval, add);
                sumATR15 += temp;
                ATR_ChangeFromAVG15.add(temp);
            }
            avgATR15 = sumATR15 / ((double) (klinesList15.size() / add));

            ArrayList<Double> percentATR15 = new ArrayList<>();
            for (Double number : ATR_ChangeFromAVG15) {
                number = ((number * 100) / avgATR15) - 100;
                percentATR15.add(number);
                ATRValues15 += df2.format(number) + "% ";
            }

            int priceDirection15 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines15m.subList(0, 16));
            int priceDirection3 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines3m.subList(0, 15));
            int priceDirection4 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines4h.subList(0, 8));

            StrategyParameters params3m = new StrategyParameters(8, "3m", 60, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params15m = new StrategyParameters(2, "15m", 60, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params4h = new StrategyParameters(1, "4h", 60, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy3m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines3m, 4, params3m, getApplicationContext());
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 4, params15m, getApplicationContext());
            StrategyResultV2 strategy4h = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 4, params4h, getApplicationContext());

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 4 ---> [Volume45]: " + volumeOfLast45mKlines + " [%OfVolumeRise45]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades45m]:" + nrOfTradesLast45mKlinesSum + " AvgATR15: " + df.format(avgATR15) + " ATR%15: " + ATRValues15 + " PriceDirection: " + priceDirection3 + " " + priceDirection15 + " " + priceDirection4 + " LastPriceChange(6,15,30,2,4): " + PriceLogValue + " [TrendMACD<>4h-15m]: " + strategy4h.getPassedMACD() + " " + strategy15m.getPassedMACD() + " [WT<>4h-15m]: " + strategy4h.getPassedWT() + " " + strategy15m.getPassedWT() + " [RSI<>4h-15m]: " + strategy4h.getPassedRSI() + " " + strategy15m.getPassedRSI();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");

            long hours4 = 14400000;

            if (strategy4h.getPassedRSI() == 1 && strategy15m.getPassedWT() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours4));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();

            } else if (strategy4h.getPassedRSI() == -1 && strategy15m.getPassedWT() == 1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours4));
                data.moveToFirst();
                if (data.getCount() == 0) {

                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }

        }

    }

    private void strategyNr3_RSI_EMA_15m(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h) {

        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 500000; //1000000
        int acceptablePercentOfVolumeRise = 25;
        int acceptableNrOfTrades = 1000;

        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
        }

        if (coinKlines3m.size() > 15) {
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines3m, 15);
        } else {
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }

        if (nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && !symbol.contains("BUSDUSDT") && coinKlines15m.size() >= 90 && coinKlines3m.size() >= 90 && coinKlines4h.size() >= 90) { //  && volumeOfLast45mKlines >= acceptableVolume && percentOfRiseOfNumberOfVolumeInLast45min > acceptablePercentOfVolumeRise &&

            @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("HH:mm:ss");

            StrategyParameters params3m = new StrategyParameters(2, "3m", 60, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params15m = new StrategyParameters(4, "15m", 60, 30, 70, 90, 30, 0.5f, 12.0f);
            StrategyParameters params4h = new StrategyParameters(4, "4h", 60, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy3m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines3m, 3, params3m, getApplicationContext());
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 3, params15m, getApplicationContext());
            StrategyResultV2 strategy4h = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 3, params4h, getApplicationContext());

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 3 ---> [Volume45]: " + volumeOfLast45mKlines + " [%OfVolumeRise45]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades45m]:" + nrOfTradesLast45mKlinesSum + " [TrendEMA<>4h-15m]: " + strategy4h.getPassedEMA() + " " + strategy15m.getPassedEMA() + " [WT<>4h-15m]: " + strategy4h.getPassedWT() + " " + strategy15m.getPassedWT() + " [RSI<>4h-15m]: " + strategy4h.getPassedRSI() + " " + strategy15m.getPassedRSI();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");

            long hours4 = 14400000;

            if (strategy15m.getPassedEMA() == 1 && strategy15m.getPassedRSI() == 1 && strategy4h.getPassedEMA() == 1 ) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours4));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();

            } else if (strategy15m.getPassedRSI() == -1 && strategy15m.getPassedEMA() == -1 && strategy4h.getPassedEMA() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours4));
                data.moveToFirst();
                if (data.getCount() == 0) {

                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }

        }

    }

    private void strategyNr6_EMA_4h_WT_ADX_EMA_15m(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h) {

        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 250000; //1000000
       // int acceptablePercentOfVolumeRise = 10;
        int acceptableNrOfTrades = 1000;

        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
        }

        if (coinKlines3m.size() > 15) {
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines3m, 15);
        } else {
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }

        @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("HH:mm:ss");
        @SuppressLint("SimpleDateFormat") DateFormat df4 = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

        String e = "LEVEL 0,5 [" + symbol + "] " + coinKlines4h.get(0).toString() + " size: " + coinKlines4h.size() + " CurrentTime: " + df4.format(System.currentTimeMillis()) + " (" + System.currentTimeMillis() + ") "
                + " OpenTime: " + df4.format(coinKlines4h.get(0).gettOpenTime()) + " (" + coinKlines4h.get(0).gettOpenTime() + ") " + " difference:" + (System.currentTimeMillis() - coinKlines4h.get(0).gettOpenTime());
        Log.e(TAG, e);

        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines4h.get(0).gettOpenTime();
        long minutes20 = 1200000;
        long hours8 = 28800000;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes20 && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT") && coinKlines15m.size() >= 59 && coinKlines3m.size() >= 39 && coinKlines4h.size() >= 19) { // && percentOfRiseOfNumberOfVolumeInLast45min > acceptablePercentOfVolumeRise


            StrategyParameters params3m = new StrategyParameters(2, "3m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params15m = new StrategyParameters(3, "15m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params4h = new StrategyParameters(1, "4h", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy3m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines3m, 6, params3m, getApplicationContext());
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 6, params15m, getApplicationContext());
            StrategyResultV2 strategy4h = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 6, params4h, getApplicationContext());

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 6 ---> [TrendEMA<>4h-15m]: " + strategy4h.getPassedEMA() + " " + strategy15m.getPassedEMA() + " [WT<>4h-15m]: " + strategy4h.getPassedWT() + " " + strategy15m.getPassedWT() + " [ADX<>4h-15m]: " + strategy4h.getPassedADX() + " " + strategy15m.getPassedADX();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");


            if (strategy4h.getPassedEMA() == 1 && strategy15m.getPassedEMA() == 1 && strategy15m.getPassedADX() == 1 && strategy15m.getPassedWT() == 1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();

            } else if  (strategy4h.getPassedEMA() == -1 && strategy15m.getPassedEMA() == -1 && strategy15m.getPassedADX() == 1 && strategy15m.getPassedWT() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {

                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }

        }

    }

    private void strategyNr6_RSI_EMA_4h(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h) {

        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 1500000; //1000000
        int acceptablePercentOfVolumeRise = 25;
        int acceptableNrOfTrades = 3000;

        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
        }

        if (coinKlines3m.size() > 15) {
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines3m, 15);
        } else {
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }

        if (nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && percentOfRiseOfNumberOfVolumeInLast45min > acceptablePercentOfVolumeRise && !symbol.contains("BUSDUSDT") && coinKlines15m.size() >= 59 && coinKlines3m.size() >= 39 && coinKlines4h.size() >= 19) {

            DecimalFormat df = new DecimalFormat("0.000000");
            DecimalFormat df2 = new DecimalFormat("0.00");
            @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("HH:mm:ss");

            StrategyParameters params3m = new StrategyParameters(2, "3m", 60, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params15m = new StrategyParameters(5, "15m", 60, 31, 69, 90, 39, 0.5f, 12.0f);
            StrategyParameters params4h = new StrategyParameters(5, "4h", 60, 31, 69, 90, 39, 0.5f, 12.0f);
            StrategyResultV2 strategy3m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines3m, 3, params3m, getApplicationContext());
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 3, params15m, getApplicationContext());
            StrategyResultV2 strategy4h = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 3, params4h, getApplicationContext());

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 4 ---> [Volume45]: " + volumeOfLast45mKlines + " [%OfVolumeRise45]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades45m]:" + nrOfTradesLast45mKlinesSum + " [TrendEMA<>4h-15m]: " + strategy4h.getPassedEMA() + " " + strategy15m.getPassedEMA() + " [WT<>4h-15m]: " + strategy4h.getPassedWT() + " " + strategy15m.getPassedWT() + " [RSI<>4h-15m]: " + strategy4h.getPassedRSI() + " " + strategy15m.getPassedRSI();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");

            long hours4 = 14400000;
            long hours8 = 28800000;

            if (strategy4h.getPassedEMA() == 1 && strategy4h.getPassedRSI() == 1 && strategy4h.getPassedADX() == 1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();

            } else if (strategy4h.getPassedRSI() == -1 && strategy4h.getPassedEMA() == -1 && strategy4h.getPassedADX() == 1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {

                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }

        }

    }

    private void strategyNr5_WT_PPO_ADX(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h) {

        //Wcześniej był sam volumen do sprawdzenia
        float volumeFromLast30min, volumeFromPrevious30min;
        int acceptableVolume = 3300000; //1000000

        if (coinKlines15m.size() > 2) {
            volumeFromLast30min = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 2);
            volumeFromPrevious30min = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 2, 4);
        } else {
            volumeFromLast30min = 0;
            volumeFromPrevious30min = 0;
        }

        float nrOfTradesFromLast30min, nrOfTradesInPrevious30min;
        int acceptableNrOfTrades = 44000;

        if (coinKlines15m.size() > 2) {
            nrOfTradesFromLast30min = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 2);
            nrOfTradesInPrevious30min = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 2, 4);
        } else {
            nrOfTradesFromLast30min = 0;
            nrOfTradesInPrevious30min = 0;
        }

        boolean isVolumeAndNrOfTradesAccepted = (volumeFromLast30min > volumeFromPrevious30min * 1.25) && (nrOfTradesFromLast30min > nrOfTradesInPrevious30min * 1.25) && volumeFromLast30min > acceptableVolume && nrOfTradesFromLast30min > acceptableNrOfTrades;


        String info2 = "LEVEL 0 [" + symbol + "] volume: " + volumeFromLast30min + " previous: " + volumeFromPrevious30min + " nrTrades: " + nrOfTradesFromLast30min + " previous: " + nrOfTradesInPrevious30min + " approved?: " + isVolumeAndNrOfTradesAccepted;
        Log.e(TAG, info2);


        if (isVolumeAndNrOfTradesAccepted && !symbol.contains("BUSDUSDT") && coinKlines15m.size() >= 58 && coinKlines3m.size() >= 38 && coinKlines4h.size() >= 15) {

            DecimalFormat df = new DecimalFormat("0.000000");
            DecimalFormat df2 = new DecimalFormat("0.00");

            int predict15, priceDirection15;
            int nrOfTradesLast15mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 2);

            ArrayList<Double> PriceChangePercents = new ArrayList<>();
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 2))); //6m
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 5))); //15m
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines3m.subList(0, 10))); //30m
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines15m.subList(0, 8))); //2h
            PriceChangePercents.add(ServiceFunctionsStrategyDefault.percentOfPriceChange(coinKlines15m.subList(0, 16))); //4h
            String PriceLogValue = "";

            for (Double number : PriceChangePercents) {
                PriceLogValue += df2.format(number) + "% ";
            }

            predict15 = ServiceFunctionsStrategyDefault.predict(coinKlines15m);
            priceDirection15 = ServiceFunctionsStrategyDefault.predictPriceDirection(coinKlines15m.subList(0, 12));
//            String info2 = "Size 15m: " + coinKlines15m.size() + " Size 3m: " + coinKlines3m.size();
//            ServiceFunctionsOther.writeToFile(info2, getApplicationContext(), "result");
//
//            @SuppressLint("SimpleDateFormat") DateFormat df5 = new SimpleDateFormat("HH:mm:ss - dd");
//            for (int i = 0; i < coinKlines15m.size(); i++)  {
//                info2 = "KlineSize i: " + i + " " + df5.format(new Date (coinKlines15m.get(i).gettCloseTime())) + " " + coinKlines15m.get(i).gettClosePrice();
//                ServiceFunctionsOther.writeToFile(info2, getApplicationContext(), "result");
//            }

            StrategyParameters params3m = new StrategyParameters(10, "3m", 53, 30, 70, 90, 36, 0.75f, 12.0f);
            StrategyParameters params15m = new StrategyParameters(10, "15m", 53, 30, 70, 90, 36, 0.75f, 12.0f);
            StrategyParameters params4h = new StrategyParameters(10, "4h", 53, 30, 70, 90, 36, 0.75f, 12.0f);
            StrategyResultV2 strategyNr15 = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 5, params15m, getApplicationContext()); // 53, 10, 0.75, 12.1, 36
            StrategyResultV2 strategyNr3 = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines3m, 5, params3m, getApplicationContext());

            int start = 0;
            int add = 2;
            double sumATR15 = 0;
            double avgATR15 = 0;
            ArrayList<Double> ATR_ChangeFromAVG15 = new ArrayList<>();
            List<Kline> klinesList15 = coinKlines15m.subList(0, 18);
            String ATRValues15 = "";
            for (int i = 0; i < (klinesList15.size() / add); i++) {
                ArrayList<Kline> klinesListInterval = new ArrayList<>(klinesList15.subList(start + add * i, add + add * i));
                double temp = ServiceFunctionsStrategyDefault.calculateATR(klinesListInterval, add);
                sumATR15 += temp;
                ATR_ChangeFromAVG15.add(temp);
            }
            avgATR15 = sumATR15 / ((double) (klinesList15.size() / add));
            for (Double number : ATR_ChangeFromAVG15) {
                number = ((number * 100) / avgATR15) - 100;
                ATRValues15 += df2.format(number) + "% ";
            }

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 5 Volume15m: " + volumeFromLast30min + " AvgATR15: " + df.format(avgATR15) + " ATR%15m: " + ATRValues15 + " Predicted(3m, 15m, 4h): " + predict15 + " PriceDirection: " + priceDirection15 + " LastPriceChange(6m, 15m, 30m, 2h, 4h): " + PriceLogValue + " PPO(3,15) " + strategyNr3.getPassedPPO() + " " + strategyNr15.getPassedPPO() + " WT(3,15) " + strategyNr3.getPassedWT() + " " + strategyNr15.getPassedWT() + " ADX(3,15) " + strategyNr3.getPassedADX() + " " + strategyNr15.getPassedADX();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");

            @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("HH:mm:ss");
            long hours4 = 14400000;

            if (strategyNr15.getPassedWT() == 1 && strategyNr15.getPassedADX() == 1 && strategyNr15.getPassedPPO() == 1) {//&& percentATR15.get(0) < 70 && percentATR15.get(1) < 70 && percentATR15.get(0) > -25 && percentATR15.get(1) > -25 && PriceChangePercents.get(4) > -5) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours4));
                data.moveToFirst();
                if (data.getCount() == 0) {

                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                            volumeFromLast30min, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                            volumeFromLast30min, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }

            } else if (strategyNr15.getPassedWT() == -1 && strategyNr15.getPassedADX() == 1 && strategyNr15.getPassedPPO() == -1) { //&& percentATR15.get(0) < 70 && percentATR15.get(1) < 70 && percentATR15.get(0) > -25 && percentATR15.get(1) > -25 && PriceChangePercents.get(4) < 5) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours4));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df3.format(System.currentTimeMillis()) + " " + df3.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                            volumeFromLast30min, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                            volumeFromLast30min, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
            }

        }

    }

}