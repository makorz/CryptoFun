package com.example.cryptofun.ui.view;

import android.util.Log;

import java.io.Serializable;

public class OrderListViewElement implements Serializable, Comparable<OrderListViewElement> {

    private String symbol;
    private boolean isItReal;
    private float entryAmount;
    private float entryPrice;
    private float currentPrice;
    private float stopLimitPrice;
    private float takeProfitPrice;
    private float amountRightNow;
    private String timeWhenPlaced;
    private int margin;

    public OrderListViewElement(String symbol, boolean isItReal, float entryAmount, float entryPrice, float currentPrice, float stopLimitPrice, float takeProfitPrice, float amountRightNow, String timeWhenPlaced, int margin) {
        this.symbol = symbol;
        this.isItReal = isItReal;
        this.entryAmount = entryAmount;
        this.entryPrice = entryPrice;
        this.currentPrice = currentPrice;
        this.stopLimitPrice = stopLimitPrice;
        this.takeProfitPrice = takeProfitPrice;
        this.amountRightNow = amountRightNow;
        this.timeWhenPlaced = timeWhenPlaced;
        this.margin = margin;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public boolean isItReal() {
        return isItReal;
    }

    public void setItReal(boolean itReal) {
        this.isItReal = itReal;
    }

    public float getEntryAmount() {
        return entryAmount;
    }

    public void setEntryAmount(float entryAmount) {
        this.entryAmount = entryAmount;
    }

    public float getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(float entryPrice) {
        this.entryPrice = entryPrice;
    }

    public float getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(float currentPrice) {
        this.currentPrice = currentPrice;
    }

    public float getStopLimitPrice() {
        return stopLimitPrice;
    }

    public void setStopLimitPrice(float stopLimitPrice) {
        this.stopLimitPrice = stopLimitPrice;
    }

    public float getTakeProfitPrice() {
        return takeProfitPrice;
    }

    public void setTakeProfitPrice(float takeProfitPrice) {
        this.takeProfitPrice = takeProfitPrice;
    }

    public float getAmountRightNow() {
        return amountRightNow;
    }

    public void setAmountRightNow(float amountRightNow) {
        this.amountRightNow = amountRightNow;
    }

    public float getPercentOfPriceChange() {
        return ((getCurrentPrice() / getEntryPrice()) * 100) - 100;
    }

    public float getPercentOfAmountChange() {
        return ((getCurrentAmount() / getEntryAmount()) * 100) - 100;
    }

    public float getCurrentAmount() {

        float marg = (float) this.margin;
        Log.e("ORDER1", String.valueOf(getPercentOfPriceChange()));
        Log.e("ORDER12", String.valueOf(marg));
        Log.e("ORDER13", String.valueOf(this.entryAmount));

        Log.e("ORDER14", String.valueOf((marg * this.entryAmount)));
        Log.e("ORDER15", String.valueOf((marg * this.entryAmount) * (getPercentOfPriceChange() / 100)));
        Log.e("ORDER16", String.valueOf(this.entryAmount * (marg - 1)));
        Log.e("ORDER17", String.valueOf((this.entryAmount * marg * 0.0003f * 2f)));

        float result = (marg * this.entryAmount) + (marg * this.entryAmount) * (getPercentOfPriceChange() / 100) - this.entryAmount * (marg - 1) - (this.entryAmount * marg * 0.0003f * 2f);
        Log.e("ORDER2", String.valueOf(result));
        return result;

    }

    public String getTimeWhenPlaced() {
        return timeWhenPlaced;
    }

    public void setTimeWhenPlaced(String timeWhenPlaced) {
        this.timeWhenPlaced = timeWhenPlaced;
    }

    @Override
    public int compareTo(OrderListViewElement element) {
        return this.getTimeWhenPlaced().compareTo(element.getTimeWhenPlaced());
    }

}

