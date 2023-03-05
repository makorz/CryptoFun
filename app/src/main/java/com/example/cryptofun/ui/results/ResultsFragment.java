package com.example.cryptofun.ui.results;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.databinding.FragmentResultsBinding;

public class ResultsFragment extends Fragment {

    private static final String TAG = "ResultsFrag";

    private FragmentResultsBinding binding;
    private TextView realBalance, testBalance, automaticBalance1, automaticBalance2, automaticBalance3, automaticBalance4, automaticBalance5;
    private DBHandler databaseDB;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.e(TAG, "CreateView");

        databaseDB = DBHandler.getInstance(getContext());

        binding = FragmentResultsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        realBalance = binding.tvRealNumberBalance;
        testBalance = binding.tvTestNumberBalance;
        automaticBalance1 = binding.tvTestNumberBalance1;
        automaticBalance2 = binding.tvTestNumberBalance2;
        automaticBalance3 = binding.tvTestNumberBalance3;
        automaticBalance4 = binding.tvTestNumberBalance4;
        automaticBalance5 = binding.tvTestNumberBalance5;

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("OrdersStatus"));

        return root;
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("OrdersStatus")) {
                Bundle bundle = intent.getBundleExtra("bundleOrdersStatus");
                Log.e(TAG, "OrderStatus" + Thread.currentThread());
                realBalance.setText(bundle.getString("realBalance"));
                testBalance.setText(bundle.getString("testBalance"));
                automaticBalance1.setText(bundle.getString("autoBalance1"));
                automaticBalance2.setText(bundle.getString("autoBalance2"));
                automaticBalance3.setText(bundle.getString("autoBalance3"));
                automaticBalance4.setText(bundle.getString("autoBalance4"));
                automaticBalance5.setText(bundle.getString("autoBalance5"));

            }
        }
    };

    @Override
    public void onResume() {
        Log.e(TAG, "Resume.");
        super.onResume();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.e(TAG, "View Created.");
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "View Destroyed.");
        //databaseDB.close();
        binding = null;
    }

    @Override
    public void onPause() {
        Log.e(TAG, "Pause.");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "Stop.");
        super.onStop();
    }
}