package app.makorz.cryptofun.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import app.makorz.cryptofun.data.ApprovedToken;
import app.makorz.cryptofun.data.StrategyParameters;
import app.makorz.cryptofun.data.StrategyResultV2;
import app.makorz.cryptofun.data.database.DBHandler;
import app.makorz.cryptofun.data.database.Kline;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

public class ServiceMainStrategies {

    private static final String TAG = "MainStrategies";

    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String TABLE_NAME_APPROVED_HISTORIC = "historic_approved_tokens";

    public static void strategyNr8_15m_OverEMA_BasedOnGLOBAL(String symbol, List<Kline> coinKlines15m, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 1000000;
        int acceptableNrOfTrades = 2000;
        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
        }
        //Need to wait a while before new 15h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines15m.get(0).gettOpenTime();
        long minutes3 = 180000;
        long hours8 = 28800000;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes3 && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT")) { //

            StrategyParameters params15m = new StrategyParameters(9, "15m", 60, 30, 70, 90, 30, 0.5f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 8, params15m, context);
            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 8 ---> [Volume]: " + volumeOfLast45mKlines + " [NrOfTrades]: " + nrOfTradesLast45mKlinesSum +  " [Strategy<>15m]: " + strategy15m;
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy15m.getPassedEMA() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy15m.getPassedEMA() == -1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr7_EMA_4h_WT_ADX_EMA_15m(String symbol, List<Kline> coinKlines15m, List<Kline> coinKlines4h, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 1000000;
        int acceptablePercentOfVolumeRise = 0;
        int acceptableNrOfTrades = 5000;
        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines15m, 4);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }
        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines4h.get(0).gettOpenTime();
        long minutes20 = 1200000;
        long hours8 = 28800000;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes20 && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && percentOfRiseOfNumberOfVolumeInLast45min > acceptablePercentOfVolumeRise && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT")) { //

            StrategyParameters params15m = new StrategyParameters(3, "15m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params4h = new StrategyParameters(1, "4h", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 7, params15m, context);
            StrategyResultV2 strategy4h = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 7, params4h, context);
            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 6 ---> [Volume]: " + volumeOfLast45mKlines + " [%OfVolumeRise]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades]: " + nrOfTradesLast45mKlinesSum + " [4hKlineOpenTime]: " + df.format(coinKlines4h.get(0).gettOpenTime()) + " [CurrentTime]: " +  df.format(System.currentTimeMillis()) + " [TrendEMA<>4h-15m]: " + strategy4h.getPassedEMA() + " " + strategy15m.getPassedEMA() + " [WT<>4h-15m]: " + strategy4h.getPassedWT() + " " + strategy15m.getPassedWT() + " [ADX<>4h-15m]: " + strategy4h.getPassedADX() + " " + strategy15m.getPassedADX();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy4h.getPassedEMA() == 1 && strategy15m.getPassedEMA() == 1 && strategy15m.getPassedADX() == 1 && strategy15m.getPassedWT() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy4h.getPassedEMA() == -1 && strategy15m.getPassedEMA() == -1 && strategy15m.getPassedADX() == 1 && strategy15m.getPassedWT() == -1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr6_KlinesCrossEMA_RSI_Appear(String symbol, List<Kline> coinKlines, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

        //Need to wait a while after new kline appear (here is suited for 4h hours klines
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines.get(0).gettOpenTime();
        long minutes30 = 1800000;
        long hours24 = 48 * 60 * 60 * 1000L;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes30 && !symbol.contains("BUSDUSDT")) {

            StrategyParameters params = new StrategyParameters(40, "4h", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines, 6, params, context);

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 6 ---> [FirstConditionEMA_HLC3]: " + strategy.getPassedEMA() + " [SecondConditionEMA_HLC3_Cross]: " + strategy.getPassedAROON() + " [ThirdCondition_RSI]: " + strategy.getPassedRSI();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");
            
            if (strategy.getPassedEMA() == 1 && strategy.getPassedAROON() == 1 && strategy.getPassedRSI() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours24));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy.getPassedRSI() == -1 && strategy.getPassedEMA() == -1 && strategy.getPassedAROON() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours24));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr8_KlinesCrossEMA_RSI_Appear_15m(String symbol, List<Kline> coinKlines, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

        //Need to wait a while after new kline appear (here is suited for 4h hours klines
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines.get(0).gettOpenTime();
        long minutes5 = 300000;
        long hours12 = 12 * 60 * 60 * 1000L;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes5 && !symbol.contains("BUSDUSDT")) {

            StrategyParameters params = new StrategyParameters(20, "15m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines, 6, params, context);

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 6 ---> [FirstConditionEMA_HLC3]: " + strategy.getPassedEMA() + " [SecondConditionEMA_HLC3_Cross]: " + strategy.getPassedAROON() + " [ThirdCondition_RSI]: " + strategy.getPassedRSI();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy.getPassedEMA() == 1 && strategy.getPassedAROON() == 1 && strategy.getPassedRSI() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours12));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy.getPassedRSI() == -1 && strategy.getPassedEMA() == -1 && strategy.getPassedAROON() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours12));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr5_EMA_ADX_percentDifferenceEMA_15m(String symbol, List<Kline> coinKlines15m, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 200000;
        int acceptableNrOfTrades = 1000;
        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
        }

        /*
        How to use strategy i Ta4J library

        StrategyResult strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J_nr2(coinKlines15m, context);
        String strategyResult15m = "Strategy15m: [" + strategy15m.getBestStrategy() + "] Positions(M,A): " + strategy15m.getNrOfPositions().get(0).toString() + " " + strategy15m.getNrOfPositions().get(1).toString() + " vsBuyAndHold(M,A): "
                + strategy15m.getVsBuyAndHoldProfit().get(0).toString() + " " + strategy15m.getVsBuyAndHoldProfit().get(1).toString() + " total(M,A): " + strategy15m.getTotalProfit().get(0).toString() + " " + strategy15m.getTotalProfit().get(1).toString();

        if (strategy15m.getNrOfPositions().get(0) == 0 && strategy15m.getNrOfPositions().get(1) == 0) {
            strategyResult15m = "Strategy15m: NO POSITIONS";
        }
        int finalWaveTrend15m = strategy15m.getWaveTrendIndicator();

        */

        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines15m.get(0).gettOpenTime();
        long minutes3 = 30000; //18000
        long hours6 = 21600000;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes3 && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT")) { //

            StrategyParameters params15m = new StrategyParameters(4, "15m", 70, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 1, params15m, context);

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 1 ---> [Volume]: " + volumeOfLast45mKlines + " [NrOfTrades]: " + nrOfTradesLast45mKlinesSum + " [15hKlineOpenTime]: " + df.format(coinKlines15m.get(0).gettOpenTime()) + " [CurrentTime]: " +  df.format(System.currentTimeMillis()) + " [TrendEMA<>15m-3m]: " + strategy15m.getPassedEMA() + " [WT<>15m-3m]: " + strategy15m.getPassedWT() + " [ADX<>15m-3m]: " + strategy15m.getPassedADX();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy15m.getPassedEMA() == 1 && strategy15m.getPassedWT() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours6));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if ( strategy15m.getPassedEMA() == -1 && strategy15m.getPassedWT() == -1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours6));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr10_Ichimoku_4h(String symbol, List<Kline> coinKlines, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines.get(0).gettOpenTime();
        long minutes = 30 * 30 * 1000L;
        long hours = 72 * 60 * 60 * 1000L;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes && !symbol.contains("BUSDUSDT")) { //

            StrategyParameters params = new StrategyParameters(30, "4h", 70, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines, 10, params, context);

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 5 ---> [15hKlineOpenTime]: " + df.format(coinKlines.get(0).gettOpenTime()) + " [CurrentTime]: " +  df.format(System.currentTimeMillis()) + " [Ichimoku]: " + strategy.getPassedICHIMOKU() + " [IchimokuSecondCondition]: " + strategy.getPassedAROON() + " [ADX<>15m-3m]: " + strategy.getPassedADX();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy.getPassedICHIMOKU() == 1 && strategy.getPassedAROON() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if ( strategy.getPassedICHIMOKU() == -1 && strategy.getPassedAROON() == -1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr10_Ichimoku_15m(String symbol, List<Kline> coinKlines, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 200000;
        int acceptableNrOfTrades = 1000;
        if (coinKlines.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines, 0, 3);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
        }


        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines.get(0).gettOpenTime();
        long minutes = 3 * 30 * 1000L;
        long hours = 12 * 60 * 60 * 1000L;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT")) { //

            StrategyParameters params = new StrategyParameters(70, "15m", 70, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines, 10, params, context);

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 5 ---> [15hKlineOpenTime]: " + df.format(coinKlines.get(0).gettOpenTime()) + " [CurrentTime]: " +  df.format(System.currentTimeMillis()) + " [Ichimoku]: " + strategy.getPassedICHIMOKU() + " [IchimokuSecondCondition]: " + strategy.getPassedAROON() + " [ADX<>15m-3m]: " + strategy.getPassedADX();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy.getPassedICHIMOKU() == 1 && strategy.getPassedAROON() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if ( strategy.getPassedICHIMOKU() == -1 && strategy.getPassedAROON() == -1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr4_RSI_EMA_4h(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 1000000;
        int acceptablePercentOfVolumeRise = 50;
        int acceptableNrOfTrades = 5000;
        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines15m, 4);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }
        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines4h.get(0).gettOpenTime();
        long minutes20 = 1200000;
        long hours8 = 28800000;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes20 && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && percentOfRiseOfNumberOfVolumeInLast45min > acceptablePercentOfVolumeRise && !symbol.contains("BUSDUSDT")) {

            StrategyParameters params3m = new StrategyParameters(2, "3m", 60, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params15m = new StrategyParameters(5, "15m", 60, 31, 69, 90, 39, 0.5f, 12.0f);
            StrategyParameters params4h = new StrategyParameters(5, "4h", 60, 31, 69, 90, 39, 0.5f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 3, params15m, context);
            StrategyResultV2 strategy4h = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 3, params4h, context);
            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 4 ---> [Volume]: " + volumeOfLast45mKlines + " [%OfVolumeRise]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades]:" + nrOfTradesLast45mKlinesSum + " [TrendEMA<>4h-15m]: " + strategy4h.getPassedEMA() + " " + strategy15m.getPassedEMA() + " [WT<>4h-15m]: " + strategy4h.getPassedWT() + " " + strategy15m.getPassedWT() + " [RSI<>4h-15m]: " + strategy4h.getPassedRSI() + " " + strategy15m.getPassedRSI();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy4h.getPassedEMA() == 1 && strategy4h.getPassedRSI() == 1 && strategy4h.getPassedADX() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy4h.getPassedRSI() == -1 && strategy4h.getPassedEMA() == -1 && strategy4h.getPassedADX() == 1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }


    public static void strategyNr3_MultiTimeframe_IchimokuRsiMacd(String symbol, List<Kline> coinKlines15m, List<Kline> coinKlines4h, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 250000;
        int acceptableNrOfTrades = 1000;
        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines15m, 4);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }
        //Need to wait a while before new 4h klines appears
//        long differenceBetweenCurrentTimeAndOpenTime15m = System.currentTimeMillis() - coinKlines15m.get(0).gettOpenTime();
//        long differenceBetweenCurrentTimeAndOpenTime4h = System.currentTimeMillis() - coinKlines4h.get(0).gettOpenTime();
//        long minutes5 = 300000;
//        long minutes20 = 1200000;
        long hours8 = 28800000;
//        differenceBetweenCurrentTimeAndOpenTime15m > minutes5 && differenceBetweenCurrentTimeAndOpenTime4h > minutes20 && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume &&

        if (  !symbol.contains("BUSDUSDT")) {

            StrategyParameters params15m = new StrategyParameters(16, "15m", 60, 31, 69, 90, 39, 0.5f, 12.0f);
            StrategyParameters params4h = new StrategyParameters(8, "4h", 60, 31, 69, 90, 39, 0.5f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 12, params15m, context);
            StrategyResultV2 strategy4h = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 12, params4h, context);


            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 3 ---> [Volume]: " + volumeOfLast45mKlines + " [%OfVolumeRise]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades]:" + nrOfTradesLast45mKlinesSum + " [MACD<>15m,4h]: " + strategy15m.getPassedMACD() + strategy4h.getPassedMACD() + " [RSI<>15m,4h]: " + strategy15m.getPassedRSI() + strategy4h.getPassedRSI() + " [ICHIMOKU<>15m,4h]: " + strategy15m.getPassedICHIMOKU() + strategy4h.getPassedICHIMOKU();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy4h.getPassedICHIMOKU() == 1 && strategy15m.getPassedRSI() == 1 && strategy15m.getPassedMACD() == 1 && strategy15m.getPassedICHIMOKU() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy4h.getPassedICHIMOKU() == -1 && strategy15m.getPassedRSI() == -1 && strategy15m.getPassedMACD() == -1 && strategy15m.getPassedICHIMOKU() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr6_AdvancedIchimoku_15m4h(String symbol, List<Kline> coinKlines15m, List<Kline> coinKlines4h, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 250000;
        int acceptableNrOfTrades = 1000;
        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines15m, 4);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }
        
        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime15m = System.currentTimeMillis() - coinKlines15m.get(0).gettOpenTime();
        long differenceBetweenCurrentTimeAndOpenTime4h = System.currentTimeMillis() - coinKlines4h.get(0).gettOpenTime();
        long minutes5 = 300000;
        long minutes20 = 1200000;
        long hours8 = 28800000;


        if (differenceBetweenCurrentTimeAndOpenTime15m > minutes5 && differenceBetweenCurrentTimeAndOpenTime4h > minutes20 && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume &&  !symbol.contains("BUSDUSDT")) {

            StrategyParameters params15m = new StrategyParameters(9, "15m", 60, 31, 69, 90, 20, 0.5f, 12.0f);
            StrategyParameters params4h = new StrategyParameters(2, "4h", 60, 31, 69, 90, 25, 0.5f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 13, params15m, context);
            StrategyResultV2 strategy4h = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 13, params4h, context);

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 6 ---> [Volume]: " + volumeOfLast45mKlines + " [%OfVolumeRise]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades]:" + nrOfTradesLast45mKlinesSum + " [MACD<>15m,4h]: " + strategy15m.getPassedMACD() + " " + strategy4h.getPassedMACD() + " [ADX<>15m,4h]: " + strategy15m.getPassedADX() + " " + strategy4h.getPassedADX() + " [ICHIMOKU<>15m,4h]: " + strategy15m.getPassedICHIMOKU() + " " + strategy4h.getPassedICHIMOKU() + " [EMA<>15m,4h]: " + strategy15m.getPassedEMA() + " " + strategy4h.getPassedEMA();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy15m.getPassedEMA() == 1 && strategy15m.getPassedADX() == 1 && strategy15m.getPassedMACD() == 1 && strategy15m.getPassedICHIMOKU() == 1 &&
                    strategy4h.getPassedEMA() == 1 && strategy4h.getPassedADX() == 1 && strategy4h.getPassedMACD() == 1 && strategy4h.getPassedICHIMOKU() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy15m.getPassedEMA() == -1 && strategy15m.getPassedADX() == 1 && strategy15m.getPassedMACD() == -1 && strategy15m.getPassedICHIMOKU() == -1 &&
                    strategy4h.getPassedEMA() == -1 && strategy4h.getPassedADX() == 1 && strategy4h.getPassedMACD() == -1 && strategy4h.getPassedICHIMOKU() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }



    public static void strategyNr3_EMA_KlineSize_WT_15m(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 250000;
        int acceptableNrOfTrades = 1000;
        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines15m, 4);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }
        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines15m.get(0).gettOpenTime();
        long minutes5 = 300000;
        long hours8 = 28800000;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes5 && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT")) {

            StrategyParameters params15m = new StrategyParameters(30, "15m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 2, params15m, context);

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 3 ---> [Volume]: " + volumeOfLast45mKlines + " [%OfVolumeRise]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades]:" + nrOfTradesLast45mKlinesSum + " [TrendEMA<>15m]: " + strategy15m.getPassedEMA() + " [WT<>15m]: " + strategy15m.getPassedWT();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            // When not using strategy on 3m we need to reverse collection
            Collections.reverse(coinKlines3m);
            Log.e(TAG, "LAST: " + df.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime()) + " FIRST: " + df.format(coinKlines3m.get(0).gettCloseTime()));

            if (strategy15m.getPassedEMA() == 1 && strategy15m.getPassedWT() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy15m.getPassedRSI() == -1 && strategy15m.getPassedEMA() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr2_EMA_KlineSize_WT_4h(String symbol, List<Kline> coinKlines15m, List<Kline> coinKlines4h, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 500000;
        int acceptableNrOfTrades = 1000;
        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, 0, 3);
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines15m, 4);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }
        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines4h.get(0).gettOpenTime();
        long minutes60 = 3600000;
        long hours8 = 28800000;
        // When not using strategy on 3m we need to reverse collection
        Collections.reverse(coinKlines15m);
        Log.e(TAG, "LAST: " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime()) + " FIRST: " + df.format(coinKlines15m.get(0).gettCloseTime()));

        if (differenceBetweenCurrentTimeAndOpenTime > minutes60 && nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT")) {

            StrategyParameters params4h = new StrategyParameters(30, "4h", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 strategy4h = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 2, params4h, context);
            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 2 ---> [Volume]: " + volumeOfLast45mKlines + " [%OfVolumeRise]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades]:" + nrOfTradesLast45mKlinesSum + " [TrendEMA<>4h]: " + strategy4h.getPassedEMA() + " [WT<>4h]: " + strategy4h.getPassedWT() + " [RSI<>4h]: " + strategy4h.getPassedRSI();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy4h.getPassedEMA() == 1 && strategy4h.getPassedWT() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy4h.getPassedWT() == -1 && strategy4h.getPassedEMA() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } 
                data.close();
            }
        }
    }

    public static void strategyNr1_quickTestStrategy_15m(String symbol, List<Kline> coinKlines15m, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        float volumeOfLast45mKlines;
        int acceptableVolume = 2000000;
        if (coinKlines15m.size() > 3) {
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, 0, 3);
        } else {
            volumeOfLast45mKlines = 0;
        }
        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines15m.get(0).gettOpenTime();
        long minutes3 = 30000;
        long hours8 = 28800000;

        if (differenceBetweenCurrentTimeAndOpenTime > minutes3 && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT")) {

            StrategyParameters params15m = new StrategyParameters(4, "15m", 53, 30, 70, 90, 20, 0.5f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 0, params15m, context);
            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 1 ---> [Volume]: " + volumeOfLast45mKlines + " [TrendEMA<>4h]: " + strategy15m.getPassedEMA() + " [WT<>4h]: " + strategy15m.getPassedWT() + " [RSI<>4h]: " + strategy15m.getPassedRSI();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy15m.getPassedEMA() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy15m.getPassedEMA() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8));
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }


/*
    Version1: (priceDirection3 == 0 && priceDirection15 == 0 && waveTrendPredict15 == 0) --> 4,5/10
    Version2: (isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0) && priceDirection3 == 0 && priceDirection15 == 0) --> 6,5/10
    Version3: isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0)  && sumDirection >= -2  --> 3/10
    Version4: isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 1) && (priceDirection3 +  predict3) >= 1) --> 3/10
    Version5 - currently API 30 : isKlineApprovedForLongOrShort(statusOf3mKlines, statusOf15mKlines, 0) && priceDirection3 == -1 && priceDirection15 == -1 && predict3 == -1
    Version6 - currently API 27 :  priceDirection3 == -1 && priceDirection15 == 1 && priceDirection4 == -1
    !!!! Combined last two: 5 - used when (last15mPriceChange > 0.5 || last15mPriceChange < -0.5) --- 6 - used when (last15mPriceChange > 3 || last15mPriceChange < -3) --- 6 first before 5 !!!!
                         *This combined quite well works if market is going down or up, not when it's stabilised

     API 27 (01.07) --> ((waveTrendPredict15 + waveTrendPredict4 ) == -2 || (waveTrendPredict15 + waveTrendPredict4 ) == 2) && (priceDirection4 + predict15 == -2)
     API 30 (01.07) -->  isKlineApprovedForLongOrShort(statusListOf3mToCheck, statusListOf15mToCheck, 0)  && priceDirection3 == -1 && priceDirection4 == 1 &&  ((waveTrendPredict15 + waveTrendPredict4 + waveTrendPredict3) == 0)
*/

}
