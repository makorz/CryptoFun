package app.makorz.cryptofun.ui.orders;

import static android.content.Context.INPUT_METHOD_SERVICE;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import app.makorz.cryptofun.data.MarkPrice;
import app.makorz.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.FragmentOrdersBinding;

import app.makorz.cryptofun.services.CallbackButton;
import app.makorz.cryptofun.services.ServiceFunctionsAPI;
import app.makorz.cryptofun.retrofit.RetrofitClientFutures;

import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class OrdersFragment extends Fragment implements CallbackButton {

    private static final String TAG = "OrdersFrag";
    private static final String TABLE_NAME_ORDERS = "current_orders";
    private FragmentOrdersBinding binding;
    private ArrayList<OrderListViewElement> mItems = new ArrayList<>();
    private Button placeOrderButton;
    private RadioGroup testOrRealRG, isolatedOrCrossedRG, longOrShortRG;
    private EditText symbolET, amountET, stopLimitET, takeProfitET, marginET;
    private DBHandler databaseDB;
    private RecyclerView recyclerView;
    private OrderListAdapter adapter;
    private ProgressBar orderLoading;
    View root;
    InputMethodManager imm;
    CallbackButton callbackButton = this;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.e(TAG, "CreateView");

        imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        root = binding.getRoot();

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
                Log.e(TAG, "APRVServiceReceived - Orders" + Thread.currentThread());
                Bundle bundle = intent.getBundleExtra("bundleOrdersStatus");
                onReceiveNewList(adapter, (ArrayList<OrderListViewElement>) bundle.getSerializable("ordersList"));
            }
        }
    };

    private void buttonsJob() {
        placeOrderButton.setOnClickListener(v -> {

            // Hide the keyboard
            imm.hideSoftInputFromWindow(root.getWindowToken(), 0);

            setButtonEnabled(false);
            // Get the ID of the selected RadioButton
            int selectedId = testOrRealRG.getCheckedRadioButtonId();
            int selectedId2 = isolatedOrCrossedRG.getCheckedRadioButtonId();
            int selectedId3 = longOrShortRG.getCheckedRadioButtonId();
            int accountNr;
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

            float stopLimitValue = 0;
            try {
                stopLimitValue = Float.parseFloat(stopLimit);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            float takeProfitValue = 0;
            try {
                takeProfitValue = Float.parseFloat(takeProfit);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            int marginValue = 0;
            try {
                marginValue = Integer.parseInt(margin);
            } catch (NumberFormatException e) {
                // The text is not a valid integer
            }

            if (selectedId != -1 && selectedId2 != -1 && selectedId3 != -1 && amountValue >= 8) {
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
                Cursor data;

                float balance = 0;
                if (isItReal) {

                    accountNr = 3;

                    data = databaseDB.retrieveParam(3);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 3");
                    } else {
                        data.moveToFirst();
                        balance = data.getFloat(4);
                    }
                } else {

                    accountNr = 2;

                    data = databaseDB.retrieveParam(2);
                    if (data.getCount() == 0) {
                        Log.e(TAG, "There is no param nr 2");
                    } else {
                        data.moveToFirst();
                        balance = data.getFloat(4);
                    }
                }
                data.close();

                if (balance * 0.99 < amountValue) {
                    showInfoForUser(getContext(), "Amount of USDT is higher than Your balance, check balance and adjust entered number.");
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("Are you sure you want to place this order?");

                    float finalBalance = balance;
                    boolean finalIsItShort = isItShort;
                    boolean finalIsItCrossed = isItCrossed;
                    int finalMarginValue = marginValue;
                    float finalTakeProfitValue = takeProfitValue;
                    float finalStopLimitValue = stopLimitValue;
                    int finalAmountValue = amountValue;
                    boolean finalIsItReal = isItReal;

                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            getMarkPrice(symbol)
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

                                            ServiceFunctionsAPI.makeOrderFunction(finalIsItReal, symbol, finalAmountValue, finalStopLimitValue, finalTakeProfitValue, finalMarginValue, markPrice.getMarkPrice(), finalIsItCrossed, finalIsItShort, System.currentTimeMillis(), finalBalance, accountNr, getContext(), callbackButton);

                                        }

                                        @Override
                                        public void onError(@NonNull Throwable e) {
                                            // handle any errors that occur
                                            Log.e(TAG, "An error has occurred: " + e.getMessage());
                                            setButtonEnabled(true);
                                        }

                                        @Override
                                        public void onComplete() {
                                            // do something when the observable completes
                                        }
                                    });

                        }
                    });
                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setButtonEnabled(true);
                        }
                    });

                    builder.setCancelable(false);
                    builder.show();

                }

            } else {
                // No RadioButton is selected
                Toast.makeText(getContext(), "Check if RadioButtons are marked or amount value is bigger than 10 USDT", Toast.LENGTH_SHORT).show();
                setButtonEnabled(true);
            }

        });
    }

    public Observable<MarkPrice> getMarkPrice(String symbol) {
        return RetrofitClientFutures.getInstance()
                .getMyApi()
                .getMarkPrice(symbol)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void setButtonEnabled(boolean enabled) {
        if (enabled) {
            placeOrderButton.setEnabled(true);
            placeOrderButton.setVisibility(View.VISIBLE);
            orderLoading.setVisibility(View.GONE);
        } else {
            placeOrderButton.setEnabled(false);
            placeOrderButton.setVisibility(View.GONE);
            orderLoading.setVisibility(View.VISIBLE);
        }
    }

    public void onReceiveNewList(OrderListAdapter adapter, ArrayList<OrderListViewElement> receivedOrdersList) {
        ArrayList<OrderListViewElement> currentOrders = new ArrayList<>();
        DBHandler db = DBHandler.getInstance(getContext());
        Cursor data = db.retrieveAllFromTable(TABLE_NAME_ORDERS);
        if (receivedOrdersList == null) {
            data.moveToFirst();
            if (data.getCount() == 0) {
                Log.e(TAG, "No active orders, callback");
            } else {
                do {
                    OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getLong(13), data.getString(14), data.getFloat(15));
                    currentOrders.add(tempToken);

                } while (data.moveToNext());
                Log.e(TAG, "Some active orders, callback " + currentOrders.size());
                adapter.updateList(currentOrders);
            }
            data.close();
        } else {
            Log.e(TAG, "No callback " + receivedOrdersList.size());
            adapter.updateList(receivedOrdersList);
            for (OrderListViewElement obj : receivedOrdersList) {
                Log.e(TAG, obj.toString());
            }
        }

    }

    private void showInfoForUser(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                setButtonEnabled(true);
            }
        });
        setButtonEnabled(true);
        builder.show();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "Resume.");
        setButtonEnabled(true);
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //databaseDB.close();
        binding = null;
    }

    @Override
    public void onSuccess() {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG,"Callback returned");
                setButtonEnabled(true);
                onReceiveNewList(adapter, null);

            }
        });
    }

    @Override
    public void onError() {
        setButtonEnabled(true);
    }
}