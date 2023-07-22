package com.example.cryptofun.ui.orders;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

public class OrderListViewElement implements Serializable, Comparable<OrderListViewElement> {

    private String symbol;
    private int isItReal;
    private float entryAmount;
    private float entryPrice;
    private float currentPrice;
    private float stopLimitPrice;
    private float takeProfitPrice;
    private long timeWhenPlaced;
    private int margin;
    private int isItShort;
    private int isItCrossed;
    private int accountNumber;
    private long orderID;
    private String orderType;
    private float quantity;

    public OrderListViewElement(String symbol, int isItReal, float entryAmount, float entryPrice, float currentPrice, float stopLimitPrice, float takeProfitPrice, long timeWhenPlaced, int margin, int isItShort, int isItCrossed, int accountNumber, long orderID, String orderType, float quantity) {
        this.symbol = symbol;
        this.isItReal = isItReal;
        this.entryAmount = entryAmount;
        this.entryPrice = entryPrice;
        this.currentPrice = currentPrice;
        this.stopLimitPrice = stopLimitPrice;
        this.takeProfitPrice = takeProfitPrice;
        this.timeWhenPlaced = timeWhenPlaced;
        this.margin = margin;
        this.isItShort = isItShort;
        this.isItCrossed = isItCrossed;
        this.accountNumber = accountNumber;
        this.orderID = orderID;
        this.orderType = orderType;
        this.quantity = quantity;
    }

    public float getQuantity() {
        return quantity;
    }

    public void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public long getOrderID() {
        return orderID;
    }

    public void setOrderID(long orderID) {
        this.orderID = orderID;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getIsItReal() {
        return isItReal;
    }

    public void setIsItReal(int isItReal) {
        this.isItReal = isItReal;
    }

    public int getIsItShort() {
        return isItShort;
    }

    public void setIsItShort(int isItShort) {
        this.isItShort = isItShort;
    }

    public int getIsItCrossed() {
        return isItCrossed;
    }

    public void setIsItCrossed(int isItCrossed) {
        this.isItCrossed = isItCrossed;
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

    public float getPercentOfPriceChange() {
        return ((getCurrentPrice() / getEntryPrice()) * 100) - 100;
    }

    public float getPercentOfAmountChange() {
        return ((getCurrentAmount() / getEntryAmount()) * 100) - 100;
    }

    public float getCurrentAmount() {
        float marg = (float) this.margin;
        float result;
        if (this.getIsItShort() >= 1 ) {
            result = (marg * this.entryAmount) + (marg * this.entryAmount) * (- getPercentOfPriceChange() / 100) - this.entryAmount * (marg - 1) - (this.entryAmount * marg * 0.0003f * 2f);
        } else {
            result = (marg * this.entryAmount) + (marg * this.entryAmount) * (getPercentOfPriceChange() / 100) - this.entryAmount * (marg - 1) - (this.entryAmount * marg * 0.0003f * 2f);
        }

        return result;
    }

    public long getTimeWhenPlaced() {
        return timeWhenPlaced;
    }

    public void setTimeWhenPlaced(long timeWhenPlaced) {
        this.timeWhenPlaced = timeWhenPlaced;
    }

//    @Override
//    public int compareTo(OrderListViewElement element) {
//        return this.getTimeWhenPlaced().compareTo(element.getTimeWhenPlaced());


    @Override
    public int compareTo(OrderListViewElement element) {
        return Long.compare(this.getTimeWhenPlaced(), element.getTimeWhenPlaced());
    }

    @NonNull
    @Override
    public String toString() {

        return "OrderListViewElement{symbol=" + symbol + " isItReal=" + isItReal + " entryAmount=" + entryAmount + " entryPrice=" + entryPrice + " currentPrice=" + currentPrice
                + " stopLimitPrice=" + stopLimitPrice + " takeProfitPrice=" + takeProfitPrice + " timeWhenPlaced=" + timeWhenPlaced + " margin=" + margin + " isItShort=" + isItShort
                +  " isItCrossed=" + isItCrossed + " accountNumber=" + accountNumber + " orderID=" + orderID + " orderType=" + orderType + " quantity=" + quantity + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        OrderListViewElement other = (OrderListViewElement) obj;
        return Objects.equals(symbol, other.symbol)
                && quantity == other.quantity
                && accountNumber == other.accountNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, quantity, accountNumber);
    }

}

