package app.makorz.cryptofun.ui.results;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class ResultsAccountElement implements Serializable{

    private String accountNr;
    private int nrOfAllOrders;
    private int nrOfShortOrders;
    private int nrOfLongOrders;
    private int nrOfAllGoodOrders;
    private int nrOfShortGoodOrders;
    private int nrOfLongGoodOrders;
    private float percentOfAllGoodOrders;
    private float percentOfShortGoodOrders;
    private float percentOfLongGoodOrders;
    private float percentOf3d;
    private float percentOf7d;
    private float percentOf30d;
    private float percentOfAllTime;

    public ResultsAccountElement(String accountNr, int nrOfAllOrders, int nrOfShortOrders, int nrOfLongOrders, int nrOfAllGoodOrders, int nrOfShortGoodOrders, int nrOfLongGoodOrders, float percentOfAllGoodOrders, float percentOfShortGoodOrders, float percentOfLongGoodOrders, float percentOf3d, float percentOf7d, float percentOf30d, float percentOfAllTime) {
        this.accountNr = accountNr;
        this.nrOfAllOrders = nrOfAllOrders;
        this.nrOfShortOrders = nrOfShortOrders;
        this.nrOfLongOrders = nrOfLongOrders;
        this.nrOfAllGoodOrders = nrOfAllGoodOrders;
        this.nrOfShortGoodOrders = nrOfShortGoodOrders;
        this.nrOfLongGoodOrders = nrOfLongGoodOrders;
        this.percentOfAllGoodOrders = percentOfAllGoodOrders;
        this.percentOfShortGoodOrders = percentOfShortGoodOrders;
        this.percentOfLongGoodOrders = percentOfLongGoodOrders;
        this.percentOf3d = percentOf3d;
        this.percentOf7d = percentOf7d;
        this.percentOf30d = percentOf30d;
        this.percentOfAllTime = percentOfAllTime;
    }

    public String getAccountNr() {
        return accountNr;
    }

    public void setAccountNr(String accountNr) {
        this.accountNr = accountNr;
    }

    public int getNrOfAllOrders() {
        return nrOfAllOrders;
    }

    public void setNrOfAllOrders(int nrOfAllOrders) {
        this.nrOfAllOrders = nrOfAllOrders;
    }

    public int getNrOfShortOrders() {
        return nrOfShortOrders;
    }

    public void setNrOfShortOrders(int nrOfShortOrders) {
        this.nrOfShortOrders = nrOfShortOrders;
    }

    public int getNrOfLongOrders() {
        return nrOfLongOrders;
    }

    public void setNrOfLongOrders(int nrOfLongOrders) {
        this.nrOfLongOrders = nrOfLongOrders;
    }

    public int getNrOfAllGoodOrders() {
        return nrOfAllGoodOrders;
    }

    public void setNrOfAllGoodOrders(int nrOfAllGoodOrders) {
        this.nrOfAllGoodOrders = nrOfAllGoodOrders;
    }

    public int getNrOfShortGoodOrders() {
        return nrOfShortGoodOrders;
    }

    public void setNrOfShortGoodOrders(int nrOfShortGoodOrders) {
        this.nrOfShortGoodOrders = nrOfShortGoodOrders;
    }

    public int getNrOfLongGoodOrders() {
        return nrOfLongGoodOrders;
    }

    public void setNrOfLongGoodOrders(int nrOfLongGoodOrders) {
        this.nrOfLongGoodOrders = nrOfLongGoodOrders;
    }

    public float getPercentOfAllGoodOrders() {
        return percentOfAllGoodOrders;
    }

    public void setPercentOfAllGoodOrders(float percentOfAllGoodOrders) {
        this.percentOfAllGoodOrders = percentOfAllGoodOrders;
    }

    public float getPercentOfShortGoodOrders() {
        return percentOfShortGoodOrders;
    }

    public void setPercentOfShortGoodOrders(float percentOfShortGoodOrders) {
        this.percentOfShortGoodOrders = percentOfShortGoodOrders;
    }

    public float getPercentOfLongGoodOrders() {
        return percentOfLongGoodOrders;
    }

    public void setPercentOfLongGoodOrders(float percentOfLongGoodOrders) {
        this.percentOfLongGoodOrders = percentOfLongGoodOrders;
    }

    public float getPercentOf3d() {
        return percentOf3d;
    }

    public void setPercentOf3d(float percentOf3d) {
        this.percentOf3d = percentOf3d;
    }

    public float getPercentOf7d() {
        return percentOf7d;
    }

    public void setPercentOf7d(float percentOf7d) {
        this.percentOf7d = percentOf7d;
    }

    public float getPercentOf30d() {
        return percentOf30d;
    }

    public void setPercentOf30d(float percentOf30d) {
        this.percentOf30d = percentOf30d;
    }

    public float getPercentOfAllTime() {
        return percentOfAllTime;
    }

    public void setPercentOfAllTime(float percentOfAllTime) {
        this.percentOfAllTime = percentOfAllTime;
    }

    @NonNull
    @Override
    public String toString() {

        return "ResultAccountElement{nr=" + accountNr + " nrOfAllOrders=" + nrOfAllOrders + " nrOfShortOrders=" + nrOfShortOrders + " nrOfLongOrders=" + nrOfLongOrders + " nrOfAllGoodOrders=" + nrOfAllGoodOrders + " nrOfShortGoodOrders=" + nrOfShortGoodOrders + " nrOfLongGoodOrders=" + nrOfLongGoodOrders + " percentOfAllGoodOrders=" + percentOfAllGoodOrders + " percentOfShortGoodOrders=" + percentOfShortGoodOrders + " percentOfLongGoodOrders=" + percentOfLongGoodOrders + " percentOf3d=" + percentOf3d  +  " percentOf7d=" + percentOf7d + " percentOf30d=" + percentOf30d + " percentOfAllTime=" + percentOfAllTime + '}';

    }
}
