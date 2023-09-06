package com.example.cryptofun.services;

import android.content.Context;
import android.util.Log;

import com.example.cryptofun.data.StrategyResult;
import com.example.cryptofun.data.StrategyResultV2;
import com.example.cryptofun.data.database.Kline;

import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BarSeriesManager;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.criteria.VersusBuyAndHoldCriterion;
import org.ta4j.core.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.AbstractEMAIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.PPOIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.adx.MinusDIIndicator;
import org.ta4j.core.indicators.adx.PlusDIIndicator;
import org.ta4j.core.indicators.aroon.AroonDownIndicator;
import org.ta4j.core.indicators.aroon.AroonUpIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.indicators.numeric.NumericIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ServiceFunctionsStrategyTa4J {

    public static StrategyResult strategyTa4J_nr2(List<Kline> klines, Context context) {

        // Create a new empty time series
        Collections.reverse(klines);
        BarSeries series = new BaseBarSeriesBuilder().withName("My_Crypto_Series").build();

        // Load the klines into the time series
        for (int i = 0; i < klines.size(); i++) {

            if (i > 0 && klines.get(i).gettCloseTime() <= klines.get(i - 1).gettCloseTime()) {
                break;
            }
            long endTimeMillis = klines.get(i).gettCloseTime();

            ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault());
            double openPrice = klines.get(i).gettOpenPrice();
            double highPrice = klines.get(i).gettHighPrice();
            double lowPrice = klines.get(i).gettLowPrice();
            double closePrice = klines.get(i).gettClosePrice();
            series.addBar(endTime, openPrice, highPrice, lowPrice, closePrice);
        }
        //Log.e(TAG, series.getBarData().toString());

        BarSeriesManager seriesManager = new BarSeriesManager(series);
        // Moving momentum strategy.
        Strategy strategy1 = buildStrategyMomentum(series);
        // ADX Indicator strategy.
        Strategy strategy2 = buildStrategyADX(series);

        // Running the strategies
        TradingRecord tradingRecord1 = seriesManager.run(strategy1);
        TradingRecord tradingRecord2 = seriesManager.run(strategy2);

        // AnalysisOfStrategies
        AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new GrossReturnCriterion());
        AnalysisCriterion total = new GrossReturnCriterion();
        Strategy bestStrategy = vsBuyAndHold.chooseBest(seriesManager, Arrays.asList(strategy1, strategy2));

        ArrayList<Integer> nrOfPositions = new ArrayList<>();
        ArrayList<Double> vsBuyAndHoldProfit = new ArrayList<>();
        ArrayList<Double> totalProfit = new ArrayList<>();

        nrOfPositions.add(tradingRecord1.getPositionCount());
        nrOfPositions.add(tradingRecord2.getPositionCount());
        vsBuyAndHoldProfit.add(vsBuyAndHold.calculate(series, tradingRecord1).doubleValue());
        vsBuyAndHoldProfit.add(vsBuyAndHold.calculate(series, tradingRecord2).doubleValue());
        totalProfit.add(total.calculate(series, tradingRecord1).doubleValue());
        totalProfit.add(total.calculate(series, tradingRecord2).doubleValue());
        int finalWaveTrendScore = calculateForStrategyNr2(series, 10, 21, 45, 65, -45, -65, context); //53 -level2

        return new StrategyResult(nrOfPositions, vsBuyAndHoldProfit, totalProfit, finalWaveTrendScore, bestStrategy.getName());

    }

    public static int calculateForStrategyNr2(BarSeries series, int n1, int n2, int obLevel1, int obLevel2, int osLevel1, int osLevel2, Context context) {

        // Compute indicators
        NumericIndicator ap = NumericIndicator.of(new TypicalPriceIndicator(series)); //Median originally
        NumericIndicator esa = NumericIndicator.of(new EMAIndicator(ap, n1));
        NumericIndicator diff = ap.minus(esa);
        NumericIndicator x = diff.abs();
        NumericIndicator d = NumericIndicator.of(new EMAIndicator(x, n1));
        NumericIndicator z = d.multipliedBy(0.015);
        NumericIndicator ci = diff.dividedBy(z);
        double multiplier2 = (2.0 / (n2 + 1));

        // Compute the channel index -- overwrite of calculate was need because value at index 0 waa NaN
        NumericIndicator tci = NumericIndicator.of(new AbstractEMAIndicator(ci, n2, multiplier2) {
            @Override
            protected Num calculate(int index) {
                //Log.e(TAG, "ABSTRACT: " + ci.getValue(index) + " " + ci.numOf(index)+ " " + ci.getValue(index).isNaN());
                if (index == 0) {
                    return DecimalNum.valueOf(0);
                }
                Num prevValue = getValue(index - 1);
                return ci.getValue(index).minus(prevValue).multipliedBy(DecimalNum.valueOf(multiplier2)).plus(prevValue);
            }
        });
        // NumericIndicator tci = NumericIndicator.of(new EMAIndicator(diff, n2));
        ATRIndicator atr = new ATRIndicator(series, n1);

        //MACD
        NumericIndicator close = NumericIndicator.of(new ClosePriceIndicator(series));
        MACDIndicator macd = new MACDIndicator(close, 12, 26);
        int signalLinePeriod = 9;
        EMAIndicator signalLine = new EMAIndicator(macd, signalLinePeriod);


        Num numObLevel2 = DecimalNum.valueOf(obLevel2);
        Num numObLevel1 = DecimalNum.valueOf(obLevel1);
        Num numOsLevel2 = DecimalNum.valueOf(osLevel2);
        Num numOsLevel1 = DecimalNum.valueOf(osLevel1);

        // Compute WaveTrend indicators
        Num wt1 = tci.getValue(series.getEndIndex());
        SMAIndicator sma = new SMAIndicator(tci, 4);
        Num wt2 = sma.getValue(series.getEndIndex());

        //Below is the same as SMAIndicator
//        Num wt22 = tci.getValue(series.getEndIndex()).multipliedBy(DecimalNum.valueOf("0.25"))
//                .plus(tci.getValue(series.getEndIndex() - 1).multipliedBy(DecimalNum.valueOf("0.25")))
//                .plus(tci.getValue(series.getEndIndex() - 2).multipliedBy(DecimalNum.valueOf("0.25")))
//                .plus(tci.getValue(series.getEndIndex() - 3).multipliedBy(DecimalNum.valueOf("0.25")));
//
//        for(int i = series.getEndIndex(); i <= series.getEndIndex(); i++) {
//            Log.e("BuildStrategy ", "index: " + i + " ap: " + ap.getValue(i) + " esa: " + esa.getValue(i) + " diff: " + diff.getValue(i) + " x: " + x.getValue(i) + " d: " + d.getValue(i) + " z: " + z.getValue(i)
//                    + " ci: " + ci.getValue(i) + " tci: " + tci.getValue(i) + " wt1: " + wt1.doubleValue() + " wt2: " + wt2.doubleValue() + " atr: " + atr.getValue(i) + " macd: " + macd.getValue(i)
//                    + " signalLine: " + signalLine.getValue(i));
//        }

        DecimalFormat df = new DecimalFormat("0.00000000000");
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.R) {
            // My version of finished - on API30 and higher
            // Check alert conditions and return the corresponding value
            if ((wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numObLevel2))) {
                return -1;
            } else if ((wt2.isLessThan(wt1) && wt1.isLessThan(numOsLevel2))) {
                return 1;
            } else {
                return 0;
            }

        } else {
            int i = series.getEndIndex();
            String info = "Index: " + i + " MedianPrice(ap): " + df.format(ap.getValue(i).doubleValue()) + " EMA(" + n1 + "): " + df.format(esa.getValue(i).doubleValue()) + " EMA(" + n2 + "): " + df.format(d.getValue(i).doubleValue()) + " AbstractEMA(wt1): " + df.format(wt1.doubleValue()) + " SMA(wt2): " + df
                    .format(wt2.doubleValue()) + " ATR: " + df.format(atr.getValue(i).doubleValue()) + " MACD: " + df.format(macd.getValue(i).doubleValue()) + " signalMACD: " + df.format(signalLine.getValue(i).doubleValue());
            Log.e("StrategyValues ", info);
            ServiceFunctionsOther.writeToFile(info, context, "result");
            // Check alert conditions and return the corresponding value
            if ((wt1.isGreaterThan(wt2) && wt1.isGreaterThan(numObLevel2))) {
                return -1;
            } else if ((wt1.isLessThan(wt2) && wt1.isLessThan(numOsLevel2))) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public static StrategyResultV2 strategyTa4J_nr3(List<Kline> klines, int oLevel, int barsToWait, Context context) {

        // Create a new empty time series
        Collections.reverse(klines);
        BarSeries series = new BaseBarSeriesBuilder().withName("My_Crypto_Series").build();

        // Load the klines into the time series
        for (int i = 0; i < klines.size(); i++) {

            if (i > 0 && klines.get(i).gettCloseTime() <= klines.get(i - 1).gettCloseTime()) {
                break;
            }
            long endTimeMillis = klines.get(i).gettCloseTime();

            ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault());
            double openPrice = klines.get(i).gettOpenPrice();
            double highPrice = klines.get(i).gettHighPrice();
            double lowPrice = klines.get(i).gettLowPrice();
            double closePrice = klines.get(i).gettClosePrice();
            series.addBar(endTime, openPrice, highPrice, lowPrice, closePrice);
        }

        // Compute indicators
        NumericIndicator ap = NumericIndicator.of(new TypicalPriceIndicator(series));
        NumericIndicator esa = NumericIndicator.of(new EMAIndicator(ap, 10));
        NumericIndicator diff = ap.minus(esa);
        NumericIndicator x = diff.abs();
        NumericIndicator d = NumericIndicator.of(new EMAIndicator(x, 10));
        NumericIndicator z = d.multipliedBy(0.015);
        NumericIndicator ci = diff.dividedBy(z);
        double multiplier2 = (2.0 / (21 + 1));

        // Compute the channel index -- overwrite of calculate was need because value at index 0 waa NaN
        NumericIndicator tci = NumericIndicator.of(new AbstractEMAIndicator(ci, 21, multiplier2) {
            @Override
            protected Num calculate(int index) {
                //Log.e(TAG, "ABSTRACT: " + ci.getValue(index) + " " + ci.numOf(index)+ " " + ci.getValue(index).isNaN());
                if (index == 0) {
                    return DecimalNum.valueOf(0);
                }
                Num prevValue = getValue(index - 1);
                return ci.getValue(index).minus(prevValue).multipliedBy(DecimalNum.valueOf(multiplier2)).plus(prevValue);
            }
        });
        SMAIndicator sma = new SMAIndicator(tci, 4);

        Num numObLevel2 = DecimalNum.valueOf(oLevel);
        Num numOsLevel2 = DecimalNum.valueOf(-oLevel);

        //MACD
        NumericIndicator close = NumericIndicator.of(new ClosePriceIndicator(series));
        NumericIndicator macd = NumericIndicator.of(new MACDIndicator(close, 12, 26));
        int signalLinePeriod = 9;
        NumericIndicator signalLine = NumericIndicator.of(new EMAIndicator(macd, signalLinePeriod));

        //ATR
        ATRIndicator atr = new ATRIndicator(series, 10);

        // Compute WaveTrend indicators
        DecimalFormat df = new DecimalFormat("0.00000000000");
        int resultWT = 0;
        int resultMACD = 0;

        for (int i = series.getEndIndex() - barsToWait; i <= series.getEndIndex(); i++) {

            Num wt1 = tci.getValue(i);
            Num wt2 = sma.getValue(i);
            Num macdEnd = macd.getValue(i);
            Num signalEnd = signalLine.getValue(i);

            if ((resultWT == 0 && wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numObLevel2))) {
                resultWT = -1;
                String info = "Passed WT SHORT index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue());
                Log.e("StrategyValues ", info);
                ServiceFunctionsOther.writeToFile(info, context, "result");
                continue;
            }

            if ((resultWT == 0 && wt1.isGreaterThan(wt2) && wt1.isLessThan(numOsLevel2))) {
                resultWT = 1;
                String info = "Passed WT LONG index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue());
                Log.e("StrategyValues ", info);
                ServiceFunctionsOther.writeToFile(info, context, "result");
                continue;
            }

            if (resultWT == -1 && resultMACD == 0 && macdEnd.isLessThan(signalEnd)) {
                resultMACD = -1;
                String info = "Passed MACD SHORT index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue());
                Log.e("StrategyValues ", info);
                ServiceFunctionsOther.writeToFile(info, context, "result");
            }

            if (resultWT == 1 && resultMACD == 0 && macdEnd.isGreaterThan(signalEnd)) {
                resultMACD = 1;
                String info = "Passed MACD LONG index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue());
                Log.e("StrategyValues ", info);
                ServiceFunctionsOther.writeToFile(info, context, "result");
            }
            
        }

//        NumericIndicator high = NumericIndicator.of(new HighPriceIndicator(series));
//        NumericIndicator low = NumericIndicator.of(new LowPriceIndicator(series));
//        NumericIndicator highLow = high.minus(low).dividedBy(2);
//        NumericIndicator aoSlow = NumericIndicator.of(new EMAIndicator(highLow, 34));
//        NumericIndicator aoFast = NumericIndicator.of(new EMAIndicator(highLow, 5));
//        NumericIndicator ao = aoFast.minus(aoSlow);
//        NumericIndicator lsma = NumericIndicator.of(new Lsma(macd, signalLinePeriod));
//        length = input.int(title="LSMA Length", defval=250)
//        offset = input.int(title="LSMA Offset", defval=0)
//        lsma = ta.linreg(wt1, 250, 0)

        //Below is the same as SMAIndicator
//        Num wt22 = tci.getValue(series.getEndIndex()).multipliedBy(DecimalNum.valueOf("0.25"))
//                .plus(tci.getValue(series.getEndIndex() - 1).multipliedBy(DecimalNum.valueOf("0.25")))
//                .plus(tci.getValue(series.getEndIndex() - 2).multipliedBy(DecimalNum.valueOf("0.25")))
//                .plus(tci.getValue(series.getEndIndex() - 3).multipliedBy(DecimalNum.valueOf("0.25")));
//

        return new StrategyResultV2(resultMACD, resultWT, -2, -2, -2, -2, -2);
    }

    public static StrategyResultV2 strategyTa4J_nr5(List<Kline> klines, int oLevel, int barsToWait, float bottomPPO, float topPPO, int adxLimit, Context context) {

        // Create a new empty time series
        Collections.reverse(klines);
        BarSeries series = new BaseBarSeriesBuilder().withName("My_Crypto_Series").build();

        // Load the klines into the time series
        for (int i = 0; i < klines.size(); i++) {

            if (i > 0 && klines.get(i).gettCloseTime() <= klines.get(i - 1).gettCloseTime()) {
                break;
            }
            long endTimeMillis = klines.get(i).gettCloseTime();

            ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault());
            double openPrice = klines.get(i).gettOpenPrice();
            double highPrice = klines.get(i).gettHighPrice();
            double lowPrice = klines.get(i).gettLowPrice();
            double closePrice = klines.get(i).gettClosePrice();
            series.addBar(endTime, openPrice, highPrice, lowPrice, closePrice);
        }

        // Compute indicators
        NumericIndicator ap = NumericIndicator.of(new TypicalPriceIndicator(series));
        NumericIndicator esa = NumericIndicator.of(new EMAIndicator(ap, 10));
        NumericIndicator diff = ap.minus(esa);
        NumericIndicator x = diff.abs();
        NumericIndicator d = NumericIndicator.of(new EMAIndicator(x, 10));
        NumericIndicator z = d.multipliedBy(0.015);
        NumericIndicator ci = diff.dividedBy(z);
        double multiplier2 = (2.0 / (21 + 1));

        // Compute the channel index -- overwrite of calculate was need because value at index 0 waa NaN
        NumericIndicator tci = NumericIndicator.of(new AbstractEMAIndicator(ci, 21, multiplier2) {
            @Override
            protected Num calculate(int index) {
                //Log.e(TAG, "ABSTRACT: " + ci.getValue(index) + " " + ci.numOf(index)+ " " + ci.getValue(index).isNaN());
                if (index == 0) {
                    return DecimalNum.valueOf(0);
                }
                Num prevValue = getValue(index - 1);
                return ci.getValue(index).minus(prevValue).multipliedBy(DecimalNum.valueOf(multiplier2)).plus(prevValue);
            }
        });
        SMAIndicator sma = new SMAIndicator(tci, 4);

        Num numObLevel2 = DecimalNum.valueOf(oLevel);
        Num numOsLevel2 = DecimalNum.valueOf(-oLevel);

        //MACD
        NumericIndicator close = NumericIndicator.of(new ClosePriceIndicator(series));
        NumericIndicator macd = NumericIndicator.of(new MACDIndicator(close, 12, 26));
        int signalLinePeriod = 9;
        NumericIndicator signalLine = NumericIndicator.of(new EMAIndicator(macd, signalLinePeriod));

        //ATR
        NumericIndicator atr = NumericIndicator.of(new ATRIndicator(series, 10));

        //ADX
        NumericIndicator adx = NumericIndicator.of(new ADXIndicator(series, 14,14));
        Num limitADX = DecimalNum.valueOf(adxLimit);

        //PPO
        NumericIndicator ppo = NumericIndicator.of(new PPOIndicator(close, 10,21));
        Num limitBottomPPO_forShort = DecimalNum.valueOf(bottomPPO);
        Num limitTopPPO_forShort = DecimalNum.valueOf(topPPO);
        Num limitBottomPPO_forLong = DecimalNum.valueOf(topPPO).negate();
        Num limitTopPPO_forLong = DecimalNum.valueOf(bottomPPO).negate();
        Num ppoBefore = ppo.getValue(series.getEndIndex() - barsToWait);

        // Compute Indicators
        DecimalFormat df = new DecimalFormat("0.00000000000");
        int resultWT = 0;
        int resultADX = 0;
        int resultPPO = 0;

        for (int i = series.getEndIndex() - barsToWait; i <= series.getEndIndex(); i++) {

            Num wt1 = tci.getValue(i);
            Num wt2 = sma.getValue(i);
            Num adxEnd = adx.getValue(i);
            Num ppoEnd = ppo.getValue(i);

            if ((resultWT == 0 && wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numObLevel2))) {
                resultWT = -1;
                String info = "Passed WT SHORT index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue()) + " adx: " + df.format(adx.getValue(i).doubleValue()) + " ppo: " + df.format(ppo.getValue(i).doubleValue()) + "ppoBefore: " + df.format(ppoBefore.doubleValue());
                Log.e("StrategyValues ", info);
                ServiceFunctionsOther.writeToFile(info, context, "result");
            }

            if ((resultWT == 0 && wt1.isGreaterThan(wt2) && wt1.isLessThan(numOsLevel2))) {
                resultWT = 1;
                String info = "Passed WT LONG index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue()) + " adx: " + df.format(adx.getValue(i).doubleValue()) + " ppo: " + df.format(ppo.getValue(i).doubleValue()) + "ppoBefore: " + df.format(ppoBefore.doubleValue());
                Log.e("StrategyValues ", info);
                ServiceFunctionsOther.writeToFile(info, context, "result");
            }

            if ((resultWT == -1 || resultWT == 1) && resultADX == 0 && adxEnd.isGreaterThan(limitADX)) {
                resultADX = 1;
                String info = "Passed ADX SHORT/LONG index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue()) + " adx: " + df.format(adx.getValue(i).doubleValue()) + " ppo: " + df.format(ppo.getValue(i).doubleValue()) + "ppoBefore: " + df.format(ppoBefore.doubleValue());
                Log.e("StrategyValues ", info);
                ServiceFunctionsOther.writeToFile(info, context, "result");
            }

            if (resultWT == 1 && resultPPO == 0 && ppoEnd.isGreaterThan(limitBottomPPO_forLong) && ppoEnd.isLessThan(limitTopPPO_forLong) && ppoEnd.isGreaterThan(ppoBefore)) {
                resultPPO = 1;
                String info = "Passed PPO LONG index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue()) + " adx: " + df.format(adx.getValue(i).doubleValue()) + " ppo: " + df.format(ppo.getValue(i).doubleValue()) + "ppoBefore: " + df.format(ppoBefore.doubleValue());
                Log.e("StrategyValues ", info);
                ServiceFunctionsOther.writeToFile(info, context, "result");
            }

            if (resultWT == -1 && resultPPO == 0 && ppoEnd.isGreaterThan(limitBottomPPO_forShort) && ppoEnd.isLessThan(limitTopPPO_forShort) && ppoEnd.isLessThan(ppoBefore)) {
                resultPPO = -1;
                String info = "Passed PPO SHORT index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue()) + " adx: " + df.format(adx.getValue(i).doubleValue()) + " ppo: " + df.format(ppo.getValue(i).doubleValue()) + "ppoBefore: " + df.format(ppoBefore.doubleValue());
                Log.e("StrategyValues ", info);
                ServiceFunctionsOther.writeToFile(info, context, "result");
            }

        }

        return new StrategyResultV2(-2, resultWT, resultPPO, resultADX, -2, -2, -2);
    }

    public static StrategyResultV2 strategyTa4J_nr6(List<Kline> klines, int oLevel, int barsToWait, int rsiLong, int rsiShort, Context context) {

        // Create a new empty time series
        Collections.reverse(klines);
        BarSeries series = new BaseBarSeriesBuilder().withName("My_Crypto_Series").build();

        // Load the klines into the time series
        for (int i = 0; i < klines.size(); i++) {

            if (i > 0 && klines.get(i).gettCloseTime() <= klines.get(i - 1).gettCloseTime()) {
                break;
            }
            long endTimeMillis = klines.get(i).gettCloseTime();

            ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault());
            double openPrice = klines.get(i).gettOpenPrice();
            double highPrice = klines.get(i).gettHighPrice();
            double lowPrice = klines.get(i).gettLowPrice();
            double closePrice = klines.get(i).gettClosePrice();
            series.addBar(endTime, openPrice, highPrice, lowPrice, closePrice);
        }

        // Compute indicators
        NumericIndicator ap = NumericIndicator.of(new TypicalPriceIndicator(series));
        NumericIndicator esa = NumericIndicator.of(new EMAIndicator(ap, 10));
        NumericIndicator diff = ap.minus(esa);
        NumericIndicator x = diff.abs();
        NumericIndicator d = NumericIndicator.of(new EMAIndicator(x, 10));
        NumericIndicator z = d.multipliedBy(0.015);
        NumericIndicator ci = diff.dividedBy(z);
        double multiplier2 = (2.0 / (21 + 1));

        // Compute the channel index -- overwrite of calculate was need because value at index 0 waa NaN
        NumericIndicator tci = NumericIndicator.of(new AbstractEMAIndicator(ci, 21, multiplier2) {
            @Override
            protected Num calculate(int index) {
                //Log.e(TAG, "ABSTRACT: " + ci.getValue(index) + " " + ci.numOf(index)+ " " + ci.getValue(index).isNaN());
                if (index == 0) {
                    return DecimalNum.valueOf(0);
                }
                Num prevValue = getValue(index - 1);
                return ci.getValue(index).minus(prevValue).multipliedBy(DecimalNum.valueOf(multiplier2)).plus(prevValue);
            }
        });
        SMAIndicator sma = new SMAIndicator(tci, 4);
        Num numObLevel2 = DecimalNum.valueOf(oLevel);
        Num numOsLevel2 = DecimalNum.valueOf(-oLevel);

        //MACD
        NumericIndicator close = NumericIndicator.of(new ClosePriceIndicator(series));
        NumericIndicator macd = NumericIndicator.of(new MACDIndicator(close, 12, 26));
        NumericIndicator signalLine = NumericIndicator.of(new EMAIndicator(macd, 9));
        //ATR
        NumericIndicator atr = NumericIndicator.of(new ATRIndicator(series, 10));
        //ADX
        NumericIndicator adx = NumericIndicator.of(new ADXIndicator(series, 14,14));
        //PPO
        NumericIndicator ppo = NumericIndicator.of(new PPOIndicator(close, 10,21));

        //AROON
        NumericIndicator low = NumericIndicator.of(new LowPriceIndicator(series));
        NumericIndicator high = NumericIndicator.of(new HighPriceIndicator(series));
        NumericIndicator aroonLow = NumericIndicator.of(new AroonDownIndicator(low, 5));
        NumericIndicator aroonHigh = NumericIndicator.of(new AroonUpIndicator(high, 5));

        //RSI
        Num rsiLongLimit = DecimalNum.valueOf(rsiLong);
        Num rsiShortLimit = DecimalNum.valueOf(rsiShort);
        NumericIndicator rsi = NumericIndicator.of(new RSIIndicator(close,5));

        // Compute Indicators
        DecimalFormat df = new DecimalFormat("0.00000000000");
        int resultAROON = 0;
        int resultRSI = 0;
        Num arronLimit = DecimalNum.valueOf(90);

        if (series.getBarCount() > barsToWait) {
            for (int i = series.getEndIndex() - barsToWait; i <= series.getEndIndex(); i++) {

                Num rsiEnd = rsi.getValue(i);
                Num aroonLowEnd = aroonLow.getValue(i);
                Num aroonHighEnd = aroonHigh.getValue(i);

                if (resultRSI == 0 && rsiEnd.isGreaterThan(rsiShortLimit)) {
                    resultRSI = -1;
                    String info = "Passed RSI SHORT index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue()) + " adx: " + df.format(adx.getValue(i).doubleValue()) + " ppo: " + df.format(ppo.getValue(i).doubleValue()) + " aroonUp: " + df.format(aroonHigh.getValue(i).doubleValue()) + " aroonDown: " + df.format(aroonLow.getValue(i).doubleValue()) + " rsi: " + df.format(rsi.getValue(i).doubleValue());
                    Log.e("StrategyValues ", info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                }

                if (resultRSI == 0 && rsiEnd.isLessThan(rsiLongLimit)) {
                    resultRSI = 1;
                    String info = "Passed RSI LONG index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue()) + " adx: " + df.format(adx.getValue(i).doubleValue()) + " ppo: " + df.format(ppo.getValue(i).doubleValue()) + " aroonUp: " + df.format(aroonHigh.getValue(i).doubleValue()) + " aroonDown: " + df.format(aroonLow.getValue(i).doubleValue()) + " rsi: " + df.format(rsi.getValue(i).doubleValue());
                    Log.e("StrategyValues ", info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                }

                if (resultRSI == -1 && resultAROON == 0 && aroonLowEnd.isGreaterThan(aroonHighEnd) && aroonLowEnd.isGreaterThan(arronLimit)) {
                    resultAROON = -1;
                    String info = "Passed AROON SHORT index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue()) + " adx: " + df.format(adx.getValue(i).doubleValue()) + " ppo: " + df.format(ppo.getValue(i).doubleValue()) + " aroonUp: " + df.format(aroonHigh.getValue(i).doubleValue()) + " aroonDown: " + df.format(aroonLow.getValue(i).doubleValue()) + " rsi: " + df.format(rsi.getValue(i).doubleValue());
                    Log.e("StrategyValues ", info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                }

                if (resultRSI == 1 && resultAROON == 0 && aroonHighEnd.isGreaterThan(aroonLowEnd) && aroonLowEnd.isGreaterThan(arronLimit)) {
                    resultAROON = 1;
                    String info = "Passed AROON LONG index: " + i + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + " hlc3: " + df.format(ap.getValue(i).doubleValue()) + " ema10: " + df.format(esa.getValue(i).doubleValue()) + " wt1: " + df.format(tci.getValue(i).doubleValue()) + " wt2: " + df.format(sma.getValue(i).doubleValue()) + " atr: " + df.format(atr.getValue(i).doubleValue()) + " macd: " + df.format(macd.getValue(i).doubleValue()) + " signalLine: " + df.format(signalLine.getValue(i).doubleValue()) + " adx: " + df.format(adx.getValue(i).doubleValue()) + " ppo: " + df.format(ppo.getValue(i).doubleValue()) + " aroonUp: " + df.format(aroonHigh.getValue(i).doubleValue()) + " aroonDown: " + df.format(aroonLow.getValue(i).doubleValue()) + " rsi: " + df.format(rsi.getValue(i).doubleValue());
                    Log.e("StrategyValues ", info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                }

            }
        }
        return new StrategyResultV2(-2, -2, -2, -2, resultAROON, -2, resultRSI);
    }

    public static int strategyTa4J_nr4(List<Kline> klines, Context context) {

        // Create a new empty time series
        Collections.reverse(klines);
        BarSeries series = new BaseBarSeriesBuilder().withName("My_Crypto_Series").build();

        // Load the klines into the time series
        for (int i = 0; i < klines.size(); i++) {

            if (i > 0 && klines.get(i).gettCloseTime() <= klines.get(i - 1).gettCloseTime()) {
                break;
            }
            long endTimeMillis = klines.get(i).gettCloseTime();

            ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault());
            double openPrice = klines.get(i).gettOpenPrice();
            double highPrice = klines.get(i).gettHighPrice();
            double lowPrice = klines.get(i).gettLowPrice();
            double closePrice = klines.get(i).gettClosePrice();
            series.addBar(endTime, openPrice, highPrice, lowPrice, closePrice);
        }

        // Compute indicators
        NumericIndicator ap = NumericIndicator.of(new TypicalPriceIndicator(series));
        NumericIndicator esa = NumericIndicator.of(new EMAIndicator(ap, 10));
        NumericIndicator diff = ap.minus(esa);
        NumericIndicator x = diff.abs();
        NumericIndicator d = NumericIndicator.of(new EMAIndicator(x, 10));
        NumericIndicator z = d.multipliedBy(0.015);
        NumericIndicator ci = diff.dividedBy(z);
        double multiplier2 = (2.0 / (21 + 1));

        // Compute the channel index -- overwrite of calculate was need because value at index 0 waa NaN
        NumericIndicator tci = NumericIndicator.of(new AbstractEMAIndicator(ci, 21, multiplier2) {
            @Override
            protected Num calculate(int index) {
                //Log.e(TAG, "ABSTRACT: " + ci.getValue(index) + " " + ci.numOf(index)+ " " + ci.getValue(index).isNaN());
                if (index == 0) {
                    return DecimalNum.valueOf(0);
                }
                Num prevValue = getValue(index - 1);
                return ci.getValue(index).minus(prevValue).multipliedBy(DecimalNum.valueOf(multiplier2)).plus(prevValue);
            }
        });
        // NumericIndicator tci = NumericIndicator.of(new EMAIndicator(diff, n2));
        ATRIndicator atr = new ATRIndicator(series, 10);

        Num numObLevel2 = DecimalNum.valueOf(15);
        Num numOsLevel2 = DecimalNum.valueOf(-15);

        // Compute WaveTrend indicators
        Num wt1 = tci.getValue(series.getEndIndex());
        SMAIndicator sma = new SMAIndicator(tci, 4);
        Num wt2 = sma.getValue(series.getEndIndex());
        Num ema10 = esa.getValue(series.getEndIndex());
        Num median = ap.getValue(series.getEndIndex());

        //MACD
        NumericIndicator close = NumericIndicator.of(new ClosePriceIndicator(series));
        NumericIndicator macd = NumericIndicator.of(new MACDIndicator(close, 12, 26));
        int signalLinePeriod = 9;
        NumericIndicator signalLine = NumericIndicator.of(new EMAIndicator(macd, signalLinePeriod));
        Num macdEnd = macd.getValue(series.getEndIndex());
        Num signalEnd = signalLine.getValue(series.getEndIndex());

        DecimalFormat df = new DecimalFormat("0.00000000000");
        int i = series.getEndIndex();

        String info = "Index: " + i + " MedianPrice(ap): " + df.format(ap.getValue(i).doubleValue()) + " EMA(10): " + df.format(esa.getValue(i).doubleValue()) + " AbstractEMA(wt1): " + df.format(wt1.doubleValue()) + " SMA(wt2): " + df.format(wt2.doubleValue()) + " ATR: " + df.format(atr.getValue(i).doubleValue()) + " MACD: " + df.format(macd.getValue(i).doubleValue()) + " signalMACD: " + df.format(signalLine.getValue(i).doubleValue());
        Log.e("StrategyValues ", info);
        ServiceFunctionsOther.writeToFile(info, context, "result");
        // Check alert conditions and return the corresponding value
        if (wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numObLevel2) && wt2.isGreaterThan(numObLevel2)&& macdEnd.isLessThan(signalEnd)) {
            return -1;
        } else if ((wt1.isGreaterThan(wt2) && wt1.isLessThan(numOsLevel2) && wt2.isLessThan(numOsLevel2) && macdEnd.isGreaterThan(signalEnd))) {
            return 1;
        } else {
            return 0;
        }

    }

    public static Strategy buildStrategyMomentum(BarSeries series) {

        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // The bias is bullish when the shorter-moving average moves above the longer
        // moving average.
        // The bias is bearish when the shorter-moving average moves below the longer
        // moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 5); //9
        EMAIndicator longEma = new EMAIndicator(closePrice, 26); //26

        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);

        MACDIndicator macd = new MACDIndicator(closePrice, 5, 26); // 9, 12
        EMAIndicator emaMacd = new EMAIndicator(macd, 18);

        // Entry rule
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillK, 20)) // Signal 1
                .and(new OverIndicatorRule(macd, emaMacd)); // Signal 2

        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillK, 80)) // Signal 1
                .and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2

        return new BaseStrategy("MOMENTUM", entryRule, exitRule);

    }

    public static Strategy buildStrategyADX(BarSeries series) {

        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
        final SMAIndicator smaIndicator = new SMAIndicator(closePriceIndicator, 50);

        final int adxBarCount = 14;
        final ADXIndicator adxIndicator = new ADXIndicator(series, adxBarCount);
        final OverIndicatorRule adxOver20Rule = new OverIndicatorRule(adxIndicator, 20);

        final PlusDIIndicator plusDIIndicator = new PlusDIIndicator(series, adxBarCount);
        final MinusDIIndicator minusDIIndicator = new MinusDIIndicator(series, adxBarCount);

        final Rule plusDICrossedUpMinusDI = new CrossedUpIndicatorRule(plusDIIndicator, minusDIIndicator);
        final Rule plusDICrossedDownMinusDI = new CrossedDownIndicatorRule(plusDIIndicator, minusDIIndicator);
        final OverIndicatorRule closePriceOverSma = new OverIndicatorRule(closePriceIndicator, smaIndicator);
        final Rule entryRule = adxOver20Rule.and(plusDICrossedUpMinusDI).and(closePriceOverSma);

        final UnderIndicatorRule closePriceUnderSma = new UnderIndicatorRule(closePriceIndicator, smaIndicator);
        final Rule exitRule = adxOver20Rule.and(plusDICrossedDownMinusDI).and(closePriceUnderSma);

        return new BaseStrategy("ADX", entryRule, exitRule, adxBarCount);

    }


}
