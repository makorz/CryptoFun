package app.makorz.cryptofun.data;

import java.io.Serializable;

public class ApprovedToken implements Serializable, Comparable<ApprovedToken> {

    private String symbol;
    private Integer longOrShort; //long 1, short 0
    private Integer nrOfTradesOnKlines;
    private Float volumeOnKlines;
    private Long time;
    private Float priceOnTimeOfApprove;
    private Float closePrice;


    public ApprovedToken(String symbol, Integer longOrShort, Integer nrOfTradesOnKlines, Float volumeOnKlines, Long time) {
        this.symbol = symbol;
        this.longOrShort = longOrShort;
        this.nrOfTradesOnKlines = nrOfTradesOnKlines;
        this.volumeOnKlines = volumeOnKlines;
        this.time = time;
    }

    public ApprovedToken(String symbol, Integer longOrShort, Integer nrOfTradesOnKlines, Float volumeOnKlines, Long time, Float priceOnTimeOfApprove) {
        this.symbol = symbol;
        this.longOrShort = longOrShort;
        this.nrOfTradesOnKlines = nrOfTradesOnKlines;
        this.volumeOnKlines = volumeOnKlines;
        this.time = time;
        this.priceOnTimeOfApprove = priceOnTimeOfApprove;
    }

    public ApprovedToken(String symbol, Integer longOrShort, Float closePrice, Long time, Float priceOnTimeOfApprove) {
        this.symbol = symbol;
        this.longOrShort = longOrShort;
        this.closePrice = closePrice;
        this.time = time;
        this.priceOnTimeOfApprove = priceOnTimeOfApprove;
    }

    public Float getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(Float closePrice) {
        this.closePrice = closePrice;
    }

    public Float getPriceOnTimeOfApprove() {
        return priceOnTimeOfApprove;
    }

    public void setPriceOnTimeOfApprove(Float priceOnTimeOfApprove) {
        this.priceOnTimeOfApprove = priceOnTimeOfApprove;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Float getVolumeOnKlines() {
        return volumeOnKlines;
    }

    public void setVolumeOnKlines(Float volumeOnKlines) {
        this.volumeOnKlines = volumeOnKlines;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Integer getLongOrShort() {
        return longOrShort;
    }

    public void setLongOrShort(Integer longOrShort) {
        this.longOrShort = longOrShort;
    }

    public Integer getNrOfTradesOnKlines() {
        return nrOfTradesOnKlines;
    }

    public void setNrOfTradesOnKlines(Integer nrOfTradesOnKlines) {
        this.nrOfTradesOnKlines = nrOfTradesOnKlines;
    }

//    @Override
//    public int compareTo(ApprovedToken apToken) {
//        return this.getVolumeOnKlines().compareTo(apToken.getVolumeOnKlines());
//    }

//    @Override
//    public int compareTo(ApprovedToken apToken) {
//        return this.getNrOfTradesOnKlines().compareTo(apToken.getNrOfTradesOnKlines());
//    }

    @Override
    public int compareTo(ApprovedToken apToken) {
        return this.getPriceOnTimeOfApprove().compareTo(apToken.getPriceOnTimeOfApprove());
    }

}
