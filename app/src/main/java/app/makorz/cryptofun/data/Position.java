package app.makorz.cryptofun.data;

import androidx.annotation.NonNull;

public class Position {
    
    String symbol;
    float initialMargin;
    float maintMargin;
    float unrealizedProfit;
    float positionInitialMargin;
    float openOrderInitialMargin;
    float leverage;
    boolean isolated;
    float entryPrice;
    float maxNotional;
    float bidNotional;
    float askNotional;
    String positionSide;
    float positionAmt;
    long updateTime;
    
    /*
     "symbol": "BTCUSDT",    // symbol name
      "initialMargin": "0",   // initial margin required with current mark price 
      "maintMargin": "0",     // maintenance margin required
      "unrealizedProfit": "0.00000000",  // unrealized profit
      "positionInitialMargin": "0",      // initial margin required for positions with current mark price
      "openOrderInitialMargin": "0",     // initial margin required for open orders with current mark price
      "leverage": "100",      // current initial leverage
      "isolated": true,       // if the position is isolated
      "entryPrice": "0.00000",    // average entry price
      "maxNotional": "250000",    // maximum available notional with current leverage
      "bidNotional": "0",  // bids notional, ignore
      "askNotional": "0",  // ask notional, ignore
      "positionSide": "BOTH",     // position side
      "positionAmt": "0",         // position amount
      "updateTime": 0           // last update time
     */

    public Position(String symbol, float initialMargin, float maintMargin, float unrealizedProfit, float positionInitialMargin, float openOrderInitialMargin, float leverage, boolean isolated, float entryPrice, float maxNotional, float bidNotional, float askNotional, String positionSide, float positionAmt, long updateTime) {
        this.symbol = symbol;
        this.initialMargin = initialMargin;
        this.maintMargin = maintMargin;
        this.unrealizedProfit = unrealizedProfit;
        this.positionInitialMargin = positionInitialMargin;
        this.openOrderInitialMargin = openOrderInitialMargin;
        this.leverage = leverage;
        this.isolated = isolated;
        this.entryPrice = entryPrice;
        this.maxNotional = maxNotional;
        this.bidNotional = bidNotional;
        this.askNotional = askNotional;
        this.positionSide = positionSide;
        this.positionAmt = positionAmt;
        this.updateTime = updateTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public float getInitialMargin() {
        return initialMargin;
    }

    public void setInitialMargin(float initialMargin) {
        this.initialMargin = initialMargin;
    }

    public float getMaintMargin() {
        return maintMargin;
    }

    public void setMaintMargin(float maintMargin) {
        this.maintMargin = maintMargin;
    }

    public float getUnrealizedProfit() {
        return unrealizedProfit;
    }

    public void setUnrealizedProfit(float unrealizedProfit) {
        this.unrealizedProfit = unrealizedProfit;
    }

    public float getPositionInitialMargin() {
        return positionInitialMargin;
    }

    public void setPositionInitialMargin(float positionInitialMargin) {
        this.positionInitialMargin = positionInitialMargin;
    }

    public float getOpenOrderInitialMargin() {
        return openOrderInitialMargin;
    }

    public void setOpenOrderInitialMargin(float openOrderInitialMargin) {
        this.openOrderInitialMargin = openOrderInitialMargin;
    }

    public float getLeverage() {
        return leverage;
    }

    public void setLeverage(float leverage) {
        this.leverage = leverage;
    }

    public boolean isIsolated() {
        return isolated;
    }

    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
    }

    public float getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(float entryPrice) {
        this.entryPrice = entryPrice;
    }

    public float getMaxNotional() {
        return maxNotional;
    }

    public void setMaxNotional(float maxNotional) {
        this.maxNotional = maxNotional;
    }

    public float getBidNotional() {
        return bidNotional;
    }

    public void setBidNotional(float bidNotional) {
        this.bidNotional = bidNotional;
    }

    public float getAskNotional() {
        return askNotional;
    }

    public void setAskNotional(float askNotional) {
        this.askNotional = askNotional;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public float getPositionAmt() {
        return positionAmt;
    }

    public void setPositionAmt(float positionAmt) {
        this.positionAmt = positionAmt;
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

        return "Position{symbol=" + symbol + " initialMargin=" + initialMargin + " maintMargin=" + maintMargin + " unrealizedProfit=" + unrealizedProfit
                + " positionInitialMargin=" + positionInitialMargin + " openOrderInitialMargin=" + openOrderInitialMargin + " leverage=" + leverage
                + " isolated=" + isolated + " entryPrice=" + entryPrice + " maxNotional=" + maxNotional +  " bidNotional=" + bidNotional
                + " askNotional=" + askNotional + " positionSide=" + positionSide + " positionAmt=" + positionAmt + " updateTime=" + updateTime + '}';
    }

}
