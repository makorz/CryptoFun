package app.makorz.cryptofun.data.database;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class rawTable_Kline {

//            1499040000000,      // Kline open time
//            "0.01634790",       // Open price
//            "0.80000000",       // High price
//            "0.01575800",       // Low price
//            "0.01577100",       // Close price
//            "148976.11427815",  // Volume
//            1499644799999,      // Kline close time
//            "2434.19055334",    // Quote asset volume
//            308,                // Number of trades
//            "1756.87402397",    // Taker buy base asset volume
//            "28.46694368",      // Taker buy quote asset volume
//            "0"                 // Unused field. Ignore.

    public String tokenSymbol;
    public long openTime;
    public float openPrice;
    public float highPrice;
    public float lowPrice;
    public float closePrice;
    public float volume;
    public long closeTime;
    public long numberOfTrades;
    public String klineInterval;

    public rawTable_Kline(String tokenSymbol, long openTime, float openPrice, float highPrice, float lowPrice, float closePrice, float volume, long closeTime, long numberOfTrades, String klineInterval) {
        this.tokenSymbol = tokenSymbol;
        this.openTime = openTime;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.closeTime = closeTime;
        this.numberOfTrades = numberOfTrades;
        this.klineInterval = klineInterval;
    }

    public String getKlineInterval() {
        return klineInterval;
    }

    public void setKlineInterval(String klineInterval) {
        this.klineInterval = klineInterval;
    }


    public String getTokenSymbol() {
        return tokenSymbol;
    }

    public void setTokenSymbol(String tokenSymbol) {
        this.tokenSymbol = tokenSymbol;
    }

    public long getOpenTime() {
        return openTime;
    }

    public void setOpenTime(long openTime) {
        this.openTime = openTime;
    }

    public float getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(float openPrice) {
        this.openPrice = openPrice;
    }

    public float getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(float highPrice) {
        this.highPrice = highPrice;
    }

    public float getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(float lowPrice) {
        this.lowPrice = lowPrice;
    }

    public float getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(float closePrice) {
        this.closePrice = closePrice;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    public long getNumberOfTrades() {
        return numberOfTrades;
    }

    public void setNumberOfTrades(long numberOfTrades) {
        this.numberOfTrades = numberOfTrades;
    }

    @NonNull
    @Override
    public String toString() {

        return "RawKline{symbol=" + tokenSymbol + " openTime=" + openTime + " openPrice=" + openPrice + " highPrice=" + highPrice
                + " lowPrice=" + lowPrice + " closePrice=" + closePrice + " volume=" + volume
                + " closePrice=" + closeTime + " nrOfTrades=" + numberOfTrades + '}';


    }
}
