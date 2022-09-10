package com.example.cryptofun.data;

import io.reactivex.rxjava3.core.Observable;

public class KlineRequest {

    private Observable<?> request;
    private String symbol;
    private String interval;
    private String[][] dataOfSymbolInterval;

    public KlineRequest(Observable<?> request, String symbol, String interval) {
        this.request = request;
        this.symbol = symbol;
        this.interval = interval;
    }

    public KlineRequest(Observable<?> request, String symbol, String interval, String[][] dataOfSymbolInterval) {
        this.request = request;
        this.symbol = symbol;
        this.interval = interval;
        this.dataOfSymbolInterval = dataOfSymbolInterval;
    }

    public Observable<?> getRequest() {
        return request;
    }

    public void setRequest(Observable<?> request) {
        this.request = request;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String[][] getDataOfSymbolInterval() {
        return dataOfSymbolInterval;
    }

    public void setDataOfSymbolInterval(String[][] dataOfSymbolInterval) {
        this.dataOfSymbolInterval = dataOfSymbolInterval;
    }
}
