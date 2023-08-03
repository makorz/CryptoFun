package com.example.cryptofun.retrofit;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClientFutures {

    private static RetrofitClientFutures instance = null;
    private final ApiEndpointInterface myApi;
    private static final String TAG = "RetrofitClientFutures";

    private RetrofitClientFutures() {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30,TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
//                .pingInterval(3, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

       // Log.e(TAG, "URL: " + retrofit.baseUrl() + " " + retrofit);
        myApi = retrofit.create(ApiEndpointInterface.class);
    }

    public static synchronized RetrofitClientFutures getInstance() {
        if (instance == null) {
            instance = new RetrofitClientFutures();
        }
        return instance;
    }

    public ApiEndpointInterface getMyApi() {
        return myApi;
    }
}