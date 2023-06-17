package com.example.cryptofun.ui.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.data.RealOrder;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.CardViewOrderBinding;
import com.example.cryptofun.ui.retrofit.RetrofitClientSecretTestnet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.ViewHolder> {

    private ArrayList<OrderListViewElement> items;
    private DBHandler databaseDB;

    private static final String TAG = "OrderListAdapter";

    private static final String TABLE_NAME_CONFIG = "config";
    private static final String DESCRIPTION = "description";
    private static final String VALUE_STRING = "value_string";
    private static final String VALUE_INT = "value_int";
    private static final String VALUE_REAL = "value_real";
    private static final String ID = "id";

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView symbol;
        TextView testOrReal;
        TextView entryPrice;
        TextView currentPrice;
        TextView stopLimitPrice;
        TextView takeProfitPrice;
        TextView percentOfPriceChange;
        TextView percentOfAmountChange;
        TextView margin;
        TextView time;
        TextView entryAmount;
        TextView currentAmount;
        TextView isolatedOrCrossed;
        TextView longOrShort;


        public ViewHolder(CardViewOrderBinding b) {
            super(b.getRoot());
            symbol = b.tvSymbol;
            testOrReal = b.tvTestOrReal;
            entryPrice = b.tvEntryPrice;
            currentPrice = b.tvCurrentPrice;
            stopLimitPrice = b.tvSLprice;
            takeProfitPrice = b.tvTPPrice;
            percentOfPriceChange = b.tvPriceChange;
            percentOfAmountChange = b.tvContractAmountChange;
            margin = b.tvMargin;
            time = b.tvTimeOfCreatingContract;
            entryAmount = b.tvEntryAmount;
            currentAmount = b.tvCurrentAmount;
            isolatedOrCrossed = b.tvIsolatedOrCrossed;
            longOrShort = b.tvLongOrShort;

        }
    }

    //data is passed to constructor
    public OrderListAdapter(ArrayList<OrderListViewElement> items) {
        // LayoutInflater inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @NonNull
    @Override
    //inflates the row layout from xml when needed
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardViewOrderBinding binding = CardViewOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }


    @Override
    //binds the data to elements in each row
    public void onBindViewHolder(@NonNull OrderListAdapter.ViewHolder holder, int position) {

        Context context = holder.itemView.getContext();
        databaseDB = DBHandler.getInstance(context);
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showDeleteConfirmationDialog(context, holder.getAdapterPosition());
                notifyDataSetChanged();
                return true;
            }
        });
        int darkGreenColor = ContextCompat.getColor(context, R.color.dark_green);
        int darkRedColor = ContextCompat.getColor(context, R.color.dark_red);
        int testBlue = ContextCompat.getColor(context, R.color.dark_blue_test);

        DecimalFormat df = new DecimalFormat("0.0000");
        DecimalFormat df2 = new DecimalFormat("0.00");
        DecimalFormat df3 = new DecimalFormat("0.0");
        String symbol = items.get(position).getSymbol();
        int isItReal = items.get(position).getIsItReal();
        int isItShort = items.get(position).getIsItShort();
        int isItCrossed = items.get(position).getIsItCrossed();
        float entryPrice = items.get(position).getEntryPrice();
        float currentPrice = items.get(position).getCurrentPrice();
        float stopLimitPrice = items.get(position).getStopLimitPrice();
        float takeProfitPrice = items.get(position).getTakeProfitPrice();
        float percentOfPriceChange = items.get(position).getPercentOfPriceChange();
        float percentOfAmountChange = items.get(position).getPercentOfAmountChange();
        float entryAmount = items.get(position).getEntryAmount();
        float currentAmount = items.get(position).getCurrentAmount();
        String type = items.get(position).getOrderType();
        int margin = items.get(position).getMargin();
        long time = items.get(position).getTimeWhenPlaced();

        holder.symbol.setText(symbol);
        if (isItReal == 1) {
            holder.testOrReal.setText("REAL");
            holder.symbol.setText(symbol);
            holder.symbol.setTextColor(Color.BLACK);
            holder.testOrReal.setTextColor(Color.BLACK);
            holder.margin.setTextColor(Color.BLACK);
            holder.time.setTextColor(Color.BLACK);
            holder.longOrShort.setTextColor(Color.BLACK);
            holder.isolatedOrCrossed.setTextColor(Color.BLACK);
        } else {
            holder.testOrReal.setText("TEST");
            holder.testOrReal.setTextColor(testBlue);
            holder.margin.setTextColor(testBlue);
            holder.time.setTextColor(testBlue);
            holder.symbol.setTextColor(testBlue);
            holder.longOrShort.setTextColor(testBlue);
            holder.isolatedOrCrossed.setTextColor(testBlue);
        }

        if (isItShort == 1) {
            holder.longOrShort.setText("SHORT");
        } else {
            holder.longOrShort.setText("LONG");
        }

        if (isItCrossed == 1) {
            holder.isolatedOrCrossed.setText("CROSSED");
        } else {
            holder.isolatedOrCrossed.setText("ISOLATED");
        }

        if (currentPrice >= 100 || entryPrice >= 100) {
            holder.entryPrice.setText("EP: " + df3.format(entryPrice));
            holder.currentPrice.setText("CP: " + df3.format(currentPrice));
            holder.stopLimitPrice.setText("SL: " + df3.format(stopLimitPrice));
            holder.takeProfitPrice.setText("TP: " + df3.format(takeProfitPrice));
        } else {
            holder.entryPrice.setText("EP: " + df.format(entryPrice));
            holder.currentPrice.setText("CP: " + df.format(currentPrice));
            holder.stopLimitPrice.setText("SL: " + df.format(stopLimitPrice));
            holder.takeProfitPrice.setText("TP: " + df.format(takeProfitPrice));
        }

        holder.percentOfPriceChange.setText("PC: " + df2.format(percentOfPriceChange) + "%");
        holder.percentOfAmountChange.setText("AC: " + df2.format(percentOfAmountChange) + "%");
        holder.margin.setText("MARGIN: " + margin);

        @SuppressLint("SimpleDateFormat")
        DateFormat df4 = new SimpleDateFormat("MM.dd HH:mm");
        holder.time.setText(df4.format(time));

        holder.entryAmount.setText("EA: " + df2.format(entryAmount));
        holder.currentAmount.setText("CA: " + df2.format(currentAmount));
        holder.stopLimitPrice.setTextColor(darkRedColor);
        holder.takeProfitPrice.setTextColor(darkGreenColor);
        holder.currentPrice.setTextColor(Color.BLACK);
        holder.entryPrice.setTextColor(Color.BLACK);
        holder.currentAmount.setTextColor(Color.BLACK);
        holder.entryAmount.setTextColor(Color.BLACK);

        if (percentOfPriceChange > 0.1) {
            holder.percentOfPriceChange.setTextColor(darkGreenColor);
        } else if (percentOfPriceChange < -0.1) {
            holder.percentOfPriceChange.setTextColor(darkRedColor);
        } else {
            holder.percentOfPriceChange.setTextColor(testBlue);
        }

        if (percentOfAmountChange > 0.1 ) {
            holder.percentOfAmountChange.setTextColor(darkGreenColor);
        } else if (percentOfAmountChange < -0.1) {
            holder.percentOfAmountChange.setTextColor(darkRedColor);
        } else {
            holder.percentOfAmountChange.setTextColor(testBlue);
        }

        if (type.equals("STOP_MARKET")) {
            holder.testOrReal.setText("REAL-STOP");
            holder.symbol.setTextColor(darkRedColor);
            holder.entryAmount.setText("");
            holder.currentAmount.setText("");
            holder.entryPrice.setText("");
            holder.currentPrice.setText("");
            holder.percentOfPriceChange.setText("");
            holder.percentOfAmountChange.setText("");
            holder.takeProfitPrice.setText("");
            holder.stopLimitPrice.setText("");
            holder.margin.setText("");
            holder.stopLimitPrice.setTextColor(darkRedColor);
        } else if (type.equals("TAKE_PROFIT_MARKET")) {
            holder.testOrReal.setText("REAL-TAKE");
            holder.symbol.setTextColor(darkGreenColor);
            holder.entryAmount.setText("");
            holder.currentAmount.setText("");
            holder.entryPrice.setText("");
            holder.takeProfitPrice.setText("");
            holder.stopLimitPrice.setText("");
            holder.currentPrice.setText("");
            holder.percentOfPriceChange.setText("");
            holder.percentOfAmountChange.setText("");
            holder.margin.setText("");
            holder.stopLimitPrice.setTextColor(darkRedColor);
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void showDeleteConfirmationDialog(Context context, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Are you sure you want to close this position?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                DBHandler databaseDB = DBHandler.getInstance(builder.getContext());

                float balance = 0;
                if (items.get(position).getIsItReal() == 1) {

                    if(items.get(position).getOrderType().equals("TAKE_PROFIT_MARKET") || items.get(position).getOrderType().equals("STOP_MARKET")) {
                        getAllOrders(System.currentTimeMillis(),15000, context);
                        long orderId = items.get(position).getOrderID();
                        Log.e(TAG, String.valueOf(orderId));
                        deleteOrder(items.get(position).getSymbol(), orderId, System.currentTimeMillis(), 15000, context);
                    } else if (items.get(position).getOrderType().equals("MARKET")) {
                        //TO CANCEL ORDER IN REAL MARKET WE NEED TO DO OPPOSITE ACTION WITH SAME AMOUNT OF CRYPTO
                        String side = items.get(position).getIsItShort() == 1 ? "BUY" : "SELL";

                        //Opposite side when closing
                        boolean isItShortCancelOrder = items.get(position).getIsItShort() != 1;

                        setMarketOrderToCancelCurrentOrder(items.get(position).getSymbol(),side,"MARKET", "RESULT", items.get(position).getQunatity(), System.currentTimeMillis(),
                                10000, 0,0,0,0,0, isItShortCancelOrder,false, builder.getContext());

                    }

//                    deleteAllOrders(items.get(position).getSymbol(), System.currentTimeMillis(),15000, context);
                } else {
                    Cursor data = databaseDB.retrieveParam(items.get(position).getAccountNumber());
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr " + items.get(position).getAccountNumber() );
                    } else {
                        data.moveToFirst();
                        balance = data.getFloat(4);
                    }
                    data.close();
                }
                float newBalance = balance + items.get(position).getCurrentAmount();
                Log.e(TAG, "DELETED FOR ACCOUNT NR " + items.get(position).getAccountNumber() + " previousBalance: " + balance + " current balance: " + newBalance);
                databaseDB.deleteOrder(items.get(position).getSymbol(), items.get(position).getTimeWhenPlaced(), items.get(position).getIsItReal(), items.get(position).getIsItShort(), items.get(position).getMargin());
                databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, newBalance, ID, String.valueOf(items.get(position).getAccountNumber()));
                deleteItem(position);
            }
        });
        builder.setNegativeButton("NO", null);
        builder.show();
    }

    private void deleteOrder(String symbol, long orderId, long timestamp, long recvWindow, Context context) {

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 5, symbol, 0, "", "", "", "", "0", "0", "",
                        orderId, "", "0", recvWindow, timestamp)
                .getMyApi().deleteOrder(symbol, orderId, timestamp);

        Log.e(TAG, call.toString());
        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        RealOrder realOrder = response.body();
                        Log.e(TAG, "ORDER HAS BEEN CANCELED: " + realOrder.getClientOrderId() + " " + realOrder.getSymbol() + " " + realOrder.getStatus());
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
                    Log.e(TAG, "Error response: " + errorBody);
                }
            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e(TAG, String.valueOf(t));
            }

        });

    }

    private void getAllOrders(long timestamp, long recvWindow, Context context) {

        Call<List<JsonObject>> call = RetrofitClientSecretTestnet.getInstance(context, 8, "", 0, "", "", "", "", "0", "0", "",
                        0, "", "0", recvWindow, timestamp)
                .getMyApi().getAllOrders(timestamp);

        Log.e(TAG, call.toString());
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        List<JsonObject> responseBody = response.body();

                        Gson gson = new Gson();
                        List<RealOrder> allOrders = new ArrayList<>();

                        for (JsonObject jsonObject : responseBody) {
                            RealOrder order = gson.fromJson(jsonObject, RealOrder.class);
                            allOrders.add(order);
                        }
//                        // parse the response body to get the list of orders
//                        List<RealOrder> orders = new Gson().fromJson(responseBody, new TypeToken<List<RealOrder>>() {}.getType());

                        // print the order IDs
                        for (RealOrder order : allOrders) {
                            Log.e(TAG, "Order ID: " + order.getOrderId());
                            Log.e(TAG, "Client Order ID: " + order.getClientOrderId());
                        }

//                        RealOrders orders = response.body();
//                        List<RealOrder> orderList = orders.getOrders();
//
//                        for (int i = 0; i < orderList.size(); i++) {
//                            Log.e(TAG, orderList.get(i).getClientOrderId());
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
                    Log.e(TAG, "Error response: " + errorBody);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e(TAG, String.valueOf(t));
            }

        });

    }

    private void setMarketOrderToCancelCurrentOrder(String symbol, String side, String type, String newOrderRespType, float quantity, long timestamp, long recvWindow, float stopLimitPrice, float takeProfitPrice, float entryAmount, float currentPrice, int margin, boolean isItShort, boolean isItCrossed, Context context) {

        Log.e(TAG, "Start of setMarketOrderToCancelCurrentOrder " + symbol + " quantiity before: " + quantity );
        String quantityPrepared = formatFloatForSymbol(symbol,quantity,1);
        Log.e(TAG, "Start of setMarketOrderToCancelCurrentOrder " + symbol + " quantiity after: " + quantityPrepared );

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 3, symbol, 0, "", side, type, newOrderRespType, quantityPrepared,"0", "", 0, "", "0", recvWindow, timestamp)
                .getMyApi().setMarketOrder(symbol, side, type, newOrderRespType, quantityPrepared, timestamp);

        Log.e(TAG, call.toString());
        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        RealOrder realOrder = response.body();
                        Log.e(TAG, realOrder.getClientOrderId() + " " + realOrder.getSymbol() + " " + realOrder.getStatus() + " " + realOrder.getOrderId());

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
//                        OrderListViewElement toDB = new OrderListViewElement(symbol, 1, (float) entryAmount, currentPrice, currentPrice, stopLimitPrice, takeProfitPrice, timestamp, margin, isShort, isCrossed, 1, realOrder.getOrderId(), "MARKET",quantity);
//                        databaseDB.addNewOrder(toDB);

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
                    Log.e(TAG, "Error response: " + errorBody);
                }

            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e(TAG, String.valueOf(t));
            }

        });

    }

    private void deleteAllOrders(String symbol, long timestamp, long recvWindow, Context context) {

        Call<RealOrder> call = RetrofitClientSecretTestnet.getInstance(context, 7, symbol, 0, "", "", "", "", "0", "0", "",
                        0, "", "0", recvWindow, timestamp)
                .getMyApi().deleteAllOrders(symbol, timestamp);

        Log.e(TAG, call.toString());
        call.enqueue(new Callback<RealOrder>() {
            @Override
            public void onResponse(@NonNull Call<RealOrder> call, @NonNull Response<RealOrder> response) {

                if (response.body() != null) {
                    if (response.isSuccessful()) {
                        RealOrder realOrder = response.body();
                        Log.e(TAG, "ORDER HAS BEEN CANCELED: " + realOrder.getClientOrderId() + " " + realOrder.getSymbol() + " " + realOrder.getStatus());
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
                    Log.e(TAG, "Error response: " + errorBody);
                }
            }

            @Override
            public void onFailure(@NonNull Call<RealOrder> call, @NonNull Throwable t) {
                System.out.println("An error has occurred" + t);
                Log.e(TAG, String.valueOf(t));
            }

        });

    }

    //1 - for step, 0 for tick
    public String formatFloatForSymbol(String symbol, float value, int stepOrTick) {

        DecimalFormat format = null;

        // Find the symbol in the list of symbols
        Cursor data = databaseDB.retrieveSymbolInfo(symbol);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no symbol info for " + symbol);
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

            Log.e(TAG, "Decimal places: " + decimalPlaces + " pattern: " + pattern);
            // Create the decimal format using the pattern
            format = new DecimalFormat(pattern);

        }

        // Format the value using the decimal format
        return format != null ? format.format(value) : String.valueOf(value);
    }

    private int getDecimalPlaces(String value) {
        int index = value.indexOf(".");
        return index < 0 ? 0 : value.length() - index - 1;
    }


    private void deleteItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public void updateList(ArrayList<OrderListViewElement> newList) {
        items = newList;
        notifyDataSetChanged();
    }

}