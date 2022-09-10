package com.example.cryptofun.data;

public class StatusOfKline {

    private String interval;
    private int result;


    public StatusOfKline(String interval, int result) {
        this.interval = interval;
        this.result = result;
    }


    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
