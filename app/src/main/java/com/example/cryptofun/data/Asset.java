package com.example.cryptofun.data;

import androidx.annotation.NonNull;

public class Asset {
    
    String asset;
    float walletBalance;
    float unrealizedProfit;
    float marginBalance;
    float maintMargin;
    float initialMargin;
    float positionInitialMargin;
    float openOrderInitialMargin;
    float crossWalletBalance;
    float crossUnPnl;
    float availableBalance;
    float maxWithdrawAmount;
    boolean marginAvailable;
    long updateTime;
    
    
    /*
    "asset": "USDT",            // asset name
    "walletBalance": "23.72469206",      // wallet balance
    "unrealizedProfit": "0.00000000",    // unrealized profit
    "marginBalance": "23.72469206",      // margin balance
    "maintMargin": "0.00000000",        // maintenance margin required
    "initialMargin": "0.00000000",    // total initial margin required with current mark price
    "positionInitialMargin": "0.00000000",    //initial margin required for positions with current mark price
    "openOrderInitialMargin": "0.00000000",   // initial margin required for open orders with current mark price
    "crossWalletBalance": "23.72469206",      // crossed wallet balance
    "crossUnPnl": "0.00000000"       // unrealized profit of crossed positions
    "availableBalance": "23.72469206",       // available balance
    "maxWithdrawAmount": "23.72469206",     // maximum amount for transfer out
    "marginAvailable": true,    // whether the asset can be used as margin in Multi-Assets mode
    "updateTime": 1625474304765 // last update time
     */

    public Asset(String asset, float walletBalance, float unrealizedProfit, float marginBalance, float maintMargin, float initialMargin, float positionInitialMargin, float openOrderInitialMargin, float crossWalletBalance, float crossUnPnl, float availableBalance, float maxWithdrawAmount, boolean marginAvailable, long updateTime) {
        this.asset = asset;
        this.walletBalance = walletBalance;
        this.unrealizedProfit = unrealizedProfit;
        this.marginBalance = marginBalance;
        this.maintMargin = maintMargin;
        this.initialMargin = initialMargin;
        this.positionInitialMargin = positionInitialMargin;
        this.openOrderInitialMargin = openOrderInitialMargin;
        this.crossWalletBalance = crossWalletBalance;
        this.crossUnPnl = crossUnPnl;
        this.availableBalance = availableBalance;
        this.maxWithdrawAmount = maxWithdrawAmount;
        this.marginAvailable = marginAvailable;
        this.updateTime = updateTime;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public float getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(float walletBalance) {
        this.walletBalance = walletBalance;
    }

    public float getUnrealizedProfit() {
        return unrealizedProfit;
    }

    public void setUnrealizedProfit(float unrealizedProfit) {
        this.unrealizedProfit = unrealizedProfit;
    }

    public float getMarginBalance() {
        return marginBalance;
    }

    public void setMarginBalance(float marginBalance) {
        this.marginBalance = marginBalance;
    }

    public float getMaintMargin() {
        return maintMargin;
    }

    public void setMaintMargin(float maintMargin) {
        this.maintMargin = maintMargin;
    }

    public float getInitialMargin() {
        return initialMargin;
    }

    public void setInitialMargin(float initialMargin) {
        this.initialMargin = initialMargin;
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

    public float getCrossWalletBalance() {
        return crossWalletBalance;
    }

    public void setCrossWalletBalance(float crossWalletBalance) {
        this.crossWalletBalance = crossWalletBalance;
    }

    public float getCrossUnPnl() {
        return crossUnPnl;
    }

    public void setCrossUnPnl(float crossUnPnl) {
        this.crossUnPnl = crossUnPnl;
    }

    public float getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(float availableBalance) {
        this.availableBalance = availableBalance;
    }

    public float getMaxWithdrawAmount() {
        return maxWithdrawAmount;
    }

    public void setMaxWithdrawAmount(float maxWithdrawAmount) {
        this.maxWithdrawAmount = maxWithdrawAmount;
    }

    public boolean isMarginAvailable() {
        return marginAvailable;
    }

    public void setMarginAvailable(boolean marginAvailable) {
        this.marginAvailable = marginAvailable;
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

        return "Asset{asset=" + asset + " walletBalance=" + walletBalance + " unrealizedProfit=" + unrealizedProfit + " marginBalance=" + marginBalance
                + " maintMargin=" + maintMargin + " initialMargin=" + initialMargin + " positionInitialMargin=" + positionInitialMargin
                + " openOrderInitialMargin=" + openOrderInitialMargin + " openOrderInitialMargin=" + openOrderInitialMargin + " crossWalletBalance=" + crossWalletBalance
                + " crossWalletBalance=" + crossWalletBalance + " crossUnPnl=" + crossUnPnl + " availableBalance=" + availableBalance
                + " maxWithdrawAmount=" + maxWithdrawAmount + " marginAvailable=" + marginAvailable + " updateTime=" + updateTime +'}';
    }


}
