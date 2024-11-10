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
import com.example.cryptofun.databinding.CardViewApprovedBinding;
import com.example.cryptofun.databinding.CardViewOrderBinding;
import com.example.cryptofun.databinding.ListViewCryptoBinding;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;

public class CryptoApprovedAdapter extends RecyclerView.Adapter<CryptoApprovedAdapter.ViewHolder> {

    private ArrayList<ListViewElement> items;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView title;
        TextView strategy;
        TextView percent;
        TextView percent2;
        TextView longOrShort;
        TextView firstPrice;
        TextView time;
        TextView date;

        public ViewHolder(CardViewApprovedBinding b) {
            super(b.getRoot());
            cardView = b.element;
            title = b.text;
            percent = b.textPercent;
            strategy = b.textStrategy;
            percent2 = b.textPercent2;
            longOrShort = b.textLongOrShort;
            firstPrice = b.textPrice;
            time = b.textTime;
            date = b.textDate;
        }
    }

    //data is passed to constructor
    public CryptoApprovedAdapter(ArrayList<ListViewElement> items) {
        // LayoutInflater inflater = LayoutInflater.from(context);
        this.items = items;
    }

    @NonNull
    @Override
    //inflates the row layout from xml when needed
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        CardViewApprovedBinding binding = CardViewApprovedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }


    @SuppressLint("SetTextI18n")
    @Override
    //binds the data to elements in each row
    public void onBindViewHolder(@NonNull CryptoApprovedAdapter.ViewHolder holder, int position) {

        holder.percent.setTextColor(Color.BLACK);

        String symbol = items.get(position).getText();
        String strategy = "Strategy: " + items.get(position).getStrategyNr();

        float percentChange = items.get(position).getPercentChange();
        float percentChange2 = items.get(position).getPercentChange2();
        float priceWhenCaught = items.get(position).getPriceWhenCaught();
        String time = items.get(position).getTime();
        String hour = time.substring(time.length() - 5);
        boolean isItLong = items.get(position).isItLONG();

        // Define the formatter for the new pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
        // Parse the time string to a LocalDateTime object
        LocalDateTime comparisonDateTime = LocalDateTime.parse(time, formatter);
        // Get the current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();
        // Calculate the difference in minutes
        long minutesDifference = ChronoUnit.MINUTES.between(comparisonDateTime, currentDateTime);

        // Adjust for the possibility that comparisonDateTime is from a previous or future day
        if (minutesDifference < 0) {
            minutesDifference += 24 * 60; // Add 24 hours in minutes
        }

        // Extract day and month values
        int day = comparisonDateTime.getDayOfMonth();
        String monthName = comparisonDateTime.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
        // Convert the day to ordinal form (1st, 2nd, 3rd, etc.)
        String dayWithSuffix = getDayWithSuffix(day);
        // Format the output as "4th of July"
        String readableDate = dayWithSuffix + " of " + monthName;

        Log.e("RECYCLER", symbol + " ComparisonTime " + comparisonDateTime + " Current Time: " + currentDateTime + " difference: " + minutesDifference);

        if (minutesDifference >= 0 && minutesDifference <= 60) {
            holder.cardView.setCardBackgroundColor(Color.YELLOW);
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.light_grey));
        }


        DecimalFormat dfNr = new DecimalFormat("0.00");
        DecimalFormat dfPr = new DecimalFormat("0.00000");
        int darkGreenColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.dark_green);
        holder.title.setText(symbol);
        holder.strategy.setText(strategy);
        holder.date.setText(readableDate);
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

            holder.time.setText(hour);
        }


    }


    @Override
    public int getItemCount() {
        return items.size();
    }


    // Helper method to add ordinal suffix to the day
    private static String getDayWithSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        switch (day % 10) {
            case 1: return day + "st";
            case 2: return day + "nd";
            case 3: return day + "rd";
            default: return day + "th";
        }
    }

    public void updateList(ArrayList<ListViewElement> newList) {
        items = newList;
        notifyDataSetChanged();
    }

}