package com.example.cryptofun.ui.results;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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

import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.FragmentResultsBinding;
import com.example.cryptofun.services.CallbackButton;
import com.example.cryptofun.services.OrdersService;
import com.example.cryptofun.services.ServiceFunctions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

public class ResultsFragment extends Fragment implements CallbackButton {

    private static final String TAG = "ResultsFrag";

    private FragmentResultsBinding binding;
    private TextView realBalance, testBalance, automaticBalance1, automaticBalance2, automaticBalance3, automaticBalance4, automaticBalance5, logTextView;
    CallbackButton callbackButton = this;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.e(TAG, "CreateView");

        binding = FragmentResultsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        realBalance = binding.tvRealNumberBalance;
        testBalance = binding.tvTestNumberBalance;
        automaticBalance1 = binding.tvTestNumberBalance1;
        automaticBalance2 = binding.tvTestNumberBalance2;
        automaticBalance3 = binding.tvTestNumberBalance3;
        automaticBalance4 = binding.tvTestNumberBalance4;
        automaticBalance5 = binding.tvTestNumberBalance5;
        logTextView = binding.tvLogFile;

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("OrdersStatus"));

//        if (!isMyServiceRunning(ResultsService.class, getContext())) {
//            Intent serviceIntent = new Intent(getContext(), ResultsService.class);
//            requireContext().startForegroundService(serviceIntent);
//
//        }

        if (!isMyServiceRunning(OrdersService.class, getContext())) {
            Intent serviceIntent = new Intent(getContext(), OrdersService.class);
            requireContext().startForegroundService(serviceIntent);

        }
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
                displayLogFile();
                ServiceFunctions.getRealAccountBalance(getContext(), callbackButton);

            }
        }
    };

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void displayLogFile() {
        try {
            Context context = getContext();
            assert context != null;
            FileInputStream fis = context.openFileInput("OrdersLog.txt");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            StringBuilder logText = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                logText.append(line).append("\n");
            }

            br.close();
            isr.close();
            fis.close();

            logTextView.setText(logText.toString());

        } catch (IOException e) {
            e.printStackTrace();
            logTextView.setText("Error or log file is empty.");
        }
    }

    public void onReceiveBalance() {
        DBHandler db = DBHandler.getInstance(getContext());
        DecimalFormat dfNr = new DecimalFormat("0.00");
        String balance = "";
        Cursor data = db.retrieveParam(3);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 13");
            realBalance.setText("No data");
        } else {
            data.moveToFirst();

            balance = dfNr.format(data.getFloat(4));
            realBalance.setText(balance);
        }
        data.close();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "Resume.");
        displayLogFile();
        ServiceFunctions.getRealAccountBalance(getContext(), callbackButton);
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
        displayLogFile();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "Stop.");
        super.onStop();
    }

    @Override
    public void onSuccess() {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Callback Returned");
                onReceiveBalance();
            }
        });
    }

    @Override
    public void onError() {

    }
}