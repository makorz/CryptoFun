package com.example.cryptofun.ui.view;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cryptofun.R;

import java.text.DecimalFormat;
import java.util.ArrayList;


public class GridViewAdapter extends ArrayAdapter<GridViewElement> {

    public GridViewAdapter(@NonNull Context context, ArrayList<GridViewElement> courseModelArrayList) {
        super(context, 0, courseModelArrayList);
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView = convertView;
        if (listItemView == null) {
            // Layout Inflater inflates each item to be displayed in GridView.
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.card_view_crypto, parent, false);
        }


        GridViewElement element = getItem(position);
        TextView courseTVTitle = listItemView.findViewById(R.id.text);
        TextView courseTVValue = listItemView.findViewById(R.id.value);
        TextView courseTVPercent = listItemView.findViewById(R.id.percent);

        if ( !element.getSymbol().contains("%")) {
            DecimalFormat df = new DecimalFormat("#.###");
            DecimalFormat df2 = new DecimalFormat("#.#");
            courseTVTitle.setText(element.getSymbol());
            courseTVValue.setText(df.format(element.getValue()) + "$");
            courseTVPercent.setText(df2.format(element.getPercent()) + "%");
            if (element.getPercent() < 0) {
                courseTVPercent.setTextColor(Color.RED);
            } else {
                courseTVPercent.setTextColor(Color.GREEN);
            }
        } else {
            DecimalFormat df = new DecimalFormat("#.#");
            courseTVTitle.setText(element.getSymbol());
            courseTVPercent.setText(df.format(element.getPercent()) + "%");
            courseTVPercent.setTextColor(Color.BLUE);
            float allCryptoCounted = (element.getValue() * 100) / element.getPercent();
            String value = (int) element.getValue() + "/" + Math.round(allCryptoCounted);
            courseTVValue.setText(value);
        }
        courseTVValue.setTextColor(Color.GRAY);
        courseTVTitle.setTextColor(Color.BLACK);
        return listItemView;
    }
}