package app.makorz.cryptofun.data;

public class PercentagesOfChanges {

    float underSecondThreshold;
    float underFirstThreshold;
    float underZero;
    float overZero;
    float overFirstThreshold;
    float overSecondThreshold;
    long time;

    public PercentagesOfChanges(float underSecondThreshold, float underFirstThreshold, float underZero, float overZero, float overFirstThreshold, float overSecondThreshold, long time) {
        this.underSecondThreshold = underSecondThreshold;
        this.underFirstThreshold = underFirstThreshold;
        this.underZero = underZero;
        this.overZero = overZero;
        this.overFirstThreshold = overFirstThreshold;
        this.overSecondThreshold = overSecondThreshold;
        this.time = time;
    }

    public float getUnderSecondThreshold() {
        return underSecondThreshold;
    }

    public void setUnderSecondThreshold(float underSecondThreshold) {
        this.underSecondThreshold = underSecondThreshold;
    }

    public float getUnderFirstThreshold() {
        return underFirstThreshold;
    }

    public void setUnderFirstThreshold(float underFirstThreshold) {
        this.underFirstThreshold = underFirstThreshold;
    }

    public float getUnderZero() {
        return underZero;
    }

    public void setUnderZero(float underZero) {
        this.underZero = underZero;
    }

    public float getOverZero() {
        return overZero;
    }

    public void setOverZero(float overZero) {
        this.overZero = overZero;
    }

    public float getOverFirstThreshold() {
        return overFirstThreshold;
    }

    public void setOverFirstThreshold(float overFirstThreshold) {
        this.overFirstThreshold = overFirstThreshold;
    }

    public float getOverSecondThreshold() {
        return overSecondThreshold;
    }

    public void setOverSecondThreshold(float overSecondThreshold) {
        this.overSecondThreshold = overSecondThreshold;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
