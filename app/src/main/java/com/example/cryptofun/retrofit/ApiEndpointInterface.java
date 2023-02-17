package com.example.cryptofun.retrofit;

import com.example.cryptofun.data.CoinSymbols;


import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface ApiEndpointInterface {

    String BASE_URL = "https://api.binance.com";
    String BASE_FUTURES_URL = "https://fapi.binance.com";

    //@Headers("X-MBX-APIKEY: " + "8aOPrhJRq4s7gOup6JUPtP3YRHqtnZLlczB40K913D9OEJJQtVOv5rRrq9btVCaX")
//    @GET("/api/v3/avgPrice")
//    Call<AveragePrice> getPriceAvg(@Query("symbol") String symbol);

    //@Headers("X-MBX-APIKEY: " + "8aOPrhJRq4s7gOup6JUPtP3YRHqtnZLlczB40K913D9OEJJQtVOv5rRrq9btVCaX")
    @GET("/api/v1/exchangeInfo")
    Call<CoinSymbols> getSymbols();

    //@Headers("X-MBX-APIKEY: " + "8aOPrhJRq4s7gOup6JUPtP3YRHqtnZLlczB40K913D9OEJJQtVOv5rRrq9btVCaX")
    @GET("/fapi/v1/exchangeInfo")
    Call<CoinSymbols> getFuturesSymbols();

    //@Headers("X-MBX-APIKEY: " + "8aOPrhJRq4s7gOup6JUPtP3YRHqtnZLlczB40K913D9OEJJQtVOv5rRrq9btVCaX")
//    @GET("/api/v3/uiKlines")
//    Observable<String[][]> getKlinesData(@Query("symbol") String symbol, @Query("limit") int limit, @Query("interval") String interval);

    @GET("/fapi/v1/klines")
    Observable<String[][]> getKlinesData(@Query("symbol") String symbol, @Query("limit") int limit, @Query("interval") String interval);

}
