package app.makorz.cryptofun.ui.results;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import app.makorz.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.FragmentResultsBinding;
import app.makorz.cryptofun.services.CallbackButton;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ResultsFragment extends Fragment implements CallbackButton {

    private static final String TAG = "ResultsFrag";

    private static final String TABLE_NAME_ORDERS_HISTORIC = "historic_orders";

    private FragmentResultsBinding binding;
    private TextView realBalance, testBalance, automaticBalance1, automaticBalance2, automaticBalance3, automaticBalance4, automaticBalance5, logTextView;
    CallbackButton callbackButton = this;
    private ArrayList<ResultsListElement> resultsOrderList = new ArrayList<>();
    private ArrayList<ResultsAccountElement> resultsAccountList = new ArrayList<>();
    private DBHandler databaseDB;
    private RecyclerView recyclerViewOrders, recyclerViewAccounts;
    private ResultsListAdapter adapterOrders;
    private ResultsAccountListAdapter adapterAccounts;

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

        recyclerViewOrders = binding.rwOrdersResults;
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapterOrders = new ResultsListAdapter(resultsOrderList);
        recyclerViewOrders.setAdapter(adapterOrders);

        recyclerViewAccounts = binding.rwAccountResults;
        recyclerViewAccounts.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapterAccounts = new ResultsAccountListAdapter(resultsAccountList);
        recyclerViewAccounts.setAdapter(adapterAccounts);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("OrdersStatus"));

        onReceiveBalance();
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
                loadResults();
                displayLogFile();
                onReceiveBalance();
            }
        }
    };

    private void loadResults() {
        ArrayList<ResultsListElement> closedOrders = new ArrayList<>();
        ArrayList<ResultsListElement> closedOrdersReal = new ArrayList<>();
        ArrayList<ResultsListElement> closedOrdersTestManual = new ArrayList<>();
        ArrayList<ResultsListElement> closedOrdersTestAutomatic1 = new ArrayList<>();
        ArrayList<ResultsListElement> closedOrdersTestAutomatic2 = new ArrayList<>();
        ArrayList<ResultsListElement> closedOrdersTestAutomatic3 = new ArrayList<>();
        ArrayList<ResultsListElement> closedOrdersTestAutomatic4 = new ArrayList<>();
        ArrayList<ResultsListElement> closedOrdersTestAutomatic5 = new ArrayList<>();
        ArrayList<ResultsAccountElement> accountsResults = new ArrayList<>();
        DBHandler db = DBHandler.getInstance(getContext());
        Cursor data = db.retrieveAllFromTable(TABLE_NAME_ORDERS_HISTORIC);
        data.moveToFirst();
        if (data.getCount() > 0) {
            do {
                ResultsListElement tempToken = new ResultsListElement(data.getString(1), data.getFloat(5), data.getFloat(6), data.getFloat(8), data.getInt(14), data.getInt(15), data.getFloat(9), data.getFloat(3), data.getFloat(4), data.getFloat(7), data.getLong(10), data.getLong(11), data.getInt(13), data.getInt(2));

                closedOrders.add(tempToken);

                if (tempToken.getIsItReal() == 1) {
                    closedOrdersReal.add(tempToken);
                } else {
                    switch (tempToken.getAccountNr()) {
                        case 2:
                            closedOrdersTestManual.add(tempToken);
                            break;
                        case 6:
                            closedOrdersTestAutomatic1.add(tempToken);
                            break;
                        case 7:
                            closedOrdersTestAutomatic2.add(tempToken);
                            break;
                        case 8:
                            closedOrdersTestAutomatic3.add(tempToken);
                            break;
                        case 9:
                            closedOrdersTestAutomatic4.add(tempToken);
                            break;
                        case 10:
                            closedOrdersTestAutomatic5.add(tempToken);
                            break;
                        default:
                            break;
                    }
                }

                Log.e(TAG, "Some active orders, callback " + tempToken.toString());
            } while (data.moveToNext());
        } else {
            Log.e(TAG, "No closed orders");
        }

        Collections.sort(closedOrders, Comparator.comparingLong(ResultsListElement::getTimeExit).reversed());

        String account;

        if (closedOrdersReal.size() > 0) {
            account = "REAL ACCOUNT";
            accountsResults.add(calculateAccountResults(closedOrdersReal, account));
        }
        if (closedOrdersTestManual.size() > 0) {
            account = "TEST MANUAL";
            accountsResults.add(calculateAccountResults(closedOrdersTestManual, account));
        }
        if (closedOrdersTestAutomatic1.size() > 0) {
            account = "TEST AUTOMATIC 1";
            accountsResults.add(calculateAccountResults(closedOrdersTestAutomatic1, account));
        }
        if (closedOrdersTestAutomatic2.size() > 0) {
            account = "TEST AUTOMATIC 2";
            accountsResults.add(calculateAccountResults(closedOrdersTestAutomatic2, account));
        }
        if (closedOrdersTestAutomatic3.size() > 0) {
            account = "TEST AUTOMATIC 3";
            accountsResults.add(calculateAccountResults(closedOrdersTestAutomatic3, account));
        }
        if (closedOrdersTestAutomatic4.size() > 0) {
            account = "TEST AUTOMATIC 4";
            accountsResults.add(calculateAccountResults(closedOrdersTestAutomatic4, account));
        }
        if (closedOrdersTestAutomatic5.size() > 0) {
            account = "TEST AUTOMATIC 5";
            accountsResults.add(calculateAccountResults(closedOrdersTestAutomatic5, account));
        }

        Log.e(TAG, "Some active orders, callback " + closedOrders.size() + " some accounts with history: " + accountsResults.size());
        adapterOrders.updateList(closedOrders);
        adapterAccounts.updateList(accountsResults);
        data.close();
    }


    private ResultsAccountElement calculateAccountResults (ArrayList<ResultsListElement> list, String accountNr) {

        int nrOfAllOrders = 0, nrOfShortOrders = 0, nrOfLongOrders = 0, nrOfAllGoodOrders = 0, nrOfShortGoodOrders = 0, nrOfLongGoodOrders = 0;
        float percentOfAllGoodOrders  = 0,percentOfShortGoodOrders = 0, percentOfLongGoodOrders = 0, percentOf3d = 0, percentOf7d = 0, percentOf30d = 0, percentOfAllTime = 0, moneyExit3d = 0, moneyExit7d = 0, moneyExit30d = 0, moneyExitAll = 0;

        long currentTime = System.currentTimeMillis();
        long timeDifference3d = currentTime - 3L * 24 * 60 * 60 * 1000;
        long timeDifference7d = currentTime - 7L * 24 * 60 * 60 * 1000;
        long timeDifference30d = currentTime - 30L * 24 * 60 * 60 * 1000;

        for (ResultsListElement element : list) {

            if (element.getTimeExit() > timeDifference3d) {
                moneyExit3d += element.getCurrentAmount() - element.getEntryAmount();
            }

            if (element.getTimeExit() > timeDifference7d) {
                moneyExit7d += element.getCurrentAmount() - element.getEntryAmount();
            }

            if (element.getTimeExit() > timeDifference30d) {
                moneyExit30d += element.getCurrentAmount() - element.getEntryAmount();
            }

            moneyExitAll += element.getCurrentAmount() - element.getEntryAmount();

            if (element.getIsItShort() == 1) {
                nrOfShortOrders++;
                if (element.getCurrentAmount() - element.getEntryAmount() > 0) {
                    nrOfShortGoodOrders++;
                }
            } else  {
                nrOfLongOrders++;
                if (element.getCurrentAmount() - element.getEntryAmount() > 0) {
                    nrOfLongGoodOrders++;
                }
            }

        }

        nrOfAllOrders = nrOfLongOrders + nrOfShortOrders;
        nrOfAllGoodOrders = nrOfLongGoodOrders + nrOfShortGoodOrders;
        percentOfAllGoodOrders = nrOfAllGoodOrders * 100 / (float) nrOfAllOrders;
        percentOfLongGoodOrders = nrOfLongGoodOrders * 100 / (float) nrOfLongOrders;
        percentOfShortGoodOrders = nrOfShortGoodOrders * 100 / (float) nrOfShortOrders;

        //Sum of money earned instead of percents for
        percentOf3d = moneyExit3d;
        percentOf7d = moneyExit7d;
        percentOf30d = moneyExit30d;
        percentOfAllTime = moneyExitAll;

        return new ResultsAccountElement(accountNr, nrOfAllOrders, nrOfShortOrders, nrOfLongOrders, nrOfAllGoodOrders, nrOfShortGoodOrders, nrOfLongGoodOrders,percentOfAllGoodOrders,percentOfShortGoodOrders, percentOfLongGoodOrders, percentOf3d, percentOf7d, percentOf30d, percentOfAllTime);
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

            if (!realBalance.getText().toString().equals(balance)) {
                realBalance.setText(balance);
            }

        }
        data.close();
    }

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