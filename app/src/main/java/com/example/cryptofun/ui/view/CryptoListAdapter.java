package com.example.cryptofun.ui.view;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.databinding.ListViewCryptoBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class CryptoListAdapter extends RecyclerView.Adapter<CryptoListAdapter.ViewHolder> {

    private ArrayList<ListViewElement> items;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView percent;
        TextView longOrShort;
        TextView firstPrice;
        TextView time;

        public ViewHolder(ListViewCryptoBinding b) {
            super(b.getRoot());
            title = b.text;
            percent = b.textPercent;
            longOrShort = b.textLongOrShort;
            firstPrice = b.textPrice;
            time = b.textTime;
        }
    }

    //data is passed to constructor
    public CryptoListAdapter(ArrayList<ListViewElement> items) {
        // LayoutInflater inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @NonNull
    @Override
    //inflates the row layout from xml when needed
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ListViewCryptoBinding binding = ListViewCryptoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }


    @SuppressLint("SetTextI18n")
    @Override
    //binds the data to elements in each row
    public void onBindViewHolder(@NonNull CryptoListAdapter.ViewHolder holder, int position) {

        String symbol = items.get(position).getText();
        float percentChange = items.get(position).getPercentChange();
        float priceWhenCaught = items.get(position).getPriceWhenCaught();
        String time = items.get(position).getTime();
        boolean isItLong = items.get(position).isItLONG();

        DecimalFormat dfNr = new DecimalFormat("0.##");
        DecimalFormat dfPr = new DecimalFormat("#.######");
        holder.title.setText(symbol);
        if (symbol.contains("Nothing")) {

            holder.percent.setTextColor(Color.BLACK);
            holder.longOrShort.setTextColor(Color.BLACK);
        } else {
            if (isItLong) {
                holder.longOrShort.setText("LONG");
                holder.percent.setTextColor(Color.GREEN);
                holder.longOrShort.setTextColor(Color.GREEN);
            } else {
                holder.longOrShort.setText("SHORT");
                holder.percent.setTextColor(Color.RED);
                holder.longOrShort.setTextColor(Color.RED);
            }
            holder.percent.setText(dfNr.format(percentChange) + "%");
            holder.firstPrice.setText(dfPr.format(priceWhenCaught));
            holder.time.setText(time);
        }


    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(ArrayList<ListViewElement> newList) {
        items = newList;
        notifyDataSetChanged();
    }

}