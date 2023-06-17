package com.example.cryptofun.ui.retrofit;

import com.example.cryptofun.data.AccountBalance;
import com.example.cryptofun.data.CoinSymbols;
import com.example.cryptofun.data.Leverage;
import com.example.cryptofun.data.MarkPrice;
import com.example.cryptofun.data.RealOrder;
import com.example.cryptofun.data.ResponseMargin;
import com.google.gson.JsonObject;


import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiEndpointInterface {

    String BASE_URL = "https://api.binance.com";
    String BASE_FUTURES_URL = "https://fapi.binance.com";
    String TESTNET = "https://testnet.binancefuture.com";

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
//    Observable<RealOrder> setRealOrder(@Query("symbol") String symbol, String side, String positionSide);
//
//    @GET("/fapi/v1/openOrder")

    @POST("/fapi/v1/leverage")
    Call<Leverage> setLeverage(@Query("symbol") String symbol, @Query("leverage") int leverage, @Query("timestamp") long timestamp);

    @POST("/fapi/v1/marginType")
    Call<ResponseMargin> setMarginType(@Query("symbol") String symbol, @Query("marginType") String marginType, @Query("timestamp") long timestamp);

    @POST("/fapi/v1/order")
    Call<RealOrder> setLimitOrder(@Query("symbol") String symbol, @Query("side") String side, @Query("type") String type, @Query("newOrderRespType") String newOrderRespType
            ,@Query("timeInForce") String timeInForce, @Query("quantity") String quantity, @Query("price") String price, @Query("timestamp") long timestamp );

    @POST("/fapi/v1/order")
    Call<RealOrder> setMarketOrder(@Query("symbol") String symbol, @Query("side") String side, @Query("type") String type, @Query("newOrderRespType") String newOrderRespType
            , @Query("quantity") String quantity, @Query("timestamp") long timestamp );

    @POST("/fapi/v1/order")
    Call<RealOrder> setStopLimitOrTakeProfitMarket(@Query("symbol") String symbol, @Query("side") String side, @Query("type") String type, @Query("newOrderRespType") String newOrderRespType
            , @Query("stopPrice") String stopPrice, @Query("closePosition") String closePosition, @Query("timestamp") long timestamp);

    @DELETE("/fapi/v1/order")
    Call<RealOrder> deleteOrder(@Query("symbol") String symbol, @Query("orderId") long orderId, @Query("timestamp") long timestamp);

    @DELETE("/fapi/v1/allOpenOrders")
    Call<RealOrder> deleteAllOrders(@Query("symbol") String symbol, @Query("timestamp") long timestamp);

    @GET("/fapi/v1/openOrders")
    Call<List<JsonObject>> getAllOrders(@Query("timestamp") long timestamp);

    @GET("/fapi/v2/balance")
    Call<List<AccountBalance>> getAccountBalance();




}
