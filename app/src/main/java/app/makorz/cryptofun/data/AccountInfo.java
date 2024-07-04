package app.makorz.cryptofun.data;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class AccountInfo {

    int feeTier;
    boolean canTrade;
    boolean canDeposit;
    boolean canWithdraw;
    long updateTime;
    boolean multiAssetsMargin;
    float totalInitialMargin;
    float totalMaintMargin;
    float totalWalletBalance;
    float totalUnrealizedProfit;
    float totalMarginBalance;
    float totalPositionInitialMargin;
    float totalOpenOrderInitialMargin;
    float totalCrossWalletBalance;
    float totalCrossUnPnl;
    float availableBalance;
    float maxWithdrawAmount;
    ArrayList<Asset> assets;
    ArrayList<Position> positions;

    /*
    "feeTier": 0,       // account commission tier
    "canTrade": true,   // if can trade
    "canDeposit": true,     // if can transfer in asset
    "canWithdraw": true,    // if can transfer out asset
    "updateTime": 0,        // reserved property, please ignore
    "multiAssetsMargin": false,
    "totalInitialMargin": "0.00000000",    // total initial margin required with current mark price (useless with isolated positions), only for USDT asset
    "totalMaintMargin": "0.00000000",     // total maintenance margin required, only for USDT asset
    "totalWalletBalance": "23.72469206",     // total wallet balance, only for USDT asset
    "totalUnrealizedProfit": "0.00000000",   // total unrealized profit, only for USDT asset
    "totalMarginBalance": "23.72469206",     // total margin balance, only for USDT asset
    "totalPositionInitialMargin": "0.00000000",    // initial margin required for positions with current mark price, only for USDT asset
    "totalOpenOrderInitialMargin": "0.00000000",   // initial margin required for open orders with current mark price, only for USDT asset
    "totalCrossWalletBalance": "23.72469206",      // crossed wallet balance, only for USDT asset
    "totalCrossUnPnl": "0.00000000",      // unrealized profit of crossed positions, only for USDT asset
    "availableBalance": "23.72469206",       // available balance, only for USDT asset
    "maxWithdrawAmount": "23.72469206"     // maximum amount for transfer out, only for USDT asset
    "assets": [{}],
    "positions": [{}],
     */

    public AccountInfo(int feeTier, boolean canTrade, boolean canDeposit, boolean canWithdraw, long updateTime, boolean multiAssetsMargin, float totalInitialMargin, float totalMaintMargin, float totalWalletBalance, float totalUnrealizedProfit, float totalMarginBalance, float totalPositionInitialMargin, float totalOpenOrderInitialMargin, float totalCrossWalletBalance, float totalCrossUnPnl, float availableBalance, float maxWithdrawAmount, ArrayList<Asset> assets, ArrayList<Position> positions) {
        this.feeTier = feeTier;
        this.canTrade = canTrade;
        this.canDeposit = canDeposit;
        this.canWithdraw = canWithdraw;
        this.updateTime = updateTime;
        this.multiAssetsMargin = multiAssetsMargin;
        this.totalInitialMargin = totalInitialMargin;
        this.totalMaintMargin = totalMaintMargin;
        this.totalWalletBalance = totalWalletBalance;
        this.totalUnrealizedProfit = totalUnrealizedProfit;
        this.totalMarginBalance = totalMarginBalance;
        this.totalPositionInitialMargin = totalPositionInitialMargin;
        this.totalOpenOrderInitialMargin = totalOpenOrderInitialMargin;
        this.totalCrossWalletBalance = totalCrossWalletBalance;
        this.totalCrossUnPnl = totalCrossUnPnl;
        this.availableBalance = availableBalance;
        this.maxWithdrawAmount = maxWithdrawAmount;
        this.assets = assets;
        this.positions = positions;
    }

    public int getFeeTier() {
        return feeTier;
    }

    public void setFeeTier(int feeTier) {
        this.feeTier = feeTier;
    }

    public boolean isCanTrade() {
        return canTrade;
    }

    public void setCanTrade(boolean canTrade) {
        this.canTrade = canTrade;
    }

    public boolean isCanDeposit() {
        return canDeposit;
    }

    public void setCanDeposit(boolean canDeposit) {
        this.canDeposit = canDeposit;
    }

    public boolean isCanWithdraw() {
        return canWithdraw;
    }

    public void setCanWithdraw(boolean canWithdraw) {
        this.canWithdraw = canWithdraw;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isMultiAssetsMargin() {
        return multiAssetsMargin;
    }

    public void setMultiAssetsMargin(boolean multiAssetsMargin) {
        this.multiAssetsMargin = multiAssetsMargin;
    }

    public float getTotalInitialMargin() {
        return totalInitialMargin;
    }

    public void setTotalInitialMargin(float totalInitialMargin) {
        this.totalInitialMargin = totalInitialMargin;
    }

    public float getTotalMaintMargin() {
        return totalMaintMargin;
    }

    public void setTotalMaintMargin(float totalMaintMargin) {
        this.totalMaintMargin = totalMaintMargin;
    }

    public float getTotalWalletBalance() {
        return totalWalletBalance;
    }

    public void setTotalWalletBalance(float totalWalletBalance) {
        this.totalWalletBalance = totalWalletBalance;
    }

    public float getTotalUnrealizedProfit() {
        return totalUnrealizedProfit;
    }

    public void setTotalUnrealizedProfit(float totalUnrealizedProfit) {
        this.totalUnrealizedProfit = totalUnrealizedProfit;
    }

    public float getTotalMarginBalance() {
        return totalMarginBalance;
    }

    public void setTotalMarginBalance(float totalMarginBalance) {
        this.totalMarginBalance = totalMarginBalance;
    }

    public float getTotalPositionInitialMargin() {
        return totalPositionInitialMargin;
    }

    public void setTotalPositionInitialMargin(float totalPositionInitialMargin) {
        this.totalPositionInitialMargin = totalPositionInitialMargin;
    }

    public float getTotalOpenOrderInitialMargin() {
        return totalOpenOrderInitialMargin;
    }

    public void setTotalOpenOrderInitialMargin(float totalOpenOrderInitialMargin) {
        this.totalOpenOrderInitialMargin = totalOpenOrderInitialMargin;
    }

    public float getTotalCrossWalletBalance() {
        return totalCrossWalletBalance;
    }

    public void setTotalCrossWalletBalance(float totalCrossWalletBalance) {
        this.totalCrossWalletBalance = totalCrossWalletBalance;
    }

    public float getTotalCrossUnPnl() {
        return totalCrossUnPnl;
    }

    public void setTotalCrossUnPnl(float totalCrossUnPnl) {
        this.totalCrossUnPnl = totalCrossUnPnl;
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

    public ArrayList<Asset> getAssets() {
        return assets;
    }

    public void setAssets(ArrayList<Asset> assets) {
        this.assets = assets;
    }

    public ArrayList<Position> getPositions() {
        return positions;
    }

    public void setPositions(ArrayList<Position> positions) {
        this.positions = positions;
    }

    @NonNull
    @Override
    public String toString() {

        String assetsTable = "";
        String positionsTable = "";

        for(int i = 0; i < assets.size(); i++) {

            if (i == 0) {
                assetsTable += "[";
            }

            assetsTable += assets.get(i).toString();

            if (i == (assets.size() - 1)) {
                assetsTable += "]";
            }

        }

        for(int i = 0; i < positions.size(); i++) {

            if (i == 0) {
                positionsTable += "[";
            }

            positionsTable += positions.get(i).toString();

            if (i == (positions.size() - 1)) {
                positionsTable += "]";
            }

        }

        return "AccountInfo{feeTier=" + feeTier + " canTrade=" + canTrade + " canDeposit=" + canDeposit + " canWithdraw=" + canWithdraw + " updateTime=" + updateTime
                + " multiAssetsMargin=" + multiAssetsMargin + " totalInitialMargin=" + totalInitialMargin + " totalMaintMargin=" + totalMaintMargin
                + " totalWalletBalance=" + totalWalletBalance + " totalUnrealizedProfit=" + totalUnrealizedProfit +  " totalMarginBalance=" + totalMarginBalance
                + " totalPositionInitialMargin=" + totalPositionInitialMargin + " totalOpenOrderInitialMargin=" + totalOpenOrderInitialMargin
                + " totalCrossWalletBalance=" + totalCrossWalletBalance + " totalCrossUnPnl=" + totalCrossUnPnl + " availableBalance=" + availableBalance
                + " maxWithdrawAmount=" + maxWithdrawAmount + " ASSETS=" + assetsTable + " POSITIONS=" + positionsTable + '}';
    }

}
