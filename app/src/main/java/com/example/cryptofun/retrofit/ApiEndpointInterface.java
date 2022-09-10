package com.example.cryptofun.retrofit;

import com.example.cryptofun.data.AveragePrice;
import com.example.cryptofun.data.CoinSymbols;


import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiEndpointInterface {

    String BASE_URL = "https://api.binance.com";

    @GET("/api/v3/avgPrice")
    Call<AveragePrice> getPriceAvg(@Query("symbol") String symbol);

    @GET("/api/v1/exchangeInfo")
    Call<CoinSymbols> getSymbols();

    @GET("/api/v3/uiKlines")
    Observable<String[][]> getKlinesData(@Query("symbol") String symbol, @Query("limit") int limit, @Query("interval") String interval);

}
