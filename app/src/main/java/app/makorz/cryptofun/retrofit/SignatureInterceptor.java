package app.makorz.cryptofun.retrofit;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class SignatureInterceptor implements Interceptor {

    //0 -> balance 1 -> leverage, 2 marginType, 3 - marketOrder, 4 - stopLimitMarketOrder/takeProfitMarketOrder, 5 - cancelOrder 6 - orderLimit, 7 - deleteAllOrders 8 - getAllOrders AND getAccountInfo

    private static final String TAG = "SignatureInterceptor";

    private String apiKey;
    private String secretKey;
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
    private final long recvWindow;
    private long timestamp;

    // Constructor nr 0 -> Balance
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, long recvWindow) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.recvWindow = recvWindow;

    }

    // Constructor nr 1 -> Leverage
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, int leverage, long recvWindow, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.leverage = leverage;
        this.recvWindow = recvWindow;
        this.timestamp = timestamp;
    }

    // Constructor nr 2 -> MarginType
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, String marginType, long recvWindow, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.marginType = marginType;
        this.recvWindow = recvWindow;
        this.timestamp = timestamp;
    }

    // Constructor nr 3 -> MarketOrder
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, String side, String type, String newOrderRespType, String quantity, long recvWindow, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.newOrderRespType = newOrderRespType;
        this.quantity = quantity;
        this.recvWindow = recvWindow;
        this.timestamp = timestamp;
    }

    // Constructor nr 4 -> StopLimitTakeProfit
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, String side, String type, String newOrderRespType, String stopPrice, String closePosition, long recvWindow, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.newOrderRespType = newOrderRespType;
        this.stopPrice = stopPrice;
        this.closePosition = closePosition;
        this.recvWindow = recvWindow;
        this.timestamp = timestamp;
    }

    // Constructor nr 5 -> CancelOrder
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, long orderId, long recvWindow, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.orderId = orderId;
        this.recvWindow = recvWindow;
        this.timestamp = timestamp;
    }

    // Constructor nr 6 -> LimitOrder
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, String side, String type, String newOrderRespType, String timeInForce, String quantity, String price, long recvWindow, long timestamp) {
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
        this.recvWindow = recvWindow;
        this.timestamp = timestamp;
    }

    // Constructor nr 7 -> CancelAllOrders
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, String symbol, long recvWindow, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.symbol = symbol;
        this.recvWindow = recvWindow;
        this.timestamp = timestamp;
    }

    // Constructor nr 8 -> GetAllOrders AND getAccountInfo
    public SignatureInterceptor(String apiKey, String secretKey, int typeOfRetrofitClient, long recvWindow, long timestamp) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.typeOfRetrofitClient = typeOfRetrofitClient;
        this.recvWindow = recvWindow;
        this.timestamp = timestamp;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        HttpUrl.Builder urlBuilder = request.url().newBuilder();
        String message;

        switch (typeOfRetrofitClient) {
            case 0:
                timestamp = System.currentTimeMillis();
                urlBuilder.addQueryParameter("recvWindow", String.valueOf(recvWindow));
                urlBuilder.addQueryParameter("timestamp", String.valueOf(timestamp));
                message = "recvWindow=" + recvWindow + "&timestamp=" + timestamp;
                break;
            case 1:
                message = "symbol=" + symbol + "&leverage=" + leverage + "&recvWindow=" + recvWindow + "&timestamp=" + timestamp;
                break;
            case 2:
                message = "symbol=" + symbol + "&marginType=" + marginType + "&recvWindow=" + recvWindow + "&timestamp=" + timestamp;
                break;
            case 3:
                message = "symbol=" + symbol + "&side=" + side + "&type=" + type + "&newOrderRespType=" + newOrderRespType
                        + "&quantity=" + quantity + "&recvWindow=" + recvWindow + "&timestamp=" + timestamp;
                break;
            case 4:
                message = "symbol=" + symbol + "&side=" + side + "&type=" + type + "&newOrderRespType=" + newOrderRespType
                        + "&stopPrice=" + stopPrice +  "&closePosition=" + closePosition + "&recvWindow=" + recvWindow + "&timestamp=" + timestamp;
                break;
            case 5:
                message = "symbol=" + symbol + "&orderId=" + orderId + "&recvWindow=" + recvWindow + "&timestamp=" + timestamp;
                break;
            case 6:
                message = "symbol=" + symbol + "&side=" + side + "&type=" + type + "&newOrderRespType=" + newOrderRespType + "&timeInForce=" + timeInForce
                        + "&quantity=" + quantity + "&price=" + price + "&recvWindow=" + recvWindow + "&timestamp=" + timestamp;
                break;
            case 7:
                message = "symbol=" + symbol + "&recvWindow=" + recvWindow +  "&timestamp=" + timestamp;
                break;
            case 8:
                message = "recvWindow=" + recvWindow + "&timestamp=" + timestamp;
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

        //Log.e(TAG, "URL: " + newRequest.url());
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
