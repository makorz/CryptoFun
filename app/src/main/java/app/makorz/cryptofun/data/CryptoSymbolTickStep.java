package app.makorz.cryptofun.data;

public class CryptoSymbolTickStep {

    String tickSize;
    String stepSize;
    String symbol;

    public CryptoSymbolTickStep(String tickSize, String stepSize, String symbol) {
        this.tickSize = tickSize;
        this.stepSize = stepSize;
        this.symbol = symbol;
    }

    public String getTickSize() {
        return tickSize;
    }

    public void setTickSize(String tickSize) {
        this.tickSize = tickSize;
    }

    public String getStepSize() {
        return stepSize;
    }

    public void setStepSize(String stepSize) {
        this.stepSize = stepSize;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
