package com.example.cryptofun.ui.results;

import androidx.annotation.NonNull;

import com.example.cryptofun.ui.orders.OrderListViewElement;

import java.io.Serializable;

public class ResultsListElement implements Serializable, Comparable<ResultsListElement>{

    private String symbol;
    private float entryPrice;
    private float closePrice;
    private float percentChanged;
    private int accountNr;
    private int margin;
    private float moneyEarned;
    private float entryAmount;
    private float exitAmount;
    private float percentOfMoneyChanged;
    private long timeEntry;
    private long timeExit;
    private int isItShort;
    private int isItReal;

    public ResultsListElement(String symbol, float entryPrice, float closePrice, float percentChanged, int accountNr, int margin, float moneyEarned, float entryAmount, float exitAmount, float percentOfMoneyChanged, long timeEntry, long timeExit, int isItShort, int isItReal) {
        this.symbol = symbol;
        this.entryPrice = entryPrice;
        this.closePrice = closePrice;
        this.percentChanged = percentChanged;
        this.accountNr = accountNr;
        this.margin = margin;
        this.moneyEarned = moneyEarned;
        this.entryAmount = entryAmount;
        this.exitAmount = exitAmount;
        this.percentOfMoneyChanged = percentOfMoneyChanged;
        this.timeEntry = timeEntry;
        this.timeExit = timeExit;
        this.isItShort = isItShort;
        this.isItReal = isItReal;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public float getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(float entryPrice) {
        this.entryPrice = entryPrice;
    }

    public float getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(float closePrice) {
        this.closePrice = closePrice;
    }

    public float getPercentChanged() {
        return percentChanged;
    }

    public void setPercentChanged(float percentChanged) {
        this.percentChanged = percentChanged;
    }

    public int getAccountNr() {
        return accountNr;
    }

    public void setAccountNr(int accountNr) {
        this.accountNr = accountNr;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public float getMoneyEarned() {
        return moneyEarned;
    }

    public void setMoneyEarned(float moneyEarned) {
        this.moneyEarned = moneyEarned;
    }

    public float getEntryAmount() {
        return entryAmount;
    }

    public void setEntryAmount(float entryAmount) {
        this.entryAmount = entryAmount;
    }

    public float getExitAmount() {
        return exitAmount;
    }

    public void setExitAmount(float exitAmount) {
        this.exitAmount = exitAmount;
    }

    public float getPercentOfMoneyChanged() {
        return percentOfMoneyChanged;
    }

    public void setPercentOfMoneyChanged(float percentOfMoneyChanged) {
        this.percentOfMoneyChanged = percentOfMoneyChanged;
    }

    public long getTimeEntry() {
        return timeEntry;
    }

    public void setTimeEntry(long timeEntry) {
        this.timeEntry = timeEntry;
    }

    public long getTimeExit() {
        return timeExit;
    }

    public void setTimeExit(long timeExit) {
        this.timeExit = timeExit;
    }

    public int getIsItShort() {
        return isItShort;
    }

    public void setIsItShort(int isItShort) {
        this.isItShort = isItShort;
    }

    public int getIsItReal() {
        return isItReal;
    }

    public void setIsItReal(int isItReal) {
        this.isItReal = isItReal;
    }

    public float getPercentOfPriceChange() {
        return ((getClosePrice() / getEntryPrice()) * 100) - 100;
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

    @Override
    public int compareTo(ResultsListElement element) {
        return Long.compare(this.getTimeExit(), element.getTimeExit());
    }

    @NonNull
    @Override
    public String toString() {

        return "ResultListElement{symbol=" + symbol + " isItReal=" + isItReal + " entryAmount=" + entryAmount + " exitAmount=" + exitAmount + " entryPrice=" + entryPrice + " closePrice=" + closePrice
                + " moneyEarned=" + moneyEarned + " percentOfMoneyChanged=" + percentOfMoneyChanged + " entryTime=" + timeEntry + " exitTime=" + timeExit + " isItShort=" + isItShort
                +  " margin=" + margin + " accountNumber=" + accountNr + " percentChanged=" + percentChanged + '}';
    }
}
