package app.makorz.cryptofun.ui.home;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.databinding.ListViewSimpleBinding;

import java.util.ArrayList;

public class SimpleListAdapter extends RecyclerView.Adapter<SimpleListAdapter.ViewHolder> {

    private ArrayList<ListViewElement> items;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;


        public ViewHolder(ListViewSimpleBinding b) {
            super(b.getRoot());
            title = b.text;

        }
    }

    //data is passed to constructor
    public SimpleListAdapter(ArrayList<ListViewElement> items) {
        // LayoutInflater inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @NonNull
    @Override
    //inflates the row layout from xml when needed
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ListViewSimpleBinding binding = ListViewSimpleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }


    @SuppressLint("SetTextI18n")
    @Override
    //binds the data to elements in each row
    public void onBindViewHolder(@NonNull SimpleListAdapter.ViewHolder holder, int position) {
        String symbol = items.get(position).getText();
        holder.title.setText(symbol);
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