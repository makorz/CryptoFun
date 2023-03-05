package com.example.cryptofun.data;

import com.google.gson.annotations.SerializedName;

public class MarkPrice {
    @SerializedName("symbol")
    private String symbol;
    @SerializedName("indexPrice")
    private String indexPrice;
    @SerializedName("markPrice")
    private float markPrice;
    @SerializedName("estimatedSettlePrice")
    private float estimatedSettlePrice;
    @SerializedName("lastFundingRate")
    private float lastFundingRate;
    @SerializedName("nextFundingTime")
    private long nextFundingTime;
    @SerializedName("interestRate")
    private float interestRate;
    @SerializedName("time")
    private long time;

//    {
//        "symbol": "BTCUSDT",
//            "markPrice": "11793.63104562",  // mark price
//            "indexPrice": "11781.80495970", // index price
//            "estimatedSettlePrice": "11781.16138815", // Estimated Settle Price, only useful in the last hour before the settlement starts.
//            "lastFundingRate": "0.00038246",  // This is the lasted funding rate
//            "nextFundingTime": 1597392000000,
//            "interestRate": "0.00010000",
//            "time": 1597370495002
//    }


    public MarkPrice(String symbol, String indexPrice, float markPrice, float estimatedSettlePrice, float lastFundingRate, long nextFundingTime, float interestRate, long time) {
        this.symbol = symbol;
        this.indexPrice = indexPrice;
        this.markPrice = markPrice;
        this.estimatedSettlePrice = estimatedSettlePrice;
        this.lastFundingRate = lastFundingRate;
        this.nextFundingTime = nextFundingTime;
        this.interestRate = interestRate;
        this.time = time;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getIndexPrice() {
        return indexPrice;
    }

    public void setIndexPrice(String indexPrice) {
        this.indexPrice = indexPrice;
    }

    public float getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(float markPrice) {
        this.markPrice = markPrice;
    }

    public float getEstimatedSettlePrice() {
        return estimatedSettlePrice;
    }

    public void setEstimatedSettlePrice(float estimatedSettlePrice) {
        this.estimatedSettlePrice = estimatedSettlePrice;
    }

    public float getLastFundingRate() {
        return lastFundingRate;
    }

    public void setLastFundingRate(float lastFundingRate) {
        this.lastFundingRate = lastFundingRate;
    }

    public long getNextFundingTime() {
        return nextFundingTime;
    }

    public void setNextFundingTime(long nextFundingTime) {
        this.nextFundingTime = nextFundingTime;
    }

    public float getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(float interestRate) {
        this.interestRate = interestRate;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
