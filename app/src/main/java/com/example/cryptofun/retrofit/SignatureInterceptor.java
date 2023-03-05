package com.example.cryptofun.retrofit;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cryptofun.database.DBHandler;

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

    private String apiKey;
    private String secretKey;
    private int recvWindow;
    private static final String TAG = "SignatureInterceptor";

    public SignatureInterceptor(String apiKey, String secretKey, int recvWindow) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.recvWindow = recvWindow;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long timestamp = System.currentTimeMillis();

        HttpUrl.Builder urlBuilder = request.url().newBuilder();
        urlBuilder.addQueryParameter("recvWindow", String.valueOf(recvWindow));
        urlBuilder.addQueryParameter("timestamp", String.valueOf(timestamp));

        String message = "recvWindow=" + recvWindow + "&timestamp=" + timestamp;

        Log.e("Retrofit", message);
        String signature = hmacSha256(secretKey, message);

        Log.e("Retrofit", signature + " " + urlBuilder.build());

        urlBuilder.addQueryParameter("signature", signature);

        Request newRequest = request.newBuilder()
                .header("Content-Type", "application/json")
                .header("X-MBX-APIKEY", apiKey)
                .url(urlBuilder.build())
                .build();

        Log.e(TAG, signature + " " + urlBuilder.build());

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
