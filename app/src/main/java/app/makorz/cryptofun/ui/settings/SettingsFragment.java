package app.makorz.cryptofun.ui.settings;

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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cryptofun.R;
import app.makorz.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.FragmentSettingsBinding;
import app.makorz.cryptofun.services.ServiceFunctionsOther;
import app.makorz.cryptofun.ui.settings.infoBox.PagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFrag";

    private static final String TABLE_NAME_CONFIG = "config";
    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String DESCRIPTION = "description";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private static final String VALUE_STRING = "value_string";
    private static final String VALUE_INT = "value_int";
    private static final String VALUE_REAL = "value_real";
    private static final String IS_IT_REAL = "isReal";
    private static final String WHAT_ACCOUNT = "account_nr";
    private static final String TABLE_NAME_APPROVED_HISTORIC = "historic_approved_tokens";
    private static final String ID = "id";

    private FragmentSettingsBinding binding;
    private Button resetTestBalanceButton, resetAutomaticTestBalanceButton, resetApprovedListTableAndLogFilesButton, showKeysButton, updateParamsButton, accountInfoButton;
    private EditText apikeyET, secretET, marginET, takeProfitET, stopLossET, windowET, strategyET, stopLossStrategyET, emaLengthET, offsetSarET, hoursForApprovedET, globalHoursET, firstThresholdET, secondThresholdET, howOftenAlarmET;
    private Spinner emaTypeSpinner;
    private FloatingActionButton infoButton;
    private DBHandler databaseDB;
    private SwitchCompat switchTest, switchReal, switchGroupingApproved, switchGlobal;
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
        strategyET = binding.etStrategy;
        stopLossStrategyET = binding.etStopLossStrategy;
        emaLengthET = binding.etLengthForEma;
        offsetSarET = binding.etOffsetSAR;
        emaTypeSpinner = binding.spinnerEmaType;
        resetApprovedListTableAndLogFilesButton = binding.btOtherReset;
        switchGroupingApproved = binding.switchGroupingHistoricApproved;
        hoursForApprovedET = binding.etHowLongShowHistoricApproved;
        switchGlobal = binding.switchUsingGlobalTokenTrends;
        globalHoursET = binding.etHowManyHoursToLookBackForGlobal;
        firstThresholdET = binding.etFirstPercentThreshold;
        secondThresholdET = binding.etSecondPercentThreshold;
        howOftenAlarmET = binding.etHowOftenAlarmMustRun;
        checkIfParamsArePresent();
        buttonsJob();
        return root;
    }

    private void checkIfParamsArePresent() {

        Cursor data = databaseDB.retrieveParam(3);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 3");
            databaseDB.addParam(3, "How often run alarm to update database.", "", 1, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 3);
            databaseDB.addParam(3, "How often run alarm to update database.", "null", 1, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(4);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 4");
            databaseDB.addParam(4, "API-KEY", "null", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 4);
            databaseDB.addParam(4, "API-KEY", "null", 0, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(5);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 5");
            databaseDB.addParam(5, "SECRET", "null", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 5);
            databaseDB.addParam(5, "SECRET", "null", 0, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(11);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 11");
            databaseDB.addParam(11, "Automatic margin", "", 1, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 11);
            databaseDB.addParam(11, "Automatic margin", "", 1, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(12);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 12");
            databaseDB.addParam(12, "Automatic SL", "", 0, 1);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 12);
            databaseDB.addParam(12, "Automatic SL", "", 0, 1);
        }
        data.close();

        data = databaseDB.retrieveParam(13);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 13");
            databaseDB.addParam(13, "Automatic TP", "", 0, 2);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 13);
            databaseDB.addParam(13, "Automatic TP", "", 0, 2);
        }
        data.close();

        data = databaseDB.retrieveParam(14);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 14");
            databaseDB.addParam(14, "recvWindow", "", 1000, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 14);
            databaseDB.addParam(14, "recvWindow", "", 1000, 0);
        }
        data.close();

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

        data = databaseDB.retrieveParam(16);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 16");
            databaseDB.addParam(16, "Stop automatic for TEST", "", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 16);
            databaseDB.addParam(16, "Stop automatic for TEST", "", 0, 0);
        } else {
            data.moveToFirst();
            int value = data.getInt(3);
            switchTest.setChecked(value == 1);
        }
        data.close();

        data = databaseDB.retrieveParam(17);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 17");
            databaseDB.addParam(17, "Active Strategy Nr:", "", 1, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 17);
            databaseDB.addParam(17, "Active Strategy Nr:", "", 1, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(18);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 18");
            databaseDB.addParam(18, "Active SL Strategy Nr:", "", 1, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 18);
            databaseDB.addParam(18, "Active SL Strategy Nr:", "", 1, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(19);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 19");
            databaseDB.addParam(19, "Length of EMA for SL:", "", 31, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 19);
            databaseDB.addParam(19, "Length of EMA for SL:", "", 31, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(20);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 20");
            databaseDB.addParam(20, "What EMA for SL:", "hlc3", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 20);
            databaseDB.addParam(20, "What EMA for SL:", "hlc3", 0, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(21);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 21");
            databaseDB.addParam(21, "Offset for SAR: ", "", 0, 1);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 21);
            databaseDB.addParam(21, "Offset for SAR: ", "", 0, 1);
        }
        data.close();

        data = databaseDB.retrieveParam(22);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 22");
            databaseDB.addParam(22, "How many hours back to show historic approved tokens.", "", 12, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 22);
            databaseDB.addParam(22, "How many hours back to show historic approved tokens.", "", 12, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(23);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 23");
            databaseDB.addParam(23, "Group historic approved token on home screen?", "", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 23);
            databaseDB.addParam(23, "Group historic approved token on home screen?", "", 0, 0);
        } else {
            data.moveToFirst();
            int value = data.getInt(3);
            switchGroupingApproved.setChecked(value == 1);
        }
        data.close();

        data = databaseDB.retrieveParam(24);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 24");
            databaseDB.addParam(24, "Global criteria turn on.", "", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 24);
            databaseDB.addParam(24, "Global criteria turn on", "", 0, 0);
        } else {
            data.moveToFirst();
            int value = data.getInt(3);
            switchGlobal.setChecked(value == 1);
        }
        data.close();

        data = databaseDB.retrieveParam(25);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 25");
            databaseDB.addParam(25, "How many hours back for global criteria.", "", 4, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 25);
            databaseDB.addParam(25, "How many hours back for global criteria.", "", 4, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(26);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 26");
            databaseDB.addParam(26, "First % threshold.", "", 1, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 26);
            databaseDB.addParam(26, "First % threshold.", "", 1, 0);
        }
        data.close();

        data = databaseDB.retrieveParam(27);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 27");
            databaseDB.addParam(27, "Second % threshold.", "", 2, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 27);
            databaseDB.addParam(27, "Second % threshold.", "", 2, 0);
        }
        data.close();

    }

    private void buttonsJob() {

//        accountInfoButton.setOnClickListener(v -> {
//
//            Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
//            List<String> listOfSymbols = new ArrayList<>();
//            data.moveToFirst();
//            if (data.getCount() == 0) {
//                Log.e(TAG, "Table " + TABLE_SYMBOL_AVG + " is empty. [settingsFragment]");
//            } else {
//                data.moveToFirst();
//                do {
//                    listOfSymbols.add(data.getString(0));
//                } while (data.moveToNext());
//                for (int i = 0; i < listOfSymbols.size(); i++) {
//                    countBestCryptoToBuy(listOfSymbols.get(i));
//                }
//            }
//            data.close();
//        });

        emaTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_STRING, emaTypeSpinner.getSelectedItem().toString(), ID, "20");
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        resetApprovedListTableAndLogFilesButton.setOnClickListener(v -> {

            // Hide the keyboard
            imm.hideSoftInputFromWindow(root.getWindowToken(), 0);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("RESET TEST BALANCE");
            builder.setMessage("Are You sure You want reset approved tokens and logs");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    resetRealBalance();
                    databaseDB.clearTable(TABLE_NAME_APPROVED_HISTORIC);
                    ServiceFunctionsOther.deleteFile(getContext(), "orders");
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
            String strategy = strategyET.getText().toString();
            String stopLossStrategy = stopLossStrategyET.getText().toString();
            String offsetSAR = offsetSarET.getText().toString();
            String lengthEMA = emaLengthET.getText().toString();
            String howManyHours = hoursForApprovedET.getText().toString();
            String howManyHoursGlobal = globalHoursET.getText().toString();
            String firstThreshold = firstThresholdET.getText().toString();
            String secondThreshold = secondThresholdET.getText().toString();
            String howOftenAlarm = howOftenAlarmET.getText().toString();


            String finalMessage = "Are You sure You want update params of automatic to following values:\n\n";
            int nrOfParams = 0;

            if (!howOftenAlarm.equals("")) {
                if (Integer.parseInt(howOftenAlarm) > 60 || Integer.parseInt(howOftenAlarm) < 1) {
                    Toast.makeText(getContext(), "Update database between 1 and 60min.", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "DB update alarm: " + howOftenAlarm + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }

            if (!margin.equals("") && margin.matches("\\d+")) {
                if (Integer.parseInt(margin) >= 25 || Integer.parseInt(margin) < 2) {
                    Toast.makeText(getContext(), "Margin should be between 0 and 25", Toast.LENGTH_SHORT).show();
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
                if (Float.parseFloat(takeP) > 1000 || Float.parseFloat(takeP) < 0) {
                    Toast.makeText(getContext(), "TakeProfit should be between 0 and 1000", Toast.LENGTH_SHORT).show();
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
            if (!strategy.equals("")) {
                if (Float.parseFloat(strategy) > 8 || Float.parseFloat(strategy) < 1) {
                    Toast.makeText(getContext(), "Strategy between 1 and 8", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "Main Strategy: " + strategy + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (!stopLossStrategy.equals("")) {
                if (Float.parseFloat(stopLossStrategy) > 3 || Float.parseFloat(stopLossStrategy) < 1) {
                    Toast.makeText(getContext(), "SL Strategy between 1 and 3", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "SL Strategy: " + stopLossStrategy + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
              if (!offsetSAR.equals("")) {
                if (Float.parseFloat(offsetSAR) > 25 || Float.parseFloat(offsetSAR) < 0.1) {
                    Toast.makeText(getContext(), "Offset between 0.1 and 25", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "SL offset SAR: " + offsetSAR + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (!lengthEMA.equals("")) {
                if (Float.parseFloat(lengthEMA) > 200 || Float.parseFloat(lengthEMA) < 10) {
                    Toast.makeText(getContext(), "SL EMA length between 10 and 200", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "SL EMA length: " + lengthEMA + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (!howManyHours.equals("")) {
                if (Integer.parseInt(howManyHours) > 24 || Integer.parseInt(howManyHours) < 6) {
                    Toast.makeText(getContext(), "Hours should be between 6 and 24", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "Hours to show historic: " + howManyHours + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (!howManyHoursGlobal.equals("")) {
                if (Integer.parseInt(howManyHoursGlobal) > 12 || Integer.parseInt(howManyHoursGlobal) < 1) {
                    Toast.makeText(getContext(), "Global Hours should be between 1 and 12", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "Hours global criteria: " + howManyHoursGlobal + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (!firstThreshold.equals("")) {
                if(secondThreshold.equals("")) {
                    Toast.makeText(getContext(), "To fill thresholds enter value for second as well.", Toast.LENGTH_SHORT).show();
                } else if (Integer.parseInt(firstThreshold) > 10 || Integer.parseInt(firstThreshold) < 1) {
                    Toast.makeText(getContext(), "First % threshold should be between 1 and 10", Toast.LENGTH_SHORT).show();
                } else if (Integer.parseInt(firstThreshold) > Integer.parseInt(secondThreshold)) {
                    Toast.makeText(getContext(), "First % threshold should be lower than second % threshold.", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "First threshold: " + firstThreshold + "\n\n";
                    finalMessage += add;
                    nrOfParams++;
                }
            }
            if (!secondThreshold.equals("")) {
                if(firstThreshold.equals("")) {
                    Toast.makeText(getContext(), "To fill thresholds enter value for first as well.", Toast.LENGTH_SHORT).show();
                }else  if (Integer.parseInt(secondThreshold) > 20 || Integer.parseInt(secondThreshold) < 2) {
                    Toast.makeText(getContext(), "Second % threshold should be between 1 and 10", Toast.LENGTH_SHORT).show();
                } else if (Integer.parseInt(firstThreshold) > Integer.parseInt(secondThreshold)) {
                    Toast.makeText(getContext(), "Second % threshold should be higher than second % threshold.", Toast.LENGTH_SHORT).show();
                } else {
                    String add = "Second threshold: " + secondThreshold + "\n\n";
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

                    if (finalMessage1.contains("DB update alarm:")) {
                        databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(howOftenAlarm), ID, "3");
                    }
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
                    if (finalMessage1.contains("Main Strategy:")) {
                        databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(strategy), ID, "17");
                    }
                    if (finalMessage1.contains("SL Strategy:")) {
                        databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(stopLossStrategy), ID, "18");
                    }
                    if (finalMessage1.contains("SL EMA")) {
                        databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(lengthEMA), ID, "19");
                    }
                    if (finalMessage1.contains("SL offset SAR:")) {
                        databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_REAL, Float.parseFloat(offsetSAR), ID, "21");
                    }
                    if (finalMessage1.contains("Hours to show historic:")) {
                        databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(howManyHours), ID, "22");
                    }
                    if (finalMessage1.contains("Hours global criteria:")) {
                        databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(howManyHoursGlobal), ID, "25");
                    }
                    if (finalMessage1.contains("First threshold: ")) {
                        databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(firstThreshold), ID, "26");
                    }
                    if (finalMessage1.contains("Second threshold:")) {
                        databaseDB.updateWithWhereClauseREAL(TABLE_NAME_CONFIG, VALUE_INT, Integer.parseInt(secondThreshold), ID, "27");
                    }

                    marginET.setText("");
                    stopLossET.setText("");
                    takeProfitET.setText("");
                    apikeyET.setText("");
                    secretET.setText("");
                    windowET.setText("");
                    strategyET.setText("");
                    stopLossStrategyET.setText("");
                    emaLengthET.setText("");
                    offsetSarET.setText("");
                    hoursForApprovedET.setText("");
                    globalHoursET.setText("");
                    firstThresholdET.setText("");
                    secondThresholdET.setText("");
                    howOftenAlarmET.setText("");
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

            String alarm = "";
            String apikey = "";
            String secret = "";
            String margin = "";
            String stopL = "";
            String takeP = "";
            String window = "";
            String strategy = "";
            String stopLossStrategy = "";
            String offsetSAR = "";
            String lengthEMA = "";
            String emaType = "";
            String howManyHours = "";
            String howManyHoursGlobal = "";
            String firstThreshold = "";
            String secondThreshold = "";

            Cursor data = databaseDB.retrieveParam(3);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 3");
            } else {
                data.moveToFirst();
                alarm = String.valueOf(data.getInt(3));
            }
            data.close();
            data = databaseDB.retrieveParam(4);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 4");
            } else {
                data.moveToFirst();
                apikey = data.getString(2);
            }
            data.close();
            data = databaseDB.retrieveParam(5);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 5");
            } else {
                data.moveToFirst();
                secret = data.getString(2);
            }
            data.close();

            data = databaseDB.retrieveParam(11);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 11");
            } else {
                data.moveToFirst();
                margin = String.valueOf(data.getInt(3));
            }
            data.close();

            data = databaseDB.retrieveParam(12);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 12");
            } else {
                data.moveToFirst();
                stopL = String.valueOf(data.getFloat(4));
            }
            data.close();

            data = databaseDB.retrieveParam(13);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 13");
            } else {
                data.moveToFirst();
                takeP = String.valueOf(data.getFloat(4));
            }
            data.close();

            data = databaseDB.retrieveParam(14);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 14");
            } else {
                data.moveToFirst();
                window = String.valueOf(data.getInt(3));
            }
            data.close();

            data = databaseDB.retrieveParam(17);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 17");
            } else {
                data.moveToFirst();
                strategy = String.valueOf(data.getInt(3));
            }
            data.close();

            data = databaseDB.retrieveParam(18);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 18");
            } else {
                data.moveToFirst();
                stopLossStrategy = String.valueOf(data.getInt(3));
            }
            data.close();

            data = databaseDB.retrieveParam(19);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 19");
            } else {
                data.moveToFirst();
                lengthEMA = String.valueOf(data.getInt(3));
            }
            data.close();

            data = databaseDB.retrieveParam(20);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 20");
            } else {
                data.moveToFirst();
                emaType = data.getString(2);
            }
            data.close();

            data = databaseDB.retrieveParam(21);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 21");
            } else {
                data.moveToFirst();
                offsetSAR = String.valueOf(data.getFloat(4));
            }
            data.close();

            data = databaseDB.retrieveParam(22);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 22");
            } else {
                data.moveToFirst();
                howManyHours = String.valueOf(data.getInt(3));
            }
            data.close();

            data = databaseDB.retrieveParam(25);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 25");
            } else {
                data.moveToFirst();
                howManyHoursGlobal = String.valueOf(data.getInt(3));
            }
            data.close();

            data = databaseDB.retrieveParam(26);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 26");
            } else {
                data.moveToFirst();
                firstThreshold = String.valueOf(data.getInt(3));
            }
            data.close();

            data = databaseDB.retrieveParam(27);
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param nr 27");
            } else {
                data.moveToFirst();
                secondThreshold = String.valueOf(data.getInt(3));
            }
            data.close();

            String toastMessage;
            if (switchGlobal.isChecked()) {
                toastMessage = "API-KEY: " + apikey + "\n\n" + "SECRET: " + secret + "\n\n" + "MARGIN: " + margin + "\n\n" + "SL: " + stopL + "\n\n" + "TP: " + takeP + "\n\n" + "recvWindow: " + window + "\n\n" + "Strategy: " + strategy + "\n\n" + "How often update DB: " + alarm + "\n\n" + "SL Strategy: " + stopLossStrategy + "\n\n" + "SL EMA length: " + lengthEMA + "\n\n" + "SL EMA type: " + emaType + "\n\n" + "SL offset SAR: " + offsetSAR + "\n\n" + "Hours to show historic: " + howManyHours + "\n\n" + "Hours global criteria: " + howManyHoursGlobal + "\n\n" + "First % threshold: " + firstThreshold + "\n\n" + "Second % threshold: " + secondThreshold;

            } else {
                toastMessage = "API-KEY: " + apikey + "\n\n" + "SECRET: " + secret + "\n\n" + "MARGIN: " + margin + "\n\n" + "SL: " + stopL + "\n\n" + "TP: " + takeP + "\n\n" + "recvWindow: " + window + "\n\n" + "Strategy: " + strategy + "\n\n" + "How often update DB: " + alarm + "\n\n" + "SL Strategy: " + stopLossStrategy + "\n\n" + "SL EMA length: " + lengthEMA + "\n\n" + "SL EMA type: " + emaType + "\n\n" + "SL offset SAR: " + offsetSAR + "\n\n" + "Hours to show historic: " + howManyHours;

            }

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

        switchGroupingApproved.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, 1, ID, "23");
                } else {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, 0, ID, "23");
                }
            }
        });

        switchGlobal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, 1, ID, "24");
                } else {
                    databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, 0, ID, "24");
                }
            }
        });

    }

    private void resetTestBalance() {
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "2");
        databaseDB.deleteResultsOfClosedOrders(0,false);
    }

    private void resetRealBalance() {
        databaseDB.deleteResultsOfClosedOrders(1,false);
    }

    private void resetAutomaticTestBalance() {
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "6");
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "7");
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "8");
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "9");
        databaseDB.updateWithWhereClause(TABLE_NAME_CONFIG, VALUE_REAL, String.valueOf(100), ID, "10");
        databaseDB.deleteResultsOfClosedOrders(0,true);

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