package app.makorz.cryptofun.data;

import android.content.Context;

import androidx.annotation.NonNull;

import app.makorz.cryptofun.services.CallbackButton;

public class MarketOrder {

    String symbol;
    String side;
    String type;
    String newOrderRespType;
    String marginType;
    int leverage;
    float quantity;
    long timestamp;
    long recvWindow;
    float stopLimitPrice;
    float takeProfitPrice;
    float entryAmount;
    float currentPrice;
    boolean isItShort;
    boolean isItCrossed;
    Context context;
    CallbackButton callbackButton;

    public MarketOrder(String symbol, String side, String type, String newOrderRespType, String marginType, int leverage, float quantity, long timestamp, long recvWindow, float stopLimitPrice, float takeProfitPrice, float entryAmount, float currentPrice, boolean isItShort, boolean isItCrossed, Context context, CallbackButton callbackButton) {
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.newOrderRespType = newOrderRespType;
        this.marginType = marginType;
        this.leverage = leverage;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.recvWindow = recvWindow;
        this.stopLimitPrice = stopLimitPrice;
        this.takeProfitPrice = takeProfitPrice;
        this.entryAmount = entryAmount;
        this.currentPrice = currentPrice;
        this.isItShort = isItShort;
        this.isItCrossed = isItCrossed;
        this.context = context;
        this.callbackButton = callbackButton;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNewOrderRespType() {
        return newOrderRespType;
    }

    public void setNewOrderRespType(String newOrderRespType) {
        this.newOrderRespType = newOrderRespType;
    }

    public String getMarginType() {
        return marginType;
    }

    public void setMarginType(String marginType) {
        this.marginType = marginType;
    }

    public int getLeverage() {
        return leverage;
    }

    public void setLeverage(int leverage) {
        this.leverage = leverage;
    }

    public float getQuantity() {
        return quantity;
    }

    public void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getRecvWindow() {
        return recvWindow;
    }

    public void setRecvWindow(long recvWindow) {
        this.recvWindow = recvWindow;
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

    public float getEntryAmount() {
        return entryAmount;
    }

    public void setEntryAmount(float entryAmount) {
        this.entryAmount = entryAmount;
    }

    public float getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(float currentPrice) {
        this.currentPrice = currentPrice;
    }


    public boolean isItShort() {
        return isItShort;
    }

    public void setItShort(boolean itShort) {
        isItShort = itShort;
    }

    public boolean isItCrossed() {
        return isItCrossed;
    }

    public void setItCrossed(boolean itCrossed) {
        isItCrossed = itCrossed;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public CallbackButton getCallbackButton() {
        return callbackButton;
    }

    public void setCallbackButton(CallbackButton callbackButton) {
        this.callbackButton = callbackButton;
    }

    @NonNull
    @Override
    public String toString() {

        return "MarketOrder{symbol=" + symbol + " side=" + side + " type=" + type + " newOrderRespType=" + newOrderRespType + " marginType=" + marginType + " leverage=" + leverage + " quantity=" + quantity + " timestamp=" + timestamp
                + " recvWindow=" + recvWindow + " stopLimitPrice=" + stopLimitPrice +  " takeProfitPrice=" + takeProfitPrice + " entryAmount=" + entryAmount + " currentPrice=" + currentPrice + " isItShort=" + isItShort
                + " isItCrossed=" + isItCrossed + " context=" + context + " callbackButton=" + callbackButton + '}';
    }
}
