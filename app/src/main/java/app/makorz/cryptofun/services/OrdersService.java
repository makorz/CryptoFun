package app.makorz.cryptofun.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.makorz.cryptofun.data.database.DBHandler;
import app.makorz.cryptofun.data.database.Kline;
import app.makorz.cryptofun.ui.orders.OrderListViewElement;
import app.makorz.cryptofun.ui.results.ResultsListElement;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OrdersService extends Service implements CallbackButton {

    private static final String TAG = "ORDService";

    private DBHandler databaseDB;
    private Handler handler;
    private static final String TABLE_NAME_ORDERS = "current_orders";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String VALUE_REAL = "value_real";
    private static final String ID = "id";
    private static final String CURRENT_PRICE = "current_price";
    private static final String STOP_LIMIT = "stop_limit_price";

    ArrayList<OrderListViewElement> returnList = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        databaseDB = DBHandler.getInstance(getApplicationContext());

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "START");
                        // It's for foreground services, because in newest Android, background are not working. Foreground need to inform user that it is running
                        Notification notification = ServiceFunctionsOther.createNotificationSimple("Orders verification.", TAG, getApplicationContext());
                        // Notification ID cannot be 0.
                        startForeground(1, notification);
                        updatingCurrentOrders();

                    }
                }
        ).start();

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessageToActivity(ArrayList<OrderListViewElement> listOrders, String test, ArrayList<String> automatic) {

        //Sort by time placed of Orders
        Collections.sort(listOrders, Collections.reverseOrder(new Comparator<OrderListViewElement>() {
            public int compare(OrderListViewElement o1, OrderListViewElement o2) {
                return Long.compare(o1.getTimeWhenPlaced(), o2.getTimeWhenPlaced());
            }
        }));

        Intent intent = new Intent("OrdersStatus");
        Log.e(TAG, "SendMessage " + Thread.currentThread() + " " + Thread.activeCount() + " size: " + returnList.size());
        Bundle bundle = new Bundle();
        // 1 - 30min, 2 - 2h, 3 - 6h
        bundle.putSerializable("ordersList", (Serializable) listOrders);
        bundle.putString("testBalance", test);
//        bundle.putString("realBalance", real);
        bundle.putString("autoBalance1", automatic.get(0));
        bundle.putString("autoBalance2", automatic.get(1));
        bundle.putString("autoBalance3", automatic.get(2));
        bundle.putString("autoBalance4", automatic.get(3));
        bundle.putString("autoBalance5", automatic.get(4));
        intent.putExtra("bundleOrdersStatus", bundle);
        LocalBroadcastManager.getInstance(OrdersService.this).sendBroadcast(intent);

        //Wait 3 second before stopping service, error:  Process: com.example.cryptofun, PID: 6921 android.app.ForegroundServiceDidNotStartInTimeException: Context.startForegroundService() did not then cal Service.startForeground():
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopForeground(true);
                stopSelf();
            }
        }, 2000);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroy");
    }

    private void updatingCurrentOrders() {

        Cursor data = databaseDB.retrieveAllFromTable(TABLE_NAME_ORDERS);
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "No active orders " + returnList.size());
            returnList.clear();
        } else {
            do {

                OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getLong(13), data.getString(14), data.getFloat(15));
                returnList.add(tempToken);
                Log.e(TAG, tempToken.toString());
            } while (data.moveToNext());
        }
        data.close();


        for (int i = 0; i < returnList.size(); i++) {

            float price = 0;
            Cursor data2 = databaseDB.retrieveLastClosePrice(returnList.get(i).getSymbol());
            data2.moveToFirst();
            if (data2.getCount() == 0) {
                Log.e(TAG, "There is no crypto with that symbol");
            } else {

                price = data2.getFloat(6);
                float percentPrevious = returnList.get(i).getPercentOfPriceChange();
                float previousPrice = returnList.get(i).getCurrentPrice();

                if (price != previousPrice) {

                    databaseDB.updatePricesOfCryptoInOrder(returnList.get(i).getSymbol(), CURRENT_PRICE, price, returnList.get(i).getTimeWhenPlaced());
                    returnList.get(i).setCurrentPrice(price);
                    float percentNow = returnList.get(i).getPercentOfPriceChange();
                    String symbol = returnList.get(i).getSymbol();

                    long currentTime = System.currentTimeMillis();
                    long orderTime = returnList.get(i).getTimeWhenPlaced();
                    long minutes45 = 2700000;
                    long eightHours = 28800000;
                    long twelveHours = 43200000;
                    long tenHours = 36000000;
                    long threeHours = 10800000;
                    long minutes15 = 900000;
                    long halfHour = 1800000;


                    int stopLossStrategy, strategy;
                    int lengthOfEMA;
                    String whatEMA;
                    float offsetSAR;

                    data = databaseDB.retrieveParam(18);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 18");
                        stopLossStrategy = 1;
                    } else {
                        data.moveToFirst();
                        stopLossStrategy = data.getInt(3);
                    }
                    data.close();

                    data = databaseDB.retrieveParam(18);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 18");
                        strategy = 1;
                    } else {
                        data.moveToFirst();
                        strategy = data.getInt(3);
                    }
                    data.close();

                    data = databaseDB.retrieveParam(19);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 19");
                        lengthOfEMA = 31;
                    } else {
                        data.moveToFirst();
                        lengthOfEMA = data.getInt(3);
                    }
                    data.close();
                    data = databaseDB.retrieveParam(20);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 20");
                        whatEMA = "close";
                    } else {
                        data.moveToFirst();
                        whatEMA = data.getString(2);
                    }
                    data.close();
                    data = databaseDB.retrieveParam(21);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 21");
                        offsetSAR = 1;
                    } else {
                        data.moveToFirst();
                        offsetSAR = data.getFloat(4);
                    }
                    data.close();


                    int whatToDoWithSL = 0;
                    long timeSinceEntry = System.currentTimeMillis() - returnList.get(i).getTimeWhenPlaced();
                    float percentOfPriceChangeToReact, stopLossValue, multiplier, stopLimitPrice;
                    List<Kline> coinKlines15m = new ArrayList<>();

                    String infoOfOrder = "LEVEL 5 [" + symbol + "] CurrentPrice: " + price + " PreviousPrice: " + previousPrice + " NowPriceChange%: " + percentNow + " PreviousPriceChange%: " + percentPrevious + " SL Strategy: " + stopLossStrategy + " typeEMA: " + whatEMA + " lengthEMA: " + lengthOfEMA + " offsetSAR: " + offsetSAR + " " + returnList.get(i).toString();
                    Log.e(TAG, infoOfOrder);
                    ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "result");

                    switch (stopLossStrategy) {

                        case 2:
                            // EMA
                            data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol, "15m");
                            data.moveToFirst();
                            if (data.getCount() > 0) {

                                do {
                                    Kline tempKline = new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                                            data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10));
                                    coinKlines15m.add(tempKline);

                                } while (data.moveToNext());
                            }
                            data.close();

                            percentOfPriceChangeToReact = 1f;

                            if (returnList.get(i).getIsItShort() == 1) {

                                stopLimitPrice = (float) ServiceFunctionsStrategyTa4J.getEMAValue(coinKlines15m, lengthOfEMA, whatEMA);

                                if (price > returnList.get(i).getStopLimitPrice()) {
                                    whatToDoWithSL = 1;
                                } else if (price < returnList.get(i).getTakeProfitPrice()) {
                                    whatToDoWithSL = 2;
                                } else if (percentNow < -percentOfPriceChangeToReact && timeSinceEntry > halfHour && stopLimitPrice > returnList.get(i).getCurrentPrice() && stopLimitPrice < returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 1 && returnList.get(i).getOrderType().equals("MARKET")) {
                                    whatToDoWithSL = 3;
                                } else if (percentNow < -percentOfPriceChangeToReact && timeSinceEntry > halfHour && stopLimitPrice > returnList.get(i).getCurrentPrice() && stopLimitPrice < returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 0 && returnList.get(i).getOrderType().equals("MARKET")) {
                                    whatToDoWithSL = 4;
                                }

                            } else {


                                stopLimitPrice = (float) ServiceFunctionsStrategyTa4J.getEMAValue(coinKlines15m, lengthOfEMA, whatEMA);

                                if (price < returnList.get(i).getStopLimitPrice()) {
                                    whatToDoWithSL = 1;
                                } else if (price > returnList.get(i).getTakeProfitPrice()) {
                                    whatToDoWithSL = 2;
                                } else if (percentNow > percentOfPriceChangeToReact && timeSinceEntry > halfHour && stopLimitPrice < returnList.get(i).getCurrentPrice() && stopLimitPrice > returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 1 && returnList.get(i).getOrderType().equals("MARKET")) {
                                    whatToDoWithSL = 3;
                                } else if (percentNow > percentOfPriceChangeToReact && timeSinceEntry > halfHour && stopLimitPrice < returnList.get(i).getCurrentPrice() && stopLimitPrice > returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 0 && returnList.get(i).getOrderType().equals("MARKET")) {
                                    whatToDoWithSL = 4;
                                }
                            }
                            break;
                        case 3:
                            //Manual Limits
                            percentOfPriceChangeToReact = 1.5f;
                            stopLossValue = 2f;
                            if ((percentNow > 1.5 && percentNow < 2) || (percentNow < -1.5 && percentNow > -2)) {
                                stopLossValue = 1.75f;
                            } else if ((percentNow >= 2 && percentNow < 3) || (percentNow <= -2 && percentNow > -3)) {
                                stopLossValue = 1.25f;
                            } else if ((percentNow >= 3) || (percentNow <= -3)) {
                                stopLossValue = 1f;
                            }

                            if (returnList.get(i).getIsItShort() == 1) {

                                multiplier = 1 + stopLossValue / 100;
                                stopLimitPrice = returnList.get(i).getCurrentPrice() * multiplier;

                                if (price > returnList.get(i).getStopLimitPrice()) {
                                    whatToDoWithSL = 1;
                                } else if (price < returnList.get(i).getTakeProfitPrice()) {
                                    whatToDoWithSL = 2;
                                } else if (percentNow < -percentOfPriceChangeToReact && timeSinceEntry > minutes45 && percentPrevious > percentNow && returnList.get(i).getOrderType().equals("MARKET") && stopLimitPrice < returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 1) {
                                    whatToDoWithSL = 3;
                                } else if (percentNow < -percentOfPriceChangeToReact && timeSinceEntry > minutes45 && percentPrevious > percentNow && returnList.get(i).getOrderType().equals("MARKET") && stopLimitPrice < returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 0) {
                                    whatToDoWithSL = 4;
                                }

                            } else {

                                multiplier = 1 - stopLossValue / 100;
                                stopLimitPrice = returnList.get(i).getCurrentPrice() * multiplier;

                                if (price < returnList.get(i).getStopLimitPrice()) {
                                    whatToDoWithSL = 1;
                                } else if (price > returnList.get(i).getTakeProfitPrice()) {
                                    whatToDoWithSL = 2;
                                } else if (percentNow > percentOfPriceChangeToReact && timeSinceEntry > minutes45 && percentPrevious < percentNow && returnList.get(i).getOrderType().equals("MARKET") && stopLimitPrice > returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 1) {
                                    whatToDoWithSL = 3;
                                } else if (percentNow > percentOfPriceChangeToReact && timeSinceEntry > minutes45 && percentPrevious < percentNow && returnList.get(i).getOrderType().equals("MARKET") && stopLimitPrice > returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 0) {
                                    whatToDoWithSL = 4;
                                }
                            }

                            break;
                        default:
                            //SAR
                            if (strategy == 20) {
                                data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol,"4h");
                            } else {
                                data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol,"15m");
                            }
                            data.moveToFirst();
                            if (data.getCount() > 0) {

                                do {
                                    Kline tempKline = new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                                            data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10));
                                    coinKlines15m.add(tempKline);

                                } while (data.moveToNext());
                            }
                            data.close();

                            percentOfPriceChangeToReact = 1f;

                            if (returnList.get(i).getIsItShort() == 1) {

                                stopLimitPrice = (float) ServiceFunctionsStrategyTa4J.getSARValue(coinKlines15m) * ((100f + offsetSAR) / 100);

                                if (price > returnList.get(i).getStopLimitPrice()) {
                                    whatToDoWithSL = 1;
                                } else if (price < returnList.get(i).getTakeProfitPrice()) {
                                    whatToDoWithSL = 2;
                                } else if (percentNow < -percentOfPriceChangeToReact && timeSinceEntry > halfHour && stopLimitPrice > returnList.get(i).getCurrentPrice() && stopLimitPrice < returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 1 && returnList.get(i).getOrderType().equals("MARKET")) {
                                    whatToDoWithSL = 3;
                                } else if (percentNow < -percentOfPriceChangeToReact && timeSinceEntry > halfHour && stopLimitPrice > returnList.get(i).getCurrentPrice() && stopLimitPrice < returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 0 && returnList.get(i).getOrderType().equals("MARKET")) {
                                    whatToDoWithSL = 4;
                                }

                            } else {

                                stopLimitPrice = (float) ServiceFunctionsStrategyTa4J.getSARValue(coinKlines15m) * ((100f - offsetSAR) / 100);

                                if (price < returnList.get(i).getStopLimitPrice()) {
                                    whatToDoWithSL = 1;
                                } else if (price > returnList.get(i).getTakeProfitPrice()) {
                                    whatToDoWithSL = 2;
                                } else if (percentNow > percentOfPriceChangeToReact && timeSinceEntry > halfHour && stopLimitPrice < returnList.get(i).getCurrentPrice() && stopLimitPrice > returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 1 && returnList.get(i).getOrderType().equals("MARKET")) {
                                    whatToDoWithSL = 3;
                                } else if (percentNow > percentOfPriceChangeToReact && timeSinceEntry > halfHour && stopLimitPrice < returnList.get(i).getCurrentPrice() && stopLimitPrice > returnList.get(i).getStopLimitPrice() && returnList.get(i).getIsItReal() == 0 && returnList.get(i).getOrderType().equals("MARKET")) {
                                    whatToDoWithSL = 4;
                                }
                            }
                            break;
                    }

                    String info, type;
                    boolean isThereStopLimitForThatSymbol = false;
                    long stopLimitOrderId = 0;
                    long timeOfStopLimitOrderPlacement = 0;
                    float exitAmount, moneyEarned;
                    ResultsListElement historicOrder;

                    switch (whatToDoWithSL) {

                        case 1: // SHORT/LONG - SL made // We don't take stop limit price when closing test order - we take amount when we check prices, so it can be lower if big move was made, stop limit would prevent of that
                            type = ((returnList.get(i).getIsItShort() == 1) ? "SHORT" : "LONG");
                            returnList.get(i).setCurrentPrice(returnList.get(i).getStopLimitPrice());
                            info = "LEVEL 7 [" + symbol + "] STOP LIMIT " + type + ": CurrentPrice: " + price + " StopLimitPrice: " + returnList.get(i).getStopLimitPrice() + " " + returnList.get(i).getAccountNumber();
                            Log.e(TAG, info);
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "orders");
                            exitAmount = returnList.get(i).getCurrentAmount();
                            moneyEarned = exitAmount - returnList.get(i).getEntryAmount();
                            historicOrder = new ResultsListElement(symbol, returnList.get(i).getEntryPrice(), returnList.get(i).getStopLimitPrice(), returnList.get(i).getPercentOfPriceChange(), returnList.get(i).getAccountNumber(), returnList.get(i).getMargin(), moneyEarned, returnList.get(i).getEntryAmount(), exitAmount, returnList.get(i).getPercentOfAmountChange(), returnList.get(i).getTimeWhenPlaced(), System.currentTimeMillis(), returnList.get(i).getIsItShort(), returnList.get(i).getIsItReal());
                            databaseDB.addNewHistoricOrder(historicOrder);
                            closeOrder(i, returnList);
                            break;
                        case 2: // SHORT/LONG - TP made
                            type = ((returnList.get(i).getIsItShort() == 1) ? "SHORT" : "LONG");
                            returnList.get(i).setCurrentPrice(returnList.get(i).getTakeProfitPrice());
                            info = "LEVEL 7 [" + symbol + "] TAKE PROFIT " + type + ": CurrentPrice: " + price + " TakeProfitPrice: " + returnList.get(i).getTakeProfitPrice() + " " + returnList.get(i).getAccountNumber();
                            Log.e(TAG, info);
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "orders");
                            exitAmount = returnList.get(i).getCurrentAmount();
                            moneyEarned = exitAmount - returnList.get(i).getEntryAmount();
                            historicOrder = new ResultsListElement(symbol, returnList.get(i).getEntryPrice(), returnList.get(i).getTakeProfitPrice(), returnList.get(i).getPercentOfPriceChange(), returnList.get(i).getAccountNumber(), returnList.get(i).getMargin(), moneyEarned, returnList.get(i).getEntryAmount(), exitAmount, returnList.get(i).getPercentOfAmountChange(), returnList.get(i).getTimeWhenPlaced(), System.currentTimeMillis(), returnList.get(i).getIsItShort(), returnList.get(i).getIsItReal());
                            databaseDB.addNewHistoricOrder(historicOrder);
                            closeOrder(i, returnList);
                            break;
                        case 3: // SHORT/LONG - Set new SL for REAL order
                            type = ((returnList.get(i).getIsItShort() == 1) ? "SHORT" : "LONG");
                            info = "LEVEL 7 [" + symbol + "] NEW SL REAL " + type + ": CurrentPrice: " + price + " PreviousStopLimitPrice: " + returnList.get(i).getStopLimitPrice();
                            returnList.get(i).setStopLimitPrice(stopLimitPrice);
                            databaseDB.updatePricesOfCryptoInOrder(returnList.get(i).getSymbol(), STOP_LIMIT, stopLimitPrice, returnList.get(i).getTimeWhenPlaced());
                            for (int j = 0; j < returnList.size(); j++) {
                                if (returnList.get(j).getSymbol().equals(symbol) && returnList.get(j).getOrderType().equals("STOP_MARKET") && returnList.get(j).getIsItReal() == 1) {
                                    isThereStopLimitForThatSymbol = true;
                                    stopLimitOrderId = returnList.get(j).getOrderID();
                                    timeOfStopLimitOrderPlacement = returnList.get(j).getTimeWhenPlaced();
                                }
                            }

                            info += " NewStopLimitPrice: " + returnList.get(i).getStopLimitPrice() + " NowPriceChange%: " + percentNow + " PreviousPriceChange%: " + percentPrevious + " orderID: " + stopLimitOrderId;
                            Log.e(TAG, info);
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "orders");

                            String buyOrSell = ((returnList.get(i).getIsItShort() == 1) ? "BUY" : "SELL");
                            if (isThereStopLimitForThatSymbol) {
                                ServiceFunctionsAPI.updateStopLimitForOrder(returnList.get(i).getSymbol(), stopLimitOrderId, buyOrSell, "STOP_MARKET", "RESULT", stopLimitPrice, "true", timeOfStopLimitOrderPlacement, System.currentTimeMillis(), 0, getApplicationContext(), returnList.get(i), null);
                            } else {
                                ServiceFunctionsAPI.setStopLimitOrTakeProfitMarket(returnList.get(i).getSymbol(), buyOrSell, "STOP_MARKET", "RESULT", stopLimitPrice, "true", System.currentTimeMillis(), 0, returnList.get(i), getApplicationContext(), null);
                            }
                            break;
                        case 4: // SHORT/LONG - Set new SL for TEST order
                            type = ((returnList.get(i).getIsItShort() == 1) ? "SHORT" : "LONG");
                            info = "LEVEL 7 [" + symbol + "] NEW SL TEST " + type + ": CurrentPrice: " + price + " PreviousStopLimitPrice: " + returnList.get(i).getStopLimitPrice();
                            returnList.get(i).setStopLimitPrice(stopLimitPrice);
                            databaseDB.updatePricesOfCryptoInOrder(returnList.get(i).getSymbol(), STOP_LIMIT, stopLimitPrice, returnList.get(i).getTimeWhenPlaced());
                            info += " NewStopLimitPrice: " + returnList.get(i).getStopLimitPrice() + " NowPriceChange%: " + percentNow + " PreviousPriceChange%: " + percentPrevious;
                            Log.e(TAG, info);
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "orders");
                            break;
                        case 5:
                            info = "LEVEL 7 [" + symbol + "] TIME PASSED: CurrentPrice: " + price + " TakeProfitPrice: " + returnList.get(i).getTakeProfitPrice() + " StopLimitPrice: " + returnList.get(i).getStopLimitPrice();
                            Log.e(TAG, info);
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
                            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "orders");
                            closeOrder(i, returnList);
                            break;
                        default:
                            break;
                    }
                }
            }
            data2.close();
        }
        getTestAccountBalance();

    }

    /*

    TODO:
       -Trzeba sprawdzać jakie zlecenia mamy dla danego symbolu realnego, jeśli nie udało się zrobić stop limita przy tworzeniu zlecenia, to trzeba próbowac i aktualziować co chwilę
        Sprawdzać trzeba wszystkie pozycje dla symboli z naszej bazy. Bo jeśli zakończy się samoistnie, to pozostanie w naszej bazie do momentu kiedy zrealizujemy cele zamknięcia (np. teraz delkatnie musnął stop limit i wszystko się zamkneło na
        koncie binance natomiast u nas nadal wisiały ordery (bo aktualizowane są w większym interwale niz realne działanie binance.
        -Zastanów się nad dodanie ATR (Avarage true Range - sprawdza to wielkość świec i można to wziąć pod uwagę przy tworzeniu stop lossów jako mnożnik (wpółczynnik) bezpieczeństwa
     */

    private void closeOrder(int position, ArrayList<OrderListViewElement> returnList) {

        Log.e(TAG, "Order was closed for: " + returnList.get(position).toString());

        if (returnList.get(position).getIsItReal() == 1) {
            if (returnList.get(position).getOrderType().equals("TAKE_PROFIT_MARKET") || returnList.get(position).getOrderType().equals("STOP_MARKET")) {
                boolean areStopOrTakeRelevant = false;
                for (int i = 0; i < returnList.size(); i++) {
                    if (returnList.get(i).getSymbol().equals(returnList.get(position).getSymbol()) && returnList.get(i).getOrderType().equals("MARKET") && returnList.get(i).getIsItReal() == 1) {
                        areStopOrTakeRelevant = true;
                    }
                }
                if (!areStopOrTakeRelevant) {
                    ServiceFunctionsAPI.deleteOrder(returnList.get(position), System.currentTimeMillis(), getApplicationContext(), null);
                }
            } else if (returnList.get(position).getOrderType().equals("MARKET")) {
                ServiceFunctionsAPI.getPositions(returnList.get(position).getSymbol(), System.currentTimeMillis(), getApplicationContext(), returnList.get(position), null, false);
            }

        } else {
            float balance = 0;
            databaseDB.deleteOrder(returnList.get(position).getSymbol(), returnList.get(position).getTimeWhenPlaced(), returnList.get(position).getIsItReal(),
                    returnList.get(position).getIsItShort(), returnList.get(position).getMargin());
            Cursor data = databaseDB.retrieveParam(returnList.get(position).getAccountNumber());
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr " + returnList.get(position).getAccountNumber());
            } else {
                data.moveToFirst();
                balance = data.getFloat(4);
            }
            data.close();
            String nrOfParameterForAccount = String.valueOf(returnList.get(position).getAccountNumber());
            String info = "LEVEL 8 " + returnList.get(position).toString() + " balance: " + balance + " newMoney: " + returnList.get(position).getCurrentAmount();
            Log.e(TAG, info);
            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "result");
            ServiceFunctionsOther.writeToFile(info, getApplicationContext(), "orders");
            databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, balance + returnList.get(position).getCurrentAmount(), ID, nrOfParameterForAccount);
        }
    }

    private void getTestAccountBalance() {

        int isAutomaticForRealEnabled = 0;
        Cursor data2 = databaseDB.retrieveParam(15);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 15");
        } else {
            data2.moveToFirst();
            isAutomaticForRealEnabled = data2.getInt(3);
        }
        data2.close();

        String testBalance = "";
        ArrayList<String> automaticBalance = new ArrayList<>();

        data2 = databaseDB.retrieveParam(2);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 2");
            databaseDB.addParam(2, "Test account balance", "", 0, 100);
            testBalance = "100.00";
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 2);
            databaseDB.addParam(2, "Last Update Time", "", 0, 100);
            testBalance = "100.00";
        } else {
            data2.moveToFirst();
            float balance = data2.getFloat(4);
            DecimalFormat dfNr = new DecimalFormat("0.00");
            testBalance = dfNr.format(balance);
        }
        data2.close();


        for (int i = 6; i < 11; i++) {
            Cursor data = databaseDB.retrieveParam(i);
            data.moveToFirst();
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param for test account " + (i - 5));
                databaseDB.addParam(i, "Test account nr " + (i - 5) + " balance", "", 0, 100);
                automaticBalance.add("100.00");
            } else if (data.getCount() >= 2) {
                databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, i);
                databaseDB.addParam(i, "Test account nr " + (i - 5) + " balance", "", 0, 100);
                automaticBalance.add("100.00");
            } else {
                float balance = data.getFloat(4);
                DecimalFormat dfNr = new DecimalFormat("0.00");
                automaticBalance.add(dfNr.format(balance));
            }
            data.close();
        }

        if (isAutomaticForRealEnabled == 1) {
            ServiceFunctionsAPI.getRealAccountBalance(getApplicationContext(), null);
        }

        sendMessageToActivity(returnList, testBalance, automaticBalance);

    }

    @Override
    public void onSuccess() {

    }

    @Override
    public void onError() {

    }
}
