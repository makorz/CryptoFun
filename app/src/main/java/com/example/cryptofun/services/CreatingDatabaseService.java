package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.data.CoinSymbol;
import com.example.cryptofun.data.CoinSymbols;
import com.example.cryptofun.data.KlineRequest;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.database.rawTable_Kline;
import com.example.cryptofun.retrofit.RetrofitClient;

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


    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private List<String> listOfSymbols = new ArrayList<>();
    private DBHandler databaseDB;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        databaseDB = new DBHandler(this);

        Log.e("BACKGROUNDTASK", "SIZE ON START -> " + listOfSymbols.size() );
        getDataOfCryptoFromAPI();
        return START_STICKY;
        //return super.onStartCommand(intent, flags, startId);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void getDataOfCryptoKlines() {

        if (databaseDB.retrieveAllFromTable(TABLE_NAME_KLINES_DATA).getCount() != 0) {
            databaseDB.clearTable(TABLE_NAME_KLINES_DATA);
        }

        List<rawTable_Kline> klinesDataList = new ArrayList<>();
        List<KlineRequest> request = new ArrayList<>();

        for (int i = 0; i < listOfSymbols.size(); i++) {
            // Make a collection of all requests you need to call at once, there can be any number of requests, not only 3. You can have 2 or 5, or 100.
            int LIMIT_OF_15M = 6;
            request.add(new KlineRequest(RetrofitClient.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT_OF_15M, "15m"),
                    listOfSymbols.get(i), "15m"));
            int LIMIT_OF_3M = 5;
            request.add(new KlineRequest(RetrofitClient.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT_OF_3M, "3m"),
                    listOfSymbols.get(i), "3m"));
            int LIMIT_OF_1D = 7;
            request.add(new KlineRequest(RetrofitClient.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT_OF_1D, "1d"),
                    listOfSymbols.get(i), "1d"));
            int LIMIT_OF_4H = 12;
            request.add(new KlineRequest(RetrofitClient.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT_OF_4H, "4h"),
                    listOfSymbols.get(i), "4h"));

        }

        List<Observable<?>> observableRequestList = new ArrayList<>();
        Log.e("OBSERVABLE", "request size: " + String.valueOf(request.size()));
        Log.e("OBSERVABLE", "listOfCrypto size: " + String.valueOf(listOfSymbols.size()));
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
                                //Do something on error completion of requests
                                Log.e("OBSERVABLE", " IS FUCKED" + e.toString());
                            }
                        }
                );
    }

    private void getSymbolsList() {

        Cursor data = databaseDB.retrieveAllFromTable(TABLE_SYMBOL_AVG);
        if (data.getCount() == 0) {
            Call<CoinSymbols> call = RetrofitClient.getInstance().getMyApi().getSymbols();
            call.enqueue(new Callback<CoinSymbols>() {
                @Override
                public void onResponse(Call<CoinSymbols> call, Response<CoinSymbols> response) {
                    assert response.body() != null;
                    CoinSymbols symbols = response.body();
                    List<CoinSymbol> tokenList = symbols.getSymbols();

                    if (listOfSymbols.size() > 0) {
                        listOfSymbols.clear();
                    }
                    for (int i = 0; i < tokenList.size(); i++) {

                        if (tokenList.get(i).getSymbol().contains("USDT") && !tokenList.get(i).getSymbol().contains("UP")
                                && !tokenList.get(i).getSymbol().contains("DOWN") && tokenList.get(i).getStatus().equals("TRADING") &&
                                tokenList.get(i).getPermissions().contains("MARGIN")) {

                            listOfSymbols.add(tokenList.get(i).getSymbol());
                        }
                    }
                    databaseDB.addNewCrypto(listOfSymbols);
                    Log.e("BACKGROUNDTASK-GETSYMBOLS", "SIZE ON START -> " + listOfSymbols.size() );
                    getDataOfCryptoFromAPI();
                }

                @Override
                public void onFailure(Call<CoinSymbols> call, Throwable t) {
                    System.out.println("An error has occured" + t);
                }

            });
        }
    }

    @SuppressLint("SetTextI18n")
    public void getDataOfCryptoFromAPI() {

        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        if (data.getCount() == 0) {
            Log.e("CRTService", "Table " + TABLE_SYMBOL_AVG + " is empty.");
            getSymbolsList();
            data.close();
        } else {
            Cursor data2 = databaseDB.retrieveAllFromTable(TABLE_NAME_KLINES_DATA);
            if (data2.getCount() == 0) {
                Log.e("CRTService", "Table " + TABLE_NAME_KLINES_DATA + " is empty.");
                getDataOfCryptoKlines();
                    data2.close();
            } else {
                Cursor data3 = databaseDB.retrieveAllFromTable(TABLE_SYMBOL_AVG);
                Log.e("CRTService", String.valueOf(listOfSymbols.size()));
                if (data3.getCount() != 0 && listOfSymbols.size() == 0) {
                    data3.moveToFirst();
                    while (data3.moveToNext()) {
                        Log.e("SYMBOLS", data3.getString(1));
                        listOfSymbols.add(data3.getString(1));
                    }
                    data3.close();
                }
                sendMessageToActivity(listOfSymbols, true);
            }

        }
    }

    private void sendMessageToActivity(List<String> msg, boolean finished) {
        Intent intent = new Intent("DB_created");
        Log.e("CRTService", "SendMessage1");
       // Bundle bundle = new Bundle();
       // bundle.putStringArrayList("listOfSymbols", (ArrayList<String>) msg);
        intent.putExtra("finishedCRTDB", finished);
        LocalBroadcastManager.getInstance(CreatingDatabaseService.this).sendBroadcast(intent);
    }


}