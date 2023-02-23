package com.example.cryptofun.ui.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.databinding.CardViewOrderBinding;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.ViewHolder>{

    private ArrayList<OrderListViewElement> items;

    public static class ViewHolder extends RecyclerView.ViewHolder{

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

        public ViewHolder(CardViewOrderBinding b){
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

        }
    }

    //data is passed to constructor
    public OrderListAdapter(ArrayList<OrderListViewElement> items){
       // LayoutInflater inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @NonNull
    @Override
    //inflates the row layout from xml when needed
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        CardViewOrderBinding binding = CardViewOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }


    @Override
    //binds the data to elements in each row
    public void onBindViewHolder(@NonNull OrderListAdapter.ViewHolder holder, int position){

        Context context = holder.itemView.getContext();
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showDeleteConfirmationDialog(context, holder.getAdapterPosition());
                notifyDataSetChanged();
                return true;
            }
        });

       //holder.itemView.setBackgroundColor(Color.parseColor("#f5f5f5"));

        DecimalFormat df = new DecimalFormat("#.####");
        DecimalFormat df2 = new DecimalFormat("#.00");
        String symbol = items.get(position).getSymbol();
        boolean isItReal = items.get(position).isItReal();
        float entryPrice = items.get(position).getEntryPrice();
        float currentPrice = items.get(position).getCurrentPrice();
        float stopLimitPrice = items.get(position).getStopLimitPrice();
        float takeProfitPrice = items.get(position).getTakeProfitPrice();
        float percentOfPriceChange = items.get(position).getPercentOfPriceChange();
        float percentOfAmountChange = items.get(position).getPercentOfAmountChange();
        float entryAmount = items.get(position).getEntryAmount();
        float currentAmount = items.get(position).getCurrentAmount();
        int margin = items.get(position).getMargin();
        String time = items.get(position).getTimeWhenPlaced();

        holder.symbol.setText(symbol);
        if (isItReal) {
            holder.testOrReal.setText("REAL");
            holder.symbol.setText(symbol);
            holder.symbol.setTextColor(Color.parseColor("#37474f"));
        } else {
            holder.testOrReal.setText("TEST");
            holder.symbol.setTextColor(Color.parseColor("#78909c"));
        }

        holder.entryPrice.setText("EP: " + df.format(entryPrice));
        holder.currentPrice.setText("CP: " + df.format(currentPrice));
        holder.entryAmount.setText("EA: " + df2.format(entryAmount) + "$");
        holder.currentAmount.setText("CA: " + df2.format(currentAmount) + "$");

        holder.stopLimitPrice.setText("SL: " + df.format(stopLimitPrice));
        holder.takeProfitPrice.setText("TP: " + df.format(takeProfitPrice));
        holder.percentOfPriceChange.setText("PC: " + df2.format(percentOfPriceChange) + "%");
        holder.percentOfAmountChange.setText("AC: " + df2.format(percentOfAmountChange) + "%");
        holder.margin.setText("MARG: " + margin);
        holder.time.setText(time);

        holder.stopLimitPrice.setTextColor(Color.parseColor("#ef5350"));
        holder.takeProfitPrice.setTextColor(Color.parseColor("#81c784"));
        holder.currentPrice.setTextColor(Color.BLACK);
        holder.entryPrice.setTextColor(Color.BLACK);
        holder.currentAmount.setTextColor(Color.BLACK);
        holder.entryAmount.setTextColor(Color.BLACK);

        if (percentOfPriceChange > 0) {
            holder.percentOfPriceChange.setTextColor(Color.GREEN);
        } else if (percentOfPriceChange < 0) {
            holder.percentOfPriceChange.setTextColor(Color.RED);
        }

        if (percentOfAmountChange > 0) {
            holder.percentOfAmountChange.setTextColor(Color.GREEN);
        } else if (percentOfPriceChange < 0) {
            holder.percentOfAmountChange.setTextColor(Color.RED);
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