package com.example.cryptofun.services;

import com.example.cryptofun.data.database.Kline;

import java.util.List;

public class ServiceFunctionsStrategyDefault {

    // Count $ volume in certain interval
    public static float countMoneyVolumeAtInterval(List<Kline> data, int firstKline, int lastKline) {

        if (firstKline - lastKline > 0) {
            return 0;
        }

        int volume = 0;
        float moneyInUSD = 0;
        float openPriceSum = 0;

        for (int i = firstKline; i < lastKline; i++) {
            openPriceSum += data.get(i).gettOpenPrice();
            volume += data.get(i).gettVolume();
        }

        float averageOpenPrice = openPriceSum / (lastKline - firstKline);
        moneyInUSD = averageOpenPrice * volume;

        return moneyInUSD;

    }


    // Count if volume has raised in second part of provided klines (intervals)
    public static float countBeforeAndAfter(List<Kline> data, int nrOfKlinesToInspect) {

        // e.g We are taking 8 klines - then comparing 4 to 4
        float result;
        int nrBefore = 1;
        int nrAfter = 1;

        for (int i = 0; i < (nrOfKlinesToInspect / 2); i++) {
            nrAfter += data.get(i).gettVolume();
        }

        for (int i = (nrOfKlinesToInspect / 2); i < nrOfKlinesToInspect; i++) {
            nrBefore += data.get(i).gettVolume();
        }
        result = (((float) nrAfter / (float) nrBefore) * 100) - 100;
        return result;
    }

    // WaveTrend Strategy without library Ta4J
    // Functions economic to check trend of cryptos
    public static int predictWaveTrend(List<Kline> klines) {
        int n1 = 10; // Channel Length
        int n2 = 21; // Average Length
        int obLevel1 = 45; // Over Bought Level 1
        int obLevel2 = 53; // Over Bought Level 2
        int osLevel1 = -45; // Over Sold Level 1
        int osLevel2 = -53; // Over Sold Level 2

        float[] ap = new float[klines.size()];
        for (int i = 0; i < klines.size(); i++) {
            ap[i] = (float) ((klines.get(i).gettHighPrice() + klines.get(i).gettLowPrice() + klines.get(i).gettClosePrice()) / 3.0);
        }

        float[] esa = ema(ap, n1);
        float[] d = ema(abs(subtract(ap, esa)), n1);
        float[] ci = divide(subtract(ap, esa), multiply(0.015f, d));
        float[] tci = ema(ci, n2);

        float[] wt1 = tci;
        float[] wt2 = sma(wt1, 4);

        // Log.e(TAG, Arrays.toString(tci) + "\n wt2 " + Arrays.toString(wt2) + "\n ap " + Arrays.toString(ap)+ "\n esa " + Arrays.toString(esa)+ "\n d" + Arrays.toString(d)+ "\n ci " + Arrays.toString(ci));

        boolean isRising = crossOver(wt1, wt2) && wt1[wt1.length - 1] > obLevel2;
        boolean isFalling = crossOver(wt2, wt1) && wt1[wt1.length - 1] < osLevel2;

        if (isRising) {
            return 1;
        } else if (isFalling) {
            return -1;
        } else {
            return 0;
        }
    }

    // Part of "predictWaveTrend"
    private static float[] ema(float[] x, int n) {
        float[] result = new float[x.length];
        float multiplier = (float) (2.0 / (n + 1));
        //Log.e(TAG, "1111" + Arrays.toString(x) + multiplier );
        result[0] = x[0];
        for (int i = 1; i < x.length; i++) {
            result[i] = (x[i] - result[i - 1]) * multiplier + result[i - 1];
        }
        //Log.e(TAG, "2222" + Arrays.toString(result) + multiplier );
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] sma(float[] x, int n) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            float sum = 0;
            int count = 0;
            for (int j = i; j >= Math.max(i - n + 1, 0); j--) {
                sum += x[j];
                count++;
            }
            result[i] = sum / count;
        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] abs(float[] x) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = Math.abs(x[i]);
        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] subtract(float[] x, float[] y) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = x[i] - y[i];
        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] multiply(float x, float[] y) {
        float[] result = new float[y.length];
        for (int i = 0; i < y.length; i++) {
            result[i] = x * y[i];
        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static float[] divide(float[] x, float[] y) {
        float[] result = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            if (x[i] == 0) {
                result[i] = 0f;
            } else {
                result[i] = x[i] / y[i];
            }

        }
        return result;
    }

    // Part of "predictWaveTrend"
    private static boolean crossOver(float[] x, float[] y) {
        int length = x.length;
        if (length < 2) {
            return false;
        }
        boolean xGreaterThanY = x[length - 2] > y[length - 2];
        boolean xLessThanY = x[length - 2] < y[length - 2];
        boolean xCrossesOver = x[length - 1] > y[length - 1];
        boolean yCrossesOver = y[length - 1] > x[length - 1];

        //Log.e(TAG, xGreaterThanY + " " + xLessThanY + " " + xCrossesOver + " " + yCrossesOver + " " + Arrays.toString(x) + " " + Arrays.toString(y));

        if (xGreaterThanY && yCrossesOver) {
            return true;
        }
        if (xLessThanY && xCrossesOver) {
            return true;
        }
        return false;
    }







}
