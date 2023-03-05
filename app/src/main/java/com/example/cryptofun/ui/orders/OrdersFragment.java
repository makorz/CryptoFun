package com.example.cryptofun.ui.orders;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.data.AccountBalance;
import com.example.cryptofun.data.MarkPrice;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.databinding.FragmentOrdersBinding;

import com.example.cryptofun.retrofit.RetrofitClientFutures;
import com.example.cryptofun.retrofit.RetrofitClientSecret;
import com.example.cryptofun.services.ApprovingService;
import com.example.cryptofun.services.UpdatingDatabaseService;
import com.example.cryptofun.ui.view.CryptoListAdapter;
import com.example.cryptofun.ui.view.GridViewElement;
import com.example.cryptofun.ui.view.ListViewElement;
import com.example.cryptofun.ui.view.OrderListAdapter;
import com.example.cryptofun.ui.view.OrderListViewElement;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersFragment extends Fragment {


    private static final String TAG = "OrdersFrag";

    private static final String TABLE_NAME_CONFIG = "config";
    private static final String DESCRIPTION = "description";
    private static final String VALUE_STRING = "value_string";
    private static final String VALUE_INT = "value_int";
    private static final String ID = "id";
    private static final String VALUE_REAL = "value_real";

    private FragmentOrdersBinding binding;
    private ArrayList<OrderListViewElement> mItems = new ArrayList<>();
    private Button placeOrderButton;
    private RadioGroup testOrRealRG, isolatedOrCrossedRG, longOrShortRG;
    private EditText symbolET, amountET, stopLimitET, takeProfitET, marginET;
    private DBHandler databaseDB;
    private RecyclerView recyclerView;
    private OrderListAdapter adapter;
    private ProgressBar orderLoading;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.e(TAG, "CreateView");
        binding = FragmentOrdersBinding.inflate(inflater, container, false);

        placeOrderButton = binding.btOrder;
        testOrRealRG = binding.radioGroupRealTest;
        isolatedOrCrossedRG = binding.radioGroupIsolatedCrossed;
        longOrShortRG = binding.radioGroupLongShort;
        symbolET = binding.etSymbol;
        amountET = binding.etAmountUsdt;
        stopLimitET = binding.etSlPercentage;
        takeProfitET = binding.etTpPercentage;
        marginET = binding.etMargin;
        orderLoading = binding.progressBarOrder;

        recyclerView = binding.rwCurrentOrders;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter = new OrderListAdapter(mItems);
        recyclerView.setAdapter(adapter);
        buttonsJob();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("OrdersStatus"));


        return binding.getRoot();
    }

    // Receive broadcast from ApprovingService
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("OrdersStatus")) {
                Bundle bundle = intent.getBundleExtra("bundleOrdersStatus");
                Log.e(TAG, "APRVServiceReceived - Orders" + Thread.currentThread());
                mItems = (ArrayList<OrderListViewElement>) bundle.getSerializable("ordersList");
                onReceiveNewList(adapter, mItems);

            }
        }
    };

    private void buttonsJob() {
        placeOrderButton.setOnClickListener(v -> {

            placeOrderButton.setEnabled(false);
            // Get the ID of the selected RadioButton
            int selectedId = testOrRealRG.getCheckedRadioButtonId();
            int selectedId2 = isolatedOrCrossedRG.getCheckedRadioButtonId();
            int selectedId3 = longOrShortRG.getCheckedRadioButtonId();
            boolean isItReal = false;
            boolean isItCrossed = false;
            boolean isItShort = false;
            String symbol = symbolET.getText().toString().toUpperCase();
            String amount = amountET.getText().toString();
            String stopLimit = stopLimitET.getText().toString();
            String takeProfit = takeProfitET.getText().toString();
            String margin = marginET.getText().toString();

            int amountValue = 0;
            try {
                amountValue = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            int stopLimitValue = 0;
            try {
                stopLimitValue = Integer.parseInt(stopLimit);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            int takeProfitValue = 0;
            try {
                takeProfitValue = Integer.parseInt(takeProfit);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            int marginValue = 0;
            try {
                marginValue = Integer.parseInt(margin);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            if (selectedId != -1 && selectedId2 != -1 && selectedId3 != -1) {
                if (selectedId == R.id.rb_real) {
                    isItReal = true;
                }
                if (selectedId2 == R.id.rb_crossed) {
                    isItCrossed = true;
                }
                if (selectedId3 == R.id.rb_short) {
                    isItShort = true;
                }

                databaseDB = DBHandler.getInstance(getContext());
                float balance = 0;
                if (isItReal) {
                    Log.e(TAG, "Real not yet");
                } else {
                    Cursor data = databaseDB.retrieveParam(2);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 2");
                    } else {
                        data.moveToFirst();
                        balance = data.getFloat(4);
                    }
                    data.close();
                }

                if (balance * 0.99 < amountValue) {
                    showInfoForUser(getContext(), "Amount of USDT is higher than Your balance, check balance and adjust entered number.");
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("Are you sure you want to place this order?");

                    float finalBalance = balance;
                    boolean finalIsItShort = isItShort;
                    boolean finalIsItCrossed = isItCrossed;
                    int finalMarginValue = marginValue;
                    int finalTakeProfitValue = takeProfitValue;
                    int finalStopLimitValue = stopLimitValue;
                    int finalAmountValue = amountValue;
                    boolean finalIsItReal = isItReal;
                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            placeOrderButton.setVisibility(View.GONE);
                            orderLoading.setVisibility(View.VISIBLE);

                            getMarkPrice(finalIsItReal, symbol)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Observer<MarkPrice>() {
                                        @Override
                                        public void onSubscribe(@NonNull Disposable d) {
                                            // do something when the subscription is made
                                        }

                                        @Override
                                        public void onNext(@NonNull MarkPrice markPrice) {
                                            // handle the MarkPrice object returned by the API
                                            Log.e(TAG, "User Order Made for " + symbol + ".");

                                            makeOrder(finalIsItReal, symbol, finalAmountValue, finalStopLimitValue, finalTakeProfitValue, finalMarginValue, markPrice.getMarkPrice(), finalIsItCrossed, finalIsItShort, System.currentTimeMillis(), finalBalance);
                                        }

                                        @Override
                                        public void onError(@NonNull Throwable e) {
                                            // handle any errors that occur
                                            Log.e(TAG, "An error has occurred: " + e.getMessage());
                                            placeOrderButton.setEnabled(true);
                                            placeOrderButton.setVisibility(View.VISIBLE);
                                            orderLoading.setVisibility(View.GONE);
                                        }

                                        @Override
                                        public void onComplete() {
                                            // do something when the observable completes
                                        }
                                    });


//                            getMarkPrice(finalIsItReal, symbol, finalAmountValue, finalStopLimitValue, finalTakeProfitValue, finalMarginValue, finalIsItCrossed, finalIsItShort, System.currentTimeMillis(), finalBalance);
                        }
                    });
                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            placeOrderButton.setEnabled(true);
                        }
                    });
                    placeOrderButton.setEnabled(true);
                    builder.show();

                }

            } else {
                // No RadioButton is selected
                Toast.makeText(getContext(), "Please mark RadioButtons", Toast.LENGTH_SHORT).show();
                placeOrderButton.setEnabled(true);
            }

        });
    }

    public Observable<MarkPrice> getMarkPrice(boolean isItReal, String symbol) {
        return RetrofitClientFutures.getInstance()
                .getMyApi()
                .getMarkPrice(symbol)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void makeOrder(boolean isItReal, String symbol, int entryAmount, int stopLimit, int takeProfit, int margin, float currentPrice, boolean isItCrossed, boolean isItShort, long time, float balance) {

        if (isItReal) {
            Log.e(TAG, "REAL");
            int accountNr = 3;
        } else {
            Log.e(TAG, "TEST");
            Log.e(TAG, isItCrossed + " " + isItShort);
            float stopLimitPrice = currentPrice * (1 - (float) stopLimit / 100);
            float takeProfitPrice = currentPrice * (1 + (float) takeProfit / 100);
            int isItShortValue = 0;
            int isItRealValue = 0;
            int isItCrossedValue = 0;
            if (isItShort) {
                stopLimitPrice = currentPrice * (1 + (float) stopLimit / 100);
                takeProfitPrice = currentPrice * (1 - (float) takeProfit / 100);
                isItShortValue = 1;
            }
            if (isItCrossed) {
                isItCrossedValue = 1;
            }
            int accountNr = 2;

            Log.e(TAG, isItCrossedValue + " " + isItShortValue);
            OrderListViewElement toDB = new OrderListViewElement(symbol, isItRealValue, (float) entryAmount, currentPrice, currentPrice, stopLimitPrice, takeProfitPrice, time, margin, isItShortValue, isItCrossedValue,accountNr);
            mItems.add(toDB);
            for (int i = 0; i < mItems.size(); i++) {
                Log.e(TAG, mItems.get(i).getSymbol() + mItems.get(i).getCurrentPrice() + mItems.get(i).getEntryPrice());
            }
            databaseDB.addNewOrder(toDB);
            databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, balance - entryAmount, ID, "2");
            onReceiveNewList(adapter, mItems);
            placeOrderButton.setEnabled(true);
            placeOrderButton.setVisibility(View.VISIBLE);
            orderLoading.setVisibility(View.GONE);

        }


    }

    public void onReceiveNewList(OrderListAdapter adapter, ArrayList<OrderListViewElement> newList) {
        adapter.updateList(newList);
    }

    private void showInfoForUser(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                placeOrderButton.setEnabled(true);
            }
        });
        placeOrderButton.setEnabled(true);
        builder.show();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "Resume.");
        placeOrderButton.setEnabled(true);
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //databaseDB.close();
        binding = null;
    }
}