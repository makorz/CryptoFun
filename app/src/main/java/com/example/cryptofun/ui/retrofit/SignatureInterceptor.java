package com.example.cryptofun.ui.retrofit;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class SignatureInterceptor implements Interceptor {

    private String apiKey;
    private String secretKey;
    //0 -> balance 1 -> leverage, 2 marginType, 3 - marketorder, 4 - stoplimitmarketorder/takeprofitmarketorder, 5 - cancelorder 6 - order limit
    private int typeOfRetrofitClient;
    private String symbol;
    private int leverage;
    private String marginType;
    private String side;
    private String type;
    private String newOrderRespType;
    private String timeInForce;
    String quantity;
    String price;
    String stopPrice;
    String closePosition;
    long orderId;
    private long recvWindow;
    private long timestamp;


    private static final String TAG = "SignatureInterceptor";

    // Constructor nr 0 -> Balance
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, long recvWindow) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.recvWindow = recvWindow;

    }

    // Constructor nr 1 -> Leverage
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, int leverage, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.leverage = leverage;
        this.timestamp = timestamp;
    }

    // Constructor nr 2 -> MarginType
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, String marginType, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.marginType = marginType;
        this.timestamp = timestamp;
    }

    // Constructor nr 3 -> MarketOrder
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, String side, String type, String newOrderRespType, String quantity, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.newOrderRespType = newOrderRespType;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    // Constructor nr 4 -> StopLimitTakeProfit
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, String side, String type, String newOrderRespType, String stopPrice, String closePosition, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.newOrderRespType = newOrderRespType;
        this.stopPrice = stopPrice;
        this.closePosition = closePosition;
        this.timestamp = timestamp;
    }

    // Constructor nr 5 -> CancelOrder
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, long orderId, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.orderId = orderId;
        this.timestamp = timestamp;
    }

    // Constructor nr 6 -> LimitOrder
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, String side, String type, String newOrderRespType, String timeInForce, String quantity, String price, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.newOrderRespType = newOrderRespType;
        this.timeInForce = timeInForce;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }

    // Constructor nr 7 -> CancelAllOrders
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.timestamp = timestamp;
    }

    // Constructor nr 7 -> CancelAllOrders
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, long timestamp, int openOrders) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        HttpUrl.Builder urlBuilder = request.url().newBuilder();
        String message = "";

        switch (typeOfRetrofitClient) {
            case 0:
                timestamp = System.currentTimeMillis();
                urlBuilder.addQueryParameter("recvWindow", String.valueOf(recvWindow));
                urlBuilder.addQueryParameter("timestamp", String.valueOf(timestamp));
                message = "recvWindow=" + recvWindow + "&timestamp=" + timestamp;
                break;
            case 1:
                message = "symbol=" + symbol + "&leverage=" + leverage + "&timestamp=" + timestamp;
                break;
            case 2:
                message = "symbol=" + symbol + "&marginType=" + marginType + "&timestamp=" + timestamp;
                break;
            case 3:
                message = "symbol=" + symbol + "&side=" + side + "&type=" + type + "&newOrderRespType=" + newOrderRespType
                        + "&quantity=" + quantity + "&timestamp=" + timestamp;
                break;
            case 4:
                message = "symbol=" + symbol + "&side=" + side + "&type=" + type + "&newOrderRespType=" + newOrderRespType
                        + "&stopPrice=" + stopPrice +  "&closePosition=" + closePosition + "&timestamp=" + timestamp;
                break;
            case 5:
                message = "symbol=" + symbol + "&orderId=" + orderId + "&timestamp=" + timestamp;
                break;
            case 6:
                message = "symbol=" + symbol + "&side=" + side + "&type=" + type + "&newOrderRespType=" + newOrderRespType + "&timeInForce=" + timeInForce
                        + "&quantity=" + quantity + "&price=" + price + "&timestamp=" + timestamp;
                break;
            case 7:
                message = "symbol=" + symbol + "&timestamp=" + timestamp;
                break;
            case 8:
                message = "timestamp=" + timestamp;
                break;
            default:
                message = "";
                break;
        }

        String signature = hmacSha256(secretKey, message);
        urlBuilder.addQueryParameter("signature", signature);
        Request newRequest = request.newBuilder()
                .header("Content-Type", "application/json")
                .header("X-MBX-APIKEY", apiKey)
                .url(urlBuilder.build())
                .build();

        Log.e(TAG, "URL: " + newRequest.url());
        return chain.proceed(newRequest);
    }

    private String hmacSha256(String key, String message) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            byte[] signatureBytes = hmacSha256.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
