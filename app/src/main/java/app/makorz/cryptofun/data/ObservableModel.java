package app.makorz.cryptofun.data;

public class ObservableModel {

    String symbol;
    int nrOfKlinesToDownload;
    String interval;
    int whatToDoWithDB;
    int howManyOldOnesToDelete;

    public ObservableModel(String symbol, int nrOfKlinesToDownload, String interval, int whatToDoWithDB, int howManyOldOnesToDelete) {
        this.symbol = symbol;
        this.nrOfKlinesToDownload = nrOfKlinesToDownload;
        this.interval = interval;
        this.whatToDoWithDB = whatToDoWithDB;
        this.howManyOldOnesToDelete = howManyOldOnesToDelete;
    }

    public int getHowManyOldOnesToDelete() {
        return howManyOldOnesToDelete;
    }

    public void setHowManyOldOnesToDelete(int howManyOldOnesToDelete) {
        this.howManyOldOnesToDelete = howManyOldOnesToDelete;
    }

    public int getWhatToDoWithDB() {
        return whatToDoWithDB;
    }

    public void setWhatToDoWithDB(int whatToDoWithDB) {
        this.whatToDoWithDB = whatToDoWithDB;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getNrOfKlinesToDownload() {
        return nrOfKlinesToDownload;
    }

    public void setNrOfKlinesToDownload(int nrOfKlinesToDownload) {
        this.nrOfKlinesToDownload = nrOfKlinesToDownload;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }
}
