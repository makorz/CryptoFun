package app.makorz.cryptofun.data;

import io.reactivex.rxjava3.core.Observable;

public class KlineRequest {

    private Observable<?> request;
    private String symbol;
    private String interval;
    private String[][] dataOfSymbolInterval;
    private int whatToDoInDB;
    int howManyOldOnesToDelete;

    public KlineRequest(Observable<?> request, String symbol, String interval) {
        this.request = request;
        this.symbol = symbol;
        this.interval = interval;
    }

    public KlineRequest(Observable<?> request, String symbol, String interval, int whatToDoInDB, int howManyOldOnesToDelete) {
        this.request = request;
        this.symbol = symbol;
        this.interval = interval;
        this.whatToDoInDB = whatToDoInDB;
        this.howManyOldOnesToDelete = howManyOldOnesToDelete;
    }

    public int getHowManyOldOnesToDelete() {
        return howManyOldOnesToDelete;
    }

    public void setHowManyOldOnesToDelete(int howManyOldOnesToDelete) {
        this.howManyOldOnesToDelete = howManyOldOnesToDelete;
    }

    public int getWhatToDoInDB() {
        return whatToDoInDB;
    }

    public void setWhatToDoInDB(int whatToDoInDB) {
        this.whatToDoInDB = whatToDoInDB;
    }

    public Observable<?> getRequest() {
        return request;
    }

    public void setRequest(Observable<?> request) {
        this.request = request;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String[][] getDataOfSymbolInterval() {
        return dataOfSymbolInterval;
    }

    public void setDataOfSymbolInterval(String[][] dataOfSymbolInterval) {
        this.dataOfSymbolInterval = dataOfSymbolInterval;
    }
}
