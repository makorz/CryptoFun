package com.example.cryptofun.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.databinding.CardViewCryptoBinding;
import com.example.cryptofun.ui.view.GridViewElement;

import java.text.DecimalFormat;
import java.util.List;

public class CryptoGridAdapter extends RecyclerView.Adapter<CryptoGridAdapter.ViewHolder>{

    private final List<GridViewElement> items;

    public static class ViewHolder extends RecyclerView.ViewHolder{

        TextView title;
        TextView value;
        TextView percent;

        public ViewHolder(CardViewCryptoBinding b){
            super(b.getRoot());
            title = b.text;
            value = b.value;
            percent = b.percent;
        }
    }

    //data is passed to constructor
    CryptoGridAdapter(List<GridViewElement> items){
       // LayoutInflater inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @NonNull
    @Override
    //inflates the row layout from xml when needed
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){

        CardViewCryptoBinding binding = CardViewCryptoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }


    @Override
    //binds the data to elements in each row
    public void onBindViewHolder(@NonNull CryptoGridAdapter.ViewHolder holder, int position){
        String name = items.get(position).getSymbol();
        float value = items.get(position).getValue();
        float percent = items.get(position).getPercent();

        if ( !name.contains("%")) {
            DecimalFormat df = new DecimalFormat("#.###");
            DecimalFormat df2 = new DecimalFormat("#.#");

            holder.title.setText(name);
            String valueFormatted = df.format(value) + "$";
            holder.value.setText(valueFormatted);
            String percentFormatted = df2.format(percent) + "%";
            holder.percent.setText(percentFormatted);
            if (items.get(position).getPercent() < 0) {
                holder.percent.setTextColor(Color.RED);
            } else {
                holder.percent.setTextColor(Color.GREEN);
            }
        } else {
            DecimalFormat df = new DecimalFormat("#.#");
            holder.title.setText(name);
            String percentFormatted = df.format(percent) + "%";
            holder.percent.setText(percentFormatted);
            holder.percent.setTextColor(Color.BLUE);
            float allCryptoCounted = (items.get(position).getValue() * 100) / items.get(position).getPercent();
            String value2 = (int) items.get(position).getValue() + "/" + Math.round(allCryptoCounted);
            holder.value.setText(value2);
        }
        holder.value.setTextColor(Color.GRAY);
        holder.title.setTextColor(Color.BLACK);

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

}