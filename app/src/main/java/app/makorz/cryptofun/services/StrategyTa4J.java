package app.makorz.cryptofun.services;
import android.content.Context;
import app.makorz.cryptofun.data.StrategyParameters;
import app.makorz.cryptofun.data.StrategyResultV2;
import app.makorz.cryptofun.data.database.Kline;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.AbstractEMAIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.PPOIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.aroon.AroonDownIndicator;
import org.ta4j.core.indicators.aroon.AroonUpIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.MedianPriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuChikouSpanIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuKijunSenIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuSenkouSpanAIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuSenkouSpanBIndicator;
import org.ta4j.core.indicators.ichimoku.IchimokuTenkanSenIndicator;
import org.ta4j.core.indicators.numeric.NumericIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;


import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class StrategyTa4J {

    private final BarSeries series;
    //
    // PriceIndicators
    //
    private final NumericIndicator hlc3;
    private final NumericIndicator close;
    private final NumericIndicator low;
    private final NumericIndicator high;
    private final NumericIndicator open;
    private final NumericIndicator hl2;
    //
    // EMA Indicators
    //
    private final NumericIndicator ema12;
    private final NumericIndicator ema32;
    private final NumericIndicator ema92;
    private final NumericIndicator ema181;
    //
    // Computations for WaveTrend (wt1 = tci, wt2 = sma)
    //
    private final NumericIndicator diff;
    private final NumericIndicator x;
    private final NumericIndicator d;
    private final NumericIndicator z;
    private final NumericIndicator ci;
    //
    // Compute the channel index -- overwrite of calculate was need because value at index 0 waa NaN
    //
    private final NumericIndicator tci;
    private final NumericIndicator sma;
    //
    // Computations for highLowPercentDiff
    //
    private final NumericIndicator highLowPercentDiff;
    //
    // Computations for MACD
    //
    private final NumericIndicator macd;
    private final NumericIndicator signalLine;
    //
    // Computations for ATR
    //
    private final NumericIndicator atr;
    //
    // Computations for SAR
    //
    private final NumericIndicator sar;
    //
    // Computations for ADX
    //
    private final NumericIndicator adx;
    //
    // Computations for Ichimoku
    //
    private final NumericIndicator IchimokuLeadingSpanA;
    private final NumericIndicator IchimokuLeadingSpanB;
    private final NumericIndicator IchimokuBaseLine;
    private final NumericIndicator IchimokuConversionLine;
    private final NumericIndicator IchimokuLaggingSpanLine;
    //
    // Computations for PPO
    //
    private final NumericIndicator ppo;
    //
    // Computations for RSI
    //
    private final NumericIndicator rsi;
    //
    // Computations for AROON
    //
    private final NumericIndicator aroonLow;
    private final NumericIndicator aroonHigh;

    // Constructor initializes series and static indicators for each symbol
    public StrategyTa4J(List<Kline> klines) {
        this.series = createBarSeries(klines);
        this.hlc3 = NumericIndicator.of(new TypicalPriceIndicator(series));
        this.close = NumericIndicator.of(new ClosePriceIndicator(series));
        this.low = NumericIndicator.of(new LowPriceIndicator(series));
        this.high = NumericIndicator.of(new HighPriceIndicator(series));
        this.open = NumericIndicator.of(new OpenPriceIndicator(series));
        this.hl2 = NumericIndicator.of(new MedianPriceIndicator(series));
        //
        // EMA Indicators
        //
        this.ema12 = NumericIndicator.of(new EMAIndicator(hlc3, 12));
        this.ema32 = NumericIndicator.of(new EMAIndicator(hlc3, 32));
        this.ema92 = NumericIndicator.of(new EMAIndicator(hlc3, 92));
        this.ema181 = NumericIndicator.of(new EMAIndicator(hlc3, 181));
        //
        // Computations for WaveTrend (wt1 = tci, wt2 = sma)
        //
        this.diff = hlc3.minus(ema12);
        this.x = diff.abs();
        this.d = NumericIndicator.of(new EMAIndicator(x, 12));
        this.z = d.multipliedBy(0.015);
        this.ci = diff.dividedBy(z);
        double multiplier2 = (2.0 / (21 + 1));
        //
        // Compute the channel index -- overwrite of calculate was need because value at index 0 waa NaN
        //
        this.tci = NumericIndicator.of(new AbstractEMAIndicator(ci, 21, multiplier2) {
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
        this.sma = NumericIndicator.of(new SMAIndicator(tci, 4));
        //
        // Computations for highLowPercentDiff
        //
        this.highLowPercentDiff = low.multipliedBy(100).dividedBy(high);
        //
        // Computations for MACD
        //
        this.macd = NumericIndicator.of(new MACDIndicator(close, 12, 26));
        int signalLinePeriod = 9;
        this.signalLine = NumericIndicator.of(new EMAIndicator(macd, signalLinePeriod));
        //
        // Computations for ATR
        //
        this.atr = NumericIndicator.of(new ATRIndicator(series, 10));
        //
        // Computations for SAR
        //
        this.sar = NumericIndicator.of(new ParabolicSarIndicator(series));
        //
        // Computations for ADX
        //
        this.adx = NumericIndicator.of(new ADXIndicator(series, 14, 14));
        //
        // Computations for Ichimoku
        //
        this.IchimokuLeadingSpanA = NumericIndicator.of(new IchimokuSenkouSpanAIndicator(series, 9, 26));
        this.IchimokuLeadingSpanB = NumericIndicator.of(new IchimokuSenkouSpanBIndicator(series, 52));
        this.IchimokuBaseLine = NumericIndicator.of(new IchimokuKijunSenIndicator(series, 26));
        this.IchimokuConversionLine = NumericIndicator.of(new IchimokuTenkanSenIndicator(series, 9));
        this.IchimokuLaggingSpanLine = NumericIndicator.of(new IchimokuChikouSpanIndicator(series, 10));
        //
        // Computations for PPO
        //
        this.ppo = NumericIndicator.of(new PPOIndicator(close, 12, 32));
        //
        // Computations for RSI
        //
        this.rsi = NumericIndicator.of(new RSIIndicator(close, 14));
        //
        // Computations for AROON
        //
        this.aroonLow = NumericIndicator.of(new AroonDownIndicator(low, 5));
        this.aroonHigh = NumericIndicator.of(new AroonUpIndicator(high, 5));
    }

    // Factory method to create BarSeries from klines
    private BarSeries createBarSeries(List<Kline> klines) {
        BarSeries series = new BaseBarSeriesBuilder().withName("SeriesForStrategy").build();

        for (Kline kline : klines) {
            long endTimeMillis = kline.gettCloseTime();
            ZonedDateTime endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault());
            series.addBar(endTime, kline.gettOpenPrice(), kline.gettHighPrice(), kline.gettLowPrice(), kline.gettClosePrice());
        }

        return series;
    }

    // Strategy execution function for a single symbol
    public StrategyResultV2 strategyTa4J(int strategyNr, StrategyParameters params, Context context) {

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
        Num numOfLimitBottomPPO = DecimalNum.valueOf(params.getPPOLevelBottom());
        Num numOfLimitTopPPO = DecimalNum.valueOf(params.getPPOLevelTop());
        Num numOfLimitBottomPPO_Negated = DecimalNum.valueOf(params.getPPOLevelTop()).negate();
        Num numOfLimitTopPPO_Negated = DecimalNum.valueOf(params.getPPOLevelBottom()).negate();
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

                        if (ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema92num.isLessThan(ema181num) && hlc3num.isLessThan(ema12num) && hlc3num.isLessThan(hlc3.getValue(i - 1))) {

                            if (sumOfSeries == 0) {
                                sumOfSeries++;
                                longShort = 1;
                            } else if (longShort == 1) {
                                sumOfSeries++;
                            }

                            if (sumOfSeries >= seriesSize) {
                                resultEMA = 1;
                            }
                            String info = "Passed EMA LONG " + endOFLogEntry + " SeriesSizeEMA: " + sumOfSeries;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (sumOfSeries > 0 && longShort == 1) {
                            sumOfSeries = 0;
                        }

                        if (ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) && ema92num.isGreaterThan(ema181num) && hlc3num.isGreaterThan(ema12num) && hlc3num.isGreaterThan(hlc3.getValue(i - 1))) {

                            if (sumOfSeries == 0) {
                                sumOfSeries++;
                                longShort = -1;
                            } else if (longShort == -1) {
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
            case 2:
                int sum = 0;
                int seriesLength = 2;
                int LNorSH = 0;
                Num percentLimit = DecimalNum.valueOf(98);
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num ema181num = ema181.getValue(i);
                        Num closeNum = close.getValue(i);
                        Num highNum = high.getValue(i);
                        Num lowNum = low.getValue(i);
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

                        if (i > series.getEndIndex() - seriesLength && resultWT == 1 && ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema92num.isLessThan(ema181num) && highNum.isLessThan(ema12num) && closeNum.isLessThan(close.getValue(i - 1)) && highLowPercent.isGreaterThan(percentLimit)) {

                            if (sum == 0) {
                                sum++;
                                LNorSH = 1;
                            } else if (LNorSH == 1) {
                                sum++;
                            }

                            if (sum >= seriesLength) {
                                resultEMA = 1;
                            }
                            String info = "Passed EMA LONG " + endOFLogEntry + " SeriesSizeEMA: " + sum;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (sum > 0 && LNorSH == 1) {
                            sum = 0;
                        }

                        if (i > series.getEndIndex() - seriesLength && resultWT == -1 && ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) && ema92num.isGreaterThan(ema181num) && lowNum.isGreaterThan(ema12num) && closeNum.isGreaterThan(close.getValue(i - 1)) && highLowPercent.isGreaterThan(percentLimit)) {

                            if (sum == 0) {
                                sum++;
                                LNorSH = -1;
                            } else if (LNorSH == -1) {
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
            case 4:
                int numberOfOccurrenceLong = 0;
                int numberOfOccurrenceShort = 0;
                int howManyOccurrencesRequired;
                if (params.getIntervalOfKlines().equals("3m")) {
                    howManyOccurrencesRequired = 10;
                } else {
                    howManyOccurrencesRequired = 4;
                }
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i <= series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num ema181num = ema181.getValue(i);
                        Num openNum = open.getValue(i);
                        Num closeNum = close.getValue(i);
                        Num highNum = high.getValue(i);
                        Num lowNum = low.getValue(i);
                        Num adxNum = adx.getValue(i);
                        Num sarNum = sar.getValue(i);

                        double percentDiff = (ema32.getValue(i).doubleValue() * 100) / ema181.getValue(i).doubleValue();

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [ema181]: " + df.format(ema181.getValue(i).doubleValue()) + " [emaDiff]: " + df.format(percentDiff) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue()) + " [HL%]: " + df.format(highLowPercentDiff.getValue(i).doubleValue());

                        if (resultEMA == -2 && ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema92num.isLessThan(ema181num) && closeNum.isLessThan(openNum) && adxNum.isLessThan(numOfAdxLimit) && hlc3.getValue(series.getEndIndex()).isLessThan(sar.getValue(series.getEndIndex()))) {

                            numberOfOccurrenceShort++;

                            if (numberOfOccurrenceShort >= howManyOccurrencesRequired) {
                                resultEMA = -1;
                            }

                            String info = "Passed EMA (Series) SHORT " + endOFLogEntry + " SeriesSizeEMA: " + numberOfOccurrenceShort;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultEMA == -2 && ema12num.isGreaterThan(ema32num) && ema32num.isGreaterThan(ema92num) && ema92num.isGreaterThan(ema181num) && closeNum.isGreaterThan(openNum) && adxNum.isLessThan(numOfAdxLimit) && hlc3.getValue(series.getEndIndex()).isGreaterThan(sar.getValue(series.getEndIndex()))) {

                            numberOfOccurrenceLong++;

                            if (numberOfOccurrenceLong >= howManyOccurrencesRequired) {
                                resultEMA = 1;
                            }

                            String info = "Passed EMA (Series) LONG " + endOFLogEntry + " SeriesSizeEMA: " + numberOfOccurrenceLong;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }
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

                        if (resultWT == 1 && resultPPO == -2 && ppoEnd.isGreaterThan(numOfLimitBottomPPO_Negated) && ppoEnd.isLessThan(numOfLimitTopPPO_Negated) && ppoEnd.isGreaterThan(numOfPpoBefore)) {
                            resultPPO = 1;
                            String info = "Passed PPO LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (resultWT == -1 && resultPPO == -2 && ppoEnd.isGreaterThan(numOfLimitBottomPPO) && ppoEnd.isLessThan(numOfLimitTopPPO) && ppoEnd.isLessThan(numOfPpoBefore)) {
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

                            if (size == 0) {
                                size++;
                                longyOrShorty = 1;
                            } else if (longyOrShorty == 1) {
                                size++;
                            }

                            if (size >= firstConditionSize) {
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

                            if (size == 0) {
                                size++;
                                longyOrShorty = -1;
                            } else if (longyOrShorty == -1) {
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

                            if (size == 0) {
                                size++;
                                longyOrShorty = 1;
                            } else if (longyOrShorty == 1) {
                                size++;
                            }

                            if (size >= secondConditionSize) {
                                resultAROON = 1;
                                size = 0;
                            }
                            String info = "Passed EMA Second Condition (aroon field used) LONG " + endOFLogEntry + " SeriesSizeEMA: " + size;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultEMA == 1 && size > 0 && longyOrShorty == 1) {
                            size = 0;
                        }

                        if (resultEMA == -1 && resultAROON == -2 && ema12num.isLessThan(ema32num) && ema12num.isLessThan(ema92num) && hlc3Num.isLessThan(ema12num)) {

                            if (size == 0) {
                                size++;
                                longyOrShorty = -1;
                            } else if (longyOrShorty == -1) {
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

                        if (resultEMA == 1 && resultAROON == 1 && resultRSI == -2 && rsiEnd.isGreaterThan(numOfRSITopLevel) && i < series.getEndIndex() - 1) {
                            String info = "Passed RSI NOT FILLED - TOO EARLY " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        } else if (resultEMA == 1 && resultAROON == 1 && resultRSI == -2 && rsiEnd.isGreaterThan(numOfRSITopLevel) && i >= series.getEndIndex() - 1) {
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
                                ema12num.isGreaterThan(ema92num) && closeNum.isGreaterThan(close.getValue(i - 1))) {

                            if (seriesLengthOfClosePriceAboveEMA == 0) {
                                seriesLengthOfClosePriceAboveEMA++;
                                longOrShortCounting = 1;
                            } else if (longOrShortCounting == 1) {
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
                                openNum.isLessThan(ema12num) && openNum.isLessThan(ema32num) && ema12num.isLessThan(ema32num) && ema32num.isLessThan(ema92num) && ema12num.isLessThan(ema92num) && closeNum.isLessThan(close.getValue(i - 1))) {

                            if (seriesLengthOfClosePriceAboveEMA == 0) {
                                seriesLengthOfClosePriceAboveEMA++;
                                longOrShortCounting = -1;
                            } else if (longOrShortCounting == -1) {
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
            case 20:
                if (series.getBarCount() > params.getNrOfKlinesToWait() + 26) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i < series.getEndIndex(); i++) {

                        Num ema12num = ema12.getValue(i);
                        Num ema32num = ema32.getValue(i);
                        Num ema92num = ema92.getValue(i);
                        Num ema181num = ema181.getValue(i);
                        Num aSpanNum = IchimokuLeadingSpanA.getValue(i - 26);
                        Num bSpanNum = IchimokuLeadingSpanB.getValue(i - 26);
                        Num lowNum = low.getValue(i);
                        Num highNum = high.getValue(i);
                        Num ppoNum = ppo.getValue(i);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [ema181]: " + df.format(ema181.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue()) + " [SpanA]: " + df.format(IchimokuLeadingSpanA.getValue(i - 26).doubleValue()) + " [SpanB]: " + df.format(IchimokuLeadingSpanB.getValue(i - 26).doubleValue()) + " [SpanA-not26]: " + df.format(IchimokuLeadingSpanA.getValue(i).doubleValue()) + " [SpanB-not26]: " + df.format(IchimokuLeadingSpanB.getValue(i).doubleValue()) + " [BaseLineIchimoku]: " + df.format(IchimokuBaseLine.getValue(i).doubleValue()) + " [ConversionLineIchimoku]: " + df.format(IchimokuConversionLine.getValue(i).doubleValue());

                        if (ema12num.isLessThan(ema181num) && ema32num.isLessThan(ema92num) && ema32num.isGreaterThan(ema181num) && highNum.isLessThanOrEqual(ema181num)) {
                            resultEMA = -1;
                            String info = "Passed EMA SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (ema12num.isGreaterThan(ema181num) && ema32num.isGreaterThan(ema92num) && ema32num.isLessThan(ema181num) && lowNum.isGreaterThanOrEqual(ema181num)) {
                            resultEMA = 1;
                            String info = "Passed EMA LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (aSpanNum.isLessThan(bSpanNum)) {
                            resultICHIMOKU = -1;
                            String info = "Passed ICHIMOKU SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if (aSpanNum.isGreaterThan(bSpanNum)) {
                            resultICHIMOKU = 1;
                            String info = "Passed ICHIMOKU LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }
                    }
                }
                break;
            case 21: //WT
                if (series.getBarCount() > params.getNrOfKlinesToWait()) {
                    for (int i = series.getEndIndex() - params.getNrOfKlinesToWait(); i < series.getEndIndex(); i++) {

                        Num wt1 = tci.getValue(i);
                        Num wt2 = sma.getValue(i);
                        Num wt1back = tci.getValue(i - 1);
                        Num wt2back = sma.getValue(i - 1);

                        String endOFLogEntry = " at index " + i + " of interval " + params.getIntervalOfKlines() + " from: " + series.getBar(i).getBeginTime().toString() + " to: " + series.getBar(i).getEndTime().toString() + ". \nIndicatorValues: [hlc3]: " + df.format(hlc3.getValue(i).doubleValue()) + " [close]: " + df.format(close.getValue(i).doubleValue()) + " [hl2]: " + df.format(hl2.getValue(i).doubleValue()) + " [ema12]: " + df.format(ema12.getValue(i).doubleValue()) + " [ema32]: " + df.format(ema32.getValue(i).doubleValue()) + " [ema92]: " + df.format(ema92.getValue(i).doubleValue()) + " [ema181]: " + df.format(ema181.getValue(i).doubleValue()) + " [WT1]: " + df.format(tci.getValue(i).doubleValue()) + " [WT2]: " + df.format(sma.getValue(i).doubleValue()) + " [ATR]: " + df.format(atr.getValue(i).doubleValue()) + " [SAR]: " + df.format(sar.getValue(i).doubleValue()) + " [MACD]: " + df.format(macd.getValue(i).doubleValue()) + " [SignalMACD]: " + df.format(signalLine.getValue(i).doubleValue()) + " [ADX]: " + df.format(adx.getValue(i).doubleValue()) + " [PPO]: " + df.format(ppo.getValue(i).doubleValue()) + " [UpArron]: " + df.format(aroonHigh.getValue(i).doubleValue()) + " [downArron]: " + df.format(aroonLow.getValue(i).doubleValue()) + " [RSI]: " + df.format(rsi.getValue(i).doubleValue()) + " [SpanA]: " + df.format(IchimokuLeadingSpanA.getValue(i - 26).doubleValue()) + " [SpanB]: " + df.format(IchimokuLeadingSpanB.getValue(i - 26).doubleValue()) + " [SpanA-not26]: " + df.format(IchimokuLeadingSpanA.getValue(i).doubleValue()) + " [SpanB-not26]: " + df.format(IchimokuLeadingSpanB.getValue(i).doubleValue()) + " [BaseLineIchimoku]: " + df.format(IchimokuBaseLine.getValue(i).doubleValue()) + " [ConversionLineIchimoku]: " + df.format(IchimokuConversionLine.getValue(i).doubleValue());

                        if ((resultWT == -2 && wt1back.isGreaterThan(wt2back) && wt2.isGreaterThan(wt1) && wt1.isGreaterThan(numOfWTTopLevel))) {
                            resultWT = -1;
                            String info = "Passed WT SHORT " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }

                        if ((resultWT == -2 && wt2back.isGreaterThan(wt1back) && wt1.isGreaterThan(wt2) && wt1.isLessThan(numOfWTBottomLevel))) {
                            resultWT = 1;
                            String info = "Passed WT LONG " + endOFLogEntry;
                            ServiceFunctionsOther.writeToFile(info, context, "result");
                        }
                    }
                }
                break;
            default:
                break;
        }

        return new StrategyResultV2(resultMACD, resultWT, resultPPO, resultADX, resultAROON, resultEMA, resultRSI, resultICHIMOKU);

    }
}
