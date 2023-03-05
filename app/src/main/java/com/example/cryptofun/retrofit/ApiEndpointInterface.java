package com.example.cryptofun.retrofit;

import com.example.cryptofun.data.AccountBalance;
import com.example.cryptofun.data.CoinSymbols;
import com.example.cryptofun.data.MarkPrice;


import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiEndpointInterface {

    String BASE_URL = "https://api.binance.com";
    String BASE_FUTURES_URL = "https://fapi.binance.com";

//    @GET("/api/v3/avgPrice")
//    Call<AveragePrice> getPriceAvg(@Query("symbol") String symbol);

//    @GET("/api/v3/uiKlines")
//    Observable<String[][]> getKlinesData(@Query("symbol") String symbol, @Query("limit") int limit, @Query("interval") String interval);

//    @GET("/api/v1/exchangeInfo")
//    Call<CoinSymbols> getSymbols();

    @GET("/fapi/v1/exchangeInfo")
    Call<CoinSymbols> getFuturesSymbols();

//    @GET("/fapi/v1/premiumIndex")
//    Call<MarkPrice> getMarkPrice(@Query("symbol") String symbol);

    @GET("/fapi/v1/premiumIndex")
    Observable<MarkPrice> getMarkPrice(@Query("symbol") String symbol);

    @GET("/fapi/v1/klines")
    Observable<String[][]> getKlinesData(@Query("symbol") String symbol, @Query("limit") int limit, @Query("interval") String interval);

//    @DELETE("/fapi/v1/order")
//
//    @POST("/fapi/v1/order")
//
//    @GET("/fapi/v1/openOrder")

    @GET("/fapi/v2/balance")
    Call<List<AccountBalance>> getAccountBalance();




}
