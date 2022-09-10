package com.example.cryptofun.data;

import com.google.gson.annotations.SerializedName;

public class CoinSymbol {

    @SerializedName("symbol")
    private String symbol;
    @SerializedName("status")
    private String status;

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public CoinSymbol(String name) {
        this.symbol = name;
    }

    public String getSymbol() {
        return symbol;
    }
}
