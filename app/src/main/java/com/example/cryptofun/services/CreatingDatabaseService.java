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
import com.example.cryptofun.data.CryptoSymbolTickStep;
import com.example.cryptofun.data.FilterInfo;
import com.example.cryptofun.data.KlineRequest;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.data.database.rawTable_Kline;
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
                        Log.e(TAG, "SIZE ON START -> " + listOfSymbols.size() );

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

    private void sendMessageToActivity( ) {
        Intent intent = new Intent("DB_created");
        Log.e(TAG, "Finished Creating DB");
        intent.putExtra("finishedCRTDB", true);
        LocalBroadcastManager.getInstance(CreatingDatabaseService.this).sendBroadcast(intent);
        stopForeground(true);
        stopSelf();
    }

    @SuppressLint("CheckResult")
    private void getDataOfCryptoKlines() {

        if (databaseDB.retrieveAllFromTable(TABLE_NAME_KLINES_DATA).getCount() != 0) {
            databaseDB.clearTable(TABLE_NAME_KLINES_DATA);
        }

        List<rawTable_Kline> klinesDataList = new ArrayList<>();
        List<KlineRequest> request = new ArrayList<>();
        Log.e(TAG, "LIST OF SYMBOLS -> " + listOfSymbols);

        int LIMIT3m = 40;
        int LIMIT15m = 60;
        int LIMIT4h = 20;
        for (int i = 0; i < listOfSymbols.size(); i++) {
            // Make a collection of all requests you need to call at once, there can be any number of requests, not only 3. You can have 2 or 5, or 100.
            request.add(new KlineRequest(RetrofitClientFutures.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT15m, "15m"),
                    listOfSymbols.get(i), "15m"));
            request.add(new KlineRequest(RetrofitClientFutures.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT3m, "3m"),
                    listOfSymbols.get(i), "3m"));
            request.add(new KlineRequest(RetrofitClientFutures.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT4h, "4h"),
                    listOfSymbols.get(i), "4h"));

        }

        List<Observable<?>> observableRequestList = new ArrayList<>();
        Log.e(TAG, "Observable request size: " + request.size());
        Log.e(TAG, "Observable listOfCrypto size: " + listOfSymbols.size());
        for (int i = 0; i < request.size(); i++) {
            observableRequestList.add(request.get(i).getRequest());

        }

        Observable.zip(
                        observableRequestList,
                        new Function<Object[], Object>() {
                            @Override
                            public Object apply(Object[] objects) throws Exception {
                                // Objects[] is an array of combined results of completed requests
                                Log.e(TAG, " Observable ZIP size: " + objects.length);
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
                                Log.e(TAG, "Observable PERFECT");
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
                                Log.e(TAG, "Observable FUCKED " + e.toString());
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

                    List<CryptoSymbolTickStep> cryptoSymbolTickStep = new ArrayList<>();

                    for (int i = 0; i < tokenList.size(); i++) {

                        String tickSize = "";
                        String stepSize = "";

                        if (tokenList.get(i).getSymbol().contains("USDT")
                                && tokenList.get(i).getStatus().equals("TRADING")
                                && !tokenList.get(i).getSymbol().contains("BUSD")
                                && !tokenList.get(i).getSymbol().contains("_")
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

                            List<FilterInfo> filters = tokenList.get(i).getFilters();

                            // Iterate through the filters for each symbol
                            for (FilterInfo filter : filters) {
                                String filterType = filter.getFilterType();

                                // Check if the filter type is what you're interested in
                                if (filterType.equals("PRICE_FILTER")) {
                                    tickSize = removeTrailingZeros(filter.getTickSize());
                                } else if (filterType.equals("LOT_SIZE")) {
                                    stepSize = removeTrailingZeros(filter.getStepSize());
                                // Do something with the stepSize, minQty, and maxQty data for this symbol
                                }
                            }

                            cryptoSymbolTickStep.add(new CryptoSymbolTickStep(tickSize, stepSize, tokenList.get(i).getSymbol()));

                        }

                        if (tokenList.get(i).getSymbol().contains("BUSD")
                                && tokenList.get(i).getStatus().equals("TRADING")
                                && !tokenList.get(i).getSymbol().contains("USDT")
                                && !tokenList.get(i).getSymbol().contains("_")
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

//                            arrayBUSDtemp.add(tokenList.get(i).getSymbol());

                            List<FilterInfo> filters = tokenList.get(i).getFilters();

                            // Iterate through the filters for each symbol
                            for (FilterInfo filter : filters) {
                                String filterType = filter.getFilterType();

                                // Check if the filter type is what you're interested in
                                if (filterType.equals("PRICE_FILTER")) {
                                    tickSize = removeTrailingZeros(filter.getTickSize());
                                } else if (filterType.equals("LOT_SIZE")) {
                                    stepSize = removeTrailingZeros(filter.getStepSize());
                                    // Do something with the stepSize, minQty, and maxQty data for this symbol
                                }
                            }

                            cryptoSymbolTickStep.add(new CryptoSymbolTickStep(tickSize, stepSize, tokenList.get(i).getSymbol()));

                        }

                    }

                    ArrayList<CryptoSymbolTickStep> objectsToRemove = new ArrayList<>();
                    Log.e(TAG, "List size before cut: " +  cryptoSymbolTickStep.size());

                    for (CryptoSymbolTickStep cryptoSymbol : cryptoSymbolTickStep) {

                        String symbol = cryptoSymbol.getSymbol();
                        if (symbol.endsWith("BUSD")) {

                            String symbolWithoutSuffix = symbol.substring(0, symbol.length() - 4); // Remove "BUSD" suffix

                            // Check if there is a corresponding object with "BUSDT" suffix
                            boolean hasCorrespondingObject = cryptoSymbolTickStep.stream()
                                    .anyMatch(crypto -> crypto.getSymbol().equals(symbolWithoutSuffix + "USDT"));

                            // If there is a corresponding object, add the current object to the objectsToRemove list
                            if (hasCorrespondingObject) {
                                objectsToRemove.add(cryptoSymbol);
                            }
                        }
                    }

                    cryptoSymbolTickStep.removeAll(objectsToRemove);

                    Log.e(TAG, "List size after cut: " +  cryptoSymbolTickStep.size());

//                    for (int i = 0; i < cryptoSymbolTickStep.size(); i++) {
//                        Log.e(TAG, "2 " + cryptoSymbolTickStep.get(i).getSymbol() + " " + cryptoSymbolTickStep.get(i).getStepSize() + " " + cryptoSymbolTickStep.get(i).getTickSize());
//                    }

                    for (int i = 0; i < cryptoSymbolTickStep.size(); i++) {
                        listOfSymbols.add(cryptoSymbolTickStep.get(i).getSymbol());
                    }

                    Log.e(TAG, listOfSymbols.toString());

                    databaseDB.addNewCrypto(cryptoSymbolTickStep);
                    getDataOfCryptoFromAPI();
                }

                @Override
                public void onFailure(@NonNull Call<CoinSymbols> call, @NonNull Throwable t) {
                    Log.e(TAG, "An error has occurred: " + t);
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
                        listOfSymbols.add(data3.getString(1));
                    }
                }
                data3.close();
                sendMessageToActivity();
            }

        }
    }

    public static String removeTrailingZeros(String decimalString) {
        if (!decimalString.contains(".")) {
            // Not a decimal number, return as is
            return decimalString;
        }

        // Remove trailing zeros
        decimalString = decimalString.replaceAll("0*$", "");

        // Remove decimal point if there are no digits after it
        decimalString = decimalString.replaceAll("\\.$", "");

        return decimalString;
    }




}