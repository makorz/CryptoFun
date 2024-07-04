package app.makorz.cryptofun.data;

public class StrategyParameters {

    int nrOfKlinesToWait;
    String intervalOfKlines;
    int WTLevel;
    int RSILevelBottom;
    int RSILevelTop;
    int ARRONLevel;
    int ADXLevel;
    float PPOLevelBottom;
    float PPOLevelTop;

    public StrategyParameters(int nrOfKlinesToWait, String intervalOfKlines, int WTLevel, int RSILevelBottom, int RSILevelTop, int ARRONLevel, int ADXLevel, float PPOLevelBottom, float PPOLevelTop) {
        this.nrOfKlinesToWait = nrOfKlinesToWait;
        this.intervalOfKlines = intervalOfKlines;
        this.WTLevel = WTLevel;
        this.RSILevelBottom = RSILevelBottom;
        this.RSILevelTop = RSILevelTop;
        this.ARRONLevel = ARRONLevel;
        this.ADXLevel = ADXLevel;
        this.PPOLevelBottom = PPOLevelBottom;
        this.PPOLevelTop = PPOLevelTop;
    }

    public float getPPOLevelBottom() {
        return PPOLevelBottom;
    }

    public void setPPOLevelBottom(float PPOLevelBottom) {
        this.PPOLevelBottom = PPOLevelBottom;
    }

    public float getPPOLevelTop() {
        return PPOLevelTop;
    }

    public void setPPOLevelTop(float PPOLevelTop) {
        this.PPOLevelTop = PPOLevelTop;
    }

    public int getADXLevel() {
        return ADXLevel;
    }

    public void setADXLevel(int ADXLevel) {
        this.ADXLevel = ADXLevel;
    }

    public int getNrOfKlinesToWait() {
        return nrOfKlinesToWait;
    }

    public void setNrOfKlinesToWait(int nrOfKlinesToWait) {
        this.nrOfKlinesToWait = nrOfKlinesToWait;
    }

    public String getIntervalOfKlines() {
        return intervalOfKlines;
    }

    public void setIntervalOfKlines(String intervalOfKlines) {
        this.intervalOfKlines = intervalOfKlines;
    }

    public int getWTLevel() {
        return WTLevel;
    }

    public void setWTLevel(int WTLevel) {
        this.WTLevel = WTLevel;
    }

    public int getRSILevelBottom() {
        return RSILevelBottom;
    }

    public void setRSILevelBottom(int RSILevelBottom) {
        this.RSILevelBottom = RSILevelBottom;
    }

    public int getRSILevelTop() {
        return RSILevelTop;
    }

    public void setRSILevelTop(int RSILevelTop) {
        this.RSILevelTop = RSILevelTop;
    }

    public int getARRONLevel() {
        return ARRONLevel;
    }

    public void setARRONLevel(int ARRONLevel) {
        this.ARRONLevel = ARRONLevel;
    }
}
