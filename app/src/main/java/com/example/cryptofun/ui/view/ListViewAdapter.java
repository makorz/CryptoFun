package com.example.cryptofun.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cryptofun.R;

import java.text.DecimalFormat;
import java.util.ArrayList;


public class ListViewAdapter extends ArrayAdapter<ListViewElement> {

    public ListViewAdapter(@NonNull Context context, ArrayList<ListViewElement> courseModelArrayList) {
        super(context, 0, courseModelArrayList);
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView = convertView;
        if (listItemView == null) {
            // Layout Inflater inflates each item to be displayed in GridView.
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_crypto, parent, false);
        }

        ListViewElement element = getItem(position);
        TextView courseTVTitle = listItemView.findViewById(R.id.text);
        courseTVTitle.setText(element.getText());

        return listItemView;
    }
}