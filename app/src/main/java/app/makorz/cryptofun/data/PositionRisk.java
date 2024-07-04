package app.makorz.cryptofun.data;

import androidx.annotation.NonNull;

public class PositionRisk {

    String symbol;
    float entryPrice;
    String marginType;
    boolean isAutoAddMargin;
    float leverage;
    float liquidationPrice;
    float markPrice;
    long maxNotionalValue;
    float positionAmt;
    float notional;
    float isolatedWallet;
    float unRealizedProfit;
    String positionSide;
    long updateTime;

    /*
    [{
        "entryPrice": "0.00000",
        "marginType": "isolated",
        "isAutoAddMargin": "false",
        "isolatedMargin": "0.00000000",
        "leverage": "10",
        "liquidationPrice": "0",
        "markPrice": "6679.50671178",
        "maxNotionalValue": "20000000",
        "positionAmt": "0.000",
        "notional": "0",,
        "isolatedWallet": "0",
        "symbol": "BTCUSDT",
        "unRealizedProfit": "0.00000000",
        "positionSide": "BOTH",
        "updateTime": 0
     }]
     */

    public PositionRisk(String symbol, float entryPrice, String marginType, boolean isAutoAddMargin, float leverage, float liquidationPrice, float markPrice, long maxNotionalValue, float positionAmt, float notional, float isolatedWallet, float unRealizedProfit, String positionSide, long updateTime) {
        this.symbol = symbol;
        this.entryPrice = entryPrice;
        this.marginType = marginType;
        this.isAutoAddMargin = isAutoAddMargin;
        this.leverage = leverage;
        this.liquidationPrice = liquidationPrice;
        this.markPrice = markPrice;
        this.maxNotionalValue = maxNotionalValue;
        this.positionAmt = positionAmt;
        this.notional = notional;
        this.isolatedWallet = isolatedWallet;
        this.unRealizedProfit = unRealizedProfit;
        this.positionSide = positionSide;
        this.updateTime = updateTime;
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

    public String getMarginType() {
        return marginType;
    }

    public void setMarginType(String marginType) {
        this.marginType = marginType;
    }

    public boolean isAutoAddMargin() {
        return isAutoAddMargin;
    }

    public void setAutoAddMargin(boolean autoAddMargin) {
        isAutoAddMargin = autoAddMargin;
    }

    public float getLeverage() {
        return leverage;
    }

    public void setLeverage(float leverage) {
        this.leverage = leverage;
    }

    public float getLiquidationPrice() {
        return liquidationPrice;
    }

    public void setLiquidationPrice(float liquidationPrice) {
        this.liquidationPrice = liquidationPrice;
    }

    public float getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(float markPrice) {
        this.markPrice = markPrice;
    }

    public long getMaxNotionalValue() {
        return maxNotionalValue;
    }

    public void setMaxNotionalValue(long maxNotionalValue) {
        this.maxNotionalValue = maxNotionalValue;
    }

    public float getPositionAmt() {
        return positionAmt;
    }

    public void setPositionAmt(float positionAmt) {
        this.positionAmt = positionAmt;
    }

    public float getNotional() {
        return notional;
    }

    public void setNotional(float notional) {
        this.notional = notional;
    }

    public float getIsolatedWallet() {
        return isolatedWallet;
    }

    public void setIsolatedWallet(float isolatedWallet) {
        this.isolatedWallet = isolatedWallet;
    }

    public float getUnRealizedProfit() {
        return unRealizedProfit;
    }

    public void setUnRealizedProfit(float unRealizedProfit) {
        this.unRealizedProfit = unRealizedProfit;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @NonNull
    @Override
    public String toString() {

        return "PositionRisk{symbol=" + symbol + " entryPrice=" + entryPrice + " marginType=" + marginType + " isAutoAddMargin=" + isAutoAddMargin
                + " leverage=" + leverage + " liquidationPrice=" + liquidationPrice + " markPrice=" + markPrice
                + " maxNotionalValue=" + maxNotionalValue + " positionAmt=" + positionAmt + " notional=" + notional +  " isolatedWallet=" + isolatedWallet
                + " unRealizedProfit=" + unRealizedProfit + " positionSide=" + positionSide + " updateTime=" + updateTime + '}';
    }


}
