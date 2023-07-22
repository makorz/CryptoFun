package com.example.cryptofun.retrofit;


import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.example.cryptofun.data.database.DBHandler;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClientSecretTestnet {


    //0 -> balance 1 -> leverage, 2 marginType, 3 - marketOrder, 4 - stopLimitMarketOrder/takeProfitMarketOrder, 5 - cancelOrder 6 - orderLimit, 7 - deleteAllOrders 8 - getAllOrders AND getAccountInfo

    private static final String TAG = "RetrofitClientSecretTestnet";

    private static final String ID = "id";
    private static final String TABLE_NAME_CONFIG = "config";

    private static RetrofitClientSecretTestnet instance = null;
    private final ApiEndpointInterface myApi;
    private final DBHandler databaseDB;
    //KEY for testnet, real key are inserte in app to database by user
    private String apikey = "a89c0eccaa652869edcc189767213f104649a568e3c5a29f49332a67941a872c";
    private String secret = "3867e2951c72a08544b62bd975e1c953c249e5b8c6f29b049315155dbc1f6da4";
    private long recvWindow = 30000;
    /*
        TODO:
            Pomyśleć czy nie dorzucić sprawdzenia czasu serwera binance, aby pominąć recvWindow, jest ono domyslnie na 5000

     */

    // Constructor nr 0 -> Balance
    private RetrofitClientSecretTestnet(Context context) {

        databaseDB = DBHandler.getInstance(context.getApplicationContext());
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        getData();
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        OkHttpClient client;

        if (secret.equals("null")) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } else {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new SignatureInterceptor(apikey,secret,0, recvWindow))
                    .build();

        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);
    }


    // Constructor nr 1 -> Leverage
    private RetrofitClientSecretTestnet(Context context, int typeOfRetrofitClient, String symbol, int leverage, long timestamp) {

        databaseDB = DBHandler.getInstance(context.getApplicationContext());
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret);
        getData();
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        OkHttpClient client;

        if (secret.equals("null")) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } else {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new SignatureInterceptor(apikey, secret, typeOfRetrofitClient, symbol, leverage, recvWindow, timestamp))
                    .build();
        }

       Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);
    }

    // Constructor nr 2 -> marginType
    private RetrofitClientSecretTestnet(Context context, int typeOfRetrofitClient, String symbol, String marginType, long timestamp) {

        databaseDB = DBHandler.getInstance(context.getApplicationContext());
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret);
        getData();
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        OkHttpClient client;

        if (secret.equals("null")) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } else {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new SignatureInterceptor(apikey, secret, typeOfRetrofitClient, symbol, marginType, recvWindow, timestamp))
                    .build();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);
    }

    // Constructor nr 3 -> MarketOrder
    private RetrofitClientSecretTestnet(Context context, String symbol, String side, String type, String newOrderRespType, String quantity, long timestamp) {

        databaseDB = DBHandler.getInstance(context.getApplicationContext());
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret);
        getData();
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        OkHttpClient client;

        if (secret.equals("null")) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } else {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new SignatureInterceptor(apikey,secret,3, symbol, side, type, newOrderRespType, quantity, recvWindow, timestamp))
                    .build();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);

    }

    // Constructor nr 4 -> StopLimitTakeProfit
    private RetrofitClientSecretTestnet(Context context, String symbol, String side, String type, String newOrderRespType, String stopLimit, String closePosition,long timestamp) {

        databaseDB = DBHandler.getInstance(context.getApplicationContext());
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret);
        getData();
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        OkHttpClient client;

        if (secret.equals("null")) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } else {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new SignatureInterceptor(apikey,secret, 4, symbol, side, type, newOrderRespType, stopLimit, closePosition,recvWindow, timestamp))
                    .build();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);

    }

    // Constructor nr 5 -> CancelOrder
    private RetrofitClientSecretTestnet(Context context, String symbol, long orderId, long timestamp) {

        databaseDB = DBHandler.getInstance(context.getApplicationContext());
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret);
        getData();
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        OkHttpClient client;

        if (secret.equals("null")) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } else {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new SignatureInterceptor(apikey, secret,5, symbol, orderId, recvWindow, timestamp))
                    .build();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);
    }

    // Constructor nr 6 -> LimitOrder
    private RetrofitClientSecretTestnet(Context context, String symbol, String side, String type, String newOrderRespType, String timeInForce, String quantity, String price, long timestamp) {

        databaseDB = DBHandler.getInstance(context.getApplicationContext());
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret);
        getData();
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        OkHttpClient client;

        if (secret.equals("null")) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } else {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new SignatureInterceptor(apikey,secret,6,symbol, side, type, newOrderRespType, timeInForce, quantity, price, recvWindow, timestamp))
                    .build();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);

    }

    // Constructor nr 7 -> CancelAllOrders
    private RetrofitClientSecretTestnet(Context context, String symbol, long timestamp) {

        databaseDB = DBHandler.getInstance(context.getApplicationContext());
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret);
        getData();
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        OkHttpClient client;

        if (secret.equals("null")) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } else {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new SignatureInterceptor(apikey,secret,7,symbol, recvWindow, timestamp))
                    .build();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);

    }

    // Constructor nr 8 -> AllOrders and getAccountInfo
    private RetrofitClientSecretTestnet(Context context, long timestamp) {

        databaseDB = DBHandler.getInstance(context.getApplicationContext());
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret);
        getData();
        Log.e(TAG, "API: " + apikey + ", SECRET:" + secret + ", WINDOW: " + recvWindow);
        OkHttpClient client;

        if (secret.equals("null")) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        } else {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(new SignatureInterceptor(apikey,secret,8, recvWindow, timestamp))
                    .build();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);

    }

    public static synchronized RetrofitClientSecretTestnet getInstance(Context context, int typeOfRetrofitClient, String symbol, int leverage, String marginType, String side, String type, String newOrderRespType, String quantity, String stopLimit, String closePosition, long orderId, String timeInForce, String price, long recvWindow, long timestamp) {

        //0 -> balance 1 -> leverage, 2 marginType, 3 - marketOrder, 4 - stopLimitMarketOrder/takeProfitMarketOrder, 5 - cancelOrder 6 - orderLimit
        switch (typeOfRetrofitClient) {
            case 0:
                instance = new RetrofitClientSecretTestnet(context);
                break;
            case 1:
                instance = new RetrofitClientSecretTestnet(context, typeOfRetrofitClient, symbol, leverage, timestamp);
                break;
            case 2:
                instance = new RetrofitClientSecretTestnet(context, typeOfRetrofitClient, symbol, marginType, timestamp);
                break;
            case 3:
                instance = new RetrofitClientSecretTestnet(context, symbol, side, type, newOrderRespType, quantity,  timestamp);
                break;
            case 4:
                instance = new RetrofitClientSecretTestnet(context, symbol, side, type, newOrderRespType, stopLimit, closePosition, timestamp);
                break;
            case 5:
                instance = new RetrofitClientSecretTestnet(context, symbol, orderId, timestamp);
                break;
            case 6:
                instance = new RetrofitClientSecretTestnet(context, symbol, side, type, newOrderRespType, timeInForce, quantity, price, timestamp);
                break;
            case 7:
                instance = new RetrofitClientSecretTestnet(context, symbol, timestamp);
                break;
            case 8:
                instance = new RetrofitClientSecretTestnet(context, timestamp);
                break;
            default:
                break;
        }


        return instance;
    }

    public void getData() {

        Cursor data = databaseDB.retrieveParam(4);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 4");
            databaseDB.addParam(4, "API-KEY", "null", 0, 0);
            apikey = "null";
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 4);
            databaseDB.addParam(4, "API-KEY", "null", 0, 0);
            apikey = "null";
        } else {
            data.moveToFirst();
            apikey = data.getString(2);
        }
        data.close();

        Cursor data2 = databaseDB.retrieveParam(5);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 5");
            databaseDB.addParam(5, "SECRET", "null", 0, 0);
            secret = "null";
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 5);
            databaseDB.addParam(5, "SECRET", "null", 0, 0);
            secret = "null";
        } else {
            data2.moveToFirst();
            secret = data2.getString(2);
        }
        data2.close();

        data = databaseDB.retrieveParam(14);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 14");
            databaseDB.addParam(14, "recvWindow", "", 1000, 0);
            recvWindow = 10000;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 14);
            databaseDB.addParam(14, "recvWindow", "", 1000, 0);
            recvWindow = 10000;
        } else {
            data.moveToFirst();
            recvWindow = data.getInt(3);
        }
        data.close();

    }

    public ApiEndpointInterface getMyApi() {
        return myApi;
    }

}
