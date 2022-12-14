package com.example.cryptofun.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    public Kline(String tTokenSymbol, float tVolume, long tNumberOfTrades, long tCloseTime) {
        this.tTokenSymbol = tTokenSymbol;
        this.tVolume = tVolume;
        this.tNumberOfTrades = tNumberOfTrades;
        this.tCloseTime = tCloseTime;

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
}
