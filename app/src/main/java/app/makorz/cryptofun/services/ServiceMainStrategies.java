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
import java.util.List;

public class ServiceMainStrategies {

    private static final String TAG = "MainStrategies";

    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String TABLE_NAME_APPROVED_HISTORIC = "historic_approved_tokens";

    public static void multipleStrategiesFunction(String symbol, List<Kline> coinKlines3m, List<Kline> coinKlines15m, List<Kline> coinKlines4h, Context context) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
        long hours8 = 28800000;
        float volumeOfLast45mKlines, percentOfRiseOfNumberOfVolumeInLast45min, nrOfTradesLast45mKlinesSum;
        int acceptableVolume = 1000000;
        int acceptableNrOfTrades = 10000;

        int klineSize = coinKlines15m.size();
        if (klineSize > 3) {
            int endIndex = klineSize - 1;
            int startIndex = klineSize - 4;
            volumeOfLast45mKlines = ServiceFunctionsStrategyDefault.countMoneyVolumeAtInterval(coinKlines15m, startIndex, endIndex);
            nrOfTradesLast45mKlinesSum = ServiceFunctionsStrategyDefault.countNrOfTradesAtInterval(coinKlines15m, startIndex, endIndex);
            percentOfRiseOfNumberOfVolumeInLast45min = ServiceFunctionsStrategyDefault.countBeforeAndAfter(coinKlines15m, 4);
        } else {
            volumeOfLast45mKlines = 0;
            nrOfTradesLast45mKlinesSum = 0;
            percentOfRiseOfNumberOfVolumeInLast45min = 0;
        }


        if (symbol.equals("ETHUSDT")) {
            Log.e(TAG, "LAST: " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime()) + " FIRST: " + df.format(coinKlines15m.get(0).gettCloseTime()));
        }

        //!!!!!!!!!!!!!!!!! BELOW nr_of_trades_on_klines VALUE is just StrategyNe, os i can run multiple strategies at once !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        //
        if (nrOfTradesLast45mKlinesSum >= acceptableNrOfTrades && volumeOfLast45mKlines >= acceptableVolume && !symbol.contains("BUSDUSDT") && !symbol.contains("USTCUSDT")) {

            StrategyTa4J strategy_for_3m = new StrategyTa4J(coinKlines3m);
            StrategyTa4J strategy_for_15m = new StrategyTa4J(coinKlines15m);
            StrategyTa4J strategy_for_4h = new StrategyTa4J(coinKlines4h);

            //EMA_4h_WT_ADX_EMA_15m
            StrategyParameters params15m_strategy1 = new StrategyParameters(3, "15m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyParameters params4h_strategy1 = new StrategyParameters(1, "4h", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 results15_strategy1 = strategy_for_15m.strategyTa4J(7, params15m_strategy1, context);
            StrategyResultV2 results4_strategy1 = strategy_for_4h.strategyTa4J( 7, params4h_strategy1, context);

            //EMA_KlineSize_WT_15m
            StrategyParameters params15m_strategy2 = new StrategyParameters(30, "15m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 results15_strategy2 = strategy_for_15m.strategyTa4J(2, params15m_strategy2, context);

            //ICHIMOKU_15m
            StrategyParameters params15m_strategy3 = new StrategyParameters(1, "15m", 53, 30, 70, 90, 40, 0.1f, 12.0f);
            StrategyResultV2 results15_strategy3 = strategy_for_15m.strategyTa4J( 20, params15m_strategy3, context);

            //Trend_GreenRed_15m_3m
            StrategyParameters params15m_strategy4 = new StrategyParameters(4, "15m", 53, 30, 70, 90, 16, 0.5f, 12.0f);
            StrategyResultV2 results15_strategy4 = strategy_for_15m.strategyTa4J( 4, params15m_strategy4, context);
            StrategyParameters params3m_strategy4 = new StrategyParameters(14, "3m", 53, 30, 70, 90, 30, 0.5f, 12.0f);
            StrategyResultV2 results3_strategy4 = strategy_for_3m.strategyTa4J(4, params3m_strategy4, context);

            //EMA_KlineSize_WT_4h
            StrategyParameters params4h_strategy5 = new StrategyParameters(30, "4h", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 results4_strategy5 = strategy_for_4h.strategyTa4J(2, params4h_strategy5, context);

            //KlinesCrossEMA_RSI_Appear_15m
            StrategyParameters params15m_strategy6 = new StrategyParameters(20, "15m", 53, 30, 70, 90, 40, 0.5f, 12.0f);
            StrategyResultV2 results15_strategy6 = strategy_for_15m.strategyTa4J( 6, params15m_strategy6, context);

            //ICHIMOKU_14h
            StrategyParameters params15m_strategy7 = new StrategyParameters(1, "15m", 53, 30, 70, 90, 40, 0.1f, 12.0f);
            StrategyResultV2 results15_strategy7 = strategy_for_15m.strategyTa4J( 20, params15m_strategy7, context);


            String info = "LEVEL 1 [" + symbol + "] [Volume]: " + volumeOfLast45mKlines + " [%OfVolumeRise]: " + percentOfRiseOfNumberOfVolumeInLast45min + " [NrOfTrades]: " + nrOfTradesLast45mKlinesSum
                    + "\n\tSTRATEGY: 1 (WT,EMAx2,ADX) ---> [TrendEMA<>4h-15m]: " + results4_strategy1.getPassedEMA() + " " + results15_strategy1.getPassedEMA() + " [WT<>4h,15m]: " + results4_strategy1.getPassedWT() + " " + results15_strategy1.getPassedWT() + " [ADX<>4h,15m]: " + results4_strategy1.getPassedADX() + " " + results15_strategy1.getPassedADX()
                    + "\n\tSTRATEGY: 2 (WT,EMA,KlineSize) ---> [FirstConditionEMA_HLC3]: " + results15_strategy2.getPassedEMA() + " [SecondConditionEMA_HLC3_Cross]: " + results15_strategy2.getPassedAROON() + " [ThirdCondition_RSI]: " + results15_strategy2.getPassedRSI()
                    + "\n\tSTRATEGY: 3 (Ichimoku) ---> [TrendEMA<>15m]: " + results15_strategy3.getPassedEMA() + " [ICHIMOKU<>15m]: " + results15_strategy3.getPassedICHIMOKU()
                    + "\n\tSTRATEGY: 4 (GreenRed) ---> [TrendEMA<>15m,3m]: " + results15_strategy4.getPassedEMA() + " " + results3_strategy4.getPassedEMA()
                    + "\n\tSTRATEGY: 5 (EMA-4h,WT-4h) ---> [TrendEMA<>4h]: " + results4_strategy5.getPassedEMA() + " [WT<>4h]: " + results4_strategy5.getPassedWT()
                    + "\n\tSTRATEGY: 6 (KlinesCrossEMA_RSI_Appear_15m) ---> [FirstConditionEMA_HLC3]: " + results15_strategy6.getPassedEMA() + " [SecondConditionEMA_HLC3_Cross]: " + results15_strategy6.getPassedAROON() + " [ThirdCondition_RSI]: " + results15_strategy6.getPassedRSI();
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

            if (results15_strategy6.getPassedEMA() == 1 && results15_strategy6.getPassedAROON() == 1 && results15_strategy6.getPassedRSI() == 1) {
                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),6);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " LONG STRATEGY 6] approved at: " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 6,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 1, 6,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            } else if (results15_strategy6.getPassedRSI() == -1 && results15_strategy6.getPassedEMA() == -1 && results15_strategy6.getPassedAROON() == -1) {

                Cursor data = databaseDB.checkIfSymbolWasApprovedInCertainTime(symbol, (System.currentTimeMillis() - hours8),6);
                data.moveToFirst();
                if (data.getCount() == 0) {
                    info = "LEVEL 2 [" + symbol + " SHORT STRATEGY 6] approved at:  " + df.format(System.currentTimeMillis()) + " " + df.format(coinKlines15m.get(coinKlines15m.size() - 1).gettCloseTime());
                    Log.e(TAG, info);
                    ServiceFunctionsOther.writeToFile(info, context, "result");
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 6,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED);
                    databaseDB.addApprovedNewCrypto(new ApprovedToken(symbol, 0, 6,
                            4f, System.currentTimeMillis(), coinKlines15m.get(coinKlines15m.size() - 1).tClosePrice), TABLE_NAME_APPROVED_HISTORIC);
                }
                data.close();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
