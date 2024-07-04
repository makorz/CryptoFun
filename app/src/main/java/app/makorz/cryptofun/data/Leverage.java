package app.makorz.cryptofun.data;

public class Leverage {

    String symbol;
    int leverage;
    float maxNotationValue;

//        "leverage": 21,
//            "maxNotionalValue": "1000000",
//            "symbol": "BTCUSDT"

    public Leverage(String symbol, int leverage, float maxNotationValue) {
        this.symbol = symbol;
        this.leverage = leverage;
        this.maxNotationValue = maxNotationValue;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getLeverage() {
        return leverage;
    }

    public void setLeverage(int leverage) {
        this.leverage = leverage;
    }

    public float getMaxNotationValue() {
        return maxNotationValue;
    }

    public void setMaxNotationValue(float maxNotationValue) {
        this.maxNotationValue = maxNotationValue;
    }
}
