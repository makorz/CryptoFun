package com.example.cryptofun.ui.settings;

import androidx.appcompat.app.AlertDialog;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cryptofun.MainActivity;
import com.example.cryptofun.R;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.databinding.FragmentSettingsBinding;
import com.example.cryptofun.retrofit.RetrofitClientSecret;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFrag";

    private static final String TABLE_NAME_CONFIG = "config";
    private static final String DESCRIPTION = "description";
    private static final String VALUE_STRING = "value_string";
    private static final String VALUE_INT = "value_int";
    private static final String VALUE_REAL = "value_real";
    private static final String ID = "id";

    private FragmentSettingsBinding binding;
    private Button resetTestBalanceButton, resetAutomaticTestBalanceButton, showKeysButton, updateKeysButton, updateParamsButton, updateWindowButton;
    private EditText apikeyET, secretET, marginET, takeProfitET, stopLossET, windowET;
    private FloatingActionButton infoButton;
    private DBHandler databaseDB;
    private SwitchCompat switchTest, switchReal;
    View root;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.e(TAG, "CreateView");

        databaseDB = DBHandler.getInstance(getContext());
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        resetTestBalanceButton = binding.btResetTestBalance;
        resetAutomaticTestBalanceButton = binding.btResetAutomaticBalance;
        infoButton = binding.infoFloating;
        showKeysButton = binding.btShowKeys;
        updateKeysButton = binding.btSetKeys;
        apikeyET = binding.etApiKey;
        secretET = binding.etSecret;
        switchReal = binding.switchReal;
        switchTest = binding.switchTest;
        updateParamsButton = binding.btSetParams;
        updateWindowButton = binding.btSetWindow;
        marginET = binding.etMargin;
        stopLossET = binding.etStopLoss;
        takeProfitET = binding.etTakeProfit;
        windowET = binding.etRecvWindow;

        checkIfParamsArePresent();
        buttonsJob();
        return root;
    }

    private void checkIfParamsArePresent() {
        Cursor data = databaseDB.retrieveParam(4);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 4");
            databaseDB.addParam(4, "API-KEY", "", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 4);
            databaseDB.addParam(4, "API-KEY", "", 0, 0);
        }
        data.close();

        Cursor data2 = databaseDB.retrieveParam(5);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 5");
            databaseDB.addParam(5, "SECRET", "", 0, 0);
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 5);
            databaseDB.addParam(5, "SECRET", "", 0, 0);
        }
        data2.close();

        data = databaseDB.retrieveParam(11);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 11");
            databaseDB.addParam(11, "Automatic margin", "", 1, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 11);
            databaseDB.addParam(11, "Automatic margin", "", 1, 0);
        }
        data.close();

        data2 = databaseDB.retrieveParam(12);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 12");
            databaseDB.addParam(12, "Automatic SL", "", 1, 0);
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 12);
            databaseDB.addParam(12, "Automatic SL", "", 1, 0);
        }
        data2.close();

        data = databaseDB.retrieveParam(13);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 13");
            databaseDB.addParam(13, "Automatic TP", "", 1, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 13);
            databaseDB.addParam(13, "Automatic TP", "", 1, 0);
        }
        data.close();

        data2 = databaseDB.retrieveParam(14);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 14");
            databaseDB.addParam(14, "recvWindow", "", 1000, 0);
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 14);
            databaseDB.addParam(14, "recvWindow", "", 1000, 0);
        }
        data2.close();

        data = databaseDB.retrieveParam(15);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 15");
            databaseDB.addParam(15, "Stop automatic for REAL", "", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 15);
            databaseDB.addParam(15, "Stop automatic for REAL", "", 0, 0);
        }
        data.close();


        data2 = databaseDB.retrieveParam(16);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 16");
            databaseDB.addParam(16, "Stop automatic for TEST", "", 0, 0);
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 16);
            databaseDB.addParam(16, "Stop automatic for TEST", "", 0, 0);
        }
        data2.close();

    }

    private void buttonsJob() {

        resetTestBalanceButton.setOnClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("RESET TEST BALANCE");
            builder.setMessage("Are You sure You want reset balance of test account to 100?");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    resetTestBalance();
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

        });

        resetAutomaticTestBalanceButton.setOnClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("RESET AUTOMATIC TEST BALANCE");
            builder.setMessage("Are You sure You want reset balance of automatic test accounts to 100?");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    resetAutomaticTestBalance();
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

        });

        updateKeysButton.setOnClickListener(view -> {

            String api = apikeyET.getText().toString();
            String secret = secretET.getText().toString();

            String finalMessage = "Are You sure You want update keys to following values:\n\n" + "API-KEY: " + api + "\n\n" + "SECRET: " + secret;
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("UPDATE KEYS");
            builder.setMessage(finalMessage);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_STRING, api, ID, "4");
                    databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_STRING, secret, ID, "5");
                    apikeyET.setText("");
                    secretET.setText("");
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

        });

        updateParamsButton.setOnClickListener(view -> {

            String margin = marginET.getText().toString();
            String stopL = stopLossET.getText().toString();
            String takeP = takeProfitET.getText().toString();

            String finalMessage = "Are You sure You want update params of automatic to following values:\n\n" + "MARGIN: " + margin + "\n\n" + "SL: " + stopL + "\n\n" + "TP: " + takeP;
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("UPDATE PARAMS");
            builder.setMessage(finalMessage);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(margin), ID, "11");
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(stopL), ID, "12");
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(takeP), ID, "13");
                    marginET.setText("");
                    stopLossET.setText("");
                    takeProfitET.setText("");
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

        });

        updateWindowButton.setOnClickListener(view -> {

            String recvWindow = windowET.getText().toString();

            String finalMessage = "Are You sure You want update recWindow to following values:\n\n" + "recvWindow: " + recvWindow;
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("UPDATE recWindow");
            builder.setMessage(finalMessage);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(recvWindow), ID, "14");
                    windowET.setText("");
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

        });


        infoButton.setOnClickListener(view -> {
            showPagerCustomDialog();
        });

        showKeysButton.setOnClickListener(view -> {

            String apikey = "";
            String secret = "";
            String margin = "";
            String stopL = "";
            String takeP = "";
            String window = "";

            Cursor data = databaseDB.retrieveParam(4);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 4");
            } else {
                data.moveToFirst();
                apikey = data.getString(2);
            }
            data.close();
            Cursor data2 = databaseDB.retrieveParam(5);
            if (data2.getCount() == 0) {
                Log.e(TAG, "There is no param nr 5");
            } else {
                data2.moveToFirst();
                secret = data2.getString(2);
            }
            data2.close();

            data = databaseDB.retrieveParam(11);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 11");
            } else {
                data.moveToFirst();
                margin = String.valueOf(data.getInt(3));
            }
            data.close();

            data2 = databaseDB.retrieveParam(12);
            if (data2.getCount() == 0) {
                Log.e(TAG, "There is no param nr 12");
            } else {
                data2.moveToFirst();
                stopL = String.valueOf(data2.getInt(3));
            }
            data2.close();

            data = databaseDB.retrieveParam(13);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 13");
            } else {
                data.moveToFirst();
                takeP = String.valueOf(data.getInt(3));
            }
            data.close();

            data2 = databaseDB.retrieveParam(14);
            if (data2.getCount() == 0) {
                Log.e(TAG, "There is no param nr 14");
            } else {
                data2.moveToFirst();
                window = String.valueOf(data2.getInt(3));
            }
            data2.close();

            String toastMessage = "API-KEY: " + apikey + "\n\n" + "SECRET: " + secret + "\n\n" + "MARGIN: " + margin + "\n\n" + "SL: " + stopL + "\n\n" + "TP: " + takeP + "\n\n" + "recvWindow: " + window;

            View customLayout = getLayoutInflater().inflate(R.layout.custom_snackbar, null);
            Snackbar snackbar = Snackbar.make(root, "", Snackbar.LENGTH_SHORT);
            Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
            snackbarLayout.addView(customLayout, 0);

            TextView textView = customLayout.findViewById(R.id.message_for_snack);
            textView.setText(toastMessage);
            snackbar.show();


//            Snackbar snackbar = Snackbar
//                    .make(snackBarView, toastMessage, Snackbar.LENGTH_LONG)
//                    .setAction("UNDO", new View.OnClickListener() {
//                        @Override
//                        public void onClick(View view) {
//                            Snackbar snackbar1 = Snackbar.make(snackBarView, "Message is restored!", Snackbar.LENGTH_LONG);
//                            snackbar1.show();
//                        }
//                    });
//
//            snackbar.show();

        });

    }

    private void resetTestBalance() {
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "2");
    }

    private void resetAutomaticTestBalance() {
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "6");
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "7");
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "8");
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "9");
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "10");
    }

    // Create a custom dialog with a ViewPager
    public void showPagerCustomDialog() {

        // Create the dialog and set the content view
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_page_view, null);
        ViewPager2 viewPager = dialogView.findViewById(R.id.view_pager);

        // Create a new instance of the PagerAdapter class
        PagerAdapter pagerAdapter = new PagerAdapter(getActivity());
        viewPager.setAdapter(pagerAdapter);

        //Tabs attach
        TabLayout tabLayout = dialogView.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText("PAGE " + (position + 1))
        ).attach();
        // Set any additional properties on the dialog, such as title or buttons
        //builder.setTitle("Information");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle OK button click
            }
        });

        // Show the dialog
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //databaseDB.close();
        binding = null;
    }
}