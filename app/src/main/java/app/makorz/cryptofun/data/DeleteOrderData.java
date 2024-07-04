package app.makorz.cryptofun.data;

public class DeleteOrderData {

    String symbol;
    long time;
    int isItReal;
    int isItShort;
    int isItMargin;
    int position;

    public DeleteOrderData(String symbol, long time, int isItReal, int isItShort, int isItMargin, int position) {
        this.symbol = symbol;
        this.time = time;
        this.isItReal = isItReal;
        this.isItShort = isItShort;
        this.isItMargin = isItMargin;
        this.position = position;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getTime() {
        return time;
    }

    public int getIsItReal() {
        return isItReal;
    }

    public int getIsItShort() {
        return isItShort;
    }

    public int getIsItMargin() {
        return isItMargin;
    }

    public int getPosition() {
        return position;
    }
}
