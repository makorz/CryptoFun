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
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.R;
import com.example.cryptofun.data.ApprovedToken;
import com.example.cryptofun.data.KlineRequest;
import com.example.cryptofun.data.ObservableModel;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.database.Kline;
import com.example.cryptofun.database.rawTable_Kline;
import com.example.cryptofun.retrofit.RetrofitClientFutures;

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
    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String TABLE_NAME_APPROVED_HISTORIC = "historic_approved_tokens";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String VALUE_STRING = "value_string";
    private static final String ID = "id";
    private DBHandler databaseDB;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        databaseDB = DBHandler.getInstance(getApplicationContext());
                        Log.e(TAG, "Service is running...");
                        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyService:WakeLockTag");
                        mWakeLock.acquire(5*60*1000L );

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
                .setContentTitle("UPDating")
                .setContentText("Searching for money, buddy!")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.crypto_fun_logo);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "destroy");
    }

//    public boolean isPriceStartingToGain(ArrayList<Kline> klines) {
//        double highestClosePrice = Double.MIN_VALUE;
//        double lowestClosePrice = Double.MAX_VALUE;
//
//        // Find the highest and lowest close prices
//        for (Kline kline : klines) {
//            double closePrice = kline.gettClosePrice();
//            if (closePrice > highestClosePrice) {
//                highestClosePrice = closePrice;
//            }
//            if (closePrice < lowestClosePrice) {
//                lowestClosePrice = closePrice;
//            }
//        }
//
//        // Check if the current close price is lower than the highest close price
//        // and if it has increased by at least 10% from the lowest close price
//        Kline currentKline = klines.get(0);
//        double currentClosePrice = currentKline.gettClosePrice();
//        if (currentClosePrice < highestClosePrice && currentClosePrice >= lowestClosePrice * 1.1) {
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    public boolean isPriceStartingToLose(ArrayList<Kline> klines) {
//        double highestClosePrice = Double.MIN_VALUE;
//        double lowestClosePrice = Double.MAX_VALUE;
//
//        // Find the highest and lowest close prices
//        for (Kline kline : klines) {
//            double closePrice = kline.gettClosePrice();
//            if (closePrice > highestClosePrice) {
//                highestClosePrice = closePrice;
//            }
//            if (closePrice < lowestClosePrice) {
//                lowestClosePrice = closePrice;
//            }
//        }
//
//        // Check if the current close price is higher than the lowest close price
//        // and if it has decreased by at least 10% from the highest close price
//        Kline currentKline = klines.get(0);
//        double currentClosePrice = currentKline.gettClosePrice();
//        if (currentClosePrice > lowestClosePrice && currentClosePrice <= highestClosePrice * 0.9) {
//            return true;
//        } else {
//            return false;
//        }
//    }


//    public boolean predictKlineChange(List<Kline> klinesList15Min, List<Kline> klinesList3Min) {
//        // First, we calculate the average price change over the last 3 intervals
//        double avgPriceChange3Min = 0;
//        for (int i = klinesList3Min.size() - 1; i > 0; i--) {
//            avgPriceChange3Min += (klinesList3Min.get(i).gettClosePrice() - klinesList3Min.get(i-1).gettClosePrice());
//        }
//        avgPriceChange3Min /= (klinesList3Min.size() - 1);
//
//        // Next, we calculate the average price change over the last 2 klines (30 minutes)
//        double klineAvgPriceChange = 0;
//        for (int i = klinesList15Min.size() - 1; i > klinesList15Min.size() - 3; i--) {
//            klineAvgPriceChange += (klinesList15Min.get(i).gettClosePrice() - klinesList15Min.get(i-1).gettClosePrice());
//        }
//        klineAvgPriceChange /= 2;
//
//        // Finally, we compare the two average price changes and predict whether the klines will change in the next 30 minutes
//        if (avgPriceChange3Min > klineAvgPriceChange) {
//            return true; // Prices are increasing, so we predict a change
//        } else {
//            return false; // Prices are decreasing or staying the same, so we predict no change
//        }
//    }

    private void sendMessageToActivity(String date, boolean updateStart) {
        Intent intent = new Intent("DB_updated");
        intent.putExtra("updateDate", date);
        intent.putExtra("updateStarted", updateStart);
        Log.e(TAG, "Send Broadcast Message Update Started Background" + updateStart);
        LocalBroadcastManager.getInstance(UpdatingDatabaseService.this).sendBroadcast(intent);
       // databaseDB.close();
        stopForeground(true);
        stopSelf();
    }

     private void sendInfoToActivity() {
        Intent intent = new Intent("DB_update_start");
        Log.e(TAG, "Loading icon START");
        LocalBroadcastManager.getInstance(UpdatingDatabaseService.this).sendBroadcast(intent);
    }

    private void updateDBtoCurrentValues(long timeOfUpdate) {
        sendInfoToActivity();
        Log.e(TAG, "UpdateDB START " + Thread.currentThread() + " " + Thread.activeCount());
        int maxNrOfKlines = 15;
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
        //Log.e("UpdatingExistingDB", "Observable size2: " + observableList.size());

        for (int i = 0; i < listOfSymbols.size(); i++) {
            observableList.add(updateIntervalOfDB(listOfSymbols.get(i), "3m", maxNrOfKlines, timeOfUpdate));
            observableList.add(updateIntervalOfDB(listOfSymbols.get(i), "15m", maxNrOfKlines, timeOfUpdate));
            observableList.add(updateIntervalOfDB(listOfSymbols.get(i), "4h", maxNrOfKlines, timeOfUpdate));
        }

        Iterator<ObservableModel> it = observableList.iterator();
        while (it.hasNext()) {
            ObservableModel iteratorItem = it.next();
            if (iteratorItem.getNrOfKlinesToDownload() == 0) {
                it.remove();
            }
        }
        Log.e(TAG, "Observable size: " + observableList.size());
        observableStart(observableList);
    }

    public ObservableModel updateIntervalOfDB(String symbol, String interval, int maxNrOfKlines, long timeCurrent) {

        Cursor data, data2;
        int nrOfKlines, nrOfKlinesFromLastDBUpdate;
        long closeTime;
        ObservableModel model = new ObservableModel(symbol, 0, "3m", 0, 0);
        long minutes3 = 180000;
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
                    model = new ObservableModel(symbol, 15, intervalInSwitch, 0, 0);
                } else {
                    data.moveToFirst();
                    data2.moveToFirst();
                    ///Log.e("UpdatingExistingDB", symbol + interval);
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    nrOfKlinesFromLastDBUpdate = (int) (-(closeTime - timeCurrent) / minutes3) + 2;

                    if (nrOfKlines >= maxNrOfKlines || nrOfKlines < 1 || nrOfKlinesFromLastDBUpdate > maxNrOfKlines) {
                        model = new ObservableModel(symbol, 15, intervalInSwitch, 1, 0);
                    } else {
                        if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines) {
                            int howManyOldOnes = (nrOfKlines - 1) + nrOfKlinesFromLastDBUpdate - maxNrOfKlines;
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 2, howManyOldOnes);
                        } else if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines) {
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines) {
                            if (closeTime - timeCurrent > 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate - 1, intervalInSwitch, 3, 0);
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                            }
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines) {
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
                    model = new ObservableModel(symbol, 15, intervalInSwitch, 0, 0);
                } else {
                    data.moveToFirst();
                    data2.moveToFirst();
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    nrOfKlinesFromLastDBUpdate = (int) (-(closeTime - timeCurrent) / minutes15) + 2;

                    if (nrOfKlines >= maxNrOfKlines || nrOfKlines < 1 || nrOfKlinesFromLastDBUpdate > maxNrOfKlines) {
                        model = new ObservableModel(symbol, 15, intervalInSwitch, 1, 0);
                    } else {
                        if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines) {
                            int howManyOldOnes = (nrOfKlines - 1) + nrOfKlinesFromLastDBUpdate - maxNrOfKlines;
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 2, howManyOldOnes);
                        } else if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines) {
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines) {
                            if (closeTime - timeCurrent > 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate - 1, intervalInSwitch, 3, 0);
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                            }
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines) {
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
            case "4h":
                intervalInSwitch = "4h";
                data = databaseDB.retrieveLastCloseTime(intervalInSwitch);
                data2 = databaseDB.nrOfKlinesForSymbolInInterval(symbol, intervalInSwitch);
                if (data.getCount() == 0 || data2.getCount() == 0) {
                    Log.e("UpdatingExistingDB", "Table " + TABLE_NAME_KLINES_DATA + " is empty. [updateIntervalOfDB 4h]");
                    model = new ObservableModel(symbol, 10, intervalInSwitch, 0, 0);
                } else {
                    data.moveToFirst();
                    data2.moveToFirst();
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    nrOfKlinesFromLastDBUpdate = (int) (-(closeTime - timeCurrent) / hours4) + 2;
                    if (nrOfKlines >= maxNrOfKlines || nrOfKlines < 1 || nrOfKlinesFromLastDBUpdate > maxNrOfKlines) {
                        model = new ObservableModel(symbol, 10, intervalInSwitch, 1, 0);
                    } else {
                        if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines) {
                            int howManyOldOnes = (nrOfKlines - 1) + nrOfKlinesFromLastDBUpdate - maxNrOfKlines;
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 2, howManyOldOnes);
                        } else if (nrOfKlinesFromLastDBUpdate > 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines) {
                            model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) <= maxNrOfKlines) {
                            if (closeTime - timeCurrent > 0) {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate - 1, intervalInSwitch, 3, 0);
                            } else {
                                model = new ObservableModel(symbol, nrOfKlinesFromLastDBUpdate, intervalInSwitch, 3, 0);
                            }
                        } else if (nrOfKlinesFromLastDBUpdate == 2 && (nrOfKlines - 1 + nrOfKlinesFromLastDBUpdate) > maxNrOfKlines) {
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

        Log.e(TAG, "request size: " + request.size());

        Observable.zip(
                        observableRequestList,
                        new Function<Object[], Object>() {
                            @Override
                            public Object apply(Object[] objects) throws Exception {
                                // Objects[] is an array of combined results of completed requests
                                Log.e(TAG, "OBSERVABLE  HOW MANY? --> " + objects.length);
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
                                Log.e(TAG, "PERFECT");

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
                        },

                        // Will be triggered if any error during requests will happen
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) throws Exception {
                                Log.e(TAG, "FUCKED " + e);
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
                Log.e(TAG,"Error from Observable");
                Cursor data2 = databaseDB.retrieveParam(1);
                if (data2.getCount() == 0) {
                    Log.e(TAG,"No param");
                    date = "No update time";
                } else {
                    data2.moveToFirst();
                    date = data2.getString(2);
                }
                data2.close();
            } else {
                date = "Update time:  " + df.format(new Date(stamp.getTime()));
            }
            databaseDB.clearHistoric(olderThan);
            sendMessageToActivity(date, false);
        }
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
        data.close();
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
                databaseDB.addParam(1, "Last Update Time", date,0,0);
            } else {
                if (data2.getCount() >= 2) {
                    databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 1);
                    databaseDB.addParam(1, "Last Update Time", date,0,0);
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

            //if more than 2 minutes passed since last closeTimeOfKline --> UpdateDB
            long oneAndHalfMin = 90000;
            Log.e(TAG, "A: " + tempKline.gettCloseTime() + " - B: " + oneAndHalfMin + " - C: " + System.currentTimeMillis());
            long time = (tempKline.gettCloseTime() - oneAndHalfMin - System.currentTimeMillis());


            if (time < 0) {
                Cursor data2 = databaseDB.retrieveParam(1);
                if (data2.getCount() == 0) {
                    databaseDB.addParam(1, "Last Update Time", date,0,0);
                } else {
                    if (data2.getCount() >= 2) {
                        databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 1);
                        databaseDB.addParam(1, "Last Update Time", date,0,0);
                    }
                    databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_STRING, date, ID, "1");

                }
                data2.close();
                updateDBtoCurrentValues(System.currentTimeMillis());
                Log.e(TAG, "DB is not actual " + tempKline.gettCloseTime() + " " + time + " " + System.currentTimeMillis() + " " + date + " " + date2);
                sendMessageToActivity(date, true);
            } else {
                Log.e(TAG, "DB is actual " + tempKline.gettCloseTime() + " " + time + " " + System.currentTimeMillis() + " " + date + " " + date2);
                Cursor data3 = databaseDB.retrieveAllFromTable(TABLE_NAME_KLINES_DATA);
                if (data3.getCount() > 10) {
                    Log.e(TAG, "Table " + TABLE_NAME_KLINES_DATA + " is not empty. [onStartCommand]");
                    startCountingAndReturnResult(true);

                }
                data3.close();
                sendMessageToActivity(date, false);
            }
        }
        data.close();
    }

    // Function checks what crypto is going to run in green
    private void countBestCryptoToBuy(String symbol) {

        int acceptableVolume = 250000;
        int acceptablePercentOfVolumeRise = 35;

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

        float volumeOfLast15mKlines = countMoneyVolumeAtInterval(coinKlines15m, 0, 2);

        //boolean isPriceAboutToChange = predictKlineChange(coinKlines15m, coinKlines3m);

        if (volumeOfLast15mKlines > acceptableVolume) {

            float percentOfRiseOfNumberOfVolumeInLast15min = countBeforeAndAfter(coinKlines3m, 6);

            if (percentOfRiseOfNumberOfVolumeInLast15min > acceptablePercentOfVolumeRise) {

                if (!symbol.contains("BUSDUSDT")) {

                    statusOf15mKlines = statusOf15mKlines.subList(0, 3);
                    statusOf3mKlines = statusOf3mKlines.subList(0, 8);
                    int nrOfTradesLast15mKlinesSum = countNrOfTradesAtInterval(coinKlines15m, 0, 2);

                    if (isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 1)) {
                        Log.e(TAG, "IS IT GOOOD LONG??? " + symbol + " " + percentOfRiseOfNumberOfVolumeInLast15min
                                + " " + statusOf3mKlines + " close price: " + coinKlines3m.get(0).tClosePrice);
                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                    }

                    if (isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0)) {
                        Log.e(TAG, "IS IT GOOOD SHORT??? " + symbol + " " + percentOfRiseOfNumberOfVolumeInLast15min
                                + " " + statusOf3mKlines + " close price: " + coinKlines3m.get(0).tClosePrice);
                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                    }

                }
            }
        }
    }

    //TRUE is long
    private float count8hPercentOfChange(List<Kline> data) {

        long oldestOpenTime = data.get(1).gettOpenTime();
        float oldestOpenPrice = data.get(1).gettOpenPrice();
        float percentOfChange = 0;
        long newestOpenTime = data.get(0).gettOpenTime();
        float newestClosePrice = data.get(0).gettClosePrice();

        if (oldestOpenTime + 14400000 == newestOpenTime) {
            percentOfChange = ((newestClosePrice / oldestOpenPrice) * 100) - 100;
        }

        return percentOfChange;
    }

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

    private int countNrOfTradesAtInterval(List<Kline> data, int firstKline, int lastKline) {

        int result = 0;

        if (firstKline - lastKline > 0) {
            return 0;
        }

        for (int i = firstKline; i < lastKline; i++) {
            result += data.get(i).gettNumberOfTrades();
        }

        return result;
    }

    private float countBeforeAndAfter(List<Kline> data, int nrOfKlinesToInspect) {

        // We are taking 8 klines - then comparing 4 to 4
        float result;
        int nrBefore = 1;
        int nrAfter = 1;

        for (int i = 0; i < (nrOfKlinesToInspect / 2); i++) {
            nrAfter += data.get(i).gettNumberOfTrades();
        }

        for (int i = (nrOfKlinesToInspect / 2); i < nrOfKlinesToInspect; i++) {
            nrBefore += data.get(i).gettNumberOfTrades();
        }
        result = (((float) nrAfter / (float) nrBefore) * 100) - 100;
        return result;
    }

    // Function checks if provided status list function countBestCryptoToBuy matches required criteria / shortOrLong --> Long = 1, Short = 0
    public boolean isKlineApprovedForLongOrShort(List<Integer> sumOf3m, List<Integer> sumOf15m, int shortOrLong) {

        int nrOfGreenKlines3m = 5; //5
        int nrOfGreenKlines15m = 2; //4
        boolean accepted3m = false;
        boolean accepted15m = false;
        int temp = 0;


        for (int i = 0; i < sumOf3m.size(); i++) {

            // For 3m we are looking for <number> green klines in random order
//            if (sumOf3m.get(i) == shortOrLong || sumOf3m.get(i) == 2) {
//                temp++;
//            } else {
//                temp += 0;
//            }
//
//            // Log.e("UPDService15m", String.valueOf(temp));
//            if (temp >= nrOfGreenKlines3m) {
//                accepted3m = true;
//                break;
//            }

            //For 3m we are looking for <number> green klines one after another
            if (sumOf3m.get(i) == shortOrLong) {
                temp++;
            } else if (sumOf3m.get(i) == 2 && temp > 0) {
                temp++;
            } else {
                temp = 0;
            }
            // Log.e("UPDService3m", String.valueOf(temp));
            if (temp == nrOfGreenKlines3m) {
                accepted3m = true;
                break;
            }
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


}