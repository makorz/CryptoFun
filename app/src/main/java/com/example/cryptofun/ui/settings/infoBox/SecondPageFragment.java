package com.example.cryptofun.ui.settings.infoBox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.cryptofun.R;

public class SecondPageFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.second_page_dialog, container, false);
        // Set up any UI elements or listeners for the third page
        return view;
    }
}