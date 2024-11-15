package app.makorz.cryptofun.services;

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

import app.makorz.cryptofun.data.KlineRequest;
import app.makorz.cryptofun.data.ObservableModel;
import app.makorz.cryptofun.data.database.DBHandler;
import app.makorz.cryptofun.data.database.Kline;
import app.makorz.cryptofun.data.database.rawTable_Kline;
import app.makorz.cryptofun.retrofit.RetrofitClientFutures;

import java.sql.Timestamp;
import java.text.DateFormat;
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

        String strategy = "STRATEGY: ";
        Cursor data = databaseDB.retrieveParam(17);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 17");
            databaseDB.addParam(17, "Active Strategy Nr:", "", 1, 0);
            strategy += String.valueOf(1);
        } else {
            data.moveToFirst();
            strategy += String.valueOf(data.getInt(3));
        }
        data.close();

        String stopLimitStrategy = "SL STRATEGY: ";
        data = databaseDB.retrieveParam(18);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 18");
            databaseDB.addParam(18, "Active SL Strategy Nr:", "", 1, 0);
            stopLimitStrategy += String.valueOf(1);
        } else {
            data.moveToFirst();
            stopLimitStrategy += String.valueOf(data.getInt(3));
        }
        data.close();

        Intent intent = new Intent("DB_updated");
        intent.putExtra("updateDate", date);
        intent.putExtra("currentStrategy", strategy);
        intent.putExtra("currentStopLimitStrategy", stopLimitStrategy);
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

            // Need to prevent updating kline data at 50s before last closeTime of kline so update can be done after lst close time of kline or before close time-50s (observable is prepared and when it reaches API binance it takes next kline data
            long timeSinceLastUpdate = 50000;
            long time = (tempKline.gettCloseTime() - timeSinceLastUpdate - System.currentTimeMillis());
            long time2 = (tempKline.gettCloseTime() - System.currentTimeMillis());
            if (time > 0 || time2 < 0) {
                Log.e(TAG, "[DB IS NOT ACTUAL] LastKlineCLoseTime: " + tempKline.gettCloseTime() + " " + df2.format(tempKline.gettCloseTime()) + " --> difference: " + timeSinceLastUpdate + " --> currentTime: " + System.currentTimeMillis() + " " + df2.format(System.currentTimeMillis()) + " --> result: " + time + " (" + date + ") (" + date2 + ")");

                databaseDB.deleteKlinesOlderThan("3m", System.currentTimeMillis() - 200L * (3 * 60 * 1000)); // delete older than 20h in 3m interval
                databaseDB.deleteKlinesOlderThan("15m", System.currentTimeMillis() - 200L * (15 * 60 * 1000)); // delete older than 50h in 15m interval
                databaseDB.deleteKlinesOlderThan("4h", System.currentTimeMillis() - 200L * (4 * 60 * 60 * 1000)); // delete older than 33day in 4h interval

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
        final int MAX_NR_OF_KLINES = 200;
        ObservableModel model = new ObservableModel(symbol, 0, interval, 0, 0);

        try (Cursor latestCursor = databaseDB.retrieveLastCloseTime2(symbol, interval);
             Cursor allRowsCursor = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA,symbol, interval)) {

            // 1. Check if the table has any data for this interval
            if (allRowsCursor.getCount() == 0) {
                // If empty, need full reload
                Log.e(TAG, "X emptyData - full " + symbol + " on interval " + interval + ". Initiating full reload.");
                return new ObservableModel(symbol, MAX_NR_OF_KLINES, interval, 1, 0);
            }

            // 2. Check consistency by verifying no gaps between consecutive klines
            boolean isConsistent = checkKlineConsistency(allRowsCursor, interval);
            if (!isConsistent) {
                Log.e(TAG, "X gaps/doubles - full " + symbol + " on interval " + interval + ". Initiating full reload.");
                return new ObservableModel(symbol, MAX_NR_OF_KLINES, interval, 1, 0);
            }

            // 3. Calculate how many klines we need to update
            if (latestCursor.moveToFirst()) {
                @SuppressLint("Range") long lastCloseTime = latestCursor.getLong(latestCursor.getColumnIndex("close_time"));
                long timeDifference = timeCurrent - lastCloseTime;

                // Determine number of klines required since last update
                long intervalMillis = getIntervalMillis(interval);
                int klinesNeeded = (int) Math.ceil((double) timeDifference / intervalMillis);

                if (klinesNeeded > 0) {
                    // 4. Check if we need to fetch a full set or just missing klines
                    if (klinesNeeded >= MAX_NR_OF_KLINES) {
                        logUpdateDecision(symbol, "A - full ", lastCloseTime, timeCurrent, klinesNeeded, MAX_NR_OF_KLINES, intervalMillis);
                        return new ObservableModel(symbol, MAX_NR_OF_KLINES, interval, 1, 0);
                    } else {
                        logUpdateDecision(symbol, "B - part ", lastCloseTime, timeCurrent, klinesNeeded, MAX_NR_OF_KLINES, intervalMillis);
                        return new ObservableModel(symbol, klinesNeeded+3, interval, 3, 0);
                    }
                } else if (klinesNeeded == 0) {
                    logUpdateDecision(symbol, "C- single ", lastCloseTime, timeCurrent, klinesNeeded, MAX_NR_OF_KLINES, intervalMillis);
                    return new ObservableModel(symbol, 1, interval, 2, 0);
                } else {
                    // Data is consistent and up-to-date, no update needed
                    logUpdateDecision(symbol, "D - full ", lastCloseTime, timeCurrent, klinesNeeded, MAX_NR_OF_KLINES, intervalMillis);
                    return model;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while updating interval: " + e.getMessage());
        }

        return model;
    }

    // Helper method to calculate interval in milliseconds
    private long getIntervalMillis(String interval) {
        switch (interval) {
            case "3m": return 180000;  // 3 minutes in milliseconds
            case "15m": return 900000; // 15 minutes in milliseconds
            case "4h": return 14400000; // 4 hours in milliseconds
            default: throw new IllegalArgumentException("Unsupported interval: " + interval);
        }
    }

    // Helper method to check if klines data is consistent (no gaps)
    @SuppressLint("Range")
    private boolean checkKlineConsistency(Cursor cursor, String interval) {
        long intervalMillis = getIntervalMillis(interval);

        if (cursor.moveToFirst()) {
            long prevCloseTime = cursor.getLong(cursor.getColumnIndex("close_time"));
            while (cursor.moveToNext()) {
                long closeTime = cursor.getLong(cursor.getColumnIndex("close_time"));
                if (prevCloseTime != closeTime + intervalMillis) {
                    Log.e(TAG, "Inconsistent data detected at " + closeTime + " prevTime " + prevCloseTime);
                    return false;
                }
                prevCloseTime = cursor.getLong(cursor.getColumnIndex("close_time"));
            }
        }
        return true;
    }

    // Helper Method to Log Update Decisions
    private void logUpdateDecision(String symbol, String caseLabel, long closeTime, long currentTime,
                                   int nrOfKlines, int maxNrOfKlines, long intervalMillis) {
        if (symbol.equals("ETHUSDT")) {
            Log.e(TAG, caseLabel + " Update Decision - Symbol: " + symbol + ", Close Time: " + closeTime +
                    ", Current Time: " + currentTime + ", Klines Difference: " + (closeTime - currentTime) / intervalMillis +
                    ", NrOfKlines: " + nrOfKlines + ", MaxNrOfKlines: " + maxNrOfKlines);
        }
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
            if (list.get(i).getSymbol().equals("ETHUSDT")) {
                Log.e(TAG, "Request [" + list.get(i).getSymbol() + "] NrOfKlines: " + list.get(i).getNrOfKlinesToDownload() + " Interval: " + list.get(i).getInterval() + " WhatToDo: " + list.get(i).getWhatToDoWithDB() + " HowManyOldOnesToDelete: " + list.get(i).getHowManyOldOnesToDelete());
            }

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

                                                        if (symbol.equals("ETHUSDT") && j > aaa.length - 10) {
                                                            Log.e(TAG, "ResultDataParse [" + j + "] " + interval + " -> " + temp.toString());
                                                        }

                                                        klinesDataList.add(temp);
                                                    }

                                                    //Get rid of nulls
                                                    klinesDataList.removeIf(iteratorItem -> iteratorItem.getTokenSymbol() == null);

                                                    switch (whatToDoInDB) {
                                                        case 1:
                                                            if (symbol.equals("ETHUSDT")) {
                                                                Log.e(TAG, "ResultDataCase1");
                                                            }
                                                            databaseDB.deleteAllKlinesForSymbolIntervalandAddNew(interval, symbol, klinesDataList);
                                                            klinesDataList.clear();
                                                            break;
                                                        case 2:
                                                            if (symbol.equals("ETHUSDT")) {
                                                                Log.e(TAG, "ResultDataCase2");
                                                            }
                                                            databaseDB.updateLastKlineValues(interval, symbol, klinesDataList);
                                                            klinesDataList.clear();
                                                            break;
                                                        case 3:
                                                            if (symbol.equals("ETHUSDT")) {
                                                                Log.e(TAG, "ResultDataCase3");
                                                            }
                                                            databaseDB.updateAndInsertNewKlineData(interval, symbol, klinesDataList);
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

            int activeStrategy;
            Cursor data2 = databaseDB.retrieveParam(17);
            data2.moveToFirst();
            if (data2.getCount() == 0) {
                Log.e(TAG, "There is no param nr 17");
                databaseDB.addParam(17, "Active Strategy Nr:", "", 1, 0);
                activeStrategy = 1;
            } else if (data2.getCount() >= 2) {
                databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 17);
                databaseDB.addParam(17, "Active Strategy Nr:", "", 1, 0);
                activeStrategy = 1;
            } else {
                activeStrategy = data2.getInt(3);
            }
            data2.close();

            for (int i = 0; i < listOfSymbols.size(); i++) {
                countBestCryptoToBuy(listOfSymbols.get(i), activeStrategy);
            }
            Timestamp stamp = new Timestamp(System.currentTimeMillis());
            //long twentyFourHours = 86400000;
            //long olderThan = System.currentTimeMillis() - twentyFourHours;
            @SuppressLint("SimpleDateFormat")
            DateFormat df = new SimpleDateFormat("HH:mm - EEE, dd");
            String date;
            if (wasThereError) {
                Log.e(TAG, "Error from Observable/Or DB is actual.");
                data2 = databaseDB.retrieveParam(1);
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

    private void countBestCryptoToBuy(String symbol, int activeStrategy) {

        List<Kline> coinKlines3m, coinKlines15m, coinKlines4h;

        switch (activeStrategy) {
            case 8:
                coinKlines3m = getList(symbol,"3m");
                coinKlines15m = getList(symbol,"15m");
                coinKlines4h = getList(symbol,"4h");
                ServiceMainStrategies.multipleStrategiesFunction(symbol, coinKlines3m, coinKlines15m, coinKlines4h, getApplicationContext());
                break;
            default:
                //ONLY TEST
                coinKlines15m = getList(symbol,"15m");
                ServiceMainStrategies.strategyNr0_quickTestStrategy_15m(symbol, coinKlines15m, getApplicationContext());
                break;
        }
    }

    private List<Kline> getList(String symbol, String interval) {

        List<Kline> list = new ArrayList<>();

        Cursor data = databaseDB.retrieveDataToFindBestCrypto2(TABLE_NAME_KLINES_DATA, symbol, interval);
        data.moveToFirst();
        if (data.getCount() > 0) {
            do {
                Kline tempKline = new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                        data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10));
                list.add(tempKline);
            } while (data.moveToNext());
        }
        data.close();
        return list;
    }
}