package com.example.cryptofun.retrofit;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient2 {

    private static RetrofitClient2 instance = null;
    private ApiEndpointInterface myApi;

    private RetrofitClient2() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);
    }

    public static synchronized RetrofitClient2 getInstance() {
        if (instance == null) {
            instance = new RetrofitClient2();
        }
        return instance;
    }

    public ApiEndpointInterface getMyApi() {
        return myApi;
    }
}