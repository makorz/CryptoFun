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

    // Count nr Of Trades committed in certain interval
    public static int countNrOfTradesAtInterval(List<Kline> data, int firstKline, int lastKline) {

        int result = 0;

        if (firstKline - lastKline >= 0) {
            return 0;
        }

        for (int i = firstKline; i < lastKline; i++) {
            result += data.get(i).gettNumberOfTrades();
        }

        return result;
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

    public static double calculateATR(List<Kline> klines, int period) {

        if (klines.isEmpty() || period < 0 || period > klines.size()) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        double sumTrueRange = 0.0;

        for (int i = 0; i < period; i++) {
            Kline kline = klines.get(i);
            double trueRange = Math.max(kline.gettHighPrice() - kline.gettLowPrice(), Math.abs(kline.gettHighPrice() - kline.gettClosePrice()));
            trueRange = Math.max(trueRange, Math.abs(kline.gettLowPrice() - kline.gettClosePrice()));
            sumTrueRange += trueRange;
        }

        return sumTrueRange / period;
    }

    private static double calculateAveragePrice(List<Kline> klines) {
        double sum = 0.0;
        for (Kline kline : klines) {
            sum += kline.gettClosePrice();
        }
        return sum / klines.size();
    }

    private static double calculateAverageVolume(List<Kline> klines) {
        double sum = 0.0;
        for (Kline kline : klines) {
            sum += kline.gettVolume();
        }
        return sum / klines.size();
    }


    public static int predictPriceDirection(List<Kline> pastHourKlines) {

        // Compute the average price over the past hour
        double sumPrice = 0;
        for (Kline kline : pastHourKlines) {
            sumPrice += kline.gettClosePrice();
        }
        double averagePrice = sumPrice / pastHourKlines.size();

        // Compute the price change over the past hour as a percentage
        double endPrice = pastHourKlines.get(0).gettClosePrice();
        double startPrice = pastHourKlines.get(pastHourKlines.size() - 1).gettClosePrice();
        double priceChange = ((endPrice - startPrice) / startPrice) * 100;

        //Log.e(TAG, "AVGPrice: " + averagePrice + "ENDPrice: " + endPrice + "PRICEChange: " + priceChange);
        // Predict the direction of the price change in the next hour
        if (priceChange > 0 && averagePrice < endPrice) {
            return 1; // The price is likely to rise in the next hour
        } else if (priceChange < 0 && averagePrice > endPrice) {
            return -1; // The price is likely to drop in the next hour
        } else {
            return 0; // The price is likely to stay relatively stable in the next hour
        }
    }

    public static double percentOfPriceChange(List<Kline> pastHourKlines) {

        // Compute the average price over the past hour
        double sumPrice = 0;
        for (Kline kline : pastHourKlines) {
            sumPrice += kline.gettClosePrice();
        }
        double averagePrice = sumPrice / pastHourKlines.size();

        // Compute the price change over the past hour as a percentage
        double endPrice = pastHourKlines.get(0).gettClosePrice();
        double startPrice = pastHourKlines.get(pastHourKlines.size() - 1).gettClosePrice();
        double priceChange = ((endPrice - startPrice) / startPrice) * 100;

        //Log.e(TAG, "AVGPrice: " + averagePrice + " ENDPrice: " + endPrice + " PRICEChange: " + priceChange);

        return priceChange;
    }



    // Function checks if provided status list function countBestCryptoToBuy matches required criteria / shortOrLong --> Long = 1, Short = 0
    public static boolean isKlineApprovedForLongOrShort(List<Integer> sumOf3m, List<Integer> sumOf15m, int shortOrLong) {

        int nrOfGreenKlines3m = 8; // 7
        int nrOfGreenKlines15m = 3;  // 3
        boolean accepted3m = false;
        boolean accepted15m = false;
        int temp = 0;


        for (int i = 0; i < sumOf3m.size(); i++) {

            // For 3m we are looking for <number> green klines in random order
            if (sumOf3m.get(i) == shortOrLong || sumOf3m.get(i) == 2) {
                temp++;
            } else {
                temp += 0;
            }

            // Log.e("UPDService15m", String.valueOf(temp));
            if (temp >= nrOfGreenKlines3m) {
                accepted3m = true;
                break;
            }

            //For 3m we are looking for <number> green klines one after another
//            if (sumOf3m.get(i) == shortOrLong) {
//                temp++;
//            } else if (sumOf3m.get(i) == 2 && temp > 0) {
//                temp++;
//            } else {
//                temp = 0;
//            }
//            // Log.e("UPDService3m", String.valueOf(temp));
//            if (temp == nrOfGreenKlines3m) {
//                accepted3m = true;
//                break;
//            }
        }

        temp = 0;
        //Log.e("UPDService15m", String.valueOf(sumOf15m));

        // For 15m we are looking for 3 green klines in a row
        for (int i = 0; i < sumOf15m.size(); i++) {

            if (sumOf15m.get(i) == shortOrLong) {
                temp++;
            } else if (sumOf15m.get(i) == 2 && temp > 0) {
                temp++;
            } else {
                temp = 0;
            }

            //Log.e("UPDService15m", String.valueOf(temp));
            if (temp == nrOfGreenKlines15m) {
                accepted15m = true;
                break;
            }
        }
        //Log.e("UPDService15m", "3m: " + accepted3m + " 15m: " + accepted15m);
        return (accepted3m && accepted15m);
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

    // Function that checks if crypto chart is going to go up or down with use of EMA, RSI and Stochastic
    public static int predict(List<Kline> klines) {

        // Check if there are enough Klines to perform analysis
        if (klines.size() < 10) {
            System.out.println("Not enough Klines to perform analysis.");
            return -2;
        }

        // Calculate the 8-period and 15-period Exponential Moving Averages (EMA)
        double ema8 = calculateEMA(klines, 8);
        double ema15 = calculateEMA(klines, 15);

        // Calculate the Relative Strength Index (RSI)
        double rsi = calculateRSI(klines, 15);

        // Calculate the Stochastic Oscillator
        double stochastic = calculateStochastic(klines, 14, 5);

        // Make a prediction based on the technical indicators
        if (ema8 > ema15 && rsi > 50 && stochastic > 20) {
            return 1; // Price is likely to rise
        } else {
            return -1; // Price is likely to fall
        }
    }

    // Part of "predict"
    private static double calculateEMA(List<Kline> klines, int period) {
        double k = 2.0 / (period + 1);
        double ema = klines.get(0).gettClosePrice();
        for (int i = 1; i < period; i++) {
            ema = ema * (1 - k) + klines.get(i).gettClosePrice() * k;
        }
        return ema;
    }

    // Part of "predict"
    private static double calculateRSI(List<Kline> klines, int period) {
        double gainSum = 0;
        double lossSum = 0;
        double prevClose = klines.get(0).gettClosePrice();
        for (int i = 1; i < period; i++) {
            double diff = klines.get(i).gettClosePrice() - prevClose;
            if (diff >= 0) {
                gainSum += diff;
            } else {
                lossSum += Math.abs(diff);
            }
            prevClose = klines.get(i).gettClosePrice();
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        double rs = avgGain / avgLoss;
        double rsi = 100 - (100 / (1 + rs));
        return rsi;
    }

    // Part of "predict"
    private static double calculateStochastic(List<Kline> klines, int periodK, int periodD) {
        double[] closes = new double[periodK];
        for (int i = 0; i < periodK; i++) {
            closes[i] = klines.get(klines.size() - 1 - i).gettClosePrice();
        }
        double minLow = getMinLow(klines, periodK);
        double maxHigh = getMaxHigh(klines, periodK);
        double k = 100 * ((closes[periodK - 1] - minLow) / (maxHigh - minLow));
        double[] ks = new double[periodD];
        ks[0] = k;
        for (int i = 1; i < periodD; i++) {
            double sum = ks[i - 1];
            if (i < periodK) {
                sum += k;
            } else {
                sum += 100 * ((closes[periodK - i] - minLow) / (maxHigh - minLow));
            }
            ks[i] = sum / (i + 1);
        }
        return ks[periodD - 1];
    }

    // Part of "predict"
    private static double getMinLow(List<Kline> klines, int period) {
        double minLow = Double.MAX_VALUE;
        for (int i = klines.size() - 1; i >= klines.size() - period; i--) {
            double low = klines.get(i).gettLowPrice();
            if (low < minLow) {
                minLow = low;
            }
        }
        return minLow;
    }

    // Part of "predict"
    private static double getMaxHigh(List<Kline> klines, int period) {
        double maxHigh = Double.MIN_VALUE;
        for (int i = klines.size() - 1; i >= klines.size() - period; i--) {
            double high = klines.get(i).gettHighPrice();
            if (high > maxHigh) {
                maxHigh = high;
            }
        }
        return maxHigh;
    }





}
