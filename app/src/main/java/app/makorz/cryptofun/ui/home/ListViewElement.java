package app.makorz.cryptofun.ui.home;

import java.io.Serializable;

public class ListViewElement implements Serializable, Comparable<ListViewElement> {

    private String text;
    private float percentChange;
    private float percentChange2;
    private float priceWhenCaught;
    private String time;
    private boolean isItLONG;

//    public ListViewElement(String text, float percentChange, float priceWhenCaught, String time, boolean isItLong) {
//        this.text = text;
//        this.percentChange = percentChange;
//        this.priceWhenCaught = priceWhenCaught;
//        this.time = time;
//        this.isItLONG = isItLong;
//    }

    public ListViewElement(String text) {
        this.text = text;

    }

    public ListViewElement(String text, float percentChange, float percentChange2, float priceWhenCaught, String time, boolean isItLONG) {
        this.text = text;
        this.percentChange = percentChange;
        this.percentChange2 = percentChange2;
        this.priceWhenCaught = priceWhenCaught;
        this.time = time;
        this.isItLONG = isItLONG;
    }

    public float getPercentChange2() {
        return percentChange2;
    }

    public void setPercentChange2(float percentChange2) {
        this.percentChange2 = percentChange2;
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

