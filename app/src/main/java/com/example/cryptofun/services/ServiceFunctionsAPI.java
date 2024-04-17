package com.example.cryptofun.services;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cryptofun.data.AccountBalance;
import com.example.cryptofun.data.AccountInfo;
import com.example.cryptofun.data.Leverage;
import com.example.cryptofun.data.MarketOrder;
import com.example.cryptofun.data.PositionRisk;
import com.example.cryptofun.data.RealOrder;
import com.example.cryptofun.data.ResponseMargin;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.retrofit.RetrofitClientSecretTestnet;
import com.example.cryptofun.ui.orders.OrderListViewElement;
import com.example.cryptofun.ui.results.ResultsListElement;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServiceFunctionsAPI {

    public static void makeOrderFunction(boolean isItReal, String symbol, int entryAmount, float stopLimit, float takeProfit, int leverage, float currentPrice, boolean isItCrossed, boolean isItShort, long time, float balance, int accountNr, Context context, final CallbackButton callbackButton) {

        DBHandler databaseDB = DBHandler.getInstance(context);

        if (isItReal) {

            Log.e("F: makeOrderFunction", "REAL");

            int recvWindow = 10000;
            Cursor data = databaseDB.retrieveParam(14);
            if (data.getCount() == 0) {
                Log.e("F: makeOrderFunction", "There is no param nr 14");
            } else {
                data.moveToFirst();
                recvWindow = data.getInt(3);
            }
            data.close();

            String marginType = "ISOLATED";
            if (isItCrossed) {
                marginType = "CROSSED";
            }

            float quantity = (float) ((entryAmount / currentPrice) * 0.99 * leverage);

            String side = "BUY";
            float stopLimitPrice = currentPrice * (1 - (float) stopLimit / 100);
            float takeProfitPrice = currentPrice * (1 + (float) takeProfit / 100);
            if (isItShort) {
                stopLimitPrice = currentPrice * (1 + (float) stopLimit / 100);
                takeProfitPrice = currentPrice * (1 - (float) takeProfit / 100);
                side = "SELL";
            }

            MarketOrder marketOrder = new MarketOrder(symbol, side, "MARKET", "RESULT", marginType, leverage, quantity, System.currentTimeMillis(), recvWindow, stopLimitPrice, takeProfitPrice, entryAmount, currentPrice, isItShort, isItCrossed, context, callbackButton);

            Log.e("F: makeOrderFunction", marketOrder.toString());

            new Thread(
                    () -> {
                        setMarginType(marketOrder);
                        Log.e("F: makeOrderFunction", "START ORDER NEW THREAD");

                        if (callbackButton != null) {
                            callbackButton.onSuccess();
                        }

                    }
            ).start();

        } else {
            Log.e("F: makeOrderFunction", "TEST");
            float stopLimitPrice = currentPrice * (1 - (float) stopLimit / 100);
            float takeProfitPrice = currentPrice * (1 + (float) takeProfit / 100);
            int isItShortValue = 0;
            int isItRealValue = 0;
            int isItCrossedValue = 0;
            if (isItShort) {
                stopLimitPrice = currentPrice * (1 + (float) stopLimit / 100);
                takeProfitPrice = currentPrice * (1 - (float) takeProfit / 100);
                isItShortValue = 1;
            }
            if (isItCrossed) {
                isItCrossedValue = 1;
            }

            float quantity = (float) ((entryAmount / currentPrice) * 0.99 * leverage);

//            Log.e("F: makeOrderFunction", isItCrossedValue + " " + isItShortValue);
            OrderListViewElement toDB = new OrderListViewElement(symbol, isItRealValue, (float) entryAmount, currentPrice, currentPrice, stopLimitPrice, takeProfitPrice, time, leverage, isItShortValue, isItCrossedValue, accountNr, 0, "MARKET", quantity);
            databaseDB.addNewOrder(toDB);
            databaseDB.updateWithWhereClauseREAL("config", "value_real", balance - entryAmount, "id", Integer.toString(accountNr));
            if (callbackButton != null) {
                callbackButton.onSuccess();
            }
        }

    }

    private static void setMarketOrder(MarketOrder marketOrder) {

        Log.e("F: setMarketOrder", marketOrder.getSymbol() + " quantity before: " + marketOrder.getQuantity());
        String quantityPrepared = formatFloatForSymbol(marketOrder.getSymbol(), marketOrder.getQuantity(), 1, marketOrder.getContext());
        Log.e("F: setMarketOrder", marketOrder.getSymbol() + " quantity after: " + quantityPrepared);

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(marketOrder.getContext(), 3, marketOrder.getSymbol(), 0, "", marketOrder.getSide(), marketOrder.getType(), marketOrder.getNewOrderRespType(), quantityPrepared, "0", "", 0, "", "0", marketOrder.getRecvWindow(), marketOrder.getTimestamp())
                .getMyApi().setMarketOrder(marketOrder.getSymbol(), marketOrder.getSide(), marketOrder.getType(), marketOrder.getNewOrderRespType(), quantityPrepared, marketOrder.getRecvWindow(), marketOrder.getTimestamp());

        Log.e("F: setMarketOrder", call.request().toString());

        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        RealOrder realOrder = response.body();
                        Log.e("F: setMarketOrder", realOrder.toString());
                        int isShort = 0;
                        int isCrossed = 0;

                        if (marketOrder.isItShort()) {
                            isShort = 1;
                        }

                        if (marketOrder.isItCrossed()) {
                            isCrossed = 1;
                        }

                        DBHandler databaseDB = DBHandler.getInstance(marketOrder.getContext());
                        OrderListViewElement toDB = new OrderListViewElement(marketOrder.getSymbol(), 1, (float) marketOrder.getEntryAmount(), realOrder.getAvgPrice(), marketOrder.getCurrentPrice(), marketOrder.getStopLimitPrice(), marketOrder.getTakeProfitPrice(), marketOrder.getTimestamp(), marketOrder.getLeverage(), isShort, isCrossed, 1, realOrder.getOrderId(), "MARKET", marketOrder.getQuantity());
                        databaseDB.addNewOrder(toDB);

                        if (marketOrder.getSide().equals("BUY")) {
                            setStopLimitOrTakeProfitMarket(marketOrder.getSymbol(), "SELL", "STOP_MARKET", marketOrder.getNewOrderRespType(), marketOrder.getStopLimitPrice(), "true", System.currentTimeMillis(), marketOrder.getRecvWindow(), toDB, marketOrder.getContext(), marketOrder.getCallbackButton());
                            setStopLimitOrTakeProfitMarket(marketOrder.getSymbol(), "SELL", "TAKE_PROFIT_MARKET", marketOrder.getNewOrderRespType(), marketOrder.getTakeProfitPrice(), "true", System.currentTimeMillis(), marketOrder.getRecvWindow(), toDB, marketOrder.getContext(), marketOrder.getCallbackButton());
                        } else {
                            setStopLimitOrTakeProfitMarket(marketOrder.getSymbol(), "BUY", "STOP_MARKET", marketOrder.getNewOrderRespType(), marketOrder.getStopLimitPrice(), "true", System.currentTimeMillis(), marketOrder.getRecvWindow(), toDB, marketOrder.getContext(), marketOrder.getCallbackButton());
                            setStopLimitOrTakeProfitMarket(marketOrder.getSymbol(), "BUY", "TAKE_PROFIT_MARKET", marketOrder.getNewOrderRespType(), marketOrder.getTakeProfitPrice(), "true", System.currentTimeMillis(), marketOrder.getRecvWindow(), toDB, marketOrder.getContext(), marketOrder.getCallbackButton());
                        }

                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = marketOrder.getSymbol() + " " + convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: setMarketOrder", errorBody);
                    //Toast.makeText(marketOrder.getContext().getApplicationContext(), errorBody, //Toast.LENGTH_SHORT).show();
                    ServiceFunctionsOther.writeToFile(errorBody, marketOrder.getContext(), "orders");
                }

            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: setMarketOrder", String.valueOf(t));
            }

        });

    }

    public static void setStopLimitOrTakeProfitMarket(String symbol, String side, String type, String newOrderRespType, float stopOrTakePrice, String closePosition, long timestamp, long recvWindow, OrderListViewElement orderElement, Context context, CallbackButton callbackButton) {

        Log.e("F: setStopLimitOrTakeProfitMarket", symbol + " stopOrTakePrice before: " + stopOrTakePrice);
        String stopPriceFinal = formatFloatForSymbol(symbol, stopOrTakePrice, 0, context);
        Log.e("F: setStopLimitOrTakeProfitMarket", symbol + " stopOrTakePrice after: " + stopPriceFinal);

        recvWindow = 10000;

        DBHandler databaseDB = DBHandler.getInstance(context);

        Cursor data = databaseDB.retrieveParam(14);
        if (data.getCount() == 0) {
            Log.e("F: makeOrderFunction", "There is no param nr 14");
        } else {
            data.moveToFirst();
            recvWindow = data.getInt(3);
        }
        data.close();

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 4, symbol, 0, "", side, type, newOrderRespType, "0",
                        stopPriceFinal, closePosition, 0, "", "0", recvWindow, timestamp)
                .getMyApi().setStopLimitOrTakeProfitMarket(symbol, side, type, newOrderRespType, stopPriceFinal, closePosition, recvWindow, timestamp);

        Log.e("F: setStopLimitOrTakeProfitMarket", call.request().toString());

        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {

                        RealOrder realOrder = response.body();
                        Log.e("F: setStopLimitOrTakeProfitMarket", realOrder.toString());


                        orderElement.setOrderType(type);
                        orderElement.setOrderID(realOrder.getOrderId());
                        orderElement.setTimeWhenPlaced(timestamp);
                        DBHandler databaseDB = DBHandler.getInstance(context);
                        databaseDB.addNewOrder(orderElement);


                        if (orderElement.getOrderType().equals("STOP_MARKET")) {
                            databaseDB.updatePricesOfCryptoInOrder(symbol, "stop_limit_price", stopOrTakePrice, orderElement.getTimeWhenPlaced());
                        } else {
                            databaseDB.updatePricesOfCryptoInOrder(symbol, "take_profit_price", stopOrTakePrice, orderElement.getTimeWhenPlaced());
                        }

                        if (callbackButton != null) {
                            callbackButton.onSuccess();
                        }


                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = symbol + " " + convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: setStopLimitOrTakeProfitMarket", errorBody);
                    //Toast.makeText(context.getApplicationContext(), errorBody, //Toast.LENGTH_SHORT).show();
                    ServiceFunctionsOther.writeToFile(errorBody, context, "orders");
                }

            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: setStopLimitOrTakeProfitMarket", String.valueOf(t));
            }

        });

    }

    private static void setLeverage(MarketOrder marketOrder) {

        Call<Leverage> call = RetrofitClientSecretTestnet.getInstance(marketOrder.getContext(), 1, marketOrder.getSymbol(), marketOrder.getLeverage(), "", "", "", "", "0", "0", "",
                0, "", "0", marketOrder.getRecvWindow(), marketOrder.getTimestamp()).getMyApi().setLeverage(marketOrder.getSymbol(), marketOrder.getLeverage(), marketOrder.getRecvWindow(), marketOrder.getTimestamp());

        Log.e("F: setLeverage", call.request().toString());

        call.enqueue(new Callback<Leverage>() {
            @Override
            public void onResponse(@NonNull Call<Leverage> call, @NonNull Response<Leverage> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        Leverage leverage1 = response.body();
                        marketOrder.setTimestamp(System.currentTimeMillis());
                        setMarketOrder(marketOrder);

                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = marketOrder.getSymbol() + " " + convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: setLeverage", errorBody);
                    //Toast.makeText(marketOrder.getContext().getApplicationContext(), errorBody, //Toast.LENGTH_SHORT).show();
                    ServiceFunctionsOther.writeToFile(errorBody, marketOrder.getContext(), "orders");
                }


            }

            @Override
            public void onFailure(@NonNull Call<Leverage> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: setLeverage", String.valueOf(t));

            }

        });

    }

    private static void setMarginType(MarketOrder marketOrder) {

        Call<ResponseMargin> call = RetrofitClientSecretTestnet.getInstance(marketOrder.getContext(), 2, marketOrder.getSymbol(), 0, marketOrder.getMarginType(), "", "", "", "0", "0", "", 0, "", "0", marketOrder.getRecvWindow(), marketOrder.getTimestamp()).getMyApi().setMarginType(marketOrder.getSymbol(), marketOrder.getMarginType(), marketOrder.getRecvWindow(), marketOrder.getTimestamp());

        Log.e("F: setMarginType", call.request().toString());

        call.enqueue(new Callback<ResponseMargin>() {
            @Override
            public void onResponse(@NonNull Call<ResponseMargin> call, @NonNull Response<ResponseMargin> response) {

                marketOrder.setTimestamp(System.currentTimeMillis());
                setLeverage(marketOrder);

                if (response.body() != null) {

                    if (response.isSuccessful()) {
                        ResponseMargin leverage1 = response.body();

                    } else {
                        System.out.println(response.code() + " " + response.message());

                    }

                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = marketOrder.getSymbol() + " " + convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    Log.e("F: setMarginType", errorBody);
                    //Toast.makeText(marketOrder.getContext().getApplicationContext(), errorBody, //Toast.LENGTH_SHORT).show();
                    ServiceFunctionsOther.writeToFile(errorBody, marketOrder.getContext(), "orders");
                }


            }

            @Override
            public void onFailure(@NonNull Call<ResponseMargin> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: setMarginType", String.valueOf(t));

            }

        });

    }

    public static int getDecimalPlaces(String value) {
        int index = value.indexOf(".");
        return index < 0 ? 0 : value.length() - index - 1;
    }

    public static void getRealAccountBalance(Context context, CallbackButton callbackButton) {

        Call<List<AccountBalance>> call = RetrofitClientSecretTestnet.getInstance(context, 0, "", 0, "", "", "", "", "0", "0", "",
                        0, "", "0", 0, 0)
                .getMyApi().getAccountBalance();

        Log.e("F: getRealAccountBalance", call.request().toString());
        call.enqueue(new Callback<List<AccountBalance>>() {
            @Override
            public void onResponse(@NonNull Call<List<AccountBalance>> call, @NonNull Response<List<AccountBalance>> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        List<AccountBalance> balanceList = response.body();
                        for (int i = 0; i < balanceList.size(); i++) {
                            if (balanceList.get(i).getAsset().contains("USDT")) {

                                DBHandler databaseDB = DBHandler.getInstance(context);
                                Cursor data2 = databaseDB.retrieveParam(3);
                                if (data2.getCount() == 0) {
                                    Log.e("F: getRealAccountBalance", "There is no param nr 3");
                                    databaseDB.addParam(3, "Real account balance", "", 0, balanceList.get(i).getAvailableBalance());
                                } else if (data2.getCount() >= 2) {
                                    databaseDB.deleteWithWhereClause("config", "id", 3);
                                    databaseDB.addParam(3, "Real Update Time", "", 0, balanceList.get(i).getAvailableBalance());
                                } else {
                                    databaseDB.updateWithWhereClauseREAL("config", "value_real", balanceList.get(i).getAvailableBalance(), "id", "3");
                                }
                                data2.close();

                                if (callbackButton != null) {
                                    callbackButton.onSuccess();
                                }
                            }
                        }
                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: getRealAccountBalance", errorBody);

                    if (callbackButton != null) {
                        callbackButton.onSuccess();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AccountBalance>> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: getRealAccountBalance", String.valueOf(t));
            }

        });

    }

    public static void setMarketOrderToCancelCurrentOrder(String symbol, String side, String type, String newOrderRespType, float quantity, long timestamp, float stopLimitPrice, float takeProfitPrice, float entryAmount, float currentPrice, int margin, boolean isItShort, boolean isItCrossed, Context context, CallbackButton callbackButton, OrderListViewElement orderToCheckAndDelete) {

        Log.e("F: setMarketOrderToCancelCurrentOrder", symbol + " quantity before: " + quantity);
        String quantityPrepared = formatFloatForSymbol(symbol, quantity, 1, context);
        Log.e("F: setMarketOrderToCancelCurrentOrder", symbol + " quantity after: " + quantityPrepared);

        long recvWindow = 10000;

        DBHandler databaseDB = DBHandler.getInstance(context);

        Cursor data = databaseDB.retrieveParam(14);
        if (data.getCount() == 0) {
            Log.e("F: makeOrderFunction", "There is no param nr 14");
        } else {
            data.moveToFirst();
            recvWindow = data.getInt(3);
        }
        data.close();

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 3, symbol, 0, "", side, type, newOrderRespType, quantityPrepared, "0", "", 0, "", "0", recvWindow, timestamp)
                .getMyApi().setMarketOrder(symbol, side, type, newOrderRespType, quantityPrepared, recvWindow, timestamp);

        Log.e("F: setMarketOrderToCancelCurrentOrder", call.request().toString());

        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        RealOrder realOrder = response.body();
                        Log.e("F: setMarketOrderToCancelCurrentOrder", realOrder.toString());

                        if (callbackButton != null) {
                            callbackButton.onSuccess();
                        } else {

                            String info = "MARKET ORDER TEST TO DELETE D: ExitA: " + orderToCheckAndDelete.getCurrentAmount() + ": EntryA: " + orderToCheckAndDelete.getEntryAmount() + " EntryP: " + orderToCheckAndDelete.getEntryPrice() + " ExitP: "  +  orderToCheckAndDelete.getStopLimitPrice();
                            ServiceFunctionsOther.writeToFile(info, context, "orders");

                            float exitAmount = orderToCheckAndDelete.getCurrentAmount();
                            float moneyEarned = exitAmount - orderToCheckAndDelete.getEntryAmount();
                            ResultsListElement historicOrder = new ResultsListElement(orderToCheckAndDelete.getSymbol(), orderToCheckAndDelete.getEntryPrice(), orderToCheckAndDelete.getStopLimitPrice(), orderToCheckAndDelete.getPercentOfPriceChange(), orderToCheckAndDelete.getAccountNumber(), orderToCheckAndDelete.getMargin(), moneyEarned, orderToCheckAndDelete.getEntryAmount(), exitAmount, orderToCheckAndDelete.getPercentOfAmountChange(), orderToCheckAndDelete.getTimeWhenPlaced(), System.currentTimeMillis(),orderToCheckAndDelete.getIsItShort(), orderToCheckAndDelete.getIsItReal());
                            databaseDB.addNewHistoricOrder(historicOrder);
                            databaseDB.deleteOrder(symbol, orderToCheckAndDelete.getTimeWhenPlaced(), 1,
                                    orderToCheckAndDelete.getIsItShort(), orderToCheckAndDelete.getMargin());
                        }

                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {

                    String errorBody;
                    try {
                        errorBody = symbol + " " + convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: setMarketOrderToCancelCurrentOrder", errorBody);
                    //Toast.makeText(context.getApplicationContext(), errorBody, //Toast.LENGTH_SHORT).show();
                    ServiceFunctionsOther.writeToFile(errorBody, context, "orders");


                    if (callbackButton != null) {
                        callbackButton.onError();
                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                Log.e("F: setMarketOrderToCancelCurrentOrder", String.valueOf(t));
                if (callbackButton != null) {
                    callbackButton.onError();
                }
            }

        });

    }

    public static void updateStopLimitForOrder(String symbol, long orderId, String side, String type, String newOrderRespType, float stopPrice, String closePosition, long previousOrderTimeStamp, long timestamp, long recvWindow, Context context, OrderListViewElement orderElement, CallbackButton callbackButton) {
        Log.e("F: updateStopLimitForOrder", orderId + " " + orderElement.getOrderID());

        recvWindow = 10000;

        DBHandler databaseDB = DBHandler.getInstance(context);

        Cursor data = databaseDB.retrieveParam(14);
        if (data.getCount() == 0) {
            Log.e("F: makeOrderFunction", "There is no param nr 14");
        } else {
            data.moveToFirst();
            recvWindow = data.getInt(3);
        }
        data.close();


        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 5, symbol, 0, "", "", "", "", "0", "0", "",
                        orderId, "", "0", recvWindow, timestamp)
                .getMyApi().deleteOrder(symbol, orderId, recvWindow, timestamp);

        Log.e("F: updateStopLimitForOrder", call.request().toString());

        long finalRecvWindow = recvWindow;

        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        //Deleting old stop limit was successful, now create a new one
                        RealOrder realOrder = response.body();
                        Log.e("F: updateStopLimitForOrder", "ORDER HAS BEEN CANCELED: " + realOrder);

                        DBHandler databaseDB = DBHandler.getInstance(context);
                        databaseDB.deleteOrder(symbol, previousOrderTimeStamp, 1, orderElement.getIsItShort(), orderElement.getMargin());

                        Log.e("F: updateStopLimitForOrder", orderElement.toString());

                        setStopLimitOrTakeProfitMarket(symbol, side, type, newOrderRespType, stopPrice, closePosition, System.currentTimeMillis(), finalRecvWindow, orderElement, context, callbackButton);

                        if (callbackButton != null) {
                            callbackButton.onSuccess();
                        }

                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody;
                    try {
                        errorBody = symbol + " " + convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: updateStopLimitForOrder", errorBody);
                    //Toast.makeText(context.getApplicationContext(), errorBody, //Toast.LENGTH_SHORT).show();
                    ServiceFunctionsOther.writeToFile(errorBody, context, "orders");
                    if (callbackButton != null && !errorBody.contains("-2011")) {
                        callbackButton.onError();
                    } else if (callbackButton != null) {
                        callbackButton.onSuccess();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: updateStopLimitForOrder", String.valueOf(t));
                if (callbackButton != null) {
                    callbackButton.onError();
                }
            }

        });

    }

    public static void deleteAllOrders(String symbol, long timestamp, long recvWindow, Context context, CallbackButton callbackButton) {

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 7, symbol, 0, "", "", "", "", "0", "0", "",
                        0, "", "0", recvWindow, timestamp)
                .getMyApi().deleteAllOrders(symbol, recvWindow, timestamp);

        Log.e("F: deleteAllOrders", call.request().toString());

        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        RealOrder realOrder = response.body();
                        Log.e("F: deleteAllOrders", "ORDER HAS BEEN CANCELED: " + realOrder);
                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = symbol + " " + convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: deleteAllOrders", errorBody);
                }
            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: deleteAllOrders", String.valueOf(t));
            }

        });

    }

    public static void deleteOrder(OrderListViewElement orderElement, long timestamp, Context context, CallbackButton callbackButton) {

        long recvWindow = 10000;

        DBHandler databaseDB = DBHandler.getInstance(context);

        Cursor data = databaseDB.retrieveParam(14);
        if (data.getCount() == 0) {
            Log.e("F: makeOrderFunction", "There is no param nr 14");
        } else {
            data.moveToFirst();
            recvWindow = data.getInt(3);
        }
        data.close();

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 5, orderElement.getSymbol(), 0, "", "", "", "", "0", "0", "",
                        orderElement.getOrderID(), "", "0", recvWindow, timestamp)
                .getMyApi().deleteOrder(orderElement.getSymbol(), orderElement.getOrderID(), recvWindow, timestamp);

        Log.e("F: deleteOrder", call.request().toString());

        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        RealOrder realOrder = response.body();
                        Log.e("F: deleteOrder", "ORDER HAS BEEN CANCELED: " + realOrder);

                        if (callbackButton != null) {
                            callbackButton.onSuccess();
                        } else {

                            if (orderElement.getOrderType().equals("MARKET")) {
                                String info = "MARKET ORDER TEST TO DELETE C: ExitA: " + orderElement.getCurrentAmount() + ": EntryA: " + orderElement.getEntryAmount() + " EntryP: " + orderElement.getEntryPrice() + " ExitP: " + orderElement.getStopLimitPrice() + " ExitPsss: " + orderElement.getCurrentAmount();
                                ServiceFunctionsOther.writeToFile(info, context, "orders");
                                float exitAmount = orderElement.getCurrentAmount();
                                float moneyEarned = exitAmount - orderElement.getEntryAmount();
                                ResultsListElement historicOrder = new ResultsListElement(orderElement.getSymbol(), orderElement.getEntryPrice(), orderElement.getStopLimitPrice(), orderElement.getPercentOfPriceChange(), orderElement.getAccountNumber(), orderElement.getMargin(), moneyEarned, orderElement.getEntryAmount(), exitAmount, orderElement.getPercentOfAmountChange(), orderElement.getTimeWhenPlaced(), System.currentTimeMillis(), orderElement.getIsItShort(), orderElement.getIsItReal());
                                databaseDB.addNewHistoricOrder(historicOrder);
                            }
                            databaseDB.deleteOrder(orderElement.getSymbol(), orderElement.getTimeWhenPlaced(), 1,
                                    orderElement.getIsItShort(), orderElement.getMargin());
                        }

                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody;
                    try {
                        errorBody = orderElement.getSymbol() + " " + convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: deleteOrder", errorBody);
                    //Toast.makeText(context.getApplicationContext(), errorBody, //Toast.LENGTH_SHORT).show();
                    ServiceFunctionsOther.writeToFile(errorBody, context, "orders");
                    if (callbackButton != null && !errorBody.contains("-2011")) {
                        callbackButton.onError();
                    } else if (callbackButton != null) {
                        callbackButton.onSuccess();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: deleteOrder", String.valueOf(t));
                if (callbackButton != null) {
                    callbackButton.onError();
                }
            }

        });

    }

    public static void getAllOrders(long timestamp, Context context, ArrayList<OrderListViewElement> returnList) {

        long recvWindow = 10000;

        DBHandler databaseDB = DBHandler.getInstance(context);

        Cursor data = databaseDB.retrieveParam(14);
        if (data.getCount() == 0) {
            Log.e("F: makeOrderFunction", "There is no param nr 14");
        } else {
            data.moveToFirst();
            recvWindow = data.getInt(3);
        }
        data.close();

        Call<List<JsonObject>> call = RetrofitClientSecretTestnet.getInstance(context, 8, "", 0, "", "", "", "", "0", "0", "",
                        0, "", "0", recvWindow, timestamp)
                .getMyApi().getAllOrders(recvWindow, timestamp);

        Log.e("F: getAllOrders", call.request().toString());

        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {

                        List<JsonObject> responseBody = response.body();
                        Gson gson = new Gson();

                        // Verification of lost orders remote and local
                        if (returnList != null) {

                            List<OrderListViewElement> localMarketOrders = new ArrayList<>();
                            List<OrderListViewElement> localTPandSLOrders = new ArrayList<>();
                            List<OrderListViewElement> testMarketOrders = new ArrayList<>();
                            List<Long> localOrderIDofStopAndTake = new ArrayList<>();
                            List<Long> remoteOrderIDofStopAndTake = new ArrayList<>();
                            ArrayList<RealOrder> remoteOrdersFull = new ArrayList<>();

                            // Create lists to analyze - from local DB
                            for (OrderListViewElement order : returnList) {
                                if (order.getIsItReal() == 1) {
                                    Log.e("F: getAllOrders", "LOCAL: " + order);
                                    if (order.getOrderType().equals("TAKE_PROFIT_MARKET") || order.getOrderType().equals("STOP_MARKET")) {
                                        localOrderIDofStopAndTake.add(order.getOrderID());
                                        localTPandSLOrders.add(order);
                                    } else {
                                        localMarketOrders.add(order);
                                    }
                                } else if (order.getAccountNumber() >= 6) {
                                    testMarketOrders.add(order);
                                }
                            }

                            //Remove TP i SL if there is no longer MARKET order
                            for (OrderListViewElement orderTPandSL : localTPandSLOrders) {
                                boolean thereIsAMarketOrderForThatSymbol = false;
                                for (OrderListViewElement orderMarket : localMarketOrders) {
                                    if (orderTPandSL.getSymbol().equals(orderMarket.getSymbol()) && orderMarket.getOrderType().equals("MARKET")){
                                        thereIsAMarketOrderForThatSymbol = true;
                                    }
                                }
                                if (!thereIsAMarketOrderForThatSymbol) {
                                    Log.e("F: getAllOrders", "DELETE TP/SL if there is no MARKET: " + orderTPandSL);
                                    deleteOrder(orderTPandSL, System.currentTimeMillis(), context.getApplicationContext(), null);
                                }

                            }

                            // Create lists to analyze - from remote API account
                            for (JsonObject jsonObject : responseBody) {
                                RealOrder order = gson.fromJson(jsonObject, RealOrder.class);
                                remoteOrderIDofStopAndTake.add(order.getOrderId());
                                remoteOrdersFull.add(order);
                                Log.e("F: getAllOrders", "REMOTE: " + order);
                            }

                            // Check if there are duplicate OrdersID locally
                            Set<Long> uniqueValues = new HashSet<>();

                            for (Long value : localOrderIDofStopAndTake) {
                                if (uniqueValues.contains(value)) {
                                    databaseDB.deleteDuplicateOrderByID(value);
                                    break;
                                } else {
                                    uniqueValues.add(value);
                                }
                            }

                            // Check positions/market orders
                            for (OrderListViewElement element : localMarketOrders) {
                                getPositions(element.getSymbol(), System.currentTimeMillis(), context, element, null, true);
                            }

                            // Delete duplicates test
                            Set<OrderListViewElement> uniqueSet = new HashSet<>();
                            List<OrderListViewElement> duplicates = testMarketOrders.stream()
                                    .filter(e -> !uniqueSet.add(e)) // Filter out non-duplicates and add duplicates to the set
                                    .collect(Collectors.toList());

                            // Perform a specific action with the duplicate objects
                            for (OrderListViewElement duplicate : duplicates) {
                                Log.e("F: getAllOrders", "DUPLICATE TESTSc: " + duplicate);
                                databaseDB.deleteOrder(duplicate.getSymbol(), duplicate.getTimeWhenPlaced(), duplicate.getIsItReal(), duplicate.getIsItShort(), duplicate.getMargin());
                            }

                            // Now, uniqueSet contains the list without duplicates
                            List<OrderListViewElement> deduplicatedList = new ArrayList<>(uniqueSet);

                            // Check if there are missing values from local in remote
                            for (Long item : localOrderIDofStopAndTake) {
                                if (!remoteOrderIDofStopAndTake.contains(item)) {
                                    Log.e("F: getAllOrders", "OrderID" + item + " is missing from remote List");

                                    for (OrderListViewElement wrongOrder : returnList) {

                                        if (wrongOrder.getOrderID() == item) {
                                            Log.e("F: getAllOrders", "DELETE of WRONG LOCAL ORDER: " + wrongOrder);
                                            databaseDB.deleteOrder(wrongOrder.getSymbol(), wrongOrder.getTimeWhenPlaced(), wrongOrder.getIsItReal(), wrongOrder.getIsItShort(), wrongOrder.getMargin());

                                        }
                                    }
                                }
                            }

                            // Check if there are missing values from remote in local
                            for (Long item : remoteOrderIDofStopAndTake) {
                                if (!localOrderIDofStopAndTake.contains(item)) {
                                    Log.e("F: getAllOrders", "OrderID " + item + " is missing from local List");

                                    for (RealOrder wrongOrder : remoteOrdersFull) {
                                        if (wrongOrder.getOrderId() == item) {

                                            int isItShort = 0;
                                            if (wrongOrder.getSide().equals("BUY")) {
                                                isItShort = 1;
                                            }

                                            Log.e("F: getAllOrders", "UPDATE LOCAL ORDERS for missing REMOTE: " + wrongOrder);
                                            databaseDB.addNewOrder(new OrderListViewElement(wrongOrder.getSymbol(), 1, 0, wrongOrder.getStopPrice(), wrongOrder.getStopPrice(), wrongOrder.getStopPrice(),
                                                    wrongOrder.getStopPrice(), wrongOrder.getUpdateTime(), 0, isItShort, 0, 1, wrongOrder.getOrderId(), wrongOrder.getType(), wrongOrder.getOrigQty()));
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: getAllOrders", errorBody);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: getAllOrders", String.valueOf(t));
            }

        });

    }

    public static void getPositions(String symbol, long timestamp, Context context, OrderListViewElement orderToCheckAndDelete, CallbackButton callbackButton, boolean checkBetweenRemoteAndLocal) {

        long recvWindow = 10000;

        DBHandler databaseDB = DBHandler.getInstance(context);

        Cursor data = databaseDB.retrieveParam(14);
        if (data.getCount() == 0) {
            Log.e("F: getPositions", "There is no param nr 14");
        } else {
            data.moveToFirst();
            recvWindow = data.getInt(3);
        }
        data.close();

        Call<List<PositionRisk>> call = RetrofitClientSecretTestnet.getInstance(context, 7, symbol, 0, "", "", "", "", "0", "0", "",
                        0, "", "0", recvWindow, timestamp)
                .getMyApi().positionsInfo(symbol, recvWindow, timestamp);

        Log.e("F: getPositions", call.request().toString());

        call.enqueue(new Callback<List<PositionRisk>>() {
            @Override
            public void onResponse(@NonNull Call<List<PositionRisk>> call, @NonNull Response<List<PositionRisk>> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        List<PositionRisk> responseBody = response.body();

                        PositionRisk positionForSymbol = null;
                        for (int i = 0; i < responseBody.size(); i++) {
                            if (responseBody.get(i).getSymbol().equals(symbol)) {
                                positionForSymbol = responseBody.get(i);
                            }
                        }

                        assert positionForSymbol != null;
                        Log.e("F: getPositions", positionForSymbol.toString());

                        if (!checkBetweenRemoteAndLocal) {

                            if ((positionForSymbol.getPositionAmt() > 0 || positionForSymbol.getPositionAmt() < 0) && orderToCheckAndDelete != null) {
                                Log.e("F: getPositions", orderToCheckAndDelete + " MARKET ORDER REAL TO DELETE");

                                //TO CANCEL ORDER IN REAL MARKET WE NEED TO DO OPPOSITE ACTION WITH SAME AMOUNT OF CRYPTO
                                String side = orderToCheckAndDelete.getIsItShort() == 1 ? "BUY" : "SELL";
                                //Opposite side when closing
                                boolean isItShortCancelOrder = orderToCheckAndDelete.getIsItShort() != 1;

                                setMarketOrderToCancelCurrentOrder(symbol, side, "MARKET", "RESULT", orderToCheckAndDelete.getQuantity(), System.currentTimeMillis(),
                                        0, 0, 0, 0, 0, isItShortCancelOrder, false, context, callbackButton, orderToCheckAndDelete);

                            } else if (orderToCheckAndDelete != null) {
                                Log.e("F: getPositions", orderToCheckAndDelete + " MARKET ORDER TEST TO DELETE");

                                String info = "MARKET ORDER TO DELETE A: ExitA: " + orderToCheckAndDelete.getCurrentAmount() + ": EntryA: " + orderToCheckAndDelete.getEntryAmount() + " EntryP: " + orderToCheckAndDelete.getEntryPrice() + " ExitP: "  +  orderToCheckAndDelete.getStopLimitPrice();
                                ServiceFunctionsOther.writeToFile(info, context, "orders");


                                if (callbackButton != null) {
                                    callbackButton.onSuccess();
                                } else {
                                    float exitAmount = orderToCheckAndDelete.getCurrentAmount();
                                    float moneyEarned = exitAmount - orderToCheckAndDelete.getEntryAmount();
                                    ResultsListElement historicOrder = new ResultsListElement(orderToCheckAndDelete.getSymbol(), orderToCheckAndDelete.getEntryPrice(), orderToCheckAndDelete.getStopLimitPrice(), orderToCheckAndDelete.getPercentOfPriceChange(), orderToCheckAndDelete.getAccountNumber(), orderToCheckAndDelete.getMargin(), moneyEarned, orderToCheckAndDelete.getEntryAmount(), exitAmount, orderToCheckAndDelete.getPercentOfAmountChange(), orderToCheckAndDelete.getTimeWhenPlaced(), System.currentTimeMillis(),orderToCheckAndDelete.getIsItShort(), orderToCheckAndDelete.getIsItReal());
                                    databaseDB.addNewHistoricOrder(historicOrder);
                                    databaseDB.deleteOrder(symbol, orderToCheckAndDelete.getTimeWhenPlaced(), 1,
                                            orderToCheckAndDelete.getIsItShort(), orderToCheckAndDelete.getMargin());
                                }
                            }

                        } else if (positionForSymbol.getPositionAmt() == 0) {

                            String info = "MARKET ORDER TEST TO DELETE B: ExitA: " + orderToCheckAndDelete.getCurrentAmount() + ": EntryA: " + orderToCheckAndDelete.getEntryAmount() + " EntryP: " + orderToCheckAndDelete.getEntryPrice() + " ExitP: "  +  orderToCheckAndDelete.getStopLimitPrice();
                            ServiceFunctionsOther.writeToFile(info, context, "orders");


                            float exitAmount = orderToCheckAndDelete.getCurrentAmount();
                            float moneyEarned = exitAmount - orderToCheckAndDelete.getEntryAmount();
                            ResultsListElement historicOrder = new ResultsListElement(orderToCheckAndDelete.getSymbol(), orderToCheckAndDelete.getEntryPrice(), orderToCheckAndDelete.getStopLimitPrice(), orderToCheckAndDelete.getPercentOfPriceChange(), orderToCheckAndDelete.getAccountNumber(), orderToCheckAndDelete.getMargin(), moneyEarned, orderToCheckAndDelete.getEntryAmount(), exitAmount, orderToCheckAndDelete.getPercentOfAmountChange(), orderToCheckAndDelete.getTimeWhenPlaced(), System.currentTimeMillis(),orderToCheckAndDelete.getIsItShort(), orderToCheckAndDelete.getIsItReal());
                            databaseDB.addNewHistoricOrder(historicOrder);
                            databaseDB.deleteOrder(symbol, orderToCheckAndDelete.getTimeWhenPlaced(), 1,
                                    orderToCheckAndDelete.getIsItShort(), orderToCheckAndDelete.getMargin());
                        }


// Log all of response
//                        String all = "";
//                        for(int i = 0; i < responseBody.size(); i++) {
//                            all += responseBody.get(i).toString();
//                        }
//
//                        int length = all.length();
//                        int startIndex = 0;
//                        int endIndex = Math.min(startIndex + 1000, length);
//
//                        while (startIndex < length) {
//                            String chunk = all.substring(startIndex, endIndex);
//                            Log.d("F: getPositions", chunk);
//
//                            startIndex = endIndex;
//                            endIndex = Math.min(startIndex + 1000, length);
//                        }


                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: getPositions", errorBody);
                }
            }


            @Override
            public void onFailure(@NonNull Call<List<PositionRisk>> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: getPositions", String.valueOf(t));
            }

        });

    }

    public static void getAccountInfo(long timestamp, Context context) {

        long recvWindow = 10000;

        DBHandler databaseDB = DBHandler.getInstance(context);

        Cursor data = databaseDB.retrieveParam(14);
        if (data.getCount() == 0) {
            Log.e("F: makeOrderFunction", "There is no param nr 14");
        } else {
            data.moveToFirst();
            recvWindow = data.getInt(3);
        }
        data.close();

        Call<AccountInfo> call = RetrofitClientSecretTestnet.getInstance(context, 8, "", 0, "", "", "", "", "0", "0", "",
                        0, "", "0", recvWindow, timestamp)
                .getMyApi().getAccountInfo(recvWindow, timestamp);

        Log.e("F: accountInfo", call.request().toString());

        call.enqueue(new Callback<AccountInfo>() {
            @Override
            public void onResponse(@NonNull Call<AccountInfo> call, @NonNull Response<AccountInfo> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        AccountInfo responseBody = response.body();

                        String all = responseBody.toString();
                        int length = all.length();
                        int startIndex = 0;
                        int endIndex = Math.min(startIndex + 1000, length);

                        while (startIndex < length) {
                            String chunk = all.substring(startIndex, endIndex);
                            Log.d("F: accountInfo", chunk);

                            startIndex = endIndex;
                            endIndex = Math.min(startIndex + 1000, length);
                        }


//                        Gson gson = new Gson();
//                        List<RealOrder> allOrders = new ArrayList<>();
//
//                        for (JsonObject jsonObject : responseBody) {
//                            RealOrder order = gson.fromJson(jsonObject, RealOrder.class);
//                            allOrders.add(order);
//                        }
////                        // parse the response body to get the list of orders
//                        List<RealOrder> orders = new Gson().fromJson(responseBody, new TypeToken<List<RealOrder>>() {}.getType());

//                        // print the order IDs
//                        for (RealOrder order : allOrders) {
//                            Log.e("F: accountInfo", "Order ID: " + order.getOrderId() + " OrderSymbol: " + order.getSymbol() + " OrderType:" + order.getType());
//                        }

//                        RealOrders orders = response.body();
//                        List<RealOrder> orderList = orders.getOrders();
//
//                        for (int i = 0; i < orderList.size(); i++) {
//                            Log.e("F: getAllOrders", orderList.get(i).getClientOrderId());
//                        }

                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = convertErrorBodyToJson(response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("F: accountInfo", errorBody);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AccountInfo> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("F: accountInfo", String.valueOf(t));
            }

        });

    }

    public static String convertErrorBodyToJson(String errorBody) {
        try {
            JSONObject jsonObject = new JSONObject(errorBody);
            int code = jsonObject.getInt("code");
            String msg = jsonObject.getString("msg");

            return "Error: " + msg + " (" + code + ")";

        } catch (JSONException e) {
            e.printStackTrace();
            return "Error: Unable to parse error response.";
        }
    }

    //1 - for step, 0 for tick
    public static String formatFloatForSymbol(String symbol, float value, int stepOrTick, Context context) {

        DecimalFormat format = null;
        DBHandler databaseDB = DBHandler.getInstance(context);
        // Find the symbol in the list of symbols
        Cursor data = databaseDB.retrieveSymbolInfo(symbol);
        if (data.getCount() == 0) {
            Log.e("F: formatFloatForSymbol", "There is no symbol info for " + symbol);
        } else {
            data.moveToFirst();

            int decimalPlaces;
            if (stepOrTick == 1) {
                decimalPlaces = getDecimalPlaces(data.getString(4));
            } else {
                decimalPlaces = getDecimalPlaces(data.getString(3));
            }
            // Create a decimal format pattern that matches the tick and step sizes
            String pattern = "0.";
            for (int i = 0; i < decimalPlaces; i++) {
                pattern += "0";
            }

            if (!pattern.contains(".0") || decimalPlaces == 1) {
                pattern = "#";
            }

            Log.e("F: formatFloatForSymbol", "Decimal places: " + decimalPlaces + " pattern: " + pattern);
            // Create the decimal format using the pattern
            format = new DecimalFormat(pattern);

        }
        data.close();
        // Format the value using the decimal format
        return format != null ? format.format(value) : String.valueOf(value);
    }



}
