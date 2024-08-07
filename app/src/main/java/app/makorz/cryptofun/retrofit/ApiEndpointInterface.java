package app.makorz.cryptofun.retrofit;

import app.makorz.cryptofun.data.AccountBalance;
import app.makorz.cryptofun.data.AccountInfo;
import app.makorz.cryptofun.data.CoinSymbols;
import app.makorz.cryptofun.data.Leverage;
import app.makorz.cryptofun.data.MarkPrice;
import app.makorz.cryptofun.data.PositionRisk;
import app.makorz.cryptofun.data.RealOrder;
import app.makorz.cryptofun.data.ResponseMargin;
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

    @GET("/fapi/v1/premiumIndex")
    Observable<MarkPrice> getMarkPrice(@Query("symbol") String symbol);

    @GET("/fapi/v1/klines")
    Observable<String[][]> getKlinesData(@Query("symbol") String symbol, @Query("limit") int limit, @Query("interval") String interval);



    @POST("/fapi/v1/leverage")
    Call<Leverage> setLeverage(@Query("symbol") String symbol, @Query("leverage") int leverage, @Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp);

    @POST("/fapi/v1/marginType")
    Call<ResponseMargin> setMarginType(@Query("symbol") String symbol, @Query("marginType") String marginType, @Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp);

    @POST("/fapi/v1/order")
    Call<RealOrder> setLimitOrder(@Query("symbol") String symbol, @Query("side") String side, @Query("type") String type, @Query("newOrderRespType") String newOrderRespType
            , @Query("timeInForce") String timeInForce, @Query("quantity") String quantity, @Query("price") String price, @Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp );

    @POST("/fapi/v1/order")
    Call<RealOrder> setMarketOrder(@Query("symbol") String symbol, @Query("side") String side, @Query("type") String type, @Query("newOrderRespType") String newOrderRespType
            , @Query("quantity") String quantity, @Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp );

    @POST("/fapi/v1/order")
    Call<RealOrder> setStopLimitOrTakeProfitMarket(@Query("symbol") String symbol, @Query("side") String side, @Query("type") String type, @Query("newOrderRespType") String newOrderRespType
            , @Query("stopPrice") String stopPrice, @Query("closePosition") String closePosition, @Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp);

    @DELETE("/fapi/v1/order")
    Call<RealOrder> deleteOrder(@Query("symbol") String symbol, @Query("orderId") long orderId, @Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp);

    @DELETE("/fapi/v1/allOpenOrders")
    Call<RealOrder> deleteAllOrders(@Query("symbol") String symbol, @Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp);

    @GET("/fapi/v2/positionRisk")
    Call<List<PositionRisk>> positionsInfo(@Query("symbol") String symbol, @Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp);

    @GET("/fapi/v1/openOrders")
    Call<List<JsonObject>> getAllOrders(@Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp);

    @GET("/fapi/v2/account")
    Call<AccountInfo> getAccountInfo(@Query("recvWindow") long recvWindow, @Query("timestamp") long timestamp);

    @GET("/fapi/v2/balance")
    Call<List<AccountBalance>> getAccountBalance();




}
