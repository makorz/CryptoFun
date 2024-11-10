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

            //!!!!!!!!!!!!!!!!! BELOW nr_of_trades_on_klines VALUE is just StrategyNe, os i can run multiple strategies at once !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            if (strategy4h.getPassedEMA() == 1 && strategy15m.getPassedEMA() == 1 && strategy15m.getPassedADX() == 1 && strategy15m.getPassedWT() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8), 7);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 7,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 7,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 7,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy4h.getPassedEMA() == -1 && strategy15m.getPassedEMA() == -1 && strategy15m.getPassedADX() == 1 && strategy15m.getPassedWT() == -1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),7);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 7,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 7,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 7,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours12),8);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 8,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 8,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 8,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy.getPassedRSI() == -1 && strategy.getPassedEMA() == -1 && strategy.getPassedAROON() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours12),8);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines.get(coinKlines.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 8,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 8,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 8,
                            4f, System.currentTimeMillis(), coinKlines.get(coinKlines.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


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
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),3);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 3,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 3,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 3,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy15m.getPassedRSI() == -1 && strategy15m.getPassedEMA() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),3);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 3,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 3,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 3,
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
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),2);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 2,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 2,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy4h.getPassedWT() == -1 && strategy4h.getPassedEMA() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),2);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 2,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 2,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } 
                data.close();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void strategyNr20_EMA_ICHIMOKU_15m(String symbol, List<Kline> coinKlines15m, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

        //Need to wait a while before new 4h klines appears
        long differenceBetweenCurrentTimeAndOpenTime = System.currentTimeMillis() - coinKlines15m.get(0).gettOpenTime();
        long minutes2 = 120000;
        long hours8 = 28800000;

        if (!symbol.contains("BUSDUSDT")) {

            StrategyParameters params15m = new StrategyParameters(1, "15m", 53, 30, 70, 90, 40, 0.1f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 20, params15m, context);

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 7 ---> [TrendEMA]: " + strategy15m.getPassedEMA() + " [ICHIMOKU]: " + strategy15m.getPassedICHIMOKU();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy15m.getPassedEMA() == 1 && strategy15m.getPassedICHIMOKU() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),20);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 20,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 20,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy15m.getPassedEMA() == -1 && strategy15m.getPassedICHIMOKU() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8), 20);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 20,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 20,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void strategyNr21_WT_15m(String symbol, List<Kline> coinKlines15m, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

        long hours8 = 28800000;

        if (!symbol.contains("BUSDUSDT") && coinKlines15m.size() > 2) {

            StrategyParameters params15m = new StrategyParameters(1, "15m", 70, 30, 70, 90, 40, 0.1f, 12.0f);
            StrategyResultV2 strategy15m = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 21, params15m, context);

            String info = "LEVEL 1 [" + symbol + "] STRATEGY: 6 ---> [WT]: " + strategy15m.getPassedWT();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (strategy15m.getPassedWT() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),21);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 21,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 21,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy15m.getPassedWT() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),21);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 21).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 21).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void strategyNr4_Trend_GreenRed_15m_3m(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        long hours8 = 28800000;
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

        Log.e(TAG, "LAST: " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime()) + " FIRST: " + df.format(coinKlines15m.get(0).gettCloseTime()));

        //!!!!!!!!!!!!!!!!! BELOW nr_of_trades_on_klines VALUE is just StrategyNe, os i can run multiple strategies at once !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        if (nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT")) {

            //EMA_4h_WT_ADX_EMA_15m
            StrategyParameters params15m_strategy1 = new StrategyParameters(3, "15m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params4h_strategy1 = new StrategyParameters(1, "4h", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 results15_strategy1 = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 7, params15m_strategy1, context);
            StrategyResultV2 results4_strategy1 = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 7, params4h_strategy1, context);

            //EMA_KlineSize_WT_15m
            StrategyParameters params15m_strategy2 = new StrategyParameters(30, "15m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 results15_strategy2 = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 2, params15m_strategy2, context);

            //ICHIMOKU
            StrategyParameters params15m_strategy3 = new StrategyParameters(1, "15m", 53, 30, 70, 90, 40, 0.1f, 12.0f);
            StrategyResultV2 results15_strategy3 = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 20, params15m_strategy3, context);

            //GreenRed
            StrategyParameters params15m_strategy4 = new StrategyParameters(4, "15m", 53, 30, 70, 90, 16, 0.5f, 12.0f);
            StrategyResultV2 results15_strategy4 = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines15m, 4, params15m_strategy4, context);
            StrategyParameters params3m_strategy4 = new StrategyParameters(14, "3m", 53, 30, 70, 90, 30, 0.5f, 12.0f);
            StrategyResultV2 results3_strategy4 = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines3m, 4, params3m_strategy4, context);

            //EMA_KlineSize_WT_4h
            StrategyParameters params4h_strategy5 = new StrategyParameters(30, "4h", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 results4_strategy5 = ServiceFunctionsStrategyTa4J.strategyTa4J(coinKlines4h, 2, params4h_strategy5, context);

            String info = "LEVEL 1 [" + symbol + "] [Volume]: " + volumeOfLast45mKlines + " [%OfVolumeRise]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades]: " + nrOfTradesLast45mKlinesSum
                    + "\n\tSTRATEGY: 1 (WT,EMAx2,ADX) ---> [TrendEMA<>4h-15m]: " + results4_strategy1.getPassedEMA() + " " + results15_strategy1.getPassedEMA() + " [WT<>4h,15m]: " + results4_strategy1.getPassedWT() + " " + results15_strategy1.getPassedWT() + " [ADX<>4h,15m]: " + results4_strategy1.getPassedADX() + " " + results15_strategy1.getPassedADX()
                    + "\n\tSTRATEGY: 2 (WT,EMA,KlineSize) ---> [FirstConditionEMA_HLC3]: " + results15_strategy2.getPassedEMA() + " [SecondConditionEMA_HLC3_Cross]: " + results15_strategy2.getPassedAROON() + " [ThirdCondition_RSI]: " + results15_strategy2.getPassedRSI()
                    + "\n\tSTRATEGY: 3 (Ichimoku) ---> [TrendEMA<>15m]: " + results15_strategy3.getPassedEMA() + " [ICHIMOKU<>15m]: " + results15_strategy3.getPassedICHIMOKU()
                    + "\n\tSTRATEGY: 4 (GreenRed) ---> [TrendEMA<>15m,3m]: " + results15_strategy4.getPassedEMA() + " " + results3_strategy4.getPassedEMA()
                    + "\n\tSTRATEGY: 5 (EMA-4h,WT-4h) ---> [TrendEMA<>4h]: " + results4_strategy5.getPassedEMA() + " [WT<>4h]: " + results4_strategy5.getPassedWT();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, context, "result");

            if (results4_strategy1.getPassedEMA() == 1 && results15_strategy1.getPassedEMA() == 1 && results15_strategy1.getPassedADX() == 1 && results15_strategy1.getPassedWT() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),1);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG STRATEGY 1] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 1,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 1,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (results4_strategy1.getPassedEMA() == -1 && results15_strategy1.getPassedEMA() == -1 && results15_strategy1.getPassedADX() == 1 && results15_strategy1.getPassedWT() == -1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),1);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT STRATEGY 1] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 1,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 1,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }

            if (results15_strategy2.getPassedEMA() == 1 && results15_strategy2.getPassedAROON() == 1 && results15_strategy2.getPassedRSI() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),2);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG STRATEGY 2] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (results15_strategy2.getPassedRSI() == -1 && results15_strategy2.getPassedEMA() == -1 && results15_strategy2.getPassedAROON() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),2);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT STRATEGY 2] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 2,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 2,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }

            if (results15_strategy3.getPassedEMA() == 1 && results15_strategy3.getPassedICHIMOKU() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),3);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG STRATEGY 3] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 3,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 3,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (results15_strategy3.getPassedEMA() == -1 && results15_strategy3.getPassedICHIMOKU() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),3);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT STRATEGY 3] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 3,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 3,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }

            if (results15_strategy4.getPassedEMA() == 1 && results3_strategy4.getPassedEMA() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),4);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG STRATEGY 4] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();

            } else if (results15_strategy4.getPassedEMA() == -1 && results3_strategy4.getPassedEMA() == -1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),4);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT STRATEGY 4] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines3m.get(coinKlines3m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 4,
                            4f, System.currentTimeMillis(), coinKlines3m.get(coinKlines3m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }

            if (results4_strategy5.getPassedEMA() == 1 && results4_strategy5.getPassedWT() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),5);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG STRATEGY 5] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 5,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 5,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (results4_strategy5.getPassedWT() == -1 && results4_strategy5.getPassedEMA() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),5);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT STRATEGY 5] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 5,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 5,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    public static void strategyNr0_quickTestStrategy_15m(String symbol, List<Kline> coinKlines15m, Context context) {

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
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),0);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 0,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 0,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 0,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (strategy15m.getPassedEMA() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),0);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 0,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 0,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                } else {
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 0,
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
