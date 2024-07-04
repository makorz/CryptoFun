package app.makorz.cryptofun.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.databinding.ListViewCryptoBinding;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class CryptoListAdapter extends RecyclerView.Adapter<CryptoListAdapter.ViewHolder> {

    private ArrayList<ListViewElement> items;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView title;
        TextView percent;
        TextView percent2;
        TextView longOrShort;
        TextView firstPrice;
        TextView time;

        public ViewHolder(ListViewCryptoBinding b) {
            super(b.getRoot());
            cardView = b.element;
            title = b.text;
            percent = b.textPercent;
            percent2 = b.textPercent2;
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

        holder.percent.setTextColor(Color.BLACK);

        String symbol = items.get(position).getText();
        float percentChange = items.get(position).getPercentChange();
        float percentChange2 = items.get(position).getPercentChange2();
        float priceWhenCaught = items.get(position).getPriceWhenCaught();
        String time = items.get(position).getTime();
        boolean isItLong = items.get(position).isItLONG();

        LocalTime currentTime = LocalTime.now();
        // Convert the string value to LocalTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime comparisonTime = LocalTime.parse(time, formatter);
        // Check if the comparisonTime is within 30 minutes before the currentTime
        long minutesDifference = ChronoUnit.MINUTES.between(comparisonTime, currentTime);
        // Adjust the difference to consider the possibility of comparisonTime being later in the next day
        if (minutesDifference < 0) {
            minutesDifference += 24 * 60; // Add 24 hours in minutes
        }

        Log.e("RECYCLER", symbol + " ComparisonTime " + comparisonTime + " Current Time: " + currentTime + " difference: " + minutesDifference);

        if (minutesDifference >= 0 && minutesDifference <= 60) {
            holder.cardView.setCardBackgroundColor(Color.YELLOW);
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.light_grey));
        }


        DecimalFormat dfNr = new DecimalFormat("0.00");
        DecimalFormat dfPr = new DecimalFormat("0.00000");
        int darkGreenColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.dark_green);
        holder.title.setText(symbol);
        if (symbol.contains("Nothing")) {
            holder.percent.setTextColor(Color.BLACK);
            holder.longOrShort.setTextColor(Color.BLACK);
        } else {
            if (isItLong) {
                holder.longOrShort.setText("LN");
                if (percentChange > 0) {
                    holder.percent.setTextColor(darkGreenColor);
                    holder.longOrShort.setTextColor(darkGreenColor);
                } else {
                    holder.percent.setTextColor(Color.BLUE);
                    holder.longOrShort.setTextColor(darkGreenColor);
                }
                if (percentChange2 > 0) {
                    holder.percent2.setTextColor(darkGreenColor);
                } else {
                    holder.percent2.setTextColor(Color.BLUE);
                }
            } else {
                holder.longOrShort.setText("SH");
                if (percentChange < 0) {
                    holder.percent.setTextColor(Color.RED);
                    holder.longOrShort.setTextColor(Color.RED);
                } else {
                    holder.percent.setTextColor(Color.BLUE);
                    holder.longOrShort.setTextColor(Color.RED);
                }
                if (percentChange2 < 0) {
                    holder.percent2.setTextColor(Color.RED);
                } else {
                    holder.percent2.setTextColor(Color.BLUE);
                }

            }

            if (minutesDifference >= 0 && minutesDifference <= 3) {
                holder.percent.setText("---");
                holder.percent2.setText("---");
            } else {
                holder.percent.setText(dfNr.format(percentChange) + "%");
                holder.percent2.setText(dfNr.format(percentChange2) + "%");
            }



            if (priceWhenCaught > 10) {
                holder.firstPrice.setText(dfNr.format(priceWhenCaught));
            } else {
                holder.firstPrice.setText(dfPr.format(priceWhenCaught));
            }

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