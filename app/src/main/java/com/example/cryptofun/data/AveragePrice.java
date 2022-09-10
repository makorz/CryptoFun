package com.example.cryptofun.data;

import com.google.gson.annotations.SerializedName;

public class AveragePrice {


    @SerializedName("price")
    private String priceAvg;

    public AveragePrice(String price) {
        this.priceAvg = price;
    }

    public String getPriceAvg() {
        return priceAvg;
    }
}
