package com.example.cryptofun.retrofit;


import android.util.Log;


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClientSecret {

    private static final String TAG = "RetrofitSecret";

    private static RetrofitClientSecret instance = null;
    private ApiEndpointInterface myApi;
    private String apikey = "1JcC2JSSv8nqzlfKlOeBt3bjs45DWEjfq73KmYK3Yik7kbsQHjMcCrFKo1Msyy02";
    private String secret = "aA5JCFOmW7YkzmwS5QENc7s87FXiQ1scjgELYi0Qg88Iaa8P2rGVnlHJOvzzkZrk";
    private int recvWindow = 10000;

    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(new SignatureInterceptor(apikey,secret,recvWindow))
            .build();

    private RetrofitClientSecret() {
       Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiEndpointInterface.BASE_FUTURES_URL)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        myApi = retrofit.create(ApiEndpointInterface.class);
        Log.e(TAG, myApi.toString());
    }

    public static synchronized RetrofitClientSecret getInstance() {
        if (instance == null) {
            instance = new RetrofitClientSecret();
        }
        return instance;
    }

    public ApiEndpointInterface getMyApi() {
        return myApi;
    }

}
