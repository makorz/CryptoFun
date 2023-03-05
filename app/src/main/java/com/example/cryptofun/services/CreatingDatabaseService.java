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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.R;
import com.example.cryptofun.data.CoinSymbol;
import com.example.cryptofun.data.CoinSymbols;
import com.example.cryptofun.data.KlineRequest;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.database.rawTable_Kline;
import com.example.cryptofun.retrofit.RetrofitClientFutures;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatingDatabaseService extends Service {

    private static final String TAG = "CRTService";

    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private List<String> listOfSymbols = new ArrayList<>();
    private DBHandler databaseDB;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        databaseDB = DBHandler.getInstance(getApplicationContext());
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.e("CRTStart", "SIZE ON START -> " + listOfSymbols.size() );

                        // It's for foreground services, because in newest Android, background are not working. Foreground need to inform user that it is running
                        Notification notification = createNotification();
                        // Notification ID cannot be 0.
                        startForeground(1, notification);
                        getDataOfCryptoFromAPI();

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
                .setContentTitle("CRTDatabase")
                .setContentText("Preparing for money, buddy!")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.drawable.crypto_fun_logo);

        return builder.build();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("CheckResult")
    private void getDataOfCryptoKlines() {

        if (databaseDB.retrieveAllFromTable(TABLE_NAME_KLINES_DATA).getCount() != 0) {
            databaseDB.clearTable(TABLE_NAME_KLINES_DATA);
        }

        List<rawTable_Kline> klinesDataList = new ArrayList<>();
        List<KlineRequest> request = new ArrayList<>();
        Log.e("CRT", "LIST OF SYMBOLS -> " + listOfSymbols);

        int LIMIT3m = 12;
        int LIMIT15m = 16;
        //int LIMIT1d = 4;
        int LIMIT4h = 6;
        for (int i = 0; i < listOfSymbols.size(); i++) {
            // Make a collection of all requests you need to call at once, there can be any number of requests, not only 3. You can have 2 or 5, or 100.
            request.add(new KlineRequest(RetrofitClientFutures.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT15m, "15m"),
                    listOfSymbols.get(i), "15m"));
            request.add(new KlineRequest(RetrofitClientFutures.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT3m, "3m"),
                    listOfSymbols.get(i), "3m"));
//            request.add(new KlineRequest(RetrofitClient.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT1d, "1d"),
//                    listOfSymbols.get(i), "1d"));
            request.add(new KlineRequest(RetrofitClientFutures.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT4h, "4h"),
                    listOfSymbols.get(i), "4h"));

        }

        List<Observable<?>> observableRequestList = new ArrayList<>();
        Log.e("OBSERVABLE", "request size: " + request.size());
        Log.e("OBSERVABLE", "listOfCrypto size: " + listOfSymbols.size());
        for (int i = 0; i < request.size(); i++) {
            observableRequestList.add(request.get(i).getRequest());

        }

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
                                Log.e("OBSERVABLE", " PERFECT");
                                Toast.makeText(getApplicationContext(), "Writing into DB", Toast.LENGTH_SHORT).show();

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
                                    getDataOfCryptoFromAPI();
                                }

                                klinesDataList.clear();

                            }
                        },

                        // Will be triggered if any error during requests will happen
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) throws Exception {
                                //x zibit
                                // Toast.makeText(CreatingDatabaseService.this, "ERROR WHILE COLLECTING DATA " +  e.toString(), Toast.LENGTH_SHORT).show();
                                Toast.makeText(getApplicationContext(), "Error: " + e, Toast.LENGTH_LONG).show();
                                Log.e("OBSERVABLE", " IS FUCKED " + e.toString());
                            }
                        }
                );
    }

    private void getSymbolsList() {

        Cursor data = databaseDB.retrieveAllFromTable(TABLE_SYMBOL_AVG);
        if (data.getCount() == 0) {
            Call<CoinSymbols> call = RetrofitClientFutures.getInstance().getMyApi().getFuturesSymbols();
            call.enqueue(new Callback<CoinSymbols>() {
                @Override
                public void onResponse(@NonNull Call<CoinSymbols> call, @NonNull Response<CoinSymbols> response) {
                    assert response.body() != null;
                    CoinSymbols symbols = response.body();
                    List<CoinSymbol> tokenList = symbols.getSymbols();

                    if (listOfSymbols.size() > 0) {
                        listOfSymbols.clear();
                    }
                    List<String> arrayUSDTtemp = new ArrayList<>();
                    List<String> arrayBUSDtemp = new ArrayList<>();
                      for (int i = 0; i < tokenList.size(); i++) {

                        if (tokenList.get(i).getSymbol().contains("USDT")

                                && tokenList.get(i).getStatus().equals("TRADING")
                                && !tokenList.get(i).getSymbol().contains("BUSD")
                                && !tokenList.get(i).getSymbol().contains("230331")
//                                && !tokenList.get(i).getSymbol().contains("EUR")
//                                && !tokenList.get(i).getSymbol().contains("PLN")
//                                && !tokenList.get(i).getSymbol().contains("UP")
//                                && !tokenList.get(i).getSymbol().contains("DOWN")
//                                && !tokenList.get(i).getSymbol().contains("BTCST")
//                                && (tokenList.get(i).getPermissions().contains("MARGIN")
//                                || (tokenList.get(i).getPermissions().contains("TRD_GRP_004") || tokenList.get(i).getPermissions().contains("TRD_GRP_005")
//                                || tokenList.get(i).getPermissions().contains("TRD_GRP_006")))
//
//                                && tokenList.get(i).getPermissions().contains("MARGIN")
//                                && (tokenList.get(i).getPermissions().contains("TRD_GRP_004") || tokenList.get(i).getPermissions().contains("TRD_GRP_005")
//                                || tokenList.get(i).getPermissions().contains("TRD_GRP_006"))
                        ) { // && tokenList.get(i).getPermissions().contains("MARGIN") && !tokenList.get(i).getSymbol().contains("BUSD")

                            String tempSymbol = tokenList.get(i).getSymbol().replace("USDT","");
                            arrayUSDTtemp.add(tempSymbol);
                            listOfSymbols.add(tokenList.get(i).getSymbol());

                        }

                        if (tokenList.get(i).getSymbol().contains("BUSD")
                                && tokenList.get(i).getStatus().equals("TRADING")
                                && !tokenList.get(i).getSymbol().contains("USDT")
                                && !tokenList.get(i).getSymbol().contains("230331")
//                                && !tokenList.get(i).getSymbol().contains("UP")
//                                && !tokenList.get(i).getSymbol().contains("DOWN")
//                                && !tokenList.get(i).getSymbol().contains("EUR")
//                                && !tokenList.get(i).getSymbol().contains("PLN")
//                                && (tokenList.get(i).getPermissions().contains("MARGIN")
//                                || (tokenList.get(i).getPermissions().contains("TRD_GRP_004") || tokenList.get(i).getPermissions().contains("TRD_GRP_005")
//                                || tokenList.get(i).getPermissions().contains("TRD_GRP_006")))
//
//                                && (tokenList.get(i).getPermissions().contains("TRD_GRP_004") || tokenList.get(i).getPermissions().contains("TRD_GRP_005")
//                                || tokenList.get(i).getPermissions().contains("TRD_GRP_006"))
                        ) { //

                            arrayBUSDtemp.add(tokenList.get(i).getSymbol());

                        }

                    }

                    for (int i = 0; i < arrayUSDTtemp.size(); i++) {
                        int finalI = i;
                        arrayBUSDtemp.removeIf(s -> s.contains(arrayUSDTtemp.get(finalI)));
                    }

                    listOfSymbols.addAll(arrayBUSDtemp);

                    Log.e("CRTDB", listOfSymbols.toString());

                    databaseDB.addNewCrypto(listOfSymbols);
                    getDataOfCryptoFromAPI();
                }

                @Override
                public void onFailure(@NonNull Call<CoinSymbols> call, @NonNull Throwable t) {
                    System.out.println("An error has occurred" + t);
                }

            });
        } else {
            listOfSymbols.clear();
            while (data.moveToNext()) {
                listOfSymbols.add(data.getString(1));
            }
        }
        data.close();
    }

    @SuppressLint("SetTextI18n")
    public void getDataOfCryptoFromAPI() {

        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_SYMBOL_AVG + " is empty.");
            getSymbolsList();
            data.close();
        } else {
            data.close();
            getSymbolsList();
            Cursor data2 = databaseDB.retrieveAllFromTable(TABLE_NAME_KLINES_DATA);
            if (data2.getCount() == 0) {
                Log.e(TAG, "Table " + TABLE_NAME_KLINES_DATA + " is empty.");
                getDataOfCryptoKlines();
                data2.close();
            } else {
                data2.close();
                Cursor data3 = databaseDB.retrieveAllFromTable(TABLE_SYMBOL_AVG);
                Log.e(TAG, String.valueOf(listOfSymbols.size()));
                if (data3.getCount() != 0 && listOfSymbols.size() == 0) {
                    data3.moveToFirst();
                    while (data3.moveToNext()) {
                        //Log.e("SYMBOLS", data3.getString(1));
                        listOfSymbols.add(data3.getString(1));
                    }
                }
                data3.close();
                sendMessageToActivity();
                //databaseDB.close();
            }

        }
    }

    private void sendMessageToActivity( ) {
        Intent intent = new Intent("DB_created");
        Log.e(TAG, "SendMessage1");
        intent.putExtra("finishedCRTDB", true);
        LocalBroadcastManager.getInstance(CreatingDatabaseService.this).sendBroadcast(intent);
        stopForeground(true);
        stopSelf();
    }


}