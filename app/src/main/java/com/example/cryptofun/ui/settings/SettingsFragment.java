package com.example.cryptofun.ui.settings;

import static android.content.Context.INPUT_METHOD_SERVICE;

import androidx.appcompat.app.AlertDialog;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cryptofun.R;
import com.example.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.FragmentSettingsBinding;
import com.example.cryptofun.services.ServiceFunctions;
import com.example.cryptofun.ui.settings.infoBox.PagerAdapter;
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
    private Button resetTestBalanceButton, resetAutomaticTestBalanceButton, showKeysButton, updateParamsButton, accountInfoButton;
    private EditText apikeyET, secretET, marginET, takeProfitET, stopLossET, windowET;
    private FloatingActionButton infoButton;
    private DBHandler databaseDB;
    private SwitchCompat switchTest, switchReal;
    View root;
    InputMethodManager imm;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.e(TAG, "CreateView");

        imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);

        databaseDB = DBHandler.getInstance(getContext());
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        resetTestBalanceButton = binding.btResetTestBalance;
        resetAutomaticTestBalanceButton = binding.btResetAutomaticBalance;
        infoButton = binding.infoFloating;
        showKeysButton = binding.btShowKeys;
        apikeyET = binding.etApiKey;
        secretET = binding.etSecret;
        switchReal = binding.switchReal;
        switchTest = binding.switchTest;
        updateParamsButton = binding.btSetParams;
        marginET = binding.etMargin;
        stopLossET = binding.etStopLoss;
        takeProfitET = binding.etTakeProfit;
        windowET = binding.etRecvWindow;
        accountInfoButton = binding.btCheckAccountInfo;

        checkIfParamsArePresent();
        buttonsJob();
        return root;
    }

    private void checkIfParamsArePresent() {
        Cursor data = databaseDB.retrieveParam(4);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 4");
            databaseDB.addParam(4, "API-KEY", "null", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 4);
            databaseDB.addParam(4, "API-KEY", "null", 0, 0);
        }
        data.close();

        Cursor data2 = databaseDB.retrieveParam(5);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 5");
            databaseDB.addParam(5, "SECRET", "null", 0, 0);
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 5);
            databaseDB.addParam(5, "SECRET", "null", 0, 0);
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
            databaseDB.addParam(12, "Automatic SL", "", 0, 1);
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 12);
            databaseDB.addParam(12, "Automatic SL", "", 0, 1);
        }
        data2.close();

        data = databaseDB.retrieveParam(13);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 13");
            databaseDB.addParam(13, "Automatic TP", "", 0, 2);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 13);
            databaseDB.addParam(13, "Automatic TP", "", 0, 2);
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
        } else {
            data.moveToFirst();
            int value = data.getInt(3);
            switchReal.setChecked(value == 1);
        }
        data.close();

        data2 = databaseDB.retrieveParam(16);
        if (data2.getCount() == 0) {
            Log.e(TAG, "There is no param nr 16");
            databaseDB.addParam(16, "Stop automatic for TEST", "", 0, 0);
        } else if (data2.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 16);
            databaseDB.addParam(16, "Stop automatic for TEST", "", 0, 0);
        } else {
            data2.moveToFirst();
            int value = data2.getInt(3);
            switchTest.setChecked(value == 1);
        }
        data2.close();

    }

    private void buttonsJob() {

        resetTestBalanceButton.setOnClickListener(v -> {

            // Hide the keyboard
            imm.hideSoftInputFromWindow(root.getWindowToken(), 0);

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

            // Hide the keyboard
            imm.hideSoftInputFromWindow(root.getWindowToken(), 0);

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

        updateParamsButton.setOnClickListener(view -> {

            // Hide the keyboard
            imm.hideSoftInputFromWindow(root.getWindowToken(), 0);

            String margin = marginET.getText().toString();
            String stopL = stopLossET.getText().toString();
            String takeP = takeProfitET.getText().toString();
            String api = apikeyET.getText().toString();
            String secret = secretET.getText().toString();
            String recvWindow = windowET.getText().toString();

            String finalMessage = "Are You sure You want update params of automatic to following values:\n\n";
            int nrOfParams = 0;

            if (!margin.equals("") && margin.matches("\\d+")) {
                if (Integer.parseInt(margin) > 100 || Integer.parseInt(margin) < 0) {
                    Toast.makeText(getContext(), "Margin should be between 0 and 100", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "Margin: " + margin + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (!stopL.equals("")) {
                if (Float.parseFloat(stopL) > 50 || Float.parseFloat(stopL) < 0) {
                    Toast.makeText(getContext(), "StopLimit should be between 0 and 50", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "StopLimit: " + stopL + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (!takeP.equals("")) {
                if (Float.parseFloat(takeP) > 50 || Float.parseFloat(takeP) < 0) {
                    Toast.makeText(getContext(), "TakeProfit should be between 0 and 100", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "TakeProfit: " + takeP + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (!api.equals("")) {
                String add = "API-KEY: " + api + "\n\n";
                finalMessage += add;
                nrOfParams++;
            }
            if (!secret.equals("")) {
                String add = "Secret: " + secret + "\n\n";
                finalMessage += add;
                nrOfParams++;
            }
            if (!recvWindow.equals("") && recvWindow.matches("\\d+")) {
                if (Integer.parseInt(recvWindow) > 60000 || Integer.parseInt(recvWindow) < 5000) {
                    Toast.makeText(getContext(), "RecvWindow should be between 5000 and 60000", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "RecvWindow: " + recvWindow + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (nrOfParams == 0) {
                finalMessage = "None of the parameters were (correctly) completed.";
            }


            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("UPDATE PARAMS");
            builder.setMessage(finalMessage);
            String finalMessage1 = finalMessage;
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    if (finalMessage1.contains("Margin:")) {
                        databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(margin), ID, "11");
                    }
                    if (finalMessage1.contains("StopLimit:")) {
                        databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, Float.parseFloat(stopL), ID, "12");
                    }
                    if (finalMessage1.contains("TakeProfit:")) {
                        databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, Float.parseFloat(takeP), ID, "13");
                    }
                    if (finalMessage1.contains("API-KEY:")) {
                        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_STRING, api, ID, "4");
                    }
                    if (finalMessage1.contains("Secret:")) {
                        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_STRING, secret, ID, "5");
                    }
                    if (finalMessage1.contains("RecvWindow:")) {
                        databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(recvWindow), ID, "14");
                    }

                    marginET.setText("");
                    stopLossET.setText("");
                    takeProfitET.setText("");
                    apikeyET.setText("");
                    secretET.setText("");
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

            // Hide the keyboard
            imm.hideSoftInputFromWindow(root.getWindowToken(), 0);

            showPagerCustomDialog();
        });

        showKeysButton.setOnClickListener(view -> {

            // Hide the keyboard
            imm.hideSoftInputFromWindow(root.getWindowToken(), 0);

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
                stopL = String.valueOf(data2.getFloat(4));
            }
            data2.close();

            data = databaseDB.retrieveParam(13);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 13");
            } else {
                data.moveToFirst();
                takeP = String.valueOf(data.getFloat(4));
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

        });

        switchTest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, 1, ID, "16");
                } else {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, 0, ID, "16");
                }
            }
        });

        switchReal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, 1, ID, "15");
                } else {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, 0, ID, "15");
                }
            }
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