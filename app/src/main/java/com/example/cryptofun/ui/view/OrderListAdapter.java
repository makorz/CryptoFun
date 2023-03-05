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
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.databinding.CardViewOrderBinding;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.ViewHolder> {

    private ArrayList<OrderListViewElement> items;

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
                    Log.e(TAG, "Real not yet");
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

    private void deleteItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public void updateList(ArrayList<OrderListViewElement> newList) {
        items = newList;
        notifyDataSetChanged();
    }

}