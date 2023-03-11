package com.example.cryptofun.data;

public class PercentagesOfChanges {

    float under3;
    float under2;
    float under1;
    float over1;
    float over2;
    float over3;
    long time;

    public PercentagesOfChanges(float under3, float under2, float under1, float over1, float over2, float over3, long time) {
        this.under3 = under3;
        this.under2 = under2;
        this.under1 = under1;
        this.over1 = over1;
        this.over2 = over2;
        this.over3 = over3;
        this.time = time;
    }

    public float getUnder3() {
        return under3;
    }

    public void setUnder3(float under3) {
        this.under3 = under3;
    }

    public float getUnder2() {
        return under2;
    }

    public void setUnder2(float under2) {
        this.under2 = under2;
    }

    public float getUnder1() {
        return under1;
    }

    public void setUnder1(float under1) {
        this.under1 = under1;
    }

    public float getOver1() {
        return over1;
    }

    public void setOver1(float over1) {
        this.over1 = over1;
    }

    public float getOver2() {
        return over2;
    }

    public void setOver2(float over2) {
        this.over2 = over2;
    }

    public float getOver3() {
        return over3;
    }

    public void setOver3(float over3) {
        this.over3 = over3;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
