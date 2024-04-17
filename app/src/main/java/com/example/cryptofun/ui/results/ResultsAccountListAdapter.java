package com.example.cryptofun.ui.results;

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
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.CardViewAccountResultsBinding;
import com.example.cryptofun.databinding.CardViewResultsBinding;
import com.example.cryptofun.services.CallbackButton;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ResultsAccountListAdapter extends RecyclerView.Adapter<ResultsAccountListAdapter.ViewHolder> implements CallbackButton {

    private static final String TAG = "ResultsAccountListAdapter";

    private ArrayList<ResultsAccountElement> items;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView accountNr;
        TextView nrOfAllOrders;
        TextView nrOfShortOrders;
        TextView nrOfLongOrders;
        TextView nrOfAllGoodOrders;
        TextView nrOfShortGoodOrders;
        TextView nrOfLongGoodOrders;
        TextView percentOfAllGoodOrders;
        TextView percentOfShortGoodOrders;
        TextView percentOfLongGoodOrders;
        TextView percentOf3d;
        TextView percentOf7d;
        TextView percentOf30d;
        TextView percentOfAllTime;

        public ViewHolder(CardViewAccountResultsBinding b) {
            super(b.getRoot());
            accountNr = b.tvAccountNr ;
            nrOfAllOrders = b.tvNrOfOrdersMade ;
            nrOfShortOrders = b.tvNrOfShortOrdersMade ;
            nrOfLongOrders = b.tvNrOfLongOrdersMade ;
            nrOfAllGoodOrders = b.tvNrOfGoodOrdersMade ;
            nrOfShortGoodOrders = b.tvNrOfGoodShortOrdersMade ;
            nrOfLongGoodOrders = b.tvNrOfGoodLongOrdersMade ;
            percentOfAllGoodOrders = b.tvPercentOfGoodOrders ;
            percentOfShortGoodOrders = b.tvPercentOfGoodShortOrders ;
            percentOfLongGoodOrders = b.tvPercentOfGoodLongOrders ;
            percentOf3d = b.tvResults3dPercents ;
            percentOf7d = b.tvResults7dPercents ;
            percentOf30d = b.tvResults30dPercents ;
            percentOfAllTime = b.tvResultsAllPercents ;
;
        }
    }

    //data is passed to constructor
    public ResultsAccountListAdapter(ArrayList<ResultsAccountElement> items) {
        // LayoutInflater inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @NonNull
    @Override
    //inflates the row layout from xml when needed
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardViewAccountResultsBinding binding = CardViewAccountResultsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }


    @SuppressLint("SetTextI18n")
    @Override
    //binds the data to elements in each row
    public void onBindViewHolder(@NonNull ResultsAccountListAdapter.ViewHolder holder, int position) {

        Context context = holder.itemView.getContext();
        int darkGreenColor = ContextCompat.getColor(context, R.color.dark_green);
        int darkRedColor = ContextCompat.getColor(context, R.color.dark_red);
        int testBlue = ContextCompat.getColor(context, R.color.dark_blue_test);
        DecimalFormat df = new DecimalFormat("0.0");
        
        if (items.get(position).getAccountNr().contains("REAL")) {
            holder.accountNr.setTextColor(Color.BLACK);
            holder.nrOfLongOrders.setTextColor(Color.BLACK);
            holder.nrOfShortOrders.setTextColor(Color.BLACK);
            holder.nrOfAllOrders.setTextColor(Color.BLACK);
            holder.nrOfLongGoodOrders.setTextColor(Color.BLACK);
            holder.nrOfShortGoodOrders.setTextColor(Color.BLACK);
            holder.nrOfAllGoodOrders.setTextColor(Color.BLACK);
        } else {
            holder.accountNr.setTextColor(testBlue);
            holder.nrOfLongOrders.setTextColor(testBlue);
            holder.nrOfShortOrders.setTextColor(testBlue);
            holder.nrOfAllOrders.setTextColor(testBlue);
            holder.nrOfLongGoodOrders.setTextColor(testBlue);
            holder.nrOfShortGoodOrders.setTextColor(testBlue);
            holder.nrOfAllGoodOrders.setTextColor(testBlue);
        }
        
        holder.accountNr.setText(items.get(position).getAccountNr());
        holder.nrOfLongOrders.setText("GOOD LONGS: " + items.get(position).getNrOfLongGoodOrders() + " of " + items.get(position).getNrOfLongOrders());
        holder.nrOfShortOrders.setText("GOOD SHORTS: " + items.get(position).getNrOfShortGoodOrders() + " of " + items.get(position).getNrOfShortOrders());
        holder.nrOfAllOrders.setText("GOOD ALL: " + items.get(position).getNrOfAllGoodOrders() + " of " + items.get(position).getNrOfAllOrders());

        holder.percentOfAllGoodOrders.setText("% of good: " + df.format(items.get(position).getPercentOfAllGoodOrders()));
        if (items.get(position).getPercentOfAllGoodOrders() >= 50) {
            holder.percentOfAllGoodOrders.setTextColor(darkGreenColor);
        } else {
            holder.percentOfAllGoodOrders.setTextColor(darkRedColor);
        }

        holder.percentOfShortGoodOrders.setText("% of good: " +df.format(items.get(position).getPercentOfShortGoodOrders()));
        if (items.get(position).getPercentOfShortGoodOrders() >= 50) {
            holder.percentOfShortGoodOrders.setTextColor(darkGreenColor);
        } else {
            holder.percentOfShortGoodOrders.setTextColor(darkRedColor);
        }

        holder.percentOfLongGoodOrders.setText("% of good: " + df.format(items.get(position).getPercentOfLongGoodOrders()));
        if (items.get(position).getPercentOfLongGoodOrders() >= 50) {
            holder.percentOfLongGoodOrders.setTextColor(darkGreenColor);
        } else {
            holder.percentOfLongGoodOrders.setTextColor(darkRedColor);
        }

        holder.percentOf3d.setText("Money earned (3d): " + df.format(items.get(position).getPercentOf3d()));
        if (items.get(position).getPercentOf3d() >= 0) {
            holder.percentOf3d.setTextColor(darkGreenColor);
        } else {
            holder.percentOf3d.setTextColor(darkRedColor);
        }

        holder.percentOf7d.setText("Money earned (7d): " +df.format(items.get(position).getPercentOf7d()));
        if (items.get(position).getPercentOf7d() >= 0) {
            holder.percentOf7d.setTextColor(darkGreenColor);
        } else {
            holder.percentOf7d.setTextColor(darkRedColor);
        }

        holder.percentOf30d.setText("Money earned (30d): " +df.format(items.get(position).getPercentOf30d()));
        if (items.get(position).getPercentOf30d() >= 0) {
            holder.percentOf30d.setTextColor(darkGreenColor);
        } else {
            holder.percentOf30d.setTextColor(darkRedColor);
        }

        holder.percentOfAllTime.setText("Money earned (all): " + df.format(items.get(position).getPercentOfAllTime()));
        if (items.get(position).getPercentOfAllTime() >= 0) {
            holder.percentOfAllTime.setTextColor(darkGreenColor);
        } else {
            holder.percentOfAllTime.setTextColor(darkRedColor);
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

    public void updateList(ArrayList<ResultsAccountElement> newList) {
        items = newList;
        notifyDataSetChanged();
    }

}