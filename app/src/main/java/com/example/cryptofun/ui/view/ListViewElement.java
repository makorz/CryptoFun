package com.example.cryptofun.ui.view;

import java.io.Serializable;

public class ListViewElement implements Serializable, Comparable<ListViewElement> {

    private String text;
    private float percentChange;
    private float priceWhenCaught;
    private String time;
    private boolean isItLONG;

    public ListViewElement(String text, float percentChange, float priceWhenCaught, String time, boolean longOrShort) {
        this.text = text;
        this.percentChange = percentChange;
        this.priceWhenCaught = priceWhenCaught;
        this.time = time;
        this.isItLONG = longOrShort;
    }

    public ListViewElement(String text) {
        this.text = text;

    }

    public float getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(float percentChange) {
        this.percentChange = percentChange;
    }

    public float getPriceWhenCaught() {
        return priceWhenCaught;
    }

    public void setPriceWhenCaught(float priceWhenCaught) {
        this.priceWhenCaught = priceWhenCaught;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isItLONG() {
        return isItLONG;
    }

    public void setItLONG(boolean itLONG) {
        this.isItLONG = itLONG;
    }

    public String getText() {
        return text;
    }

    public void setText(String symbol) {
        this.text = text;
    }


    @Override
    public int compareTo(ListViewElement element) {
        return this.getText().compareTo(element.getText());
    }

}

