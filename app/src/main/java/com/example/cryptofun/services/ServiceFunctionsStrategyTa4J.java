package com.example.cryptofun.services;

import android.content.Context;
import android.util.Log;

import com.example.cryptofun.data.StrategyParameters;
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
import org.ta4j.core.indicators.ParabolicSarIndicator;
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
import org.ta4j.core.indicators.helpers.MedianPriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuKijunSenIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuSenkouSpanAIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuSenkouSpanBIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuTenkanSenIndicator;
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

    public static StrategyResultV2 strategyTa4J(List<Kline> klines, int strategyNr, StrategyParameters params, Context context) {

        // Prepare barSeries to compute Technical Indicators (remember that this revers affects original list)
        Collections.reverse(klines);
        BarSeries series = new BaseBarSeriesBuilder().withName("SeriesForStrategy").build();

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
        //
        // PriceIndicators
        //
        NumericIndicator hlc3 = NumericIndicator.of(new TypicalPriceIndicator(series));
        NumericIndicator close = NumericIndicator.of(new ClosePriceIndicator(series));
        NumericIndicator low = NumericIndicator.of(new LowPriceIndicator(series));
        NumericIndicator high = NumericIndicator.of(new HighPriceIndicator(series));
        NumericIndicator open = NumericIndicator.of(new OpenPriceIndicator(series));
        NumericIndicator hl2 = NumericIndicator.of(new MedianPriceIndicator(series));
        //
        // EMA Indicators
        //
        NumericIndicator ema12 = NumericIndicator.of(new EMAIndicator(hlc3, 12));
        NumericIndicator ema32 = NumericIndicator.of(new EMAIndicator(hlc3, 32));
        NumericIndicator ema92 = NumericIndicator.of(new EMAIndicator(hlc3, 92));
        NumericIndicator ema181 = NumericIndicator.of(new EMAIndicator(hlc3, 181));
        //
        // Computations for WaveTrend (wt1 = tci, wt2 = sma)
        //
        NumericIndicator diff = hlc3.minus(ema12);
        NumericIndicator x = diff.abs();
        NumericIndicator d = NumericIndicator.of(new EMAIndicator(x, 12));
        NumericIndicator z = d.multipliedBy(0.015);
        NumericIndicator ci = diff.dividedBy(z);
        double multiplier2 = (2.0 / (21 + 1));
        //
        // Compute the channel index -- overwrite of calculate was need because value at index 0 waa NaN
        //
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
        NumericIndicator sma = NumericIndicator.of(new SMAIndicator(tci, 4));
        //
        // Computations for highLowPercentDiff
        //
        NumericIndicator highLowPercentDiff = low.multipliedBy(100).dividedBy(high);
        //
        // Computations for MACD
        //
        NumericIndicator macd = NumericIndicator.of(new MACDIndicator(close, 12, 26));
        int signalLinePeriod = 9;
        NumericIndicator signalLine = NumericIndicator.of(new EMAIndicator(macd, signalLinePeriod));
        //
        // Computations for ATR
        //
        NumericIndicator atr = NumericIndicator.of(new ATRIndicator(series, 10));
        //
        // Computations for SAR
        //
        NumericIndicator sar = NumericIndicator.of(new ParabolicSarIndicator(series));
        //
        // Computations for ADX
        //
        NumericIndicator adx = NumericIndicator.of(new ADXIndicator(series, 14, 14));
        //
        // Computations for Ichimoku
        //
        NumericIndicator IchimokuLeadingSpanA = NumericIndicator.of(new IchimokuSenkouSpanAIndicator(series, 9, 26));
        NumericIndicator IchimokuLeadingSpanB = NumericIndicator.of(new IchimokuSenkouSpanBIndicator(series, 52));
        NumericIndicator IchimokuBaseLine = NumericIndicator.of(new IchimokuKijunSenIndicator(series, 26));
        NumericIndicator IchimokuConversionLine = NumericIndicator.of(new IchimokuTenkanSenIndicator(series, 9));
        //
        // Computations for PPO
        //
        NumericIndicator ppo = NumericIndicator.of(new PPOIndicator(close, 12, 32));
        //
        // Computations for RSI
        //
        NumericIndicator rsi = NumericIndicator.of(new RSIIndicator(close, 14));
        //
        // Computations for AROON
        //
        NumericIndicator aroonLow = NumericIndicator.of(new AroonDownIndicator(low, 5));
        NumericIndicator aroonHigh = NumericIndicator.of(new AroonUpIndicator(high, 5));

        DecimalFormat df = new DecimalFormat("0.00000000000");
        int resultRSI = -2;
        int resultWT = -2;
        int resultMACD = -2;
        int resultPPO = -2;
        int resultEMA = -2;
        int resultADX = -2;
        int resultAROON = -2;
        int resultICHIMOKU = -2;
        Num numOfWTTopLevel = DecimalNum.valueOf(params.getWTLevel());
        Num numOfWTBottomLevel = DecimalNum.valueOf(-params.getWTLevel());
        Num numOfRSITopLevel = DecimalNum.valueOf(params.getRSILevelTop());
        Num numOfRSIBottomLevel = DecimalNum.valueOf(params.getRSILevelBottom());
        Num numOfarronLimit = DecimalNum.valueOf(params.getARRONLevel());
        Num numOfAdxLimit = DecimalNum.valueOf(params.getADXLevel());
        Num numOfLimitBottomPPO_forShort = DecimalNum.valueOf(params.getPPOLevelBottom());
        Num numOfLimitTopPPO_forShort = DecimalNum.valueOf(params.getPPOLevelTop());
        Num numOfLimitBottomPPO_forLong = DecimalNum.valueOf(params.getPPOLevelTop()).negate();
        Num numOfLimitTopPPO_forLong = DecimalNum.valueOf(params.getPPOLevelBottom()).negate();
        Num numOfPpoBefore = ppo.getValue(series.getEndIndex() - params.getNrOfKlinesToWait());

        switch (strategyNr) {
            case 0:
                //testQuickStrategy
                int sumOfSeries = 0;
                int seriesSize = 4; //4
                int longShort = 0;
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num ema181num = ema181.getValue(i);
                        Num hlc3num = hlc3.getValue(i);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [ema181]: " + df.format(ema181.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue());

                        if (ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema92num.isLessThan(ema181num) && hlc3num.isLessThan(ema12num) && hlc3num.isLessThan(hlc3.getValue(i-1))) {

                            if (sumOfSeries == 0 ) {
                                sumOfSeries++;
                                longShort = 1;
                            } else if ( longShort == 1 ){
                                sumOfSeries++;
                            }

                            if (sumOfSeries >= seriesSize ) {
                                resultEMA = 1;
                            }
                            String info = "Passed EMA LONG " + endOFLogEntry + " SeriesSizeEMA: " + sumOfSeries;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (sumOfSeries > 0 && longShort == 1) {
                            sumOfSeries = 0;
                        }

                        if (ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) && ema92num.isGreaterThan(ema181num) && hlc3num.isGreaterThan(ema12num) && hlc3num.isGreaterThan(hlc3.getValue(i-1))) {

                            if (sumOfSeries == 0 ) {
                                sumOfSeries++;
                                longShort = -1;
                            } else if ( longShort == -1 ){
                                sumOfSeries++;
                            }

                            if (sumOfSeries >= seriesSize) { // && close.getValue(series.getEndIndex()).isLessThan(open.getValue(series.getEndIndex()))
                                resultEMA = -1;
                            }
                            String info = "Passed EMA SHORT " + endOFLogEntry + " SeriesSizeEMA: " + sumOfSeries;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (sumOfSeries > 0 && longShort == -1) {
                            sumOfSeries = 0;
                        }
                    }
                }
                break;
            case 1:
                int sumOfEMASeries = 0;
                int howLongSeriesInRow = 4; //4
                int longOrShort = 0;
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num ema181num = ema181.getValue(i);
                        Num closeNum = close.getValue(i);
                        Num highNum = high.getValue(i);
                        Num lowNum = low.getValue(i);
                        Num openNum = open.getValue(i);
                        Num wt1 = tci.getValue(i);
                        Num wt2 = sma.getValue(i);

                        double percentDiff = (ema32.getValue(i).doubleValue() * 100) / ema181.getValue(i).doubleValue();

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [ema181]: " + df.format(ema181.getValue(i).doubleValue()) + " [emaDiff]: " + df.format(percentDiff) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue());

                        if (ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema92num.isLessThan(ema181num) && closeNum.isLessThan(ema12num) && highNum.isLessThan(ema12num) && lowNum.isLessThan(ema12num) && openNum.isLessThan(ema12num) && closeNum.isLessThan(close.getValue(i-1)) && percentDiff < 97) {

                            if (sumOfEMASeries == 0 ) {
                                sumOfEMASeries++;
                                longOrShort = 1;
                            } else if ( longOrShort == 1 ){
                                sumOfEMASeries++;
                            }

                            if (sumOfEMASeries >= howLongSeriesInRow ) { //&& close.getValue(series.getEndIndex()).isGreaterThan(open.getValue(series.getEndIndex()))
                                resultEMA = 1;
                            }
                            String info = "Passed EMA LONG " + endOFLogEntry + " SeriesSizeEMA: " + sumOfEMASeries;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (sumOfEMASeries > 0 && longOrShort == 1) {
                            sumOfEMASeries = 0;
                        }

                        if (ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) && ema92num.isGreaterThan(ema181num) && closeNum.isGreaterThan(ema12num) && highNum.isGreaterThan(ema12num) && lowNum.isGreaterThan(ema12num) && openNum.isGreaterThan(ema12num) && closeNum.isGreaterThan(close.getValue(i-1)) && percentDiff > 103) {

                            if (sumOfEMASeries == 0 ) {
                                sumOfEMASeries++;
                                longOrShort = -1;
                            } else if ( longOrShort == -1 ){
                                sumOfEMASeries++;
                            }

                            if (sumOfEMASeries >= howLongSeriesInRow) { // && close.getValue(series.getEndIndex()).isLessThan(open.getValue(series.getEndIndex()))
                                resultEMA = -1;
                            }
                            String info = "Passed EMA SHORT " + endOFLogEntry + " SeriesSizeEMA: " + sumOfEMASeries;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (sumOfEMASeries > 0 && longOrShort == -1) {
                            sumOfEMASeries = 0;
                        }

                        if (i > series.getEndIndex() - 3 && resultWT == -2 && wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numOfWTTopLevel)) {
                            resultWT = -1;
                            String info = "Passed WT SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (i > series.getEndIndex() - 3 && resultWT == -2 && wt1.isGreaterThan(wt2) && wt1.isLessThan(numOfWTBottomLevel)) {
                            resultWT = 1;
                            String info = "Passed WT LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                    }
                }
                break;
            case 2:
                int sum = 0;
                int seriesLength = 2;
                int LNorSH = 0;
                Num percentLimit = DecimalNum.valueOf(95);
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num ema181num = ema181.getValue(i);
                        Num closeNum = close.getValue(i);
                        Num highNum = high.getValue(i);
                        Num lowNum = low.getValue(i);
                        Num openNum = open.getValue(i);
                        Num wt1 = tci.getValue(i);
                        Num wt2 = sma.getValue(i);
                        Num highLowPercent = highLowPercentDiff.getValue(i);

                        double percentDiff = (ema32.getValue(i).doubleValue() * 100) / ema181.getValue(i).doubleValue();

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [ema181]: " + df.format(ema181.getValue(i).doubleValue()) + " [emaDiff]: " + df.format(percentDiff) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue()) + " [HL%]: " + df.format(highLowPercentDiff.getValue(i).doubleValue());

                        if (resultWT == -2 && wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numOfWTTopLevel)) {
                            resultWT = -1;
                            String info = "Passed WT SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultWT == -2 && wt1.isGreaterThan(wt2) && wt1.isLessThan(numOfWTBottomLevel)) {
                            resultWT = 1;
                            String info = "Passed WT LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (i > series.getEndIndex() - seriesLength && resultWT == 1 && ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema92num.isLessThan(ema181num) && closeNum.isLessThan(ema12num) && highNum.isLessThan(ema12num) && lowNum.isLessThan(ema12num) && openNum.isLessThan(ema12num) && closeNum.isLessThan(close.getValue(i-1)) && highLowPercent.isGreaterThan(percentLimit)) {

                            if (sum == 0 ) {
                                sum++;
                                LNorSH = 1;
                            } else if ( LNorSH == 1 ){
                                sum++;
                            }

                            if (sum >= seriesLength ) {
                                resultEMA = 1;
                            }
                            String info = "Passed EMA LONG " + endOFLogEntry + " SeriesSizeEMA: " + sum;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (sum > 0 && LNorSH == 1) {
                            sum = 0;
                        }

                        if (i > series.getEndIndex() - seriesLength && resultWT == -1 && ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) && ema92num.isGreaterThan(ema181num) && closeNum.isGreaterThan(ema12num) && highNum.isGreaterThan(ema12num) && lowNum.isGreaterThan(ema12num) && openNum.isGreaterThan(ema12num) && closeNum.isGreaterThan(close.getValue(i-1)) && highLowPercent.isGreaterThan(percentLimit)) {

                            if (sum == 0 ) {
                                sum++;
                                LNorSH = -1;
                            } else if ( LNorSH == -1 ){
                                sum++;
                            }

                            if (sum >= seriesLength) {
                                resultEMA = -1;
                            }
                            String info = "Passed EMA SHORT " + endOFLogEntry + " SeriesSizeEMA: " + sum;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (sum > 0 && LNorSH == -1) {
                            sum = 0;
                        }
                    }
                }
                break;
            case 3:
                int continuousSeriesOfRSIValues = 0;
                int continuousSeriesOfClosePriceAboveEMA = 0;
                int howLongMustBeThisSeriesInRow = 4; //4
                int howLongMustBeRSISeriesInRow = 4;
                int longOrShortWeAreCounting = 0;
                Num rsiMiddleValue = DecimalNum.valueOf(50);
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num closeNum = close.getValue(i);
                        Num highNum = high.getValue(i);
                        Num lowNum = low.getValue(i);
                        Num openNum = open.getValue(i);
                        Num rsiNum = rsi.getValue(i);
                        Num rsiOneBeforeEnd = rsi.getValue(i - 1);
                        Num adxNum = adx.getValue(i);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue());

                        if (closeNum.isGreaterThan(ema12num) && closeNum.isGreaterThan(ema32num) && highNum.isGreaterThan(ema12num) && highNum.isGreaterThan(ema32num) && lowNum.isGreaterThan(ema12num) &&
                                lowNum.isGreaterThan(ema32num) && openNum.isGreaterThan(ema12num) && openNum.isGreaterThan(ema32num) && ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) &&
                                ema12num.isGreaterThan(ema92num) && closeNum.isGreaterThan(close.getValue(i-1))) {

                            if (continuousSeriesOfClosePriceAboveEMA == 0 ) {
                                continuousSeriesOfClosePriceAboveEMA++;
                                longOrShortWeAreCounting = 1;
                            } else if ( longOrShortWeAreCounting == 1 ){
                                continuousSeriesOfClosePriceAboveEMA++;
                            }

                            if (continuousSeriesOfClosePriceAboveEMA >= howLongMustBeThisSeriesInRow && close.getValue(series.getEndIndex()).isGreaterThan(open.getValue(series.getEndIndex()))) {
                                resultEMA = 1;
                            }
                            String info = "Passed EMA LONG " + endOFLogEntry + " SeriesSizeEMA: " + continuousSeriesOfClosePriceAboveEMA;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (continuousSeriesOfClosePriceAboveEMA > 0 && longOrShortWeAreCounting == 1) {
                            continuousSeriesOfClosePriceAboveEMA = 0;
                        }

                        if (closeNum.isLessThan(ema12num) && closeNum.isLessThan(ema32num) && highNum.isLessThan(ema12num) && highNum.isLessThan(ema32num) && lowNum.isLessThan(ema12num) && lowNum.isLessThan(ema32num) &&
                                openNum.isLessThan(ema12num) && openNum.isLessThan(ema32num) && ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema12num.isLessThan(ema92num) && closeNum.isLessThan(close.getValue(i-1))) {

                            if (continuousSeriesOfClosePriceAboveEMA == 0 ) {
                                continuousSeriesOfClosePriceAboveEMA++;
                                longOrShortWeAreCounting = -1;
                            } else if ( longOrShortWeAreCounting == -1 ){
                                continuousSeriesOfClosePriceAboveEMA++;
                            }

                            if (continuousSeriesOfClosePriceAboveEMA >= howLongMustBeThisSeriesInRow && close.getValue(series.getEndIndex()).isLessThan(open.getValue(series.getEndIndex()))) {
                                resultEMA = -1;
                            }
                            String info = "Passed EMA SHORT " + endOFLogEntry + " SeriesSizeEMA: " + continuousSeriesOfClosePriceAboveEMA;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (continuousSeriesOfClosePriceAboveEMA > 0 && longOrShortWeAreCounting == -1) {
                            continuousSeriesOfClosePriceAboveEMA = 0;
                        }

                        if (rsiNum.isLessThan(numOfRSITopLevel) && rsiNum.isGreaterThan(numOfRSIBottomLevel)) {

                            if (continuousSeriesOfRSIValues == 0) {
                                continuousSeriesOfRSIValues++;
                            } else {
                                if (rsiNum.isLessThan(rsiOneBeforeEnd)) {
                                    continuousSeriesOfRSIValues++;
                                } else {
                                    continuousSeriesOfRSIValues = 0;
                                }
                            }

                            if (continuousSeriesOfRSIValues >= howLongMustBeRSISeriesInRow) {
                                resultRSI = -1;
                            }
                            String info = "Passed RSI SHORT " + endOFLogEntry + " SeriesSizeRSI: " + continuousSeriesOfRSIValues;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (rsiNum.isGreaterThan(numOfRSIBottomLevel) && rsiNum.isLessThan(numOfRSITopLevel)) {

                            if (continuousSeriesOfRSIValues == 0) {
                                continuousSeriesOfRSIValues++;
                            } else {
                                if (rsiNum.isGreaterThan(rsiOneBeforeEnd)) {
                                    continuousSeriesOfRSIValues++;
                                } else {
                                    continuousSeriesOfRSIValues = 0;
                                }
                            }

                            if (continuousSeriesOfRSIValues >= howLongMustBeRSISeriesInRow) {
                                resultRSI = 1;
                            }
                            String info = "Passed RSI LONG " + endOFLogEntry + " SeriesSizeRSI: " + continuousSeriesOfRSIValues;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (i == series.getEndIndex() && adxNum.isLessThan(numOfAdxLimit)) {
                            resultADX = 1;
                            String info = "Passed ADX LONG/SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                    }
                }
                break;
            case 4:  //RSI_WT_4h_15m
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num wt1 = tci.getValue(i);
                        Num wt2 = sma.getValue(i);
                        Num macdEnd = macd.getValue(i);
                        Num signalEnd = signalLine.getValue(i);
                        Num rsiEnd = rsi.getValue(i);

//                        int nrOfMACDSeries = 0;
//                        int requiredSeries;
//
//                        if (params.getIntervalOfKlines().equals("3m")) {
//                            requiredSeries = 10;
//                        } else {
//                            requiredSeries = 3;
//                        }

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue());

                        if (resultWT == -2 && wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numOfWTTopLevel)) {
                            resultWT = -1;
                            String info = "Passed WT SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultWT == -2 && wt1.isGreaterThan(wt2) && wt1.isLessThan(numOfWTBottomLevel)) {
                            resultWT = 1;
                            String info = "Passed WT LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultMACD == -2 && macdEnd.isLessThan(signalEnd)) {
                            resultMACD = -1;
                            String info = "Passed MACD SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultMACD == -2 && macdEnd.isGreaterThan(signalEnd)) {
                            resultMACD = 1;
                            //nrOfMACDSeries++;
                            String info = "Passed MACD LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultRSI == -2 && rsiEnd.isLessThan(numOfRSIBottomLevel)) {
                            resultRSI = -1;
                            String info = "Passed RSI SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultRSI == -2 && rsiEnd.isGreaterThan(numOfRSITopLevel)) {
                            resultRSI = 1;
                            String info = "Passed RSI LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

//                        if (i == series.getEndIndex()) {
//                            String info = "SERIES MACD: " + nrOfMACDSeries + " " + endOFLogEntry;
//                            ServiceFunctionsOther.writeToFile(info, context, "result");
//                        }
//
//                        if (nrOfMACDSeries == requiredSeries) {
//                            resultMACD = 1;
//                        } else if (nrOfMACDSeries == -1 * requiredSeries) {
//                            resultMACD = -1;
//                        }

                    }
                }
                break;
            case 5: //WT, ADX, PPO between
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num wt1 = tci.getValue(i);
                        Num wt2 = sma.getValue(i);
                        Num adxEnd = adx.getValue(i);
                        Num ppoEnd = ppo.getValue(i);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue());

                        if ((resultWT == -2 && wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numOfWTTopLevel))) {
                            resultWT = -1;
                            String info = "Passed WT SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if ((resultWT == -2 && wt1.isGreaterThan(wt2) && wt1.isLessThan(numOfWTBottomLevel))) {
                            resultWT = 1;
                            String info = "Passed WT LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if ((resultWT == -1 || resultWT == 1) && resultADX == -2 && adxEnd.isGreaterThan(numOfAdxLimit)) {
                            resultADX = 1;
                            String info = "Passed ADX LONG/SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultWT == 1 && resultPPO == -2 && ppoEnd.isGreaterThan(numOfLimitBottomPPO_forLong) && ppoEnd.isLessThan(numOfLimitTopPPO_forLong) && ppoEnd.isGreaterThan(numOfPpoBefore)) {
                            resultPPO = 1;
                            String info = "Passed PPO LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultWT == -1 && resultPPO == -2 && ppoEnd.isGreaterThan(numOfLimitBottomPPO_forShort) && ppoEnd.isLessThan(numOfLimitTopPPO_forShort) && ppoEnd.isLessThan(numOfPpoBefore)) {
                            resultPPO = -1;
                            String info = "Passed PPO SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }
                    }
                }
                break;
            case 6:
                int size = 0;
                int firstConditionSize = 4;
                int secondConditionSize = 3;
                int longyOrShorty = 0;
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num ema181num = ema181.getValue(i);
                        Num hlc3Num = hlc3.getValue(i);
                        Num rsiEnd = rsi.getValue(i);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [ema181]: " + df.format(ema181.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue());

                        if (resultEMA == -2 && ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema92num.isLessThan(ema181num) && hlc3Num.isLessThan(ema12num)) {

                            if (size == 0 ) {
                                size++;
                                longyOrShorty = 1;
                            } else if ( longyOrShorty == 1 ){
                                size++;
                            }

                            if (size >= firstConditionSize ) {
                                resultEMA = 1;
                                size = 0;
                            }
                            String info = "Passed EMA First Condition LONG " + endOFLogEntry + " SeriesSizeEMA: " + size;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultEMA == -2 && size > 0 && longyOrShorty == 1) {
                            size = 0;
                        }

                        if (resultEMA == -2 && ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) && ema92num.isGreaterThan(ema181num) && hlc3Num.isGreaterThan(ema12num)) {

                            String info2 = "What is going on " + size;
                            ServiceFunctionsOther.writeToFile(info2, context, "result");

                            if (size == 0 ) {
                                size++;
                                longyOrShorty = -1;
                            } else if ( longyOrShorty == -1 ){
                                size++;
                            }

                            if (size >= firstConditionSize) {
                                resultEMA = -1;
                                size = 0;
                            }

                            String info = "Passed EMA First Condition SHORT " + endOFLogEntry + " SeriesSizeEMA: " + size;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultEMA == -2 && size > 0 && longyOrShorty == -1) {
                            size = 0;
                        }

                        if (resultEMA == 1 && resultAROON == -2 && ema12num.isGreaterThan(ema32num) && ema12num.isGreaterThan(ema92num) && hlc3Num.isGreaterThan(ema12num)) {

                            if (size == 0 ) {
                                size++;
                                longyOrShorty = 1;
                            } else if ( longyOrShorty == 1 ){
                                size++;
                            }

                            if (size >= secondConditionSize ) {
                                resultAROON = 1;
                                size = 0;
                            }
                            String info = "Passed EMA Second Condition (aroon field used) LONG " + endOFLogEntry + " SeriesSizeEMA: " + size;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultEMA == 1 && size > 0 && longyOrShorty == 1) {
                            size = 0;
                        }

                        if (resultEMA == -1 && resultAROON == -2 && ema12num.isLessThan(ema32num) && ema12num.isLessThan(ema92num) && hlc3Num.isLessThan(ema12num)) {

                            if (size == 0 ) {
                                size++;
                                longyOrShorty = -1;
                            } else if ( longyOrShorty == -1 ){
                                size++;
                            }

                            if (size >= secondConditionSize) {
                                resultAROON = -1;
                                size = 0;
                            }

                            String info = "Passed EMA Second Condition (aroon field used) SHORT " + endOFLogEntry + " SeriesSizeEMA: " + size;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultEMA == -1 && size > 0 && longyOrShorty == -1) {
                            size = 0;
                        }

                        if (resultEMA == 1 && resultAROON == 1 && resultRSI == -2 && rsiEnd.isGreaterThan(numOfRSITopLevel) && i < series.getEndIndex() - 1  ) {
                            String info = "Passed RSI NOT FILLED - TOO EARLY "  + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultEMA == 1 && resultAROON == 1 && resultRSI == -2 && rsiEnd.isGreaterThan(numOfRSITopLevel) && i >= series.getEndIndex() - 1 ) {
                            resultRSI = 1;
                            String info = "Passed RSI Third Condition LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");

                        }

                        if (resultEMA == -1 && resultAROON == -1 && resultRSI == -2 && rsiEnd.isLessThan(numOfRSIBottomLevel) && i < series.getEndIndex() - 1) {
                            String info = "Passed RSI NOT FILLED - TOO EARLY " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultEMA == -1 && resultAROON == -1 && resultRSI == -2 && rsiEnd.isLessThan(numOfRSIBottomLevel) && i >= series.getEndIndex() - 1) {
                            resultRSI = -1;
                            String info = "Passed RSI Third Condition SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }
                    }
                }
                break;
            case 7:
                int seriesLengthOfClosePriceAboveEMA = 0;
                int howBigEMASeries; //4
                if (params.getIntervalOfKlines().equals("4h")) {
                    howBigEMASeries = 2;
                } else {
                    howBigEMASeries = 4;
                }
                int longOrShortCounting = 0;
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num closeNum = close.getValue(i);
                        Num highNum = high.getValue(i);
                        Num lowNum = low.getValue(i);
                        Num openNum = open.getValue(i);
                        Num adxNum = adx.getValue(i);
                        Num wt1 = tci.getValue(i);
                        Num wt2 = sma.getValue(i);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue());

                        if (closeNum.isGreaterThan(ema12num) && closeNum.isGreaterThan(ema32num) && highNum.isGreaterThan(ema12num) && highNum.isGreaterThan(ema32num) && lowNum.isGreaterThan(ema12num) &&
                                lowNum.isGreaterThan(ema32num) && openNum.isGreaterThan(ema12num) && openNum.isGreaterThan(ema32num) && ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) &&
                                ema12num.isGreaterThan(ema92num) && closeNum.isGreaterThan(close.getValue(i-1))) {

                            if (seriesLengthOfClosePriceAboveEMA == 0 ) {
                                seriesLengthOfClosePriceAboveEMA++;
                                longOrShortCounting = 1;
                            } else if ( longOrShortCounting == 1 ){
                                seriesLengthOfClosePriceAboveEMA++;
                            }

                            if (seriesLengthOfClosePriceAboveEMA >= howBigEMASeries && close.getValue(series.getEndIndex()).isGreaterThan(open.getValue(series.getEndIndex()))) {
                                resultEMA = 1;
                            }
                            String info = "Passed EMA LONG " + endOFLogEntry + " SeriesSizeEMA: " + seriesLengthOfClosePriceAboveEMA;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (seriesLengthOfClosePriceAboveEMA > 0 && longOrShortCounting == 1) {
                            seriesLengthOfClosePriceAboveEMA = 0;
                        }

                        if (closeNum.isLessThan(ema12num) && closeNum.isLessThan(ema32num) && highNum.isLessThan(ema12num) && highNum.isLessThan(ema32num) && lowNum.isLessThan(ema12num) && lowNum.isLessThan(ema32num) &&
                                openNum.isLessThan(ema12num) && openNum.isLessThan(ema32num) && ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema12num.isLessThan(ema92num) && closeNum.isLessThan(close.getValue(i-1))) {

                            if (seriesLengthOfClosePriceAboveEMA == 0 ) {
                                seriesLengthOfClosePriceAboveEMA++;
                                longOrShortCounting = -1;
                            } else if ( longOrShortCounting == -1 ){
                                seriesLengthOfClosePriceAboveEMA++;
                            }

                            if (seriesLengthOfClosePriceAboveEMA >= howBigEMASeries && close.getValue(series.getEndIndex()).isLessThan(open.getValue(series.getEndIndex()))) {
                                resultEMA = -1;
                            }
                            String info = "Passed EMA SHORT " + endOFLogEntry + " SeriesSizeEMA: " + seriesLengthOfClosePriceAboveEMA;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (seriesLengthOfClosePriceAboveEMA > 0 && longOrShortCounting == -1) {
                            seriesLengthOfClosePriceAboveEMA = 0;
                        }

                        if ((resultWT == -2 && wt1.isGreaterThan(wt2) && wt2.isGreaterThan(numOfWTTopLevel))) {
                            resultWT = 1;
                            String info = "Passed WT LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if ((resultWT == -2 && wt2.isGreaterThan(wt1) && wt2.isLessThan(numOfWTBottomLevel))) {
                            resultWT = -1;
                            String info = "Passed WT SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (i >= series.getEndIndex() - 1 && adxNum.isLessThan(numOfAdxLimit)) {
                            resultADX = 1;
                            String info = "Passed ADX LONG/SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                    }
                }
                break;
            case 8:
                int seriesOfEMAsLength = 0;
                int assumedLength = 8;
                int longOrShortRightNow = 0;
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num ema181num = ema181.getValue(i);
                        Num closeNum = close.getValue(i);
                        Num openNum = open.getValue(i);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue());

                        if (ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema92num.isLessThan(ema181num) && closeNum.isLessThan(ema12num) && openNum.isLessThan(ema12num)) {

                            if (seriesOfEMAsLength == 0 ) {
                                seriesOfEMAsLength++;
                                longOrShortRightNow = -1;
                            } else if ( longOrShortRightNow == -1 ){
                                seriesOfEMAsLength++;
                            }

                            if (seriesOfEMAsLength >= assumedLength ) {
                                resultEMA = -1;
                            }

                            String info = "Passed EMA SHORT " + endOFLogEntry + " SeriesSizeEMA: " + seriesOfEMAsLength;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (seriesOfEMAsLength > 0 && longOrShortRightNow == -1) {
                            seriesOfEMAsLength = 0;
                        }

                        if (ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) && ema92num.isGreaterThan(ema181num) && closeNum.isGreaterThan(ema12num) && openNum.isGreaterThan(ema12num)) {

                            if (seriesOfEMAsLength == 0 ) {
                                seriesOfEMAsLength++;
                                longOrShortRightNow = 1;
                            } else if ( longOrShortRightNow == 1 ){
                                seriesOfEMAsLength++;
                            }

                            if (seriesOfEMAsLength >= assumedLength) {
                                resultEMA = 1;
                            }
                            String info = "Passed EMA LONG " + endOFLogEntry + " SeriesSizeEMA: " + seriesOfEMAsLength;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (seriesOfEMAsLength > 0 && longOrShortRightNow == 1) {
                            seriesOfEMAsLength = 0;
                        }
                    }
                }
                break;
            case 9: //MACD, continuous ADX, WT
                int continuousSeriesOfADXValuesRisingForLongDroppingForShort = 0;
                int howLongMustBeThisSeriesInRow6 = 6;
                Num startOfSeriesADX = DecimalNum.valueOf(0);
                Num differenceForADX = DecimalNum.valueOf(5);

                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num wt1 = tci.getValue(i);
                        Num wt2 = sma.getValue(i);
                        Num adxEnd = adx.getValue(i);
                        Num adxOneBeforeEnd = adx.getValue(i - 1);
                        Num macdEnd = macd.getValue(i);
                        Num signalEnd = signalLine.getValue(i);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue());

                        if ((resultWT == -2 && wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numOfWTTopLevel))) {
                            resultWT = -1;
                            String info = "Passed WT SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if ((resultWT == -2 && wt1.isGreaterThan(wt2) && wt1.isLessThan(numOfWTBottomLevel))) {
                            resultWT = 1;
                            String info = "Passed WT LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultWT == -1 && adxEnd.isGreaterThan(numOfAdxLimit)) {

                            if (continuousSeriesOfADXValuesRisingForLongDroppingForShort == 0) {
                                continuousSeriesOfADXValuesRisingForLongDroppingForShort++;
                                startOfSeriesADX = adxEnd;
                            } else {
                                if (adxEnd.isLessThan(adxOneBeforeEnd)) {
                                    continuousSeriesOfADXValuesRisingForLongDroppingForShort++;
                                } else {
                                    continuousSeriesOfADXValuesRisingForLongDroppingForShort = 0;
                                }
                            }

                            if (continuousSeriesOfADXValuesRisingForLongDroppingForShort >= howLongMustBeThisSeriesInRow6 && (startOfSeriesADX.minus(adxEnd).isGreaterThan(differenceForADX))) {
                                resultADX = -1;
                            }
                            String info = "Passed ADX SHORT " + endOFLogEntry + " SeriesSize: " + continuousSeriesOfADXValuesRisingForLongDroppingForShort;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultWT == 1 && adxEnd.isGreaterThan(numOfAdxLimit)) {

                            if (continuousSeriesOfADXValuesRisingForLongDroppingForShort == 0) {
                                continuousSeriesOfADXValuesRisingForLongDroppingForShort++;
                            } else {
                                if (adxEnd.isGreaterThan(adxOneBeforeEnd)) {
                                    continuousSeriesOfADXValuesRisingForLongDroppingForShort++;
                                } else {
                                    continuousSeriesOfADXValuesRisingForLongDroppingForShort = 0;
                                }
                            }

                            if (continuousSeriesOfADXValuesRisingForLongDroppingForShort >= howLongMustBeThisSeriesInRow6 && (startOfSeriesADX.minus(adxEnd).isLessThan(differenceForADX.negate()))) {
                                resultADX = 1;
                            }
                            String info = "Passed ADX LONG " + endOFLogEntry + " SeriesSize: " + continuousSeriesOfADXValuesRisingForLongDroppingForShort;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultADX == -1 && resultMACD == -2 && macdEnd.isLessThan(signalEnd)) {
                            resultMACD = -1;
                            String info = "Passed MACD SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultADX == 1 && resultMACD == -2 && macdEnd.isGreaterThan(signalEnd)) {
                            resultMACD = 1;
                            String info = "Passed MACD LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }
                    }
                }
                break;
            case 10:
                int sizeIchimoku = 0;
                int firstConditionSizeIchimoku = 8;
                int secondConditionSizeIchimoku = 8;
                int longyOrShortyIchimoku = 0;
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num aSpanNum = IchimokuLeadingSpanA.getValue(i);
                        Num bSpanNum = IchimokuLeadingSpanB.getValue(i);
                        Num highNum = high.getValue(i);
                        Num lowNum = low.getValue(i);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [ema181]: " + df.format(ema181.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue()) + " [SpanA]: " + df.format(IchimokuLeadingSpanA.getValue(i).doubleValue()) + " [SpanB]: " + df.format(IchimokuLeadingSpanB.getValue(i).doubleValue()) + " [BaseLineIchimoku]: " + df.format(IchimokuBaseLine.getValue(i).doubleValue()) + " [ConversionLineIchimoku]: " + df.format(IchimokuConversionLine.getValue(i).doubleValue());

                        if (resultICHIMOKU == -2 && bSpanNum.isGreaterThan(aSpanNum) && highNum.isLessThan(bSpanNum) && lowNum.isLessThan(bSpanNum)) {

                            if (sizeIchimoku == 0 ) {
                                sizeIchimoku++;
                                longyOrShortyIchimoku = 1;
                            } else if ( longyOrShortyIchimoku == 1 ){
                                sizeIchimoku++;
                            }

                            if (sizeIchimoku >= firstConditionSizeIchimoku ) {
                                resultICHIMOKU = 1;
                                sizeIchimoku = 0;
                            }
                            String info = "Passed ICHIMOKU First Condition LONG " + endOFLogEntry + " SeriesSizeEMA: " + sizeIchimoku;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultICHIMOKU == -2 && sizeIchimoku > 0 && longyOrShortyIchimoku == 1) {
                            sizeIchimoku = 0;
                        }

                        if (resultICHIMOKU == -2 && aSpanNum.isGreaterThan(bSpanNum) && highNum.isGreaterThan(bSpanNum) && lowNum.isGreaterThan(bSpanNum)) {

                            if (sizeIchimoku == 0 ) {
                                sizeIchimoku++;
                                longyOrShortyIchimoku = -1;
                            } else if ( longyOrShortyIchimoku == -1 ){
                                sizeIchimoku++;
                            }

                            if (sizeIchimoku >= firstConditionSizeIchimoku) {
                                resultICHIMOKU = -1;
                                sizeIchimoku = 0;
                            }

                            String info = "Passed ICHIMOKU First Condition SHORT " + endOFLogEntry + " SeriesSizeEMA: " + sizeIchimoku;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultICHIMOKU == -2 && sizeIchimoku > 0 && longyOrShortyIchimoku == -1) {
                            sizeIchimoku = 0;
                        }

                        if (resultICHIMOKU == 1 && resultEMA == -2 && bSpanNum.isGreaterThan(aSpanNum) && IchimokuLeadingSpanA.getValue(i - 1).isGreaterThan(IchimokuLeadingSpanB.getValue(i - 1)) & i < series.getEndIndex() - 13) {

                            resultEMA = 1;

                            String info = "Passed ICHIMOKU Second Condition (ema used) LONG " + endOFLogEntry + " SeriesSizeEMA: " + sizeIchimoku;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }


                        if (resultICHIMOKU == -1 && resultEMA == -2 && aSpanNum.isGreaterThan(bSpanNum) && IchimokuLeadingSpanB.getValue(i - 1).isGreaterThan(IchimokuLeadingSpanA.getValue(i - 1)) & i < series.getEndIndex() - 13) {

                            resultEMA = -1;

                            String info = "Passed ICHIMOKU Second Condition (ema used) SHORT " + endOFLogEntry + " SeriesSizeEMA: " + sizeIchimoku;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultICHIMOKU == 1 && resultEMA == 1 && resultAROON == -2 && aSpanNum.isGreaterThan(bSpanNum) && highNum.isGreaterThan(aSpanNum) && lowNum.isGreaterThan(aSpanNum) && i < series.getEndIndex() - 9) {

                            if (sizeIchimoku == 0 ) {
                                sizeIchimoku++;
                                longyOrShortyIchimoku = 1;
                            } else if ( longyOrShortyIchimoku == 1 ){
                                sizeIchimoku++;
                            }

                            if (sizeIchimoku >= secondConditionSizeIchimoku ) {
                                resultAROON = 1;
                                sizeIchimoku = 0;
                            }
                            String info = "Passed ICHIMOKU Third Condition (aroon field used) LONG " + endOFLogEntry + " SeriesSizeEMA: " + sizeIchimoku;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultICHIMOKU == 1 && sizeIchimoku > 0 && longyOrShortyIchimoku == 1) {
                            sizeIchimoku = 0;
                        }

                        if (resultICHIMOKU == -1 && resultEMA == -1 && resultAROON == -2 && aSpanNum.isLessThan(bSpanNum) && highNum.isLessThan(aSpanNum) && lowNum.isLessThan(aSpanNum) && i < series.getEndIndex() - 9) {

                            if (sizeIchimoku == 0 ) {
                                sizeIchimoku++;
                                longyOrShortyIchimoku = -1;
                            } else if ( longyOrShortyIchimoku == -1 ){
                                sizeIchimoku++;
                            }

                            if (sizeIchimoku >= secondConditionSizeIchimoku) {
                                resultAROON = -1;
                                sizeIchimoku = 0;
                            }

                            String info = "Passed ICHIMOKU Third Condition (aroon field used) SHORT " + endOFLogEntry + " SeriesSizeEMA: " + sizeIchimoku;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultICHIMOKU == -1 && sizeIchimoku > 0 && longyOrShortyIchimoku == -1) {
                            sizeIchimoku = 0;
                        }
                    }
                }
                break;
            default:
                break;
        }

        return new StrategyResultV2(resultMACD, resultWT, resultPPO, resultADX, resultAROON, resultEMA, resultRSI, resultICHIMOKU);
    }

    public static double getSARValue(List<Kline> klines) {

        // Prepare barSeries to compute Technical Indicators (remember that this revers affects original list)
        Collections.reverse(klines);
        BarSeries series = new BaseBarSeriesBuilder().withName("SeriesForStrategy").build();

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

        //
        // Computations for SAR
        //
        NumericIndicator sar = NumericIndicator.of(new ParabolicSarIndicator(series));

        return sar.getValue(series.getEndIndex()).doubleValue();

    }

    public static double getEMAValue(List<Kline> klines, int emaLength, String emaSource) {

        // Prepare barSeries to compute Technical Indicators (remember that this revers affects original list)
        Collections.reverse(klines);
        BarSeries series = new BaseBarSeriesBuilder().withName("SeriesForStrategy").build();

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

        NumericIndicator source = null;

        switch (emaSource) {
            case "low":
                source = NumericIndicator.of(new LowPriceIndicator(series));
                break;
            case "high":
                source = NumericIndicator.of(new HighPriceIndicator(series));
                break;
            case "hlc3":
                source = NumericIndicator.of(new TypicalPriceIndicator(series));
                break;
            case "open":
                source = NumericIndicator.of(new OpenPriceIndicator(series));
                break;
            case "close":
                source = NumericIndicator.of(new ClosePriceIndicator(series));
                break;
            case "hl2":
                source = NumericIndicator.of(new MedianPriceIndicator(series));
                break;
            default:
                break;
        }

        assert source != null;
        NumericIndicator ema = NumericIndicator.of(new EMAIndicator(source, emaLength));

        return ema.getValue(series.getEndIndex()).doubleValue();

    }

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
        int finalWaveTrendScore;
        // Compute indicators
        NumericIndicator ap = NumericIndicator.of(new TypicalPriceIndicator(series)); //Median originally
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

        //MACD
        NumericIndicator close = NumericIndicator.of(new ClosePriceIndicator(series));
        MACDIndicator macd = new MACDIndicator(close, 12, 26);
        int signalLinePeriod = 9;
        EMAIndicator signalLine = new EMAIndicator(macd, signalLinePeriod);

        Num numObLevel2 = DecimalNum.valueOf(60);
        Num numOsLevel2 = DecimalNum.valueOf(-60);

        // Compute WaveTrend indicators
        Num wt1 = tci.getValue(series.getEndIndex());
        SMAIndicator sma = new SMAIndicator(tci, 4);
        Num wt2 = sma.getValue(series.getEndIndex());

        DecimalFormat df = new DecimalFormat("0.00000000000");
        int i = series.getEndIndex();
        String info = "Index: " + i + " MedianPrice(ap): " + df.format(ap.getValue(i).doubleValue()) + " EMA(" + 10 + "): " + df.format(esa.getValue(i).doubleValue()) + " EMA(" + 21 + "): " + df.format(d.getValue(i).doubleValue()) + " AbstractEMA(wt1): " + df.format(wt1.doubleValue()) + " SMA(wt2): " + df
                .format(wt2.doubleValue()) + " ATR: " + df.format(atr.getValue(i).doubleValue()) + " MACD: " + df.format(macd.getValue(i).doubleValue()) + " signalMACD: " + df.format(signalLine.getValue(i).doubleValue());
        Log.e("StrategyValues ", info);
        ServiceFunctionsOther.writeToFile(info, context, "result");
        // Check alert conditions and return the corresponding value
        if ((wt1.isGreaterThan(wt2) && wt1.isGreaterThan(numObLevel2))) {
            finalWaveTrendScore = -1;
        } else if ((wt1.isLessThan(wt2) && wt1.isLessThan(numOsLevel2))) {
            finalWaveTrendScore = 1;
        } else {
            finalWaveTrendScore = 0;
        }
        return new StrategyResult(nrOfPositions, vsBuyAndHoldProfit, totalProfit, finalWaveTrendScore, bestStrategy.getName());

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
