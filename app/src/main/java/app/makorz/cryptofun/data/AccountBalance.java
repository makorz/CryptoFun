package app.makorz.cryptofun.data;

import com.google.gson.annotations.SerializedName;

public class AccountBalance {

    @SerializedName("accountAlias")
    private String accountAlias;
    @SerializedName("asset")
    private String asset;
    @SerializedName("balance")
    private float balance;
    @SerializedName("crossWalletBalance")
    private float crossWalletBalance;
    @SerializedName("crossUnPnl")
    private float crossUnPnl;
    @SerializedName("availableBalance")
    private float availableBalance;
    @SerializedName("maxWithdrawAmount")
    private float maxWithdrawAmount;
    @SerializedName("updateTime")
    private long updateTime;
    @SerializedName("marginAvailable")
    private boolean marginAvailable;

//    {
//        "accountAlias": "SgsR",    // unique account code
//            "asset": "USDT",    // asset name
//            "balance": "122607.35137903", // wallet balance
//            "crossWalletBalance": "23.72469206", // crossed wallet balance
//            "crossUnPnl": "0.00000000"  // unrealized profit of crossed positions
//        "availableBalance": "23.72469206",       // available balance
//            "maxWithdrawAmount": "23.72469206",     // maximum amount for transfer out
//            "marginAvailable": true,    // whether the asset can be used as margin in Multi-Asset mode
//            "updateTime": 1617939110373
//    }


    public AccountBalance(String accountAlias, String asset, float balance, float crossWalletBalance, float crossUnPnl, float availableBalance, float maxWithdrawAmount, long updateTime, boolean marginAvailable) {
        this.accountAlias = accountAlias;
        this.asset = asset;
        this.balance = balance;
        this.crossWalletBalance = crossWalletBalance;
        this.crossUnPnl = crossUnPnl;
        this.availableBalance = availableBalance;
        this.maxWithdrawAmount = maxWithdrawAmount;
        this.updateTime = updateTime;
        this.marginAvailable = marginAvailable;
    }

    public String getAccountAlias() {
        return accountAlias;
    }

    public void setAccountAlias(String accountAlias) {
        this.accountAlias = accountAlias;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
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

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isMarginAvailable() {
        return marginAvailable;
    }

    public void setMarginAvailable(boolean marginAvailable) {
        this.marginAvailable = marginAvailable;
    }
}
