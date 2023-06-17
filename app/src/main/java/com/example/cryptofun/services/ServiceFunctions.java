package com.example.cryptofun.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cryptofun.data.AccountBalance;
import com.example.cryptofun.data.Leverage;
import com.example.cryptofun.data.RealOrder;
import com.example.cryptofun.data.ResponseMargin;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.ui.retrofit.RetrofitClientSecretTestnet;
import com.example.cryptofun.ui.view.OrderListViewElement;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServiceFunctions {

    public static void writeToFile(String data, Context context, String fileName) {

        String nameOFLogFile;
        Timestamp stamp = new Timestamp(System.currentTimeMillis());
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        @SuppressLint("SimpleDateFormat") DateFormat df2 = new SimpleDateFormat("dd");

        if (fileName.equals("result")) {
            nameOFLogFile = "result_" + df2.format(new Date(stamp.getTime())) + ".txt";
        } else {
            nameOFLogFile = "OrdersLog.txt";
        }


        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(nameOFLogFile, Context.MODE_APPEND));
            outputStreamWriter.write(df.format(new Date(stamp.getTime())) + " " +  data + "\n");
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static void makeOrderFunction(boolean isItReal, String symbol, int entryAmount, float stopLimit, float takeProfit, int margin, float currentPrice, boolean isItCrossed, boolean isItShort, long time, float balance, int accountNr, Context context, final CallbackButton callbackButton) {

        DBHandler databaseDB = DBHandler.getInstance(context);
        if (isItReal) {
            Log.e("Function: makeOrderFunction", "REAL");
            int recvWindow = 10000;
            String marginType = "ISOLATED";

            if (isItCrossed) {
                marginType = "CROSSED";
            }

            Cursor data = databaseDB.retrieveParam(14);
            if (data.getCount() == 0) {
                Log.e("Function: makeOrderFunction", "There is no param nr 14");
            } else {
                data.moveToFirst();
                recvWindow = data.getInt(3);
            }
            data.close();

            float quantity = (float) ((entryAmount / currentPrice) * 0.99 * margin);
            int finalRecvWindow = recvWindow;
            String finalMarginType = marginType;

            float stopLimitPrice = currentPrice * (1 - (float) stopLimit / 100);
            float takeProfitPrice = currentPrice * (1 + (float) takeProfit / 100);
            if (isItShort) {
                stopLimitPrice = currentPrice * (1 + (float) stopLimit / 100);
                takeProfitPrice = currentPrice * (1 - (float) takeProfit / 100);
            }

            float finalStopLimitPrice = stopLimitPrice;
            float finalTakeProfitPrice = takeProfitPrice;

            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            setMarginType(symbol, finalMarginType, System.currentTimeMillis(), finalRecvWindow, context);
                            setLeverage(symbol, margin, System.currentTimeMillis(), finalRecvWindow, context);

                            if (isItShort) {
                                setMarketOrder(symbol, "SELL", "MARKET", "RESULT", quantity, System.currentTimeMillis(), finalRecvWindow, finalStopLimitPrice, finalTakeProfitPrice, entryAmount, currentPrice, margin, isItShort, isItCrossed, context, callbackButton);
                            } else {
                                setMarketOrder(symbol, "BUY", "MARKET", "RESULT", quantity, System.currentTimeMillis(), finalRecvWindow, finalStopLimitPrice, finalTakeProfitPrice, entryAmount, currentPrice, margin, isItShort, isItCrossed, context, callbackButton);
                            }

                            Log.e("Function: makeOrderFunction", "START ORDER NEW THREAD");

                            if (callbackButton != null) {
                                callbackButton.onSuccess();
                            }


                        }
                    }
            ).start();
        } else {
            Log.e("Function: makeOrderFunction", "TEST");
            Log.e("Function: makeOrderFunction", isItCrossed + " " + isItShort);
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

            float quantity = (float) ((entryAmount / currentPrice) * 0.99 * margin);

            Log.e("Function: makeOrderFunction", isItCrossedValue + " " + isItShortValue);
            OrderListViewElement toDB = new OrderListViewElement(symbol, isItRealValue, (float) entryAmount, currentPrice, currentPrice, stopLimitPrice, takeProfitPrice, time, margin, isItShortValue, isItCrossedValue, accountNr, 0, "MARKET", quantity);
            databaseDB.addNewOrder(toDB);
            databaseDB.updateWithWhereClauseREAL("config", "value_real", balance - entryAmount, "id", Integer.toString(accountNr));
            if (callbackButton != null) {
                callbackButton.onSuccess();
            }
        }

    }

    private static void setMarketOrder(String symbol, String side, String type, String newOrderRespType, float quantity, long timestamp, long recvWindow, float stopLimitPrice, float takeProfitPrice, float entryAmount, float currentPrice, int margin, boolean isItShort, boolean isItCrossed, Context context, CallbackButton callbackButton) {

        Log.e("Function: setMarketOrder", "Start of market Order for " + symbol + " quantiity before: " + quantity);
        String quantityPrepared = formatFloatForSymbol(symbol, quantity, 1, context);
        Log.e("Function: setMarketOrder", "Start of market Order for " + symbol + " quantiity after: " + quantityPrepared);

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 3, symbol, 0, "", side, type, newOrderRespType, quantityPrepared, "0", "", 0, "", "0", recvWindow, timestamp)
                .getMyApi().setMarketOrder(symbol, side, type, newOrderRespType, quantityPrepared, timestamp);

        Log.e("Function: setMarketOrder", call.toString());
        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        RealOrder realOrder = response.body();
                        Log.e("Function: setMarketOrder", realOrder.getClientOrderId() + " " + realOrder.getSymbol() + " " + realOrder.getStatus() + " " + realOrder.getOrderId());

                        int isShort = 0;
                        int isCrossed = 0;

                        if (isItShort) {
                            isShort = 1;
                        }

                        if (isItCrossed) {
                            isCrossed = 1;
                        }

                        OrderListViewElement toDB = new OrderListViewElement(symbol, 1, (float) entryAmount, currentPrice, currentPrice, stopLimitPrice, takeProfitPrice, timestamp, margin, isShort, isCrossed, 1, realOrder.getOrderId(), "MARKET", quantity);
                        DBHandler databaseDB = DBHandler.getInstance(context);
                        databaseDB.addNewOrder(toDB);

                        if (side.equals("BUY")) {
                            setStopLimitOrTakeProfitMarket(symbol, "SELL", "STOP_MARKET", newOrderRespType, stopLimitPrice, "true", System.currentTimeMillis(), recvWindow, toDB, context, callbackButton);
                            setStopLimitOrTakeProfitMarket(symbol, "SELL", "TAKE_PROFIT_MARKET", newOrderRespType, takeProfitPrice, "true", System.currentTimeMillis(), recvWindow, toDB, context, callbackButton);
                        } else {
                            setStopLimitOrTakeProfitMarket(symbol, "BUY", "STOP_MARKET", newOrderRespType, stopLimitPrice, "true", System.currentTimeMillis(), recvWindow, toDB, context,callbackButton);
                            setStopLimitOrTakeProfitMarket(symbol, "BUY", "TAKE_PROFIT_MARKET", newOrderRespType, takeProfitPrice, "true", System.currentTimeMillis(), recvWindow, toDB, context, callbackButton);
                        }

                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = response.errorBody().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("Function: setMarketOrder", "Error response: " + errorBody);
                }

            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("Function: setMarketOrder", String.valueOf(t));
            }

        });

    }

    private static void setStopLimitOrTakeProfitMarket(String symbol, String side, String type, String newOrderRespType, float stopPrice, String closePosition, long timestamp, long recvWindow, OrderListViewElement orderElement, Context context, CallbackButton callbackButton) {

        Log.e("Function: setStopLimitOrTakeProfitMarket", "Start of stopLimit Order for " + symbol + " stopPrice before: " + stopPrice);

        String stopPriceFinal = formatFloatForSymbol(symbol, stopPrice, 0, context);

        Log.e("Function: setStopLimitOrTakeProfitMarket", "Start of stopLimit Order for " + symbol + " stopPrice after: " + stopPriceFinal);

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 4, symbol, 0, "", side, type, newOrderRespType, "0",
                        stopPriceFinal, closePosition, 0, "", "0", recvWindow, timestamp)
                .getMyApi().setStopLimitOrTakeProfitMarket(symbol, side, type, newOrderRespType, stopPriceFinal, closePosition, timestamp);

        Log.e("Function: setStopLimitOrTakeProfitMarket", call.toString());
        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {

                        RealOrder realOrder = response.body();
                        Log.e("Function: setStopLimitOrTakeProfitMarket", realOrder.getClientOrderId() + " " + realOrder.getSymbol() + " " + realOrder.getStatus());
                        orderElement.setOrderType(type);
                        orderElement.setOrderID(realOrder.getOrderId());
                        orderElement.setTimeWhenPlaced(timestamp);
                        DBHandler databaseDB = DBHandler.getInstance(context);
                        databaseDB.addNewOrder(orderElement);
                        if (callbackButton != null) {
                            callbackButton.onSuccess();
                        }


//                        if (!isMyServiceRunning(OrdersService.class)) {
//                            Intent serviceIntent = new Intent(getContext(), OrdersService.class);
//                            getContext().startForegroundService(serviceIntent);
//                        }


                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = response.errorBody().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("Function: setStopLimitOrTakeProfitMarket", "Error response: " + errorBody);
                }

            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("Function: setStopLimitOrTakeProfitMarket", String.valueOf(t));
            }

        });

    }

    private static void setLeverage(String symbol, int leverage, long timestamp, long recvWindow, Context context) {
        Call<Leverage> call = RetrofitClientSecretTestnet.getInstance(context, 1, symbol, leverage, "", "", "", "", "0", "0", "",
                0, "", "0", recvWindow, timestamp).getMyApi().setLeverage(symbol, leverage, timestamp);
        Log.e("Function: setLeverage", call.toString());
        call.enqueue(new Callback<Leverage>() {
            @Override
            public void onResponse(@NonNull Call<Leverage> call, @NonNull Response<Leverage> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        Leverage leverage1 = response.body();
                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = response.errorBody().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("Function: setLeverage", "Error response: " + errorBody);
                }


            }

            @Override
            public void onFailure(@NonNull Call<Leverage> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("Function: setLeverage", String.valueOf(t));

            }

        });

    }

    private static void setMarginType(String symbol, String marginType, long timestamp, long recvWindow, Context context) {
        Call<ResponseMargin> call = RetrofitClientSecretTestnet.getInstance(context, 2, symbol, 0, marginType, "", "", "", "0", "0", "",
                0, "", "0", recvWindow, timestamp).getMyApi().setMarginType(symbol, marginType, timestamp);

        Log.e("Function: setMarginType", call.toString());
        call.enqueue(new Callback<ResponseMargin>() {
            @Override
            public void onResponse(@NonNull Call<ResponseMargin> call, @NonNull Response<ResponseMargin> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        ResponseMargin leverage1 = response.body();
                    } else {
                        System.out.println(response.code() + " " + response.message());
                    }
                } else if (response.errorBody() != null) {
                    String errorBody = "";
                    try {
                        errorBody = response.errorBody().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("Function: setMarginType", "Error response: " + errorBody);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseMargin> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("Function: setMarginType", String.valueOf(t));

            }

        });

    }

    //1 - for step, 0 for tick
    public static String formatFloatForSymbol(String symbol, float value, int stepOrTick, Context context) {

        DecimalFormat format = null;
        DBHandler databaseDB = DBHandler.getInstance(context);
        // Find the symbol in the list of symbols
        Cursor data = databaseDB.retrieveSymbolInfo(symbol);
        if (data.getCount() == 0) {
            Log.e("Function: formatFloatForSymbol", "There is no symbol info for " + symbol);
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

            if (!pattern.contains(".0")) {
                pattern = "#";
            }

            Log.e("Function: formatFloatForSymbol", "Decimal places: " + decimalPlaces + " pattern: " + pattern);
            // Create the decimal format using the pattern
            format = new DecimalFormat(pattern);

        }

        // Format the value using the decimal format
        return format != null ? format.format(value) : String.valueOf(value);
    }

    public static int getDecimalPlaces(String value) {
        int index = value.indexOf(".");
        return index < 0 ? 0 : value.length() - index - 1;
    }

    public static void getRealAccountBalance(Context context, CallbackButton callbackButton) {


        Call<List<AccountBalance>> call = RetrofitClientSecretTestnet.getInstance(context, 0,  "", 0, "", "", "", "", "0", "0", "",
                        0,"", "0", 0, 0)
                .getMyApi().getAccountBalance();

        Log.e("Function: getRealAccountBalance", call.toString());
        call.enqueue(new Callback<List<AccountBalance>>() {
            @Override
            public void onResponse(@NonNull Call<List<AccountBalance>> call, @NonNull Response<List<AccountBalance>> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        List<AccountBalance> balanceList = response.body();
                        for (int i = 0; i < balanceList.size(); i++) {
                            Log.e("Function: getRealAccountBalance", balanceList.get(i).getAsset());
                            if (balanceList.get(i).getAsset().contains("USDT")) {

                                DBHandler databaseDB = DBHandler.getInstance(context);
                                Cursor data2 = databaseDB.retrieveParam(3);
                                if (data2.getCount() == 0) {
                                    Log.e("Function: getRealAccountBalance", "There is no param nr 3");
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
                        errorBody = response.errorBody().string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.e("Function: getRealAccountBalance", "Error response: " + errorBody);
                    if (callbackButton != null) {
                        callbackButton.onSuccess();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AccountBalance>> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e("Function: getRealAccountBalance", String.valueOf(t));
            }

        });

    }




}
