package com.example.cryptofun.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class CoinSymbol {

    @SerializedName("symbol")
    private String symbol;
    @SerializedName("status")
    private String status;
    private ArrayList<String> permissions;
    private boolean isMarginTradingAllowed;
    private ArrayList<FilterInfo> filters;

    public ArrayList<FilterInfo> getFilters() {
        return filters;
    }

    public void setFilters(ArrayList<FilterInfo> filters) {
        this.filters = filters;
    }

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

    public ArrayList<String> getPermissions() {
        return permissions;
    }

    public boolean isMarginTradingAllowed() {
        return isMarginTradingAllowed;
    }

    public void setMarginTradingAllowed(boolean marginTradingAllowed) {
        isMarginTradingAllowed = marginTradingAllowed;
    }

    public void setPermissions(ArrayList<String> permissions) {
        this.permissions = permissions;
    }
}
