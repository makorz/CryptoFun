package app.makorz.cryptofun.data;

import java.util.ArrayList;

public class StrategyResult {

    ArrayList<Integer> nrOfPositions;
    ArrayList<Double> vsBuyAndHoldProfit;
    ArrayList<Double> totalProfit;
    int waveTrendIndicator;
    String bestStrategy;

    public StrategyResult(ArrayList<Integer> nrOfPositions, ArrayList<Double> vsBuyAndHoldProfit, ArrayList<Double> totalProfit, int waveTrendIndicator, String bestStrategy) {
        this.nrOfPositions = nrOfPositions;
        this.vsBuyAndHoldProfit = vsBuyAndHoldProfit;
        this.totalProfit = totalProfit;
        this.waveTrendIndicator = waveTrendIndicator;
        this.bestStrategy = bestStrategy;
    }

    public ArrayList<Integer> getNrOfPositions() {
        return nrOfPositions;
    }

    public void setNrOfPositions(ArrayList<Integer> nrOfPositions) {
        this.nrOfPositions = nrOfPositions;
    }

    public ArrayList<Double> getVsBuyAndHoldProfit() {
        return vsBuyAndHoldProfit;
    }

    public void setVsBuyAndHoldProfit(ArrayList<Double> vsBuyAndHoldProfit) {
        this.vsBuyAndHoldProfit = vsBuyAndHoldProfit;
    }

    public ArrayList<Double> getTotalProfit() {
        return totalProfit;
    }

    public void setTotalProfit(ArrayList<Double> totalProfit) {
        this.totalProfit = totalProfit;
    }

    public int getWaveTrendIndicator() {
        return waveTrendIndicator;
    }

    public void setWaveTrendIndicator(int waveTrendIndicator) {
        this.waveTrendIndicator = waveTrendIndicator;
    }

    public String getBestStrategy() {
        return bestStrategy;
    }

    public void setBestStrategy(String bestStrategy) {
        this.bestStrategy = bestStrategy;
    }
}
