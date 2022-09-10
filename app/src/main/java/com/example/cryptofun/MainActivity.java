package com.example.cryptofun;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.cryptofun.data.CoinSymbol;
import com.example.cryptofun.data.CoinSymbols;
import com.example.cryptofun.data.KlineRequest;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.database.rawTable_Kline;
import com.example.cryptofun.retrofit.RetrofitClient;
import com.example.cryptofun.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.cryptofun.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavHostFragment navHostFragment;
    private NavController navController;
    private final List<String> listOfUSDTCryptos = new ArrayList<>();
    private final List<String> listOfSymbols = new ArrayList<>();

    private final int LIMIT_OF_15M = 3;
    private final int LIMIT_OF_3M = 6;
    private final int LIMIT_OF_1D = 2;

    private Observable<String[][]> myObservable;
    private Observer<String[][]> myObserver;

    private DBHandler databaseDB;

    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        databaseDB = new DBHandler(this);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        getDataOfCryptoFromAPI();
    }

    @SuppressLint("SetTextI18n")
    public void getDataOfCryptoFromAPI() {

        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        if (data.getCount() == 0) {
            Toast.makeText(this, "Symbols are empty...", Toast.LENGTH_LONG).show();
            getSymbolsList();
        } else {
            data.moveToFirst();
            Cursor data2 = databaseDB.retrieveAllFromTable(TABLE_NAME_KLINES_DATA);
            if (data2.getCount() == 0) {
                Toast.makeText(this, "No data for gridView", Toast.LENGTH_LONG).show();
                getDataOfCryptoKlines();
            } else {
                Cursor data3 = databaseDB.retrieveAllFromTable(TABLE_SYMBOL_AVG);
                if (data3.getCount() != 0) {
                    data3.moveToFirst();
                    while (data3.moveToNext()) {
                        Log.e("SYMBOLS", data3.getString(1));
                        listOfSymbols.add(data3.getString(1));
                    }
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList("listOfSymbols", (ArrayList<String>) listOfSymbols);
                    Log.e("MAINNN", String.valueOf(bundle.getStringArrayList("listOfSymbols")));
                    navController.navigate(R.id.navigation_home, bundle);

                }

            }

        }
    }

    private void getDataOfCryptoKlines() {
        Log.e("SIZE:", " " + listOfSymbols.size());

        if (databaseDB.retrieveAllFromTable(TABLE_NAME_KLINES_DATA).getCount() != 0) {
            clearTableFromDB(TABLE_NAME_KLINES_DATA);
        }

        List<rawTable_Kline> klinesDataList = new ArrayList<>();
        List<KlineRequest> request = new ArrayList<>();

        for (int i = 0; i < listOfSymbols.size(); i++) {
            // Make a collection of all requests you need to call at once, there can be any number of requests, not only 3. You can have 2 or 5, or 100.
            request.add(new KlineRequest(RetrofitClient.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT_OF_15M, "15m"),
                    listOfSymbols.get(i), "15m"));
            request.add(new KlineRequest(RetrofitClient.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT_OF_3M, "3m"),
                    listOfSymbols.get(i), "3m"));
            request.add(new KlineRequest(RetrofitClient.getInstance().getMyApi().getKlinesData(listOfSymbols.get(i), LIMIT_OF_1D, "1d"),
                    listOfSymbols.get(i), "1d"));

        }
        List<Observable<?>> observableRequestList = new ArrayList<>();
        for (int i = 0; i < request.size(); i++) {
            observableRequestList.add(request.get(i).getRequest());
        }

        Observable.zip(
                        observableRequestList,
                        new Function<Object[], Object>() {
                            @Override
                            public Object apply(Object[] objects) throws Exception {
                                // Objects[] is an array of combined results of completed requests
                                Log.e("ZIPPPP", "OBJECTS NUMBER:  " + objects.length);
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
                                Log.e("ZIPPPP", " PERFECT");
                                for (int i = 0; i < request.size(); i++) {
                                    String[][] aaa = request.get(i).getDataOfSymbolInterval();
                                    String symbol = request.get(i).getSymbol();
                                    for (int j = 0; j < aaa.length; j++) {

                                        String interval = "";
                                        if (aaa.length == LIMIT_OF_3M) {
                                            interval = "3m";
                                        } else if (aaa.length == LIMIT_OF_15M) {
                                            interval = "15m";
                                        } else if (aaa.length == LIMIT_OF_1D) {
                                            interval = "1d";
                                        }

                                        String bbb = aaa[j][1] + "   " + aaa[j][2];
                                        Log.e("ZIPPPP", symbol + "    " + bbb);
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
                                Log.e("ZIPPPP", " FINITO");
                                for (int i = 0; i < klinesDataList.size(); i++) {
                                    if (klinesDataList.get(i).getTokenSymbol() == null) {
                                        klinesDataList.remove(i);
                                    }
                                }

                                if (databaseDB.addNewKlineData(klinesDataList) > 5) {

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
                                Log.e("ZIPPPP", " BAD " + e.toString());
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
                    Log.e("SYMBOLS", "" + response.body());
                    CoinSymbols symbols = response.body();
                    List<CoinSymbol> tokenList = symbols.getSymbols();

                    if (listOfSymbols.size() > 0) {
                        listOfSymbols.clear();
                    }
                    for (int i = 0; i < tokenList.size(); i++) {

                        if (tokenList.get(i).getSymbol().contains("USDT") && !tokenList.get(i).getSymbol().contains("UP")
                                && !tokenList.get(i).getSymbol().contains("DOWN") && tokenList.get(i).getStatus().equals("TRADING")) {
                            listOfSymbols.add(tokenList.get(i).getSymbol());
                        }
                    }
                    databaseDB.addNewCrypto(listOfSymbols);
                    Log.e("SYMBOLS22", "" + listOfSymbols.size());
                    getDataOfCryptoFromAPI();
                }

                @Override
                public void onFailure(Call<CoinSymbols> call, Throwable t) {
                    System.out.println("An error has occured" + t);
                }

            });
        }
    }

    private void clearTableFromDB(String tableName) {
        Cursor data = databaseDB.retrieveAllFromTable(tableName);
        if (data.getCount() == 0) {
            Toast.makeText(this, "Table is empty...", Toast.LENGTH_LONG).show();
        } else {
            databaseDB.clearTable(tableName);
        }
    }

}