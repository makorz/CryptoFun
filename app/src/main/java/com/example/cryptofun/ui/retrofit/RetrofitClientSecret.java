package com.example.cryptofun.ui.retrofit;


import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.example.cryptofun.data.database.DBHandler;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClientSecret {

    private static final String TAG = "RetrofitClientSecret";

    private static final String ID = "id";
    private static final String TABLE_NAME_CONFIG = "config";

    private static RetrofitClientSecret instance = null;
    private final ApiEndpointInterface myApi;
    private final DBHandler databaseDB;
    private String apikey = "null";
    private String secret = "null";
    private int recvWindow = 1000;

    private RetrofitClientSecret(Context context) {

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
        Log.e(TAG, myApi.toString());
    }

    public static synchronized RetrofitClientSecret getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClientSecret(context);
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
            recvWindow = 1000;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 14);
            databaseDB.addParam(14, "recvWindow", "", 1000, 0);
            recvWindow = 1000;
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
