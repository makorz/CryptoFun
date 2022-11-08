package com.example.cryptofun.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CoinSymbols {

    @SerializedName("symbols")
    private List<CoinSymbol> symbols = null;

    public CoinSymbols(List<CoinSymbol> name) {
        this.symbols = name;
    }

    public List<CoinSymbol> getSymbols() {
        return symbols;
    }
}
