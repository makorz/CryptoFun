package com.example.cryptofun.ui.orders;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.data.MarkPrice;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.FragmentOrdersBinding;

import com.example.cryptofun.services.CallbackButton;
import com.example.cryptofun.services.ServiceFunctions;
import com.example.cryptofun.ui.retrofit.RetrofitClientFutures;
import com.example.cryptofun.ui.view.OrderListAdapter;
import com.example.cryptofun.ui.view.OrderListViewElement;

import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class OrdersFragment extends Fragment implements CallbackButton {


    private static final String TAG = "OrdersFrag";
    private static final String TABLE_NAME_ORDERS = "current_orders";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String DESCRIPTION = "description";
    private static final String VALUE_STRING = "value_string";
    private static final String VALUE_INT = "value_int";
    private static final String ID = "id";
    private static final String VALUE_REAL = "value_real";

    private FragmentOrdersBinding binding;
    private ArrayList<OrderListViewElement> mItems = new ArrayList<>();
    private Button placeOrderButton;
    private RadioGroup testOrRealRG, isolatedOrCrossedRG, longOrShortRG;
    private EditText symbolET, amountET, stopLimitET, takeProfitET, marginET;
    private DBHandler databaseDB;
    private RecyclerView recyclerView;
    private OrderListAdapter adapter;
    private ProgressBar orderLoading;
    View root;
    InputMethodManager imm;
    CallbackButton callbackButton = this;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.e(TAG, "CreateView");

        imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        placeOrderButton = binding.btOrder;
        testOrRealRG = binding.radioGroupRealTest;
        isolatedOrCrossedRG = binding.radioGroupIsolatedCrossed;
        longOrShortRG = binding.radioGroupLongShort;
        symbolET = binding.etSymbol;
        amountET = binding.etAmountUsdt;
        stopLimitET = binding.etSlPercentage;
        takeProfitET = binding.etTpPercentage;
        marginET = binding.etMargin;
        orderLoading = binding.progressBarOrder;

        recyclerView = binding.rwCurrentOrders;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter = new OrderListAdapter(mItems);
        recyclerView.setAdapter(adapter);
        buttonsJob();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("OrdersStatus"));

        return binding.getRoot();
    }

    // Receive broadcast from ApprovingService
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("OrdersStatus")) {
//                Bundle bundle = intent.getBundleExtra("bundleOrdersStatus");
                Log.e(TAG, "APRVServiceReceived - Orders" + Thread.currentThread());
//                mItems = (ArrayList<OrderListViewElement>) bundle.getSerializable("ordersList");
                onReceiveNewList(adapter);
            }
        }
    };

    private void buttonsJob() {
        placeOrderButton.setOnClickListener(v -> {

            // Hide the keyboard
            imm.hideSoftInputFromWindow(root.getWindowToken(), 0);

            setButtonEnabled(false);
            // Get the ID of the selected RadioButton
            int selectedId = testOrRealRG.getCheckedRadioButtonId();
            int selectedId2 = isolatedOrCrossedRG.getCheckedRadioButtonId();
            int selectedId3 = longOrShortRG.getCheckedRadioButtonId();
            int accountNr = -1;
            boolean isItReal = false;
            boolean isItCrossed = false;
            boolean isItShort = false;
            String symbol = symbolET.getText().toString().toUpperCase();
            String amount = amountET.getText().toString();
            String stopLimit = stopLimitET.getText().toString();
            String takeProfit = takeProfitET.getText().toString();
            String margin = marginET.getText().toString();

            int amountValue = 0;
            try {
                amountValue = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            float stopLimitValue = 0;
            try {
                stopLimitValue = Float.parseFloat(stopLimit);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            float takeProfitValue = 0;
            try {
                takeProfitValue = Float.parseFloat(takeProfit);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            int marginValue = 0;
            try {
                marginValue = Integer.parseInt(margin);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            if (selectedId != -1 && selectedId2 != -1 && selectedId3 != -1 && amountValue > 15) {
                if (selectedId == R.id.rb_real) {
                    isItReal = true;
                }
                if (selectedId2 == R.id.rb_crossed) {
                    isItCrossed = true;
                }
                if (selectedId3 == R.id.rb_short) {
                    isItShort = true;
                }

                databaseDB = DBHandler.getInstance(getContext());
                float balance = 0;
                Cursor data;
                if (isItReal) {

                    accountNr = 3;

                    data = databaseDB.retrieveParam(3);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 3");
                    } else {
                        data.moveToFirst();
                        balance = data.getFloat(4);
                    }
                } else {

                    accountNr = 2;

                    data = databaseDB.retrieveParam(2);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 2");
                    } else {
                        data.moveToFirst();
                        balance = data.getFloat(4);
                    }
                }
                data.close();

                if (balance * 0.99 < amountValue) {
                    showInfoForUser(getContext(), "Amount of USDT is higher than Your balance, check balance and adjust entered number.");
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("Are you sure you want to place this order?");

                    float finalBalance = balance;
                    boolean finalIsItShort = isItShort;
                    boolean finalIsItCrossed = isItCrossed;
                    int finalMarginValue = marginValue;
                    float finalTakeProfitValue = takeProfitValue;
                    float finalStopLimitValue = stopLimitValue;
                    int finalAmountValue = amountValue;
                    boolean finalIsItReal = isItReal;
                    int finalAccountNr = accountNr;
                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {


                            getMarkPrice(symbol)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Observer<MarkPrice>() {
                                        @Override
                                        public void onSubscribe(@NonNull Disposable d) {
                                            // do something when the subscription is made
                                        }

                                        @Override
                                        public void onNext(@NonNull MarkPrice markPrice) {
                                            // handle the MarkPrice object returned by the API
                                            Log.e(TAG, "User Order Made for " + symbol + ".");

                                            ServiceFunctions.makeOrderFunction(finalIsItReal, symbol, finalAmountValue, finalStopLimitValue, finalTakeProfitValue, finalMarginValue, markPrice.getMarkPrice(), finalIsItCrossed, finalIsItShort, System.currentTimeMillis(), finalBalance, finalAccountNr, getContext(), callbackButton);

//                                            makeOrder(finalIsItReal, symbol, finalAmountValue, finalStopLimitValue, finalTakeProfitValue, finalMarginValue, markPrice.getMarkPrice(), finalIsItCrossed, finalIsItShort, System.currentTimeMillis(), finalBalance);
                                        }

                                        @Override
                                        public void onError(@NonNull Throwable e) {
                                            // handle any errors that occur
                                            Log.e(TAG, "An error has occurred: " + e.getMessage());
                                            setButtonEnabled(true);
                                        }

                                        @Override
                                        public void onComplete() {
                                            // do something when the observable completes
                                        }
                                    });

                        }
                    });
                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setButtonEnabled(true);
                        }
                    });

                    builder.setCancelable(false);
                    builder.show();

                }

            } else {
                // No RadioButton is selected
                Toast.makeText(getContext(), "Please mark RadioButtons also amount value should be bigger than 15 USDT", Toast.LENGTH_SHORT).show();
                setButtonEnabled(true);
            }

        });
    }

    public Observable<MarkPrice> getMarkPrice(String symbol) {
        return RetrofitClientFutures.getInstance()
                .getMyApi()
                .getMarkPrice(symbol)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

//    private void makeOrder(boolean isItReal, String symbol, int entryAmount, float stopLimit, float takeProfit, int margin, float currentPrice, boolean isItCrossed, boolean isItShort, long time, float balance) {
//
//        if (isItReal) {
//            Log.e(TAG, "REAL");
//            int accountNr = 3;
//            int recvWindow = 10000;
//            String marginType = "ISOLATED";
//
//            if (isItCrossed) {
//                marginType = "CROSSED";
//            }
//
//            Cursor data = databaseDB.retrieveParam(14);
//            if (data.getCount() == 0) {
//                Log.e(TAG, "There is no param nr 14");
//            } else {
//                data.moveToFirst();
//                recvWindow = data.getInt(3);
//            }
//            data.close();
//
//            float quantity = (float) ((entryAmount / currentPrice) * 0.99 * margin);
////            setLimitOrder(symbol, "BUY", "LIMIT","RESULT", "GTC",1f, 23000f,System.currentTimeMillis(), recvWindow);
//            int finalRecvWindow = recvWindow;
//            String finalMarginType = marginType;
//
//            float stopLimitPrice = currentPrice * (1 - (float) stopLimit / 100);
//            float takeProfitPrice = currentPrice * (1 + (float) takeProfit / 100);
//            if (isItShort) {
//                stopLimitPrice = currentPrice * (1 + (float) stopLimit / 100);
//                takeProfitPrice = currentPrice * (1 - (float) takeProfit / 100);
//            }
//
//            float finalStopLimitPrice = stopLimitPrice;
//            float finalTakeProfitPrice = takeProfitPrice;
//            new Thread(
//                    new Runnable() {
//                        @Override
//                        public void run() {
//                            setMarginType(symbol, finalMarginType, System.currentTimeMillis(), finalRecvWindow);
//                            setLeverage(symbol, margin, System.currentTimeMillis(), finalRecvWindow);
//
//                            if (isItShort) {
//                                setMarketOrder(symbol, "SELL", "MARKET", "RESULT", quantity, System.currentTimeMillis(), finalRecvWindow, finalStopLimitPrice, finalTakeProfitPrice, entryAmount, currentPrice, margin, isItShort, isItCrossed);
//                            } else {
//                                setMarketOrder(symbol, "BUY", "MARKET", "RESULT", quantity, System.currentTimeMillis(), finalRecvWindow, finalStopLimitPrice, finalTakeProfitPrice, entryAmount, currentPrice, margin, isItShort, isItCrossed);
//                            }
//
//                            Log.e(TAG, "START ORDER NEW THREAD");
//
//                        }
//                    }
//            ).start();
//
//
//        } else {
//            Log.e(TAG, "TEST");
//            Log.e(TAG, isItCrossed + " " + isItShort);
//            float stopLimitPrice = currentPrice * (1 - (float) stopLimit / 100);
//            float takeProfitPrice = currentPrice * (1 + (float) takeProfit / 100);
//            int isItShortValue = 0;
//            int isItRealValue = 0;
//            int isItCrossedValue = 0;
//            if (isItShort) {
//                stopLimitPrice = currentPrice * (1 + (float) stopLimit / 100);
//                takeProfitPrice = currentPrice * (1 - (float) takeProfit / 100);
//                isItShortValue = 1;
//            }
//            if (isItCrossed) {
//                isItCrossedValue = 1;
//            }
//            int accountNr = 2;
//
//            float quantity = (float) ((entryAmount / currentPrice) * 0.99 * margin);
//
//            Log.e(TAG, isItCrossedValue + " " + isItShortValue);
//            OrderListViewElement toDB = new OrderListViewElement(symbol, isItRealValue, (float) entryAmount, currentPrice, currentPrice, stopLimitPrice, takeProfitPrice, time, margin, isItShortValue, isItCrossedValue, accountNr, 0, "MARKET", quantity);
//            mItems.add(toDB);
//            for (int i = 0; i < mItems.size(); i++) {
//                Log.e(TAG, mItems.get(i).getSymbol() + mItems.get(i).getCurrentPrice() + mItems.get(i).getEntryPrice());
//            }
//            databaseDB.addNewOrder(toDB);
//            databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, balance - entryAmount, ID, "2");
//            onReceiveNewList(adapter, mItems);
//            setButtonEnabled(true);
//
//
//        }
//
//
//    }

//    private void setLeverage(String symbol, int leverage, long timestamp, long recvWindow) {
//        Call<Leverage> call = RetrofitClientSecretTestnet.getInstance(getContext(), 1, symbol, leverage, "", "", "", "", "0", "0", "",
//                0, "", "0", recvWindow, timestamp).getMyApi().setLeverage(symbol, leverage, timestamp);
//        Log.e(TAG, call.toString());
//        call.enqueue(new Callback<Leverage>() {
//            @Override
//            public void onResponse(@NonNull Call<Leverage> call, @NonNull Response<Leverage> response) {
//
//                if (response.body() != null) {
//                    if (response.isSuccessful()) {
//                        Leverage leverage1 = response.body();
//                    } else {
//                        System.out.println(response.code() + " " + response.message());
//                    }
//                } else if (response.errorBody() != null) {
//                    String errorBody = "";
//                    try {
//                        errorBody = response.errorBody().string();
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                    Log.e(TAG, "Error response: " + errorBody);
//                }
//
//                setButtonEnabled(true);
//
//
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<Leverage> call, @NonNull Throwable t) {
//                System.out.println("An error has occurred" + t);
//                Log.e(TAG, String.valueOf(t));
//                setButtonEnabled(true);
//
//
//            }
//
//        });
//
//    }


//    private void setMarginType(String symbol, String marginType, long timestamp, long recvWindow) {
//        Call<ResponseMargin> call = RetrofitClientSecretTestnet.getInstance(getContext(), 2, symbol, 0, marginType, "", "", "", "0", "0", "",
//                0, "", "0", recvWindow, timestamp).getMyApi().setMarginType(symbol, marginType, timestamp);
//
//        Log.e(TAG, call.toString());
//        call.enqueue(new Callback<ResponseMargin>() {
//            @Override
//            public void onResponse(@NonNull Call<ResponseMargin> call, @NonNull Response<ResponseMargin> response) {
//
//                if (response.body() != null) {
//                    if (response.isSuccessful()) {
//                        ResponseMargin leverage1 = response.body();
//                    } else {
//                        System.out.println(response.code() + " " + response.message());
//                    }
//                } else if (response.errorBody() != null) {
//                    String errorBody = "";
//                    try {
//                        errorBody = response.errorBody().string();
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                    Log.e(TAG, "Error response: " + errorBody);
//                }
//                setButtonEnabled(true);
//
//
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<ResponseMargin> call, @NonNull Throwable t) {
//                System.out.println("An error has occurred" + t);
//                Log.e(TAG, String.valueOf(t));
//                setButtonEnabled(true);
//
//
//            }
//
//        });
//
//    }
//
//    private void setLimitOrder(String symbol, String side, String type, String newOrderRespType, String timeInForce, float quantity, float price, long timestamp, long recvWindow) {
//
//        // It is for specific number of decimals for making orders
//        DecimalFormat df = new DecimalFormat("0.00000000");
//        String quantity2 = df.format(quantity);
//        String price2 = df.format(price);
//
//        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(getContext(), 6, symbol, 0, "", side, type, newOrderRespType, quantity2,
//                        "0", "", 0, timeInForce, price2, recvWindow, timestamp)
//                .getMyApi().setLimitOrder(symbol, side, type, newOrderRespType, timeInForce, quantity2, price2, timestamp);
//
//        Log.e(TAG, call.toString());
//        call.enqueue(new Callback<RealOrder>() {
//            @Override
//            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {
//
//                if (response.body() != null) {
//                    if (response.isSuccessful()) {
//                        RealOrder realOrder = response.body();
//                        Log.e(TAG, realOrder.getClientOrderId() + " " + realOrder.getSymbol() + " " + realOrder.getStatus());
//                    } else {
//                        System.out.println(response.code() + " " + response.message());
//                    }
//                } else if (response.errorBody() != null) {
//                    String errorBody = "";
//                    try {
//                        errorBody = response.errorBody().string();
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                    Log.e(TAG, "Error response: " + errorBody);
//                }
//
//                setButtonEnabled(true);
//
//
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
//                System.out.println("An error has occurred" + t);
//                Log.e(TAG, String.valueOf(t));
//                setButtonEnabled(true);
//
//
//            }
//
//        });
//
//    }
//
//    //1 - for step, 0 for tick
//    public String formatFloatForSymbol(String symbol, float value, int stepOrTick) {
//
//        DecimalFormat format = null;
//
//        // Find the symbol in the list of symbols
//        Cursor data = databaseDB.retrieveSymbolInfo(symbol);
//        if (data.getCount() == 0) {
//            Log.e(TAG, "There is no symbol info for " + symbol);
//        } else {
//            data.moveToFirst();
//
//            int decimalPlaces;
//            if (stepOrTick == 1) {
//                decimalPlaces = getDecimalPlaces(data.getString(4));
//            } else {
//                decimalPlaces = getDecimalPlaces(data.getString(3));
//            }
//            // Create a decimal format pattern that matches the tick and step sizes
//            String pattern = "0.";
//            for (int i = 0; i < decimalPlaces; i++) {
//                pattern += "0";
//            }
//
//            if (!pattern.contains(".0")) {
//                pattern = "#";
//            }
//
//            Log.e(TAG, "Decimal places: " + decimalPlaces + " pattern: " + pattern);
//            // Create the decimal format using the pattern
//            format = new DecimalFormat(pattern);
//
//        }
//
//        // Format the value using the decimal format
//        return format != null ? format.format(value) : String.valueOf(value);
//    }
//
//    private int getDecimalPlaces(String value) {
//        int index = value.indexOf(".");
//        return index < 0 ? 0 : value.length() - index - 1;
//    }
//
//    private void setMarketOrder(String symbol, String side, String type, String newOrderRespType, float quantity, long timestamp, long recvWindow, float stopLimitPrice, float takeProfitPrice, float entryAmount, float currentPrice, int margin, boolean isItShort, boolean isItCrossed) {
//
//        Log.e(TAG, "Start of market Order for " + symbol + " quantiity before: " + quantity);
//        String quantityPrepared = formatFloatForSymbol(symbol, quantity, 1);
//        Log.e(TAG, "Start of market Order for " + symbol + " quantiity after: " + quantityPrepared);
//
//
//        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(getContext(), 3, symbol, 0, "", side, type, newOrderRespType, quantityPrepared, "0", "", 0, "", "0", recvWindow, timestamp)
//                .getMyApi().setMarketOrder(symbol, side, type, newOrderRespType, quantityPrepared, timestamp);
//
//        Log.e(TAG, call.toString());
//        call.enqueue(new Callback<RealOrder>() {
//            @Override
//            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {
//
//                if (response.body() != null) {
//                    if (response.isSuccessful()) {
//                        RealOrder realOrder = response.body();
//                        Log.e(TAG, realOrder.getClientOrderId() + " " + realOrder.getSymbol() + " " + realOrder.getStatus() + " " + realOrder.getOrderId());
//
//                        int isShort = 0;
//                        int isCrossed = 0;
//
//                        if (isItShort) {
//                            isShort = 1;
//                        }
//
//                        if (isItCrossed) {
//                            isCrossed = 1;
//                        }
//
//                        OrderListViewElement toDB = new OrderListViewElement(symbol, 1, (float) entryAmount, currentPrice, currentPrice, stopLimitPrice, takeProfitPrice, timestamp, margin, isShort, isCrossed, 1, realOrder.getOrderId(), "MARKET", quantity);
//                        mItems.add(toDB);
//                        for (int i = 0; i < mItems.size(); i++) {
//                            Log.e(TAG, mItems.get(i).getSymbol() + mItems.get(i).getCurrentPrice() + mItems.get(i).getEntryPrice());
//                        }
//                        databaseDB.addNewOrder(toDB);
//
//                        if (side.equals("BUY")) {
//                            setStopLimitOrTakeProfitMarket(symbol, "SELL", "STOP_MARKET", newOrderRespType, stopLimitPrice, "true", System.currentTimeMillis(), recvWindow, toDB);
//                            setStopLimitOrTakeProfitMarket(symbol, "SELL", "TAKE_PROFIT_MARKET", newOrderRespType, takeProfitPrice, "true", System.currentTimeMillis(), recvWindow, toDB);
//                        } else {
//                            setStopLimitOrTakeProfitMarket(symbol, "BUY", "STOP_MARKET", newOrderRespType, stopLimitPrice, "true", System.currentTimeMillis(), recvWindow, toDB);
//                            setStopLimitOrTakeProfitMarket(symbol, "BUY", "TAKE_PROFIT_MARKET", newOrderRespType, takeProfitPrice, "true", System.currentTimeMillis(), recvWindow, toDB);
//                        }
//
//                    } else {
//                        System.out.println(response.code() + " " + response.message());
//                    }
//                } else if (response.errorBody() != null) {
//                    String errorBody = "";
//                    try {
//                        errorBody = response.errorBody().string();
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                    Log.e(TAG, "Error response: " + errorBody);
//                }
//
//                setButtonEnabled(true);
//
//
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
//                System.out.println("An error has occurred" + t);
//                Log.e(TAG, String.valueOf(t));
//                setButtonEnabled(true);
//
//
//            }
//
//        });
//
//    }
//
//    private void setStopLimitOrTakeProfitMarket(String symbol, String side, String type, String newOrderRespType, float stopPrice, String closePosition, long timestamp, long recvWindow, OrderListViewElement orderElement) {
//
//        Log.e(TAG, "Start of stopLimit Order for " + symbol + " stopPrice before: " + stopPrice);
//
//        String stopPriceFinal = formatFloatForSymbol(symbol, stopPrice, 0);
//
//        Log.e(TAG, "Start of stopLimit Order for " + symbol + " stopPrice after: " + stopPriceFinal);
//
//        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(getContext(), 4, symbol, 0, "", side, type, newOrderRespType, "0",
//                        stopPriceFinal, closePosition, 0, "", "0", recvWindow, timestamp)
//                .getMyApi().setStopLimitOrTakeProfitMarket(symbol, side, type, newOrderRespType, stopPriceFinal, closePosition, timestamp);
//
//        Log.e(TAG, call.toString());
//        call.enqueue(new Callback<RealOrder>() {
//            @Override
//            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {
//
//                if (response.body() != null) {
//                    if (response.isSuccessful()) {
//
//                        RealOrder realOrder = response.body();
//                        Log.e(TAG, realOrder.getClientOrderId() + " " + realOrder.getSymbol() + " " + realOrder.getStatus());
//                        orderElement.setOrderType(type);
//                        orderElement.setOrderID(realOrder.getOrderId());
//                        orderElement.setTimeWhenPlaced(timestamp);
//                        mItems.add(orderElement);
//                        for (int i = 0; i < mItems.size(); i++) {
//                            Log.e(TAG, mItems.get(i).getSymbol() + mItems.get(i).getCurrentPrice() + mItems.get(i).getEntryPrice());
//                        }
//                        databaseDB.addNewOrder(orderElement);
//
//                        if (!isMyServiceRunning(OrdersService.class)) {
//                            Intent serviceIntent = new Intent(getContext(), OrdersService.class);
//                            getContext().startForegroundService(serviceIntent);
//                        }
//
//
//                    } else {
//                        System.out.println(response.code() + " " + response.message());
//                    }
//                } else if (response.errorBody() != null) {
//                    String errorBody = "";
//                    try {
//                        errorBody = response.errorBody().string();
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                    Log.e(TAG, "Error response: " + errorBody);
//                }
//
//                setButtonEnabled(true);
//
//
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
//                System.out.println("An error has occurred" + t);
//                Log.e(TAG, String.valueOf(t));
//                setButtonEnabled(true);
//
//
//            }
//
//        });
//
//    }

    private void setButtonEnabled(boolean enabled) {
        if (enabled) {
            placeOrderButton.setEnabled(true);
            placeOrderButton.setVisibility(View.VISIBLE);
            orderLoading.setVisibility(View.GONE);
        } else {
            placeOrderButton.setEnabled(false);
            placeOrderButton.setVisibility(View.GONE);
            orderLoading.setVisibility(View.VISIBLE);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void onReceiveNewList(OrderListAdapter adapter) {
        ArrayList<OrderListViewElement> currentOrders = new ArrayList<>();
        DBHandler db = DBHandler.getInstance(getContext());
        Cursor data = db.retrieveAllFromTable(TABLE_NAME_ORDERS);
        if (data.getCount() == 0) {
            Log.e(TAG, "No active orders");
        } else {
            while (data.moveToNext()) {

                OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getInt(13), data.getString(14), data.getFloat(15));
                currentOrders.add(tempToken);

            }
            adapter.updateList(currentOrders);
        }
        data.close();
    }

    private void showInfoForUser(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                setButtonEnabled(true);
            }
        });
        setButtonEnabled(true);
        builder.show();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "Resume.");
        setButtonEnabled(true);
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //databaseDB.close();
        binding = null;
    }

    @Override
    public void onSuccess() {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"YOYOOY");
                setButtonEnabled(true);
                onReceiveNewList(adapter);

            }
        });
    }

    @Override
    public void onError() {
        setButtonEnabled(true);
    }
}