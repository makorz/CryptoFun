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
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.data.database.Kline;
import com.example.cryptofun.data.database.rawTable_Kline;
import com.example.cryptofun.ui.retrofit.RetrofitClientFutures;

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
//    private PowerManager.WakeLock mWakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        databaseDB = DBHandler.getInstance(getApplicationContext());
                        Log.e(TAG, "Service is running...");
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "destroy");
    }

    private void sendMessageToActivity(String date, boolean updateStart) {
        Intent intent = new Intent("DB_updated");
        intent.putExtra("updateDate", date);
        intent.putExtra("updateStarted", updateStart);
        Log.e(TAG, "Send Broadcast Message Update Started Background " + updateStart);
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
        int maxNrOfKlines = 20;
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
                    model = new ObservableModel(symbol, 20, intervalInSwitch, 0, 0);
                } else {
                    data.moveToFirst();
                    data2.moveToFirst();
                    ///Log.e("UpdatingExistingDB", symbol + interval);
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    nrOfKlinesFromLastDBUpdate = (int) (-(closeTime - timeCurrent) / minutes3) + 2;

                    if (nrOfKlines >= maxNrOfKlines || nrOfKlines < 1 || nrOfKlinesFromLastDBUpdate > maxNrOfKlines) {
                        model = new ObservableModel(symbol, 20, intervalInSwitch, 1, 0);
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
                    model = new ObservableModel(symbol, 20, intervalInSwitch, 0, 0);
                } else {
                    data.moveToFirst();
                    data2.moveToFirst();
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    nrOfKlinesFromLastDBUpdate = (int) (-(closeTime - timeCurrent) / minutes15) + 2;

                    if (nrOfKlines >= maxNrOfKlines || nrOfKlines < 1 || nrOfKlinesFromLastDBUpdate > maxNrOfKlines) {
                        model = new ObservableModel(symbol, 20, intervalInSwitch, 1, 0);
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
                    model = new ObservableModel(symbol, 20, intervalInSwitch, 0, 0);
                } else {
                    data.moveToFirst();
                    data2.moveToFirst();
                    nrOfKlines = data2.getInt(0);
                    closeTime = data.getLong(8);
                    nrOfKlinesFromLastDBUpdate = (int) (-(closeTime - timeCurrent) / hours4) + 2;
                    if (nrOfKlines >= maxNrOfKlines || nrOfKlines < 1 || nrOfKlinesFromLastDBUpdate > maxNrOfKlines) {
                        model = new ObservableModel(symbol, 20, intervalInSwitch, 1, 0);
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
                Log.e(TAG, "Error from Observable");
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

            //if more than 2 minutes passed since last closeTimeOfKline --> UpdateDB
            long oneAndHalfMin = 90000;
            Log.e(TAG, "A: " + tempKline.gettCloseTime() + " - B: " + oneAndHalfMin + " - C: " + System.currentTimeMillis());
            long time = (tempKline.gettCloseTime() - oneAndHalfMin - System.currentTimeMillis());


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

        float volumeOfLast15mKlines;
        int acceptableVolume = 1000000;

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

        if (volumeOfLast15mKlines >= acceptableVolume && percentOfRiseOfNumberOfVolumeInLast15min > acceptablePercentOfVolumeRise && !symbol.contains("BUSDUSDT")) {

            List<Integer> statusListOf3mToCheck;
            List<Integer> statusListOf15mToCheck;
            statusListOf15mToCheck = statusOf15mKlines.subList(0, 3);
            statusListOf3mToCheck = statusOf3mKlines.subList(0, 9);

            String info = "LEVEL1: " + symbol + " - Volume: " + volumeOfLast15mKlines + " PercentOfVolumeRise: " + percentOfRiseOfNumberOfVolumeInLast15min + " - StatusOf3m: " + statusListOf3mToCheck + " StatusOf15m: " + statusListOf15mToCheck;
            Log.e(TAG, info);
            ServiceFunctions.writeToFile(info, getApplicationContext(),"result");

            if (isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 1)) {

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

                double last12mPriceChange = percentOfPriceChange(coinKlines3m.subList(0, 4));

                waveTrendPredict3 = predictPriceChange2(coinKlines3m);
                waveTrendPredict15 = predictPriceChange2(coinKlines15m);
                waveTrendPredict4 = predictPriceChange2(coinKlines4h);

                info = "LEVEL2 (LONG): " + symbol + " is predicted to be (3m, 15m, 4h): " + predict3 + " " + predict15 + " " + predict4 + ". Arrays size: " + coinKlines15m.size() + " " + coinKlines3m.size() + " " + coinKlines4h.size() + " Price direction: " + priceDirection3 + " " + priceDirection15 + " " + priceDirection4 + " Wave Trend: " + waveTrendPredict3 + " " + waveTrendPredict15 + " " + waveTrendPredict4 + " Last12mPriceChange: " + last12mPriceChange;
                Log.e(TAG, info);
                ServiceFunctions.writeToFile(info, getApplicationContext(),"result");

                if (last12mPriceChange < -3) { //|| last12mPriceChange > 3

                    if (priceDirection3 == 1 &&  priceDirection4 == 1) {
                        info = "LEVEL3 (LONG): " + symbol + " approved path nr 1";
                        Log.e(TAG, info);
                        ServiceFunctions.writeToFile(info, getApplicationContext(),"result");

                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);

                    }

                }
                else if (last12mPriceChange > 0.2) {

                    if (priceDirection3 == 1 && priceDirection15 == 1 && predict15 == 1 && priceDirection4 == 1) {
                        info = "LEVEL3 (LONG): " + symbol + " approved path nr 2";
                        Log.e(TAG, info);
                        ServiceFunctions.writeToFile(info, getApplicationContext(),"result");

                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);

                    }

                }


            } else if (isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 0)) {

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

                double last12mPriceChange = percentOfPriceChange(coinKlines3m.subList(0, 4));

                waveTrendPredict3 = predictPriceChange2(coinKlines3m);
                waveTrendPredict15 = predictPriceChange2(coinKlines15m);
                waveTrendPredict4 = predictPriceChange2(coinKlines4h);

                info = "LEVEL2 (SHORT): " + symbol + " is predicted to be (3m, 15m, 4h): " + predict3 + " " + predict15 + " " + predict4 + ". Arrays size: " + coinKlines15m.size() + " " + coinKlines3m.size() + " " + coinKlines4h.size() + " Price direction: " + priceDirection3 + " " + priceDirection15 + " " + priceDirection4 + " Wave Trend: " + waveTrendPredict3 + " " + waveTrendPredict15 + " " + waveTrendPredict4 + " Last12mPriceChange: " + last12mPriceChange;
                Log.e(TAG, info);
                ServiceFunctions.writeToFile(info, getApplicationContext(),"result");

                if ( last12mPriceChange > 3) { //(last12mPriceChange < -3 ||

                    if (priceDirection3 == -1 && priceDirection4 == -1) {
                        info = "LEVEL3 (SHORT): " + symbol + " approved path nr 1";
                        Log.e(TAG, info);
                        ServiceFunctions.writeToFile(info, getApplicationContext(),"result");

                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);

                    }

                }
                else if (last12mPriceChange < -0.2) {

                    if (priceDirection3 == -1 && priceDirection15 == -1 && predict15 == -1 && priceDirection4 == -1) {
                        info = "LEVEL3 (SHORT): " + symbol + " approved path nr 2";
                        Log.e(TAG, info);
                        ServiceFunctions.writeToFile(info, getApplicationContext(),"result");

                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED);
                        databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, nrOfTradesLast15mKlinesSum,
                                volumeOfLast15mKlines, System.currentTimeMillis(), coinKlines3m.get(0).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);

                    }

                }

            }

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

    public static int predictPriceChange2(List<Kline> klines) {
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

        boolean isRising = crossOver(wt1, wt2) && wt1[wt1.length - 1] > obLevel1;
        boolean isFalling = crossOver(wt2, wt1) && wt1[wt1.length - 1] < osLevel1;

        if (isRising) {
            return 1;
        } else if (isFalling) {
            return -1;
        } else {
            return 0;
        }
    }

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

    private static float[] abs(float[] x) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = Math.abs(x[i]);
        }
        return result;
    }

    private static float[] subtract(float[] x, float[] y) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = x[i] - y[i];
        }
        return result;
    }

    private static float[] multiply(float x, float[] y) {
        float[] result = new float[y.length];
        for (int i = 0; i < y.length; i++) {
            result[i] = x * y[i];
        }
        return result;
    }

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

        Log.e(TAG, "AVGPrice: " + averagePrice + "ENDPrice: " + endPrice + "PRICEChange: " + priceChange);

        return priceChange;
    }

    // Function that checks if crypto chart is going to go up or down with use of EMA, RSI and Stochastic
    public static int predict(ArrayList<Kline> klines) {

        // Check if there are enough Klines to perform analysis
        if (klines.size() < 10) {
            System.out.println("Not enough Klines to perform analysis.");
            return -2;
        }

        // Calculate the 8-period and 15-period Exponential Moving Averages (EMA)
        double ema8 = calculateEMA(klines, 8);
        double ema15 = calculateEMA(klines, 15);

        // Calculate the Relative Strength Index (RSI)
        double rsi = calculateRSI(klines, 14);

        // Calculate the Stochastic Oscillator
        double stochastic = calculateStochastic(klines, 14, 3);

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

        int nrOfGreenKlines3m = 7; //5   5
        int nrOfGreenKlines15m = 3; //4   2
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

}