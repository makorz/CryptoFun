package com.example.cryptofun.ui.view;

import java.io.Serializable;

public class GridViewElement implements Serializable {

    private String symbol;
    private float percent;
    private float value;

    public GridViewElement(String symbol, float percent, float value) {
        this.symbol = symbol;
        this.percent = percent;
        this.value = value;
    }


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
