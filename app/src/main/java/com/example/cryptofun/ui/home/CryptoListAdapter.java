package com.example.cryptofun.ui.home;


import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.cryptofun.databinding.ListViewCryptoBinding;
import com.example.cryptofun.ui.view.ListViewElement;

import java.util.List;

public class CryptoListAdapter extends RecyclerView.Adapter<CryptoListAdapter.ViewHolder>{

    private final List<ListViewElement> items;

    public static class ViewHolder extends RecyclerView.ViewHolder{

        TextView title;

        public ViewHolder(ListViewCryptoBinding b){
            super(b.getRoot());
            title = b.text;
        }
    }

    //data is passed to constructor
    CryptoListAdapter(List<ListViewElement> items){
       // LayoutInflater inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @NonNull
    @Override
    //inflates the row layout from xml when needed
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){

        ListViewCryptoBinding binding = ListViewCryptoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }


    @Override
    //binds the data to elements in each row
    public void onBindViewHolder(@NonNull CryptoListAdapter.ViewHolder holder, int position){
        String name = items.get(position).getText();
        holder.title.setText(name);

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

}