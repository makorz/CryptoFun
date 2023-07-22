package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.R;
import com.example.cryptofun.data.ApprovedToken;
import com.example.cryptofun.data.KlineRequest;
import com.example.cryptofun.data.ObservableModel;
import com.example.cryptofun.data.StrategyResult;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.data.database.Kline;
import com.example.cryptofun.data.database.rawTable_Kline;
import com.example.cryptofun.retrofit.RetrofitClientFutures;

import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BarSeriesManager;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.criteria.VersusBuyAndHoldCriterion;
import org.ta4j.core.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.AbstractEMAIndicator;
import org.ta4j.core.indicators.DoubleEMAIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.TripleEMAIndicator;
import org.ta4j.core.indicators.ZLEMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.adx.MinusDIIndicator;
import org.ta4j.core.indicators.adx.PlusDIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.CombineIndicator;
import org.ta4j.core.indicators.helpers.DifferencePercentageIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.MedianPriceIndicator;
import org.ta4j.core.indicators.numeric.NumericIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;


import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.BinaryOperator;

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
                        Notification notification = createNotification();
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
                .setContentText("Updating crypto data.")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.crypto_fun_logo);

        return builder.build();
    }

    private void sendMessageToActivity(String date, boolean updateStart) {
        Intent intent = new Intent("DB_updated");
        intent.putExtra("updateDate", date);
        intent.putExtra("updateStarted", updateStart);
        Log.e(TAG, "BROADCAST - UpdateDB Started in Background - " + updateStart);
        LocalBroadcastManager.getInstance(UpdatingDatabaseService.this).sendBroadcast(intent);
        stopForeground(true);
        stopSelf();
    }

    private void sendInfoToActivity() {
        Intent intent = new Intent("DB_update_start");
        Log.e(TAG, "BROADCAST - Loading icon START");
        LocalBroadcastManager.getInstance(UpdatingDatabaseService.this).sendBroadcast(intent);
    }

    private void checkDBLastTimeOfUpdate() {

        Cursor data = databaseDB.retrieveLastCloseTime("3m");
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
            updateDBtoCurrentValues(System.currentTimeMillis());
            sendMessageToActivity(date, true);
            data2.close();

        } else {
            data.moveToFirst();
            Kline tempKline = new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                    data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10));

            Timestamp stamp = new Timestamp(System.currentTimeMillis());
            Timestamp stamp2 = new Timestamp(tempKline.gettCloseTime());
            @SuppressLint("SimpleDateFormat")
            DateFormat df = new SimpleDateFormat("HH:mm - EEE, dd");
            String date = "Update time:  " + df.format(new Date(stamp.getTime()));
            String date2 = df.format(new Date(stamp2.getTime()));

            //if more than 1 minute passed since last closeTimeOfKline --> UpdateDB
            long timeSinceLastUpdate = 55000;
            Log.e(TAG, "KlineCLoseTime: " + tempKline.gettCloseTime() + " - oneMin: " + timeSinceLastUpdate + " - currentTime: " + System.currentTimeMillis());
            long time = (tempKline.gettCloseTime() - timeSinceLastUpdate - System.currentTimeMillis());

            if (time < 0) {
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
                Log.e(TAG, "DB is not actual " + tempKline.gettCloseTime() + " " + time + " " + System.currentTimeMillis() + " " + date + " " + date2);
                sendMessageToActivity(date, true);
            } else {
                Log.e(TAG, "DB is actual " + tempKline.gettCloseTime() + " " + time + " " + System.currentTimeMillis() + " " + date + " " + date2);
                Cursor data3 = databaseDB.howManyRows(TABLE_NAME_KLINES_DATA);
                data3.moveToFirst();
                Log.e(TAG, String.valueOf(data3.getCount() + " AAA " + data3.getLong(0)));
                if (data3.getLong(0) > 10) {
                    Log.e(TAG, "Table " + TABLE_NAME_KLINES_DATA + " is not empty. [onStartCommand]");
                    startCountingAndReturnResult(true);

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
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_SYMBOL_AVG + " is empty. updateDBtoCurrentValues");
        } else {
            while (data.moveToNext()) {
                listOfSymbols.add(data.getString(0));
            }
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

        Cursor data, data2;
        int nrOfKlines, nrOfKlinesFromLastDBUpdate;
        long closeTime;
        ObservableModel model = new ObservableModel(symbol, 0, "3m", 0, 0);
        long minutes3 = 180000;
        int maxNrOfKlines3 = 40;
        int maxNrOfKlines15 = 60;
        int maxNrOfKlines4 = 20;
        long minutes15 = 900000;
        long hours4 = 14400000;
        String intervalInSwitch;

        switch (interval) {
            case "3m":
                intervalInSwitch = "3m";
                data = databaseDB.retrieveLastCloseTime(intervalInSwitch);
                data2 = databaseDB.nrOfKlinesForSymbolInInterval(symbol, intervalInSwitch);
                if (data.getCount() == 0 || data2.getCount() == 0) {
                    Log.e("UpdatingExistingDB", "Table " + TABLE_NAME_KLINES_DATA + " is empty. [updateIntervalOfDB 3m]");
                    model = new ObservableModel(symbol, maxNrOfKlines3, intervalInSwitch, 0, 0); //20
                } else {
                    data.moveToFirst();
                    data2.moveToFirst();
                    ///Log.e("UpdatingExistingDB", symbol + interval);
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    nrOfKlinesFromLastDBUpdate = (int) (-(closeTime - timeCurrent) / minutes3) + 2;

                    if (nrOfKlines >= maxNrOfKlines3 || nrOfKlines < 1 || nrOfKlinesFromLastDBUpdate > maxNrOfKlines3) {
                        model = new ObservableModel(symbol, maxNrOfKlines3, intervalInSwitch, 1, 0); //20
                    } else {
                        if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines3) {
                            int howManyOldOnes = (nrOfKlines - 1) + nrOfKlinesFromLastDBUpdate - maxNrOfKlines3;
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 2, howManyOldOnes);
                        } else if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines3) {
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines3) {
                            if (closeTime - timeCurrent > 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate - 1, intervalInSwitch, 3, 0);
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                            }
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines3) {
                            if (closeTime - timeCurrent > 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate - 1, intervalInSwitch, 3, 0);
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                            }
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
                if (data.getCount() == 0 || data2.getCount() == 0) {
                    Log.e("UpdatingExistingDB", "Table " + TABLE_NAME_KLINES_DATA + " is empty. [updateIntervalOfDB 15m]");
                    model = new ObservableModel(symbol, maxNrOfKlines15, intervalInSwitch, 0, 0); //20
                } else {
                    data.moveToFirst();
                    data2.moveToFirst();
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    nrOfKlinesFromLastDBUpdate = (int) (-(closeTime - timeCurrent) / minutes15) + 2;

                   // Log.e("UpdatingExistingDB", nrOfKlines + " " + closeTime + " " + nrOfKlinesFromLastDBUpdate + " " + data.getCount() + " " + data2.getCount() + " " + maxNrOfKlines15);

                    /*TODO:
                        Sprawdź czy to działa   bo wykomentowana częsc wedle AndSt jest zawsze true. Zmieniłem 14.07 obserwujmy czy się nie wysypie

                    */
                    //28 1689016499999 2 5040 1 40
                    //90 1689946199999 2 16470 1 90

                    if (nrOfKlines >= maxNrOfKlines15 || nrOfKlines < 1 || nrOfKlinesFromLastDBUpdate > maxNrOfKlines15) {
                        model = new ObservableModel(symbol, maxNrOfKlines15, intervalInSwitch, 1, 0); //20
                    } else {
                        if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines15) {
                            int howManyOldOnes = (nrOfKlines - 1) + nrOfKlinesFromLastDBUpdate - maxNrOfKlines15;
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 2, howManyOldOnes);
                        } else if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines15) {
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                        } else if (nrOfKlinesFromLastDBUpdate == 2) {  //&& (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines15
                            if (closeTime - timeCurrent > 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate - 1, intervalInSwitch, 3, 0);
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                            }
                        }
//                        else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines15) {
//                            if (closeTime - timeCurrent > 0) {
//                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate - 1, intervalInSwitch, 3, 0);
//                            } else {
//                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
//                            }
//                        }
                    }

                }
                data.close();
                data2.close();
                break;
            case "4h":
                intervalInSwitch = "4h";
                data = databaseDB.retrieveLastCloseTime(intervalInSwitch);
                data2 = databaseDB.nrOfKlinesForSymbolInInterval(symbol, intervalInSwitch);
                if (data.getCount() == 0 || data2.getCount() == 0) {
                    Log.e("UpdatingExistingDB", "Table " + TABLE_NAME_KLINES_DATA + " is empty. [updateIntervalOfDB 4h]");
                    model = new ObservableModel(symbol, maxNrOfKlines4, intervalInSwitch, 0, 0); //maxNrOfKlines4
                } else {
                    data.moveToFirst();
                    data2.moveToFirst();
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    nrOfKlinesFromLastDBUpdate = (int) (-(closeTime - timeCurrent) / hours4) + 2;
                    if (nrOfKlines >= maxNrOfKlines4 || nrOfKlines < 1 || nrOfKlinesFromLastDBUpdate > maxNrOfKlines4) {
                        model = new ObservableModel(symbol, maxNrOfKlines4, intervalInSwitch, 1, 0); //maxNrOfKlines4
                    } else {
                        if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines4) {
                            int howManyOldOnes = (nrOfKlines - 1) + nrOfKlinesFromLastDBUpdate - maxNrOfKlines4;
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 2, howManyOldOnes);
                        } else if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines4) {
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines4) {
                            if (closeTime - timeCurrent > 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate - 1, intervalInSwitch, 3, 0);
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                            }
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines4) {
                            if (closeTime - timeCurrent > 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate - 1, intervalInSwitch, 3, 0);
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                            }
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

                                                    switch (whatToDoInDB) {
                                                        case 1:
                                                            databaseDB.deleteAllKlinesForSymbolInterval(interval, symbol);
                                                            break;
                                                        case 2:
                                                            databaseDB.deleteMostNewKlineForSymbolInterval(interval, symbol, 1);
                                                            databaseDB.deleteOldestKlinesForSymbolInterval(interval, symbol, howManyOldOnes);
                                                            break;
                                                        case 3:
                                                            databaseDB.deleteMostNewKlineForSymbolInterval(interval, symbol, 1);
                                                            break;
                                                        default:
                                                            break;
                                                    }
                                                }

                                                Iterator<rawTable_Kline> it = klinesDataList.iterator();
                                                while (it.hasNext()) {
                                                    rawTable_Kline iteratorItem = it.next();
                                                    if (iteratorItem.getTokenSymbol() == null) {
                                                        it.remove();
                                                    }
                                                }
                                                //Add everything to DB and refresh View
                                                if (databaseDB.addNewKlineData(klinesDataList) > 50) {
                                                    klinesDataList.clear();
                                                    startCountingAndReturnResult(false);
                                                }
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
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_SYMBOL_AVG + " is empty. [startCountingAndReturnResult]");
        } else {
            while (data.moveToNext()) {
                listOfSymbols.add(data.getString(0));
            }
            databaseDB.clearTable(TABLE_NAME_APPROVED);
            for (int i = 0; i < listOfSymbols.size(); i++) {
                countBestCryptoToBuy(listOfSymbols.get(i));
            }
            Timestamp stamp = new Timestamp(System.currentTimeMillis());
            long eightHours = 28800000;
            long olderThan = System.currentTimeMillis() - eightHours;
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
            databaseDB.clearHistoricApproved(olderThan);
            sendMessageToActivity(date, false);
        }

//        if (mWakeLock != null) {
//            mWakeLock.release();
//            mWakeLock = null;
//        }

        data.close();
    }

    // Function checks what crypto is going to run in green
    private void countBestCryptoToBuy(String symbol) {

        List<Kline> coinKlines3m = new ArrayList<>();
        List<Kline> coinKlines15m = new ArrayList<>();
        List<Kline> coinKlines4h = new ArrayList<>();

        List<Integer> statusOf3mKlines = new ArrayList<>();
        List<Integer> statusOf15mKlines = new ArrayList<>();
        List<Integer> statusOf4hKlines = new ArrayList<>();

        Cursor data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol);

        if (data.getCount() >= 0) {

            while (data.moveToNext()) {

                Kline tempKline = new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                        data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10));

                String interval = tempKline.gettKlineInterval();
                switch (interval) {
                    case "3m":
                        coinKlines3m.add(tempKline);
                        statusOf3mKlines.add(tempKline.getStatusOfKline());
                        break;
                    case "15m":
                        coinKlines15m.add(tempKline);
                        statusOf15mKlines.add(tempKline.getStatusOfKline());
                        break;
                    case "4h":
                        coinKlines4h.add(tempKline);
                        statusOf4hKlines.add(tempKline.getStatusOfKline());
                        break;
                    default:
                        break;
                }
            }
        }
        data.close();

        /* TODO:
               Przywrócić wartości jak wejdzie test na produkcji. Również wartości na kwoty zleceń i ilość zleceń w Approving service oraz w RetrfitClientSecrettEstnet pobieranie api z bazy dnaych
         */

        float volumeOfLast15mKlines;
        int acceptableVolume = 250000; //1000000

        if (coinKlines15m.size() > 2) {
            volumeOfLast15mKlines = countMoneyVolumeAtInterval(coinKlines15m, 0, 2);
        } else {
            volumeOfLast15mKlines = acceptableVolume;
        }

        float percentOfRiseOfNumberOfVolumeInLast15min;
        int acceptablePercentOfVolumeRise = 30;

        if (coinKlines3m.size() >= 2) {
            percentOfRiseOfNumberOfVolumeInLast15min = countBeforeAndAfter(coinKlines3m, 10);
        } else {
            percentOfRiseOfNumberOfVolumeInLast15min = 0;
        }

//        if (volumeOfLast15mKlines >= acceptableVolume && percentOfRiseOfNumberOfVolumeInLast15min > acceptablePercentOfVolumeRise && !symbol.contains("BUSDUSDT") && coinKlines15m.size() >= 80 && coinKlines3m.size() >= 50 && coinKlines4h.size() >= 15) {
        if (volumeOfLast15mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT") && coinKlines15m.size() >= 58 && coinKlines3m.size() >= 38 && coinKlines4h.size() >= 15) {

            List<Integer> statusListOf3mToCheck;
            List<Integer> statusListOf15mToCheck;
            statusListOf15mToCheck = statusOf15mKlines.subList(0, 3);
            statusListOf3mToCheck = statusOf3mKlines.subList(0, 10);

            DecimalFormat df = new DecimalFormat("0.000000");
            DecimalFormat df2 = new DecimalFormat("0.00");

            int predict3, predict15, predict4, priceDirection3, priceDirection15, priceDirection4, waveTrendPredict3, waveTrendPredict15, waveTrendPredict4;
            int nrOfTradesLast15mKlinesSum = countNrOfTradesAtInterval(coinKlines15m, 0, 2);

            ArrayList<Double> PriceChangePercents = new ArrayList<>();
            PriceChangePercents.add(percentOfPriceChange(coinKlines3m.subList(0, 2))); //6m
            PriceChangePercents.add(percentOfPriceChange(coinKlines3m.subList(0, 5))); //15m
            PriceChangePercents.add(percentOfPriceChange(coinKlines3m.subList(0, 10))); //30m
            PriceChangePercents.add(percentOfPriceChange(coinKlines15m.subList(0, 8))); //2h
            PriceChangePercents.add(percentOfPriceChange(coinKlines15m.subList(0, 16))); //4h
            String PriceLogValue = "";

            for (Double number : PriceChangePercents) {
                PriceLogValue += df2.format(number) + "% ";
            }

            //Standard: subList have 16-20 elements
            ArrayList<Kline> klinesList3 = new ArrayList<>(coinKlines3m.subList(0, 40));
            predict3 = predict(klinesList3, 30);
            priceDirection3 = predictPriceDirection(klinesList3.subList(0, 12));

            StrategyResult strategy3m = predictPriceMovement(coinKlines3m);
            String strategyResult3m = "Strategy3m: [" + strategy3m.getBestStrategy() + "] Positions(M,A): " + strategy3m.getNrOfPositions().get(0).toString() + " " + strategy3m.getNrOfPositions().get(1).toString() + "vsBuyAndHold(M,A): "
                    + strategy3m.getVsBuyAndHoldProfit().get(0).toString() + " " + strategy3m.getVsBuyAndHoldProfit().get(1).toString() + "total(M,A): " + strategy3m.getTotalProfit().get(0).toString() + " " + strategy3m.getTotalProfit().get(1).toString();

            if (strategy3m.getNrOfPositions().get(0) == 0 && strategy3m.getNrOfPositions().get(1) == 0) {
                strategyResult3m = "Strategy3m: NO POSITIONS";
            }
            int finalWaveTrend3m = strategy3m.getWaveTrendIndicator();
            
            ArrayList<Kline> klinesList15 = new ArrayList<>(coinKlines15m.subList(0, 32));
            predict15 = predict(klinesList15, 30);
            priceDirection15 = predictPriceDirection(klinesList15.subList(0, 12));

            StrategyResult strategy15m = predictPriceMovement(coinKlines15m);
            String strategyResult15m= "Strategy15m: [" + strategy15m.getBestStrategy() + "] Positions(M,A): " + strategy15m.getNrOfPositions().get(0).toString() + " " + strategy15m.getNrOfPositions().get(1).toString() + "vsBuyAndHold(M,A): "
                    + strategy15m.getVsBuyAndHoldProfit().get(0).toString() + " " + strategy15m.getVsBuyAndHoldProfit().get(1).toString() + "total(M,A): " + strategy15m.getTotalProfit().get(0).toString() + " " + strategy15m.getTotalProfit().get(1).toString();

            if (strategy15m.getNrOfPositions().get(0) == 0 && strategy15m.getNrOfPositions().get(1) == 0) {
                strategyResult15m = "Strategy15m: NO POSITIONS";
            }
            int finalWaveTrend15m = strategy15m.getWaveTrendIndicator();

            ArrayList<Kline> klinesList4 = new ArrayList<>(coinKlines4h.subList(0, 20));
            predict4 = predict(klinesList4, 10);
            priceDirection4 = predictPriceDirection(klinesList4.subList(0, 15));

            int start = 0;
            int add = 2;
            double sumATR = 0;
            double avgATR = 0;
            ArrayList<Double> ATR_ChangeFromAVG = new ArrayList<>();

            String ATRValues = "";
            for (int i = 0; i < (klinesList15.size() / add); i++) {
                ArrayList<Kline> klinesListInterval = new ArrayList<>(klinesList15.subList(start + add * i, add + add * i));
                double temp = calculateATR(klinesListInterval, add);
                sumATR += temp;
                ATR_ChangeFromAVG.add(temp);
            }
            avgATR = sumATR / ((double) (klinesList15.size() / add));

            ArrayList<Double> percentATR = new ArrayList<>();
            for (Double number : ATR_ChangeFromAVG) {
                number = ((number * 100) / avgATR) - 100;
                percentATR.add(number);
                ATRValues += df2.format(number) + "% ";
            }

            String info = "LEVEL1: " + symbol + " - Volume15m: " + volumeOfLast15mKlines + " %OfVolumeRise15m: " + percentOfRiseOfNumberOfVolumeInLast15min + " StatusOf(3m,15m): " + statusListOf3mToCheck + statusListOf15mToCheck + " Predicted to be (3m, 15m, 4h): " + predict3 + " " + predict15 + " " + predict4 + " Price direction: " + priceDirection3 + " " + priceDirection15 + " " + priceDirection4 + " LastPriceChange (6m, 15m, 30m, 2h, 4h): " + PriceLogValue + " WaveTrendStrategy(3m,15m): " + finalWaveTrend3m + " " + finalWaveTrend15m + " AvgATR: " + df.format(avgATR) + " ATR%15m: " + ATRValues + " " + strategyResult3m + " " + strategyResult15m + " " + percentATR.get(0) + " " + percentATR.get(1);
            Log.e(TAG, info);
            ServiceFunctions.writeToFile(info, getApplicationContext(), "result");

            @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("HH:mm:ss");

            if (finalWaveTrend3m == 1 && finalWaveTrend15m == 1 && percentATR.get(0) < 70 && percentATR.get(1) < 70) {
                info = "LEVEL2 (LONG): " + symbol + " approved at: " + df3.format(System.currentTimeMillis()) + " " +  df3.format(coinKlines3m.get(coinKlines3m.size()-1).gettCloseTime());
                Log.e(TAG, info);
                ServiceFunctions.writeToFile(info, getApplicationContext(), "result");
                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size()-1).tClosePrice), TABLE_NAME_APPROVED);
                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size()-1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);

            } else if (finalWaveTrend3m == -1 && finalWaveTrend15m == -1 && percentATR.get(0) < 70 && percentATR.get(1) < 70) {
                info = "LEVEL2 (SHORT): " + symbol + " approved at:  " + df3.format(coinKlines3m.get(coinKlines3m.size()-1)) + " " +  df3.format(coinKlines3m.get(coinKlines3m.size()-1).gettCloseTime());
                Log.e(TAG, info);
                ServiceFunctions.writeToFile(info, getApplicationContext(), "result");
                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size()-1).tClosePrice), TABLE_NAME_APPROVED);
                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size()-1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
            }

//            if (isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 0) && last30mPriceChange > -2 && last4hPriceChange > -4) {  //priceDirection3 == 1 && priceDirection15 == 1 && waveTrendPredict3 == -1 /*&& priceDirection4 == -1 && last6mPriceChange < -0.5*/
//
//                info = "LEVEL3 (SHORT FINALLY): " + symbol + " approved path nr 2";
//                Log.e(TAG, info);
//                ServiceFunctions.writeToFile(info, getApplicationContext(), "result");
//
//                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//            } else if (isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 1) && last30mPriceChange < 2 && last4hPriceChange < 4) {
//
//                info = "LEVEL3 (LONG FINALLY): " + symbol + " approved path nr 2";
//                Log.e(TAG, info);
//                ServiceFunctions.writeToFile(info, getApplicationContext(), "result");
//
//                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                        volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//            }


            // API 27 (01.07) --> ((waveTrendPredict15 + waveTrendPredict4 ) == -2 || (waveTrendPredict15 + waveTrendPredict4 ) == 2) && (priceDirection4 + predict15 == -2)
            // API 30 (01.07) -->  isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 0)  && priceDirection3 == -1 && priceDirection4 == 1 &&  ((waveTrendPredict15 + waveTrendPredict4 + waveTrendPredict3) == 0)

//Standard
//            if (isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 1)) {
//
//                if (PriceChangePercents.get(1) < -4) { // -3
//
//                    if (priceDirection3 == 1 && priceDirection4 == 1) {
//                        info = "LEVEL3 (SHORT): " + symbol + " approved path nr 1";
//                        Log.e(TAG, info);
//                        ServiceFunctions.writeToFile(info, getApplicationContext(), "result");
//
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//                    }
//                } else if (PriceChangePercents.get(1) > 0.2 && PriceChangePercents.get(1) < 2) {
//
//                    if (priceDirection3 == 1 && priceDirection15 == 1 && predict15 == 1 && priceDirection4 == 1) {  //Standardowe: priceDirection3 == 1 && priceDirection15 == 1 && predict15 == 1 && priceDirection4 == 1
//                        info = "LEVEL3 (LONG): " + symbol + " approved path nr 2";
//                        Log.e(TAG, info);
//                        ServiceFunctions.writeToFile(info, getApplicationContext(), "result");
//
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//                    }
//                }
//            } else if (isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 0) ) {
//
//                if (PriceChangePercents.get(1) > 4) {
//
//                    if (priceDirection3 == -1 && priceDirection4 == -1) {
//                        info = "LEVEL3 (LONG): " + symbol + " approved path nr 1";
//                        Log.e(TAG, info);
//                        ServiceFunctions.writeToFile(info, getApplicationContext(), "result");
//
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//                    }
//
//                } else if (PriceChangePercents.get(1) < -0.2 && PriceChangePercents.get(1) > -2) {
//
//                    if (priceDirection3 == -1 && priceDirection15 == -1 && predict15 == -1 && priceDirection4 == -1) {
//                        info = "LEVEL3 (SHORT): " + symbol + " approved path nr 2";
//                        Log.e(TAG, info);
//                        ServiceFunctions.writeToFile(info, getApplicationContext(), "result");
//
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
//                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
//                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
//
//                    }
//                }
//            }

/*
            int nrOfTradesLast15mKlinesSum = countNrOfTradesAtInterval(coinKlines15m, 0, 2);
            int predict15, predict3, predict4, priceDirection3, priceDirection15, priceDirection4, waveTrendPredict3, waveTrendPredict15, waveTrendPredict4;
            if (coinKlines15m.size() > 16 && coinKlines3m.size() > 16 && coinKlines4h.size() > 16) {
                ArrayList<Kline> klinesList15 = new ArrayList<>(coinKlines15m.subList(0, 16));
                predict15 = predict(klinesList15);
                ArrayList<Kline> klinesList3 = new ArrayList<>(coinKlines3m.subList(0, 16));
                predict3 = predict(klinesList3);
                ArrayList<Kline> klinesList4 = new ArrayList<>(coinKlines4h.subList(0, 16));
                predict4 = predict(klinesList4);

            } else {
                predict15 = 0;
                predict3 = 0;
                predict4 = 0;
            }

            priceDirection3 = predictPriceDirection(coinKlines3m);
            priceDirection15 = predictPriceDirection(coinKlines15m);
            priceDirection4 = predictPriceDirection(coinKlines4h);

            double last15mPriceChange = percentOfPriceChange(coinKlines3m.subList(0, 6));

            waveTrendPredict3 = predictPriceChange2(coinKlines3m);
            waveTrendPredict15 = predictPriceChange2(coinKlines15m);
            waveTrendPredict4 = predictPriceChange2(coinKlines4h);



            Log.e(TAG, symbol + " is predicted to be (3m, 15m, 4h): " + predict3 + " " + predict15 + " " + predict4 + ". Arrays size: " + coinKlines15m.size() + " " + coinKlines3m.size() + " " + coinKlines4h.size() + " Price direction: " + priceDirection3 + " " + priceDirection15 + " " + priceDirection4 + ". Percent Of Rise: " + percentOfRiseOfNumberOfVolumeInLast15min + " and volume: " + volumeOfLast15mKlines + ". Wave Trend: " + waveTrendPredict3 + " " + waveTrendPredict15 + " " + waveTrendPredict4);

            if ((last15mPriceChange > 3 || last15mPriceChange < -3)) {

                if (priceDirection3 == 1 && priceDirection15 == -1 && priceDirection4 == 1) { //isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 1) && (predict3 + predict15 + predict4) >= 2 && priceDirection == 1
                    Log.e(TAG, "GOOD LONG: " + symbol + " " + percentOfRiseOfNumberOfVolumeInLast15min
                            + " " + statusOf3mKlines + " " + statusOf15mKlines + " close price: " + coinKlines3m.get(0).tClosePrice);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                            volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                            volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);

                }

                if (priceDirection3 == -1 && priceDirection15 == 1 && priceDirection4 == -1) { //isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0)
                    Log.e(TAG, "GOOD SHORT: " + symbol + " " + percentOfRiseOfNumberOfVolumeInLast15min
                            + " " + statusOf3mKlines + " close price: " + coinKlines3m.get(0).tClosePrice);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                            volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                            volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }

            } else if ((last15mPriceChange > 0.5 || last15mPriceChange < -0.5)) {

                if (isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 1) && priceDirection3 == 1 && priceDirection15 == 1 && predict3 == 1) { //isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 1) && (predict3 + predict15 + predict4) >= 2 && priceDirection == 1
                    Log.e(TAG, "GOOD LONG: " + symbol + " " + percentOfRiseOfNumberOfVolumeInLast15min
                            + " " + statusOf3mKlines + " " + statusOf15mKlines + " close price: " + coinKlines3m.get(0).tClosePrice);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                            volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                            volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);

                }

                    /*
                            Version1: (priceDirection3 == 0 && priceDirection15 == 0 && waveTrendPredict15 == 0) --> 4,5/10
                            Version2: (isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0) && priceDirection3 == 0 && priceDirection15 == 0) --> 6,5/10
                            Version3: isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0)  && sumDirection >= -2  --> 3/10
                            Version4: isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 1) && (priceDirection3 +  predict3) >= 1) --> 3/10
                            Version5 - cureenty API 30 : isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0) && priceDirection3 == -1 && priceDirection15 == -1 && predict3 == -1
                            Version6 - curently API 27 :  priceDirection3 == -1 && priceDirection15 == 1 && priceDirection4 == -1
                            !!!! Combined last two: 5 - used when (last15mPriceChange > 0.5 || last15mPriceChange < -0.5) --- 6 - used when (last15mPriceChange > 3 || last15mPriceChange < -3) --- 6 first before 5 !!!!
                                                 *This combined quite well works if market is going down or up, not when it's stabilised

                     New style versions:
                            Versions 7 - same as combine version but instead predict3 we got predict15


                if (isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0) && priceDirection3 == -1 && priceDirection15 == -1 && predict3 == -1) { //isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0)
                    Log.e(TAG, "GOOD SHORT: " + symbol + " " + percentOfRiseOfNumberOfVolumeInLast15min
                            + " " + statusOf3mKlines + " close price: " + coinKlines3m.get(0).tClosePrice);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                            volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                            volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }

            }
        */

        }

    }

    public static int calculateWaveTrend(BarSeries series, int n1, int n2, int obLevel1, int obLevel2, int osLevel1, int osLevel2) {

        // Compute indicators
        NumericIndicator ap = NumericIndicator.of(new MedianPriceIndicator(series));
        NumericIndicator esa = NumericIndicator.of(new EMAIndicator(ap, n1));
        NumericIndicator diff = ap.minus(esa);
        NumericIndicator x = diff.abs();
        NumericIndicator d = NumericIndicator.of(new EMAIndicator(x, n1));
        NumericIndicator z = d.multipliedBy(0.015);
        NumericIndicator ci = diff.dividedBy(z);
        double multiplier2 = (2.0 / (n2 + 1));

        // Compute the channel index -- overwrite of calculate was need because value at index 0 waa NaN
        NumericIndicator tci = NumericIndicator.of(new AbstractEMAIndicator(ci, n2, multiplier2) {
            @Override
            protected Num calculate(int index) {
                //Log.e(TAG, "ABSTRACT: " + ci.getValue(index) + " " + ci.numOf(index)+ " " + ci.getValue(index).isNaN());
                if (index == 0) {
                    return DecimalNum.valueOf(0);
                }
                Num prevValue = getValue(index - 1);
                return ci.getValue(index).minus(prevValue).multipliedBy(DecimalNum.valueOf(multiplier2)).plus(prevValue);
            }
        });
       // NumericIndicator tci = NumericIndicator.of(new EMAIndicator(diff, n2));
        ATRIndicator atr = new ATRIndicator(series,n1);

        Num numObLevel2 = DecimalNum.valueOf(obLevel2);
        Num numObLevel1 = DecimalNum.valueOf(obLevel1);
        Num numOsLevel2 = DecimalNum.valueOf(osLevel2);
        Num numOsLevel1 = DecimalNum.valueOf(osLevel1);

        // Compute WaveTrend indicators
        Num wt1 = tci.getValue(series.getEndIndex());
        SMAIndicator sma = new SMAIndicator(tci, 4);
        Num wt2 = sma.getValue(series.getEndIndex());
        //Below is the same as SMAIndicator
//        Num wt22 = tci.getValue(series.getEndIndex()).multipliedBy(DecimalNum.valueOf("0.25"))
//                .plus(tci.getValue(series.getEndIndex() - 1).multipliedBy(DecimalNum.valueOf("0.25")))
//                .plus(tci.getValue(series.getEndIndex() - 2).multipliedBy(DecimalNum.valueOf("0.25")))
//                .plus(tci.getValue(series.getEndIndex() - 3).multipliedBy(DecimalNum.valueOf("0.25")));

//        for(int i = series.getEndIndex(); i <= series.getEndIndex(); i++) {
//            Log.e(TAG, "index: " + i + " ap: " + ap.getValue(i) + " esa: " + esa.getValue(i) + " diff: " + diff.getValue(i) + " x: " + x.getValue(i) + " d: " + d.getValue(i) + " z: " + z.getValue(i)
//                    + " ci: " + ci.getValue(i) + " tci: " + tci.getValue(i) + " wt1: " + wt1.doubleValue() + " wt2: " + wt2.doubleValue() + " atr: " + atr.getValue(i));
//        }

        // Check alert conditions and return the corresponding value
        if ((wt1.isGreaterThan(wt2) && wt1.isGreaterThan(numObLevel2))) {
            return -1;
        } else if ((wt1.isLessThan(wt2) && wt1.isLessThan(numOsLevel2))) {
            return 1;
        } else {
            return 0;
        }
    }


    public static StrategyResult predictPriceMovement(List<Kline> klines) {

        // Create a new empty time series
        Collections.reverse(klines);
        BarSeries series = new BaseBarSeriesBuilder().withName("My_Crypto_Series").build();

        // Load the klines into the time series
        for (int i = 0; i < klines.size(); i++) {

            if (i > 0 && klines.get(i).gettCloseTime() <= klines.get(i - 1).gettCloseTime()) {
                break;
            }
            long endTimeMillis = klines.get(i).gettCloseTime();

            ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault());
            double openPrice = klines.get(i).gettOpenPrice();
            double highPrice = klines.get(i).gettHighPrice();
            double lowPrice = klines.get(i).gettLowPrice();
            double closePrice = klines.get(i).gettClosePrice();
            series.addBar(endTime, openPrice, highPrice, lowPrice, closePrice);
        }
        //Log.e(TAG, series.getBarData().toString());

        BarSeriesManager seriesManager = new BarSeriesManager(series);
        // Moving momentum strategy.
        Strategy strategy1 = buildStrategy(series);
        // ADX Indicator strategy.
        Strategy strategy2 = buildStrategy2(series);

        // Running the strategies
        TradingRecord tradingRecord1 = seriesManager.run(strategy1);
        TradingRecord tradingRecord2 = seriesManager.run(strategy2);

        // AnalysisOfStrategies
        AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new GrossReturnCriterion());
        AnalysisCriterion total = new GrossReturnCriterion();
        Strategy bestStrategy = vsBuyAndHold.chooseBest(seriesManager, Arrays.asList(strategy1, strategy2));

        ArrayList<Integer> nrOfPositions = new ArrayList<>();
        ArrayList<Double> vsBuyAndHoldProfit = new ArrayList<>();;
        ArrayList<Double> totalProfit = new ArrayList<>();

        nrOfPositions.add(tradingRecord1.getPositionCount());
        nrOfPositions.add(tradingRecord2.getPositionCount());
        vsBuyAndHoldProfit.add(vsBuyAndHold.calculate(series, tradingRecord1).doubleValue());
        vsBuyAndHoldProfit.add(vsBuyAndHold.calculate(series, tradingRecord2).doubleValue());
        totalProfit.add(total.calculate(series, tradingRecord1).doubleValue());
        totalProfit.add(total.calculate(series, tradingRecord2).doubleValue());
        int finalWaveTrendScore = calculateWaveTrend(series,10,21,45,65,-45,-65); //53 -level2

        return new StrategyResult(nrOfPositions,vsBuyAndHoldProfit,totalProfit,finalWaveTrendScore,bestStrategy.getName());
    }

    public static Strategy buildStrategy(BarSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // The bias is bullish when the shorter-moving average moves above the longer
        // moving average.
        // The bias is bearish when the shorter-moving average moves below the longer
        // moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 5); //9
        EMAIndicator longEma = new EMAIndicator(closePrice, 26); //26

        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);

        MACDIndicator macd = new MACDIndicator(closePrice, 5, 26); // 9, 12
        EMAIndicator emaMacd = new EMAIndicator(macd, 18);

        // Entry rule
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillK, 20)) // Signal 1
                .and(new OverIndicatorRule(macd, emaMacd)); // Signal 2

        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillK, 80)) // Signal 1
                .and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2

        return new BaseStrategy("MOMENTUM", entryRule, exitRule);

    }

    public static Strategy buildStrategy2(BarSeries series) {

        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        final SMAIndicator smaIndicator = new SMAIndicator(closePriceIndicator, 50);

        final int adxBarCount = 14;
        final ADXIndicator adxIndicator = new ADXIndicator(series, adxBarCount);
        final OverIndicatorRule adxOver20Rule = new OverIndicatorRule(adxIndicator, 20);

        final PlusDIIndicator plusDIIndicator = new PlusDIIndicator(series, adxBarCount);
        final MinusDIIndicator minusDIIndicator = new MinusDIIndicator(series, adxBarCount);

        final Rule plusDICrossedUpMinusDI = new CrossedUpIndicatorRule(plusDIIndicator, minusDIIndicator);
        final Rule plusDICrossedDownMinusDI = new CrossedDownIndicatorRule(plusDIIndicator, minusDIIndicator);
        final OverIndicatorRule closePriceOverSma = new OverIndicatorRule(closePriceIndicator, smaIndicator);
        final Rule entryRule = adxOver20Rule.and(plusDICrossedUpMinusDI).and(closePriceOverSma);

        final UnderIndicatorRule closePriceUnderSma = new UnderIndicatorRule(closePriceIndicator, smaIndicator);
        final Rule exitRule = adxOver20Rule.and(plusDICrossedDownMinusDI).and(closePriceUnderSma);

        return new BaseStrategy("ADX", entryRule, exitRule, adxBarCount);
    }


    public static double calculateATR(List<Kline> klines, int period) {

        if (klines.isEmpty() || period < 0 || period > klines.size()) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        double sumTrueRange = 0.0;

        for (int i = 0; i < period; i++) {
            Kline kline = klines.get(i);
            double trueRange = Math.max(kline.gettHighPrice() - kline.gettLowPrice(), Math.abs(kline.gettHighPrice() - kline.gettClosePrice()));
            trueRange = Math.max(trueRange, Math.abs(kline.gettLowPrice() - kline.gettClosePrice()));
            sumTrueRange += trueRange;
        }

        return sumTrueRange / period;
    }

    private static double calculateAveragePrice(List<Kline> klines) {
        double sum = 0.0;
        for (Kline kline : klines) {
            sum += kline.gettClosePrice();
        }
        return sum / klines.size();
    }

    private static double calculateAverageVolume(List<Kline> klines) {
        double sum = 0.0;
        for (Kline kline : klines) {
            sum += kline.gettVolume();
        }
        return sum / klines.size();
    }

    // Functions economic to check trend of cryptos
    public static int predictWaveTrend(List<Kline> klines) {
        int n1 = 10; // Channel Length
        int n2 = 21; // Average Length
        int obLevel1 = 45; // Over Bought Level 1
        int obLevel2 = 53; // Over Bought Level 2
        int osLevel1 = -45; // Over Sold Level 1
        int osLevel2 = -53; // Over Sold Level 2

        float[] ap = new float[klines.size()];
        for (int i = 0; i < klines.size(); i++) {
            ap[i] = (float) ((klines.get(i).gettHighPrice() + klines.get(i).gettLowPrice() + klines.get(i).gettClosePrice()) / 3.0);
        }

        float[] esa = ema(ap, n1);
        float[] d = ema(abs(subtract(ap, esa)), n1);
        float[] ci = divide(subtract(ap, esa), multiply(0.015f, d));
        float[] tci = ema(ci, n2);

        float[] wt1 = tci;
        float[] wt2 = sma(wt1, 4);

        // Log.e(TAG, Arrays.toString(tci) + "\n wt2 " + Arrays.toString(wt2) + "\n ap " + Arrays.toString(ap)+ "\n esa " + Arrays.toString(esa)+ "\n d" + Arrays.toString(d)+ "\n ci " + Arrays.toString(ci));

        boolean isRising = crossOver(wt1, wt2) && wt1[wt1.length - 1] > obLevel2;
        boolean isFalling = crossOver(wt2, wt1) && wt1[wt1.length - 1] < osLevel2;

        if (isRising) {
            return 1;
        } else if (isFalling) {
            return -1;
        } else {
            return 0;
        }
    }

    // Part of "predictWaveTrend"
    private static float[] ema(float[] x, int n) {
        float[] result = new float[x.length];
        float multiplier = (float) (2.0 / (n + 1));
        //Log.e(TAG, "1111" + Arrays.toString(x) + multiplier );
        result[0] = x[0];
        for (int i = 1; i < x.length; i++) {
            result[i] = (x[i] - result[i - 1]) * multiplier + result[i - 1];
        }
        //Log.e(TAG, "2222" + Arrays.toString(result) + multiplier );
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] sma(float[] x, int n) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            float sum = 0;
            int count = 0;
            for (int j = i; j >= Math.max(i - n + 1, 0); j--) {
                sum += x[j];
                count++;
            }
            result[i] = sum / count;
        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] abs(float[] x) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = Math.abs(x[i]);
        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] subtract(float[] x, float[] y) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = x[i] - y[i];
        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] multiply(float x, float[] y) {
        float[] result = new float[y.length];
        for (int i = 0; i < y.length; i++) {
            result[i] = x * y[i];
        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] divide(float[] x, float[] y) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            if (x[i] == 0) {
                result[i] = 0f;
            } else {
                result[i] = x[i] / y[i];
            }

        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static boolean crossOver(float[] x, float[] y) {
        int length = x.length;
        if (length < 2) {
            return false;
        }
        boolean xGreaterThanY = x[length - 2] > y[length - 2];
        boolean xLessThanY = x[length - 2] < y[length - 2];
        boolean xCrossesOver = x[length - 1] > y[length - 1];
        boolean yCrossesOver = y[length - 1] > x[length - 1];

        //Log.e(TAG, xGreaterThanY + " " + xLessThanY + " " + xCrossesOver + " " + yCrossesOver + " " + Arrays.toString(x) + " " + Arrays.toString(y));

        if (xGreaterThanY && yCrossesOver) {
            return true;
        }
        if (xLessThanY && xCrossesOver) {
            return true;
        }
        return false;
    }

    public static int predictPriceDirection(List<Kline> pastHourKlines) {

        // Compute the average price over the past hour
        double sumPrice = 0;
        for (Kline kline : pastHourKlines) {
            sumPrice += kline.gettClosePrice();
        }
        double averagePrice = sumPrice / pastHourKlines.size();

        // Compute the price change over the past hour as a percentage
        double endPrice = pastHourKlines.get(0).gettClosePrice();
        double startPrice = pastHourKlines.get(pastHourKlines.size() - 1).gettClosePrice();
        double priceChange = ((endPrice - startPrice) / startPrice) * 100;

        //Log.e(TAG, "AVGPrice: " + averagePrice + "ENDPrice: " + endPrice + "PRICEChange: " + priceChange);
        // Predict the direction of the price change in the next hour
        if (priceChange > 0 && averagePrice < endPrice) {
            return 1; // The price is likely to rise in the next hour
        } else if (priceChange < 0 && averagePrice > endPrice) {
            return -1; // The price is likely to drop in the next hour
        } else {
            return 0; // The price is likely to stay relatively stable in the next hour
        }
    }

    public static double percentOfPriceChange(List<Kline> pastHourKlines) {

        // Compute the average price over the past hour
        double sumPrice = 0;
        for (Kline kline : pastHourKlines) {
            sumPrice += kline.gettClosePrice();
        }
        double averagePrice = sumPrice / pastHourKlines.size();

        // Compute the price change over the past hour as a percentage
        double endPrice = pastHourKlines.get(0).gettClosePrice();
        double startPrice = pastHourKlines.get(pastHourKlines.size() - 1).gettClosePrice();
        double priceChange = ((endPrice - startPrice) / startPrice) * 100;

        //Log.e(TAG, "AVGPrice: " + averagePrice + " ENDPrice: " + endPrice + " PRICEChange: " + priceChange);

        return priceChange;
    }


    // Function that checks if crypto chart is going to go up or down with use of EMA, RSI and Stochastic
    public static int predict(ArrayList<Kline> klines, int period) {

        // Check if there are enough Klines to perform analysis
        if (klines.size() < 10) {
            System.out.println("Not enough Klines to perform analysis.");
            return -2;
        }

        // Calculate the 8-period and 15-period Exponential Moving Averages (EMA)
        double ema8 = calculateEMA(klines, period / 2);
        double ema15 = calculateEMA(klines, period);

        // Calculate the Relative Strength Index (RSI)
        double rsi = calculateRSI(klines, period);

        // Calculate the Stochastic Oscillator
        double stochastic = calculateStochastic(klines, period, 5);

        // Make a prediction based on the technical indicators
        if (ema8 > ema15 && rsi > 50 && stochastic > 20) {
            return 1; // Price is likely to rise
        } else {
            return -1; // Price is likely to fall
        }
    }

    // Part of "predict"
    private static double calculateEMA(ArrayList<Kline> klines, int period) {
        double k = 2.0 / (period + 1);
        double ema = klines.get(0).gettClosePrice();
        for (int i = 1; i < period; i++) {
            ema = ema * (1 - k) + klines.get(i).gettClosePrice() * k;
        }
        return ema;
    }

    // Part of "predict"
    private static double calculateRSI(ArrayList<Kline> klines, int period) {
        double gainSum = 0;
        double lossSum = 0;
        double prevClose = klines.get(0).gettClosePrice();
        for (int i = 1; i < period; i++) {
            double diff = klines.get(i).gettClosePrice() - prevClose;
            if (diff >= 0) {
                gainSum += diff;
            } else {
                lossSum += Math.abs(diff);
            }
            prevClose = klines.get(i).gettClosePrice();
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        double rs = avgGain / avgLoss;
        double rsi = 100 - (100 / (1 + rs));
        return rsi;
    }

    // Part of "predict"
    private static double calculateStochastic(ArrayList<Kline> klines, int periodK, int periodD) {
        double[] closes = new double[periodK];
        for (int i = 0; i < periodK; i++) {
            closes[i] = klines.get(klines.size() - 1 - i).gettClosePrice();
        }
        double minLow = getMinLow(klines, periodK);
        double maxHigh = getMaxHigh(klines, periodK);
        double k = 100 * ((closes[periodK - 1] - minLow) / (maxHigh - minLow));
        double[] ks = new double[periodD];
        ks[0] = k;
        for (int i = 1; i < periodD; i++) {
            double sum = ks[i - 1];
            if (i < periodK) {
                sum += k;
            } else {
                sum += 100 * ((closes[periodK - i] - minLow) / (maxHigh - minLow));
            }
            ks[i] = sum / (i + 1);
        }
        return ks[periodD - 1];
    }

    // Part of "predict"
    private static double getMinLow(ArrayList<Kline> klines, int period) {
        double minLow = Double.MAX_VALUE;
        for (int i = klines.size() - 1; i >= klines.size() - period; i--) {
            double low = klines.get(i).gettLowPrice();
            if (low < minLow) {
                minLow = low;
            }
        }
        return minLow;
    }

    // Part of "predict"
    private static double getMaxHigh(ArrayList<Kline> klines, int period) {
        double maxHigh = Double.MIN_VALUE;
        for (int i = klines.size() - 1; i >= klines.size() - period; i--) {
            double high = klines.get(i).gettHighPrice();
            if (high > maxHigh) {
                maxHigh = high;
            }
        }
        return maxHigh;
    }

    // Count $ volume in certain interval
    private float countMoneyVolumeAtInterval(List<Kline> data, int firstKline, int lastKline) {

        if (firstKline - lastKline > 0) {
            return 0;
        }

        int volume = 0;
        float moneyInUSD = 0;
        float openPriceSum = 0;

        for (int i = firstKline; i < lastKline; i++) {
            openPriceSum += data.get(i).gettOpenPrice();
            volume += data.get(i).gettVolume();
        }

        float averageOpenPrice = openPriceSum / (lastKline - firstKline);
        moneyInUSD = averageOpenPrice * volume;

        return moneyInUSD;

    }

    // Count nr Of Trades committed in certain interval
    private int countNrOfTradesAtInterval(List<Kline> data, int firstKline, int lastKline) {

        int result = 0;

        if (firstKline - lastKline >= 0) {
            return 0;
        }

        for (int i = firstKline; i < lastKline; i++) {
            result += data.get(i).gettNumberOfTrades();
        }

        return result;
    }

    // Count if volume has raised in second part of provided klines (intervals)
    private float countBeforeAndAfter(List<Kline> data, int nrOfKlinesToInspect) {

        // We are taking 8 klines - then comparing 4 to 4
        float result;
        int nrBefore = 1;
        int nrAfter = 1;

        for (int i = 0; i < (nrOfKlinesToInspect / 2); i++) {
            nrAfter += data.get(i).gettVolume();
        }

        for (int i = (nrOfKlinesToInspect / 2); i < nrOfKlinesToInspect; i++) {
            nrBefore += data.get(i).gettVolume();
        }
        result = (((float) nrAfter / (float) nrBefore) * 100) - 100;
        return result;
    }

    // Function checks if provided status list function countBestCryptoToBuy matches required criteria / shortOrLong --> Long = 1, Short = 0
    public boolean isKlineApprovedForLongOrShort(List<Integer> sumOf3m, List<Integer> sumOf15m, int shortOrLong) {

        int nrOfGreenKlines3m = 9; // 7
        int nrOfGreenKlines15m = 3;  // 3
        boolean accepted3m = false;
        boolean accepted15m = false;
        int temp = 0;


        for (int i = 0; i < sumOf3m.size(); i++) {

            // For 3m we are looking for <number> green klines in random order
            if (sumOf3m.get(i) == shortOrLong || sumOf3m.get(i) == 2) {
                temp++;
            } else {
                temp += 0;
            }

            // Log.e("UPDService15m", String.valueOf(temp));
            if (temp >= nrOfGreenKlines3m) {
                accepted3m = true;
                break;
            }

            //For 3m we are looking for <number> green klines one after another
//            if (sumOf3m.get(i) == shortOrLong) {
//                temp++;
//            } else if (sumOf3m.get(i) == 2 && temp > 0) {
//                temp++;
//            } else {
//                temp = 0;
//            }
//            // Log.e("UPDService3m", String.valueOf(temp));
//            if (temp == nrOfGreenKlines3m) {
//                accepted3m = true;
//                break;
//            }
        }

        temp = 0;
        //Log.e("UPDService15m", String.valueOf(sumOf15m));

        // For 15m we are looking for 3 green klines in a row
        for (int i = 0; i < sumOf15m.size(); i++) {

            if (sumOf15m.get(i) == shortOrLong) {
                temp++;
            } else if (sumOf15m.get(i) == 2 && temp > 0) {
                temp++;
            } else {
                temp = 0;
            }

            //Log.e("UPDService15m", String.valueOf(temp));
            if (temp == nrOfGreenKlines15m) {
                accepted15m = true;
                break;
            }
        }
        //Log.e("UPDService15m", "3m: " + accepted3m + " 15m: " + accepted15m);
        return (accepted3m && accepted15m);
    }

    private BarSeries convertToBarSeries(List<Kline> klineData, int interval) {
        // Create a new BarSeries and populate it with bars from Kline data
        BarSeries series = new BaseBarSeries();

        Collections.reverse(klineData);

        for (int i = 0; i < klineData.size(); i++) {

            Duration timePeriod = Duration.ofMinutes(interval); // Set the desired time period here
            if (i > 0 && klineData.get(i).gettCloseTime() <= klineData.get(i - 1).gettCloseTime()) {
                break;
            }
            ZonedDateTime endTimeCurrent = Instant.ofEpochMilli(klineData.get(i).gettCloseTime()).atZone(ZoneId.systemDefault());

            // Log.e(TAG, String.valueOf(endTimeCurrent) + " " + timePeriod);
            Bar bar = new BaseBar(timePeriod, endTimeCurrent,
                    DecimalNum.valueOf(klineData.get(i).gettOpenPrice()),
                    DecimalNum.valueOf(klineData.get(i).gettHighPrice()),
                    DecimalNum.valueOf(klineData.get(i).gettLowPrice()),
                    DecimalNum.valueOf(klineData.get(i).gettClosePrice()),
                    DecimalNum.valueOf(klineData.get(i).gettVolume()), DecimalNum.valueOf(klineData.get(i).gettVolume()));

            series.addBar(bar);
        }

        return series;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "DESTROY");
    }

}