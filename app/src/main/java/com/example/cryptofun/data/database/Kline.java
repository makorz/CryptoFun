package com.example.cryptofun.data.database;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

@Entity(tableName = "kline_data")
public class Kline {

    @NonNull
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "klineId")
    public int tId;
    @ColumnInfo(name = "cryptoSymbol")
    public String tTokenSymbol;
    @ColumnInfo(name = "openTime")
    public long tOpenTime;
    @ColumnInfo(name = "openPrice")
    public float tOpenPrice;
    @ColumnInfo(name = "highPrice")
    public float tHighPrice;
    @ColumnInfo(name = "lowPrice")
    public float tLowPrice;
    @ColumnInfo(name = "closePrice")
    public float tClosePrice;
    @ColumnInfo(name = "volume")
    public float tVolume;
    @ColumnInfo(name = "closeTime")
    public long tCloseTime;
    @ColumnInfo(name = "numberOfTrades")
    public long tNumberOfTrades;
    @ColumnInfo(name = "klineInterval")
    public String tKlineInterval;

    public Kline(int tId, String tTokenSymbol, long tOpenTime, float tOpenPrice, float tHighPrice, float tLowPrice, float tClosePrice, float tVolume, long tCloseTime, long tNumberOfTrades, String tKlineInterval) {
        this.tId = tId;
        this.tTokenSymbol = tTokenSymbol;
        this.tOpenTime = tOpenTime;
        this.tOpenPrice = tOpenPrice;
        this.tHighPrice = tHighPrice;
        this.tLowPrice = tLowPrice;
        this.tClosePrice = tClosePrice;
        this.tVolume = tVolume;
        this.tCloseTime = tCloseTime;
        this.tNumberOfTrades = tNumberOfTrades;
        this.tKlineInterval = tKlineInterval;
    }

    public int getStatusOfKline() {
        float redOrGreen = this.tOpenPrice - this.tClosePrice;
        if ( redOrGreen < 0 ) {
            return 1;
        } else if (redOrGreen > 0){
            return 0;
        } else {
            return 2;
        }
    }

    public int gettId() {
        return tId;
    }

    public String gettTokenSymbol() {
        return tTokenSymbol;
    }

    public long gettOpenTime() {
        return tOpenTime;
    }

    public float gettOpenPrice() {
        return tOpenPrice;
    }

    public float gettHighPrice() {
        return tHighPrice;
    }

    public float gettLowPrice() {
        return tLowPrice;
    }

    public float gettClosePrice() {
        return tClosePrice;
    }

    public float gettVolume() {
        return tVolume;
    }

    public long gettCloseTime() {
        return tCloseTime;
    }

    public long gettNumberOfTrades() {
        return tNumberOfTrades;
    }

    public String gettKlineInterval() {
        return tKlineInterval;
    }

    @NonNull
    @Override
    public String toString() {

        @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        return "Kline{symbol=" + tTokenSymbol + " openTime=" + df3.format(tOpenTime) + " openPrice=" + tOpenPrice + " highPrice=" + tHighPrice
                + " lowPrice=" + tLowPrice + " closePrice=" + tClosePrice + " volume=" + tVolume
                + " closePrice=" + tCloseTime + " nrOfTrades=" + tNumberOfTrades + '}';
    }

}
