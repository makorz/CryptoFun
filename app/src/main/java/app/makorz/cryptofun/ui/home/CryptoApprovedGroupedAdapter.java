package app.makorz.cryptofun.ui.home;

import static app.makorz.cryptofun.ui.home.HomeFragment.strategyNames;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.databinding.CardViewApprovedBinding;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.makorz.cryptofun.data.ApprovedGroupedTokens;

public class CryptoApprovedGroupedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private static final int VIEW_TYPE_GROUP = 0;
    private static final int VIEW_TYPE_ITEM  = 1;

    private ArrayList<ApprovedGroupedTokens> groupedItems = new ArrayList<>();


    public CryptoApprovedGroupedAdapter(ArrayList<ApprovedGroupedTokens> groupedItems) {
        this.groupedItems = groupedItems;
    }

    @Override
    public int getItemViewType(int position) {
        int itemPosition = position;
        for (ApprovedGroupedTokens group : groupedItems) {
            if (itemPosition == 0) return VIEW_TYPE_GROUP; // Group header
            itemPosition--;

            if (group.isExpanded()) {
                int itemCount = group.getTokens().size();
                if (itemPosition < itemCount) return VIEW_TYPE_ITEM; // Child item
                itemPosition -= itemCount;
            }
        }
        throw new IllegalStateException("Unknown item type at position " + position);
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_GROUP) {
            // Inflate group layout
            View groupHeaderView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_aproved_group, parent, false);
            return new GroupViewHolder(groupHeaderView);
        } else {
            // Inflate item layout
            CardViewApprovedBinding binding = CardViewApprovedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ItemViewHolder(binding);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int itemPosition = position;
        for (int i = 0; i < groupedItems.size(); i++) {
            ApprovedGroupedTokens group = groupedItems.get(i);
            if (itemPosition == 0) {
                ((GroupViewHolder) holder).bind(group, i); // Pass index to identify the group
                return;
            }
            itemPosition--;

            if (group.isExpanded()) {
                int itemCount = group.getTokens().size();
                if (itemPosition < itemCount) {
                    ((ItemViewHolder) holder).bind(group.getTokens().get(itemPosition));
                    return;
                }
                itemPosition -= itemCount;
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for (ApprovedGroupedTokens group : groupedItems) {
            count++; // Group header
            if (group.isExpanded()) count += group.getTokens().size(); // Child items
        }
        return count;
    }

    public class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView strategyText;
        LinearLayout groupHeaderLayout;

        public GroupViewHolder(View itemView) {
            super(itemView);
            strategyText = itemView.findViewById(R.id.textStrategyHeader);
            groupHeaderLayout = itemView.findViewById(R.id.groupHeaderLayout);
        }

        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        public void bind(ApprovedGroupedTokens group, int groupIndex) {

            String strategyName = strategyNames.getOrDefault(group.getStrategy(), "Unknown Strategy");
            int itemCount = group.getTokens().size();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
            LocalDateTime currentDateTime = LocalDateTime.now();

            Log.e("RECYCLER", "START: " + group.getStrategy());
            // Check if any item in the group has a time within 60 minutes and number of good items
            boolean highlightGroup = false;
            int goodItemsCount = 0;
            int veryGoodItemsCount = 0;
            int averageGoodItemsCount = 0;
            for (ListViewElement item : group.getTokens()) {
                // Time condition
                LocalDateTime itemTime = LocalDateTime.parse(item.getTime(), formatter);
                long minutesDifference = ChronoUnit.MINUTES.between(itemTime, currentDateTime);
                if (minutesDifference >= 0 && minutesDifference <= 60) {
                    highlightGroup = true;
                    }
                Log.e("RECYCLER", "percent1: " + item.getPercentChange() + " percent2: " + item.getPercentChange2() + " strategy: " + item.getStrategyNr());

                // Average good item condition
                if (item.getPercentChange() > 1.5 && item.getPercentChange2() > -4 && item.isItLONG()) {
                    averageGoodItemsCount++; // Count the good items
                } else if (item.getPercentChange() < -1.5 && item.getPercentChange2() < 4 && !item.isItLONG()) {
                    averageGoodItemsCount++; // Count the good items
                }

                // Good item condition
                if (item.getPercentChange() > 2 && item.getPercentChange2() > -2.5 && item.isItLONG()) {
                    goodItemsCount++; // Count the good items
                } else if (item.getPercentChange() < -2 && item.getPercentChange2() < 2.5 && !item.isItLONG()) {
                    goodItemsCount++; // Count the good items
                }

                // Very good item condition
                if (item.getPercentChange() > 5 && item.getPercentChange2() > -2 && item.isItLONG()) {
                    veryGoodItemsCount++; // Count the good items
                } else if (item.getPercentChange() < -5 && item.getPercentChange2() < 2 && !item.isItLONG()) {
                    veryGoodItemsCount++; // Count the good items
                }
            }

            // Calculate percentage of good items
            float goodItemsPercentage = 0;
            if (goodItemsCount > 0) {
                goodItemsPercentage = (goodItemsCount / (float) itemCount) * 100;
            }
            float veryGoodItemsPercentage = 0;
            if (veryGoodItemsCount > 0) {
                veryGoodItemsPercentage = (veryGoodItemsCount / (float) itemCount) * 100;
            }
            float averageGoodItemsPercentage = 0;
            if (averageGoodItemsCount > 0) {
                averageGoodItemsPercentage = (averageGoodItemsCount / (float) itemCount) * 100;
            }

            Log.e("RECYCLER", "goodPercent: " + goodItemsPercentage + " goodItems: " + goodItemsCount + " items: " +itemCount + "strategy: " + group.getStrategy());

            strategyText.setText(group.getStrategy() + ". " + strategyName + "\n Tokens nr: [" + itemCount + "]\nMID: [" + String.format("%.1f%%", averageGoodItemsPercentage) +
                            "] --- GOOD: [" + String.format("%.1f%%", goodItemsPercentage) + "] --- GREAT: [" + String.format("%.1f%%", veryGoodItemsPercentage) +"]");

            // Set the background color based on the condition
            if (highlightGroup) {
                groupHeaderLayout.setBackgroundColor(Color.YELLOW); // Highlight color
            } else {
                groupHeaderLayout.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.light_grey)); // Default color
            }
            Log.e("RECYCLER", "STOP: " + group.getStrategy());

            strategyText.setOnClickListener(v -> {
                // Toggle expansion based on groupIndex
                Log.e("RECYCLER", "STOP2: " + group.getStrategy());
                group.setExpanded(!group.isExpanded());
                notifyDataSetChanged(); // Refresh RecyclerView after expanding/collapsing
            });
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView title, strategy, percent, percent2, longOrShort, firstPrice, time, date;

        public ItemViewHolder(CardViewApprovedBinding b) {
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

        @SuppressLint("SetTextI18n")
        public void bind(ListViewElement item) {

            percent.setTextColor(Color.BLACK);

            String symbol = item.getText();
            String strategyText = "Strategy: " + item.getStrategyNr();

            float percentChange = item.getPercentChange();
            float percentChange2 = item.getPercentChange2();
            float priceWhenCaught = item.getPriceWhenCaught();
            String timeText = item.getTime();
            String hour = timeText.substring(timeText.length() - 5);
            boolean isItLong = item.isItLONG();

            // Define the formatter for the new pattern
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
            // Parse the time string to a LocalDateTime object
            LocalDateTime comparisonDateTime = LocalDateTime.parse(timeText, formatter);
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
                cardView.setCardBackgroundColor(Color.YELLOW);
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.light_grey));
            }


            DecimalFormat dfNr = new DecimalFormat("0.00");
            DecimalFormat dfPr = new DecimalFormat("0.00000");
            int darkGreenColor = ContextCompat.getColor(itemView.getContext(), R.color.dark_green);
            title.setText(symbol);
            strategy.setText(strategyText);
            date.setText(readableDate);
            if (symbol.contains("Nothing")) {
                percent.setTextColor(Color.BLACK);
                longOrShort.setTextColor(Color.BLACK);
            } else {
                if (isItLong) {
                    longOrShort.setText("LN");
                    if (percentChange > 0) {
                        percent.setTextColor(darkGreenColor);
                        longOrShort.setTextColor(darkGreenColor);
                    } else {
                        percent.setTextColor(Color.BLUE);
                        longOrShort.setTextColor(darkGreenColor);
                    }
                    if (percentChange2 > 0) {
                        percent2.setTextColor(darkGreenColor);
                    } else {
                        percent2.setTextColor(Color.BLUE);
                    }
                } else {
                    longOrShort.setText("SH");
                    if (percentChange < 0) {
                        percent.setTextColor(Color.RED);
                        longOrShort.setTextColor(Color.RED);
                    } else {
                        percent.setTextColor(Color.BLUE);
                        longOrShort.setTextColor(Color.RED);
                    }
                    if (percentChange2 < 0) {
                        percent2.setTextColor(Color.RED);
                    } else {
                        percent2.setTextColor(Color.BLUE);
                    }

                }

                if (minutesDifference >= 0 && minutesDifference <= 3) {
                    percent.setText("---");
                    percent2.setText("---");
                } else {
                    percent.setText(dfNr.format(percentChange) + "%");
                    percent2.setText(dfNr.format(percentChange2) + "%");
                }


                if (priceWhenCaught > 10) {
                    firstPrice.setText(dfNr.format(priceWhenCaught));
                } else {
                    firstPrice.setText(dfPr.format(priceWhenCaught));
                }

                time.setText(hour);
            }
        }
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

    public void updateList(ArrayList<ApprovedGroupedTokens>  newList) {
        groupedItems = newList;
        notifyDataSetChanged();
    }

}