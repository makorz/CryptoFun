package app.makorz.cryptofun.ui.results;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.databinding.CardViewResultsBinding;
import app.makorz.cryptofun.services.CallbackButton;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ResultsListAdapter extends RecyclerView.Adapter<ResultsListAdapter.ViewHolder> implements CallbackButton {

    private static final String TAG = "ResultsListAdapter";

    private ArrayList<ResultsListElement> items;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView symbol;
        TextView entryPrice;
        TextView closePrice;
        TextView percentChanged;
        TextView accountNr;
        TextView margin;
        TextView moneyEarned;
        TextView entryAmount;
        TextView exitAmount;
        TextView percentOfMoneyChange;
        TextView timeEntry;
        TextView timeExit;
        TextView testOrReal;
        TextView longOrShort;

        public ViewHolder(CardViewResultsBinding b) {
            super(b.getRoot());
            symbol = b.tvSymbol;
            entryPrice = b.tvEntryPrice;
            closePrice = b.tvClosePrice;
            percentChanged = b.tvPercentChanged;
            accountNr = b.tvAccountNr;
            margin = b.tvMargin;
            moneyEarned = b.tvMoneyEarned;
            entryAmount = b.tvEntryAmount;
            exitAmount = b.tvExitAmount;
            percentOfMoneyChange = b.tvContractAmountChange;
            timeEntry = b.tvEntryDate;
            timeExit = b.tvCloseDate;
            longOrShort = b.tvLongOrShort;
            testOrReal = b.tvTestOrReal;
        }
    }

    //data is passed to constructor
    public ResultsListAdapter(ArrayList<ResultsListElement> items) {
        // LayoutInflater inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @NonNull
    @Override
    //inflates the row layout from xml when needed
    public ResultsListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //CardViewOrder2Binding binding = CardViewOrder2Binding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        CardViewResultsBinding binding = CardViewResultsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ResultsListAdapter.ViewHolder(binding);
    }


    @SuppressLint("SetTextI18n")
    @Override
    //binds the data to elements in each row
    public void onBindViewHolder(@NonNull ResultsListAdapter.ViewHolder holder, int position) {

        Context context = holder.itemView.getContext();
        int darkGreenColor = ContextCompat.getColor(context, R.color.dark_green);
        int darkRedColor = ContextCompat.getColor(context, R.color.dark_red);
        int testBlue = ContextCompat.getColor(context, R.color.dark_blue_test);
        DecimalFormat df = new DecimalFormat("0.0000");
        DecimalFormat df2 = new DecimalFormat("0.00");
        DecimalFormat df3 = new DecimalFormat("0.0");

        String symbol = items.get(position).getSymbol();
        int isItReal = items.get(position).getIsItReal();
        int isItShort = items.get(position).getIsItShort();
        int accountNr = items.get(position).getAccountNr();
        int margin = items.get(position).getMargin();
        float entryPrice = items.get(position).getEntryPrice();
        float closePrice = items.get(position).getClosePrice();
        float percentChanged = items.get(position).getPercentChanged();
        float moneyEarned = items.get(position).getMoneyEarned();
        long timeEntry = items.get(position).getTimeEntry();
        long timeExit = items.get(position).getTimeExit();
        float entryAmount = items.get(position).getEntryAmount();
        float exitAmount = items.get(position).getExitAmount();
        float percentOfMoneyChange = items.get(position).getPercentOfMoneyChanged();

        holder.symbol.setText(symbol);
        holder.margin.setText("MARGIN: " + margin);

        if (accountNr == 2) {
            holder.accountNr.setText("ACCOUNT MANUAL");
        } else if (accountNr >= 6) {
            holder.accountNr.setText("ACCOUNT NR: " + (accountNr - 5));
        } else {
            holder.accountNr.setText("ACCOUNT REAL");
        }

        if (isItReal == 1) {
            holder.testOrReal.setText("REAL");
            holder.symbol.setTextColor(Color.BLACK);
            holder.testOrReal.setTextColor(Color.BLACK);
            holder.margin.setTextColor(Color.BLACK);
            holder.timeEntry.setTextColor(Color.BLACK);
            holder.longOrShort.setTextColor(Color.BLACK);
            holder.timeExit.setTextColor(Color.BLACK);
            holder.accountNr.setTextColor(Color.BLACK);
            holder.entryAmount.setTextColor(Color.BLACK);
            holder.exitAmount.setTextColor(Color.BLACK);
            holder.entryPrice.setTextColor(Color.BLACK);
            holder.closePrice.setTextColor(Color.BLACK);

        } else {
            holder.testOrReal.setText("TEST");
            holder.testOrReal.setTextColor(testBlue);
            holder.margin.setTextColor(testBlue);
            holder.timeEntry.setTextColor(testBlue);
            holder.symbol.setTextColor(testBlue);
            holder.longOrShort.setTextColor(testBlue);
            holder.timeExit.setTextColor(testBlue);
            holder.accountNr.setTextColor(testBlue);
            holder.entryAmount.setTextColor(testBlue);
            holder.exitAmount.setTextColor(testBlue);
            holder.entryPrice.setTextColor(testBlue);
            holder.closePrice.setTextColor(testBlue);
        }

        if (isItShort == 1) {
            holder.longOrShort.setText("SHORT");
        } else {
            holder.longOrShort.setText("LONG");
        }

        if (closePrice >= 100 || entryPrice >= 100) {
            holder.entryPrice.setText("EP: " + df3.format(entryPrice));
            holder.closePrice.setText("CP: " + df3.format(closePrice));
        } else {
            holder.entryPrice.setText("EP: " + df.format(entryPrice));
            holder.closePrice.setText("CP: " + df.format(closePrice));
        }

        @SuppressLint("SimpleDateFormat")
        DateFormat df4 = new SimpleDateFormat("dd.MM HH:mm");
        holder.timeEntry.setText(df4.format(timeEntry));
        holder.timeExit.setText(df4.format(timeExit));

        holder.entryAmount.setText("E$: " + df2.format(entryAmount));
        holder.exitAmount.setText("C$: " + df2.format(exitAmount));

        holder.percentChanged.setText("PC: " + df2.format(percentChanged) + "%");
        if (percentChanged >= 0) {
            holder.percentChanged.setTextColor(darkGreenColor);
        } else {
            holder.percentChanged.setTextColor(darkRedColor);
        }

        holder.percentOfMoneyChange.setText("$C: " + df2.format(percentOfMoneyChange) + "%");
        if (percentOfMoneyChange >= 0) {
            holder.percentOfMoneyChange.setTextColor(darkGreenColor);
        } else {
            holder.percentOfMoneyChange.setTextColor(darkRedColor);
        }

        holder.moneyEarned.setText("$$$: " + df2.format(moneyEarned));
        if (moneyEarned >= 0) {
            holder.percentOfMoneyChange.setTextColor(darkGreenColor);
        } else {
            holder.percentOfMoneyChange.setTextColor(darkRedColor);
        }


    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void onSuccess() {
    }

    @Override
    public void onError() {
        Log.e(TAG, "ERROR RECEIVED from CALLBACK");
    }

    private void deleteItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    public void updateList(ArrayList<ResultsListElement> newList) {
        items = newList;
        notifyDataSetChanged();
    }

}