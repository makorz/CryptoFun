package app.makorz.cryptofun.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.os.BuildCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.BuildConfig;
import com.google.gson.internal.GsonBuildConfig;

import app.makorz.cryptofun.data.MarkPrice;
import app.makorz.cryptofun.data.PercentagesOfChanges;
import app.makorz.cryptofun.data.database.DBHandler;
import app.makorz.cryptofun.retrofit.RetrofitClientFutures;
import app.makorz.cryptofun.ui.home.GridViewElement;
import app.makorz.cryptofun.ui.home.ListViewElement;
import app.makorz.cryptofun.ui.orders.OrderListViewElement;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.mail.MessagingException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ApprovingService extends Service {

    private static final String TAG = "APRVService";

    private static final String TABLE_HISTORIC_PERCENTAGES = "history_percentages";
    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private static final String VALUE_INT = "value_int";
    private static final String ID = "id";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String TABLE_NAME_ORDERS = "current_orders";
    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String TABLE_NAME_APPROVED_HISTORIC = "historic_approved_tokens";

    int howManyNeedsToDo = 6;
    int serviceFinishedEverything = 0;
    int isAutomaticForTestEnabled = 0;
    int isAutomaticForRealEnabled = 0;
    private DBHandler databaseDB;
    private Handler handler;
    ArrayList<ListViewElement> lastChosenHoursTokensStat = new ArrayList<>();
    ArrayList<ListViewElement> last3MinutesTokensStat = new ArrayList<>();
    ArrayList<GridViewElement> cryptoGridViewList = new ArrayList<>();
    ArrayList<ListViewElement> cryptoForLONGOrders = new ArrayList<>();
    ArrayList<ListViewElement> cryptoForSHORTOrders = new ArrayList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        databaseDB = DBHandler.getInstance(getApplicationContext());

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        Log.e(TAG, "START of Service Thread");

                        // It's for foreground services, because in newest Android, background are not working. Foreground need to inform user that it is running
                        Notification notification = ServiceFunctionsOther.createNotificationSimple("Calculating what's best.", TAG, getApplicationContext());
                        // Notification ID cannot be 0.
                        startForeground(1, notification);
                        approvingCryptos();

                    }
                }
        ).start();

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "DESTROY");
    }

    private void sendMessageToActivity() {
        Log.e(TAG, "RESULT: " + serviceFinishedEverything + " HOW: " + howManyNeedsToDo);

        if (isAutomaticForRealEnabled == 1) {

            //Verification of missed orders remote and local
            ArrayList<OrderListViewElement> returnList = new ArrayList<>();
            Cursor data = databaseDB.retrieveAllFromTable(TABLE_NAME_ORDERS);
            data.moveToFirst();
            if (data.getCount() > 0) {
                do {
                    OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getLong(13), data.getString(14), data.getFloat(15));
                    returnList.add(tempToken);
                } while (data.moveToNext());
            }
            data.close();
            ServiceFunctionsAPI.getAllOrders(System.currentTimeMillis(), getApplicationContext(), returnList);
        }

        if (serviceFinishedEverything >= howManyNeedsToDo) {
            Intent intent = new Intent("ApprovedService");
            Log.e(TAG, "BROADCAST - Approve Finished " + Thread.currentThread() + " " + Thread.activeCount());
            Bundle bundle = new Bundle();
            // 1 - 30min, 2 - 2h, 3 - 6h
            bundle.putSerializable("list1", (Serializable) last3MinutesTokensStat);
            bundle.putSerializable("list3", (Serializable) lastChosenHoursTokensStat);
            bundle.putSerializable("cryptoGridViewList", (Serializable) cryptoGridViewList);
            intent.putExtra("bundleApprovedCrypto", bundle);
            LocalBroadcastManager.getInstance(ApprovingService.this).sendBroadcast(intent);

            //Wait 3 second before stopping service, error:  Process: com.example.cryptofun, PID: 6921 android.app.ForegroundServiceDidNotStartInTimeException: Context.startForegroundService() did not then cal Service.startForeground():
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopForeground(true);
                    stopSelf();
                }
            }, 2000);

        }
    }

    public void approvingCryptos() {

        //Wait two minutes before entering
        long howManyHours, threeMinutes = 180000;

        Cursor data = databaseDB.retrieveParam(22);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 22");
            howManyHours = 12 * 60 * 60 * 1000L;
        } else {
            data.moveToFirst();
            howManyHours = (data.getInt(3)) * 60 * 60 * 1000L;
        }
        data.close();

        boolean shouldApprovedHistoricBeGrouped;

        data = databaseDB.retrieveParam(23);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 23");
            shouldApprovedHistoricBeGrouped = false;
        } else {
            data.moveToFirst();
            int value = data.getInt(3);
            shouldApprovedHistoricBeGrouped = value != 0;
        }
        data.close();

        int strategyNumber;

        data = databaseDB.retrieveParam(17);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 17");
            strategyNumber = 0;
        } else {
            data.moveToFirst();
            strategyNumber = data.getInt(3);
        }
        data.close();

        lastChosenHoursTokensStat = getListOfSymbolsAccordingToProvidedTime(howManyHours, 0, TABLE_NAME_APPROVED_HISTORIC, shouldApprovedHistoricBeGrouped);

        for (ListViewElement symbolObj : lastChosenHoursTokensStat) {
            // Update the symbol by replacing "USDT" with an empty string
            if (symbolObj.getText().contains("USDT")) {
                String newText = symbolObj.getText().replace("USDT", "");
                symbolObj.setText(newText);
            } else {
                Log.e(TAG, "\"USDT\" not found in: " + symbolObj.getText());
            }
        }

        last3MinutesTokensStat = getListOfSymbolsAccordingToProvidedTime(threeMinutes, 0, TABLE_NAME_APPROVED, false);

        Log.e(TAG, "list3mTokensSize: " + last3MinutesTokensStat.size());

        if (!last3MinutesTokensStat.isEmpty()) {

            Log.e(TAG, "EMAIL size: " + last3MinutesTokensStat.size() + "time: " + last3MinutesTokensStat.get(0).getTime() );
            String topic = "NEW TOKENS caught at " + last3MinutesTokensStat.get(0).getTime();
            // String topic = "NEW TOKENS for strategy nr " + strategyNumber;
            StringBuilder body = new StringBuilder();
            String shortOrNot= "";
            for (int i = 0; i < last3MinutesTokensStat.size(); i++) {

                if (last3MinutesTokensStat.get(i).isItLONG()) {
                    cryptoForLONGOrders.add(last3MinutesTokensStat.get(i));
                    shortOrNot = "LONG";
                } else {
                    cryptoForSHORTOrders.add(last3MinutesTokensStat.get(i));
                    shortOrNot = "SHORT";
                }

                body.append(i + 1).append(". ").append("Strategy nr: [").append(last3MinutesTokensStat.get(i).getStrategyNr()).append("] Token: [").append(last3MinutesTokensStat.get(i).getText()).append("] Type: [").append(shortOrNot).append("] Price: [").append(last3MinutesTokensStat.get(i).getPriceWhenCaught()).append("]\n");

//                body.append(i + 1).append(". ").append(shortOrNot).append(" for token ").append(last3MinutesTokensStat.get(i).getText()).append(" with strategy ").append(last3MinutesTokensStat.get(i).getStrategyNr()).append(" found at ").append(last3MinutesTokensStat.get(i).getTime()).append(" with price ").append(last3MinutesTokensStat.get(i).getPriceWhenCaught()).append("\n");

            }

            EmailSender emailSender = new EmailSender();
            try {
                emailSender.sendEmail(BuildConfig.SMTP_USERNAME, BuildConfig.SMTP_PASSWORD, topic, String.valueOf(body));
            } catch (Exception e) {
                System.err.println("General error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        setMainParametersOnView();

    }

    public void setMainParametersOnView() {

        // First array list for grid view, other two need to calculate changes in percents hour after hour
        // ArrayList<GridViewElement> cryptoPercentageListGrid3hours = new ArrayList<>();
        float percentUnderSecondThreshold = 0, percentUnderFirstThreshold = 0, percentUnderZero = 0, percentOverZero = 0, percentOverFirstThreshold = 0, percentOverSecondThreshold = 0;
        int firstThreshold, secondThreshold, howManyHoursGlobal;
        int underSecondThreshold = 0, underFirstThreshold = 0, underZero = 0, overZero = 0, overFirstThreshold = 0, overSecondThreshold = 0;
        boolean doWeUseGlobalCriteria;
        int nrOfGridElements = 30;
        List<String> biggestSymbols = new ArrayList<>();
        List<Float> percentChange = new ArrayList<>();

        Cursor data = databaseDB.retrieveParam(24);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 24");
            doWeUseGlobalCriteria = false;
        } else {
            data.moveToFirst();
            doWeUseGlobalCriteria = data.getInt(3) != 0;
        }
        data.close();
        data = databaseDB.retrieveParam(25);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 25");
            howManyHoursGlobal = 4;
        } else {
            data.moveToFirst();
            howManyHoursGlobal = data.getInt(3);
        }
        data.close();
        data = databaseDB.retrieveParam(26);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 26");
            firstThreshold = 1;
        } else {
            data.moveToFirst();
            firstThreshold = data.getInt(3);
        }
        data.close();
        data = databaseDB.retrieveParam(27);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 27");
            secondThreshold = 2;
        } else {
            data.moveToFirst();
            secondThreshold = data.getInt(3);
        }
        data.close();

        // Check if AUTOMATIC is turned on.
        data = databaseDB.retrieveParam(16);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 16");
        } else {
            data.moveToFirst();
            isAutomaticForTestEnabled = data.getInt(3);
        }
        data.close();

        data = databaseDB.retrieveParam(15);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 15");
        } else {
            data.moveToFirst();
            isAutomaticForRealEnabled = data.getInt(3);
        }
        data.close();

        //Get symbols that will be showed for gridview
        data = databaseDB.getBiggestNrOfTradesSymbolsToCertainTime(howManyHoursGlobal, nrOfGridElements);
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_NAME_KLINES_DATA + " is empty");
        } else {
            do {
                biggestSymbols.add(data.getString(0));
            } while (data.moveToNext());
        }

        //Get percent change of crypto between certain time
        ArrayList<GridViewElement> tempList = new ArrayList<>();
        data = databaseDB.getCryptoPercentChangeAccordingToCertainTime(howManyHoursGlobal, true, 1);
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "Table " + TABLE_NAME_KLINES_DATA + " is empty");
        } else {
            do {
                //Log.e(TAG, "Percentages: " +  data.getString(0) + " " + data.getFloat(1) + " " +  data.getFloat(2) + " " + data.getFloat(3));
                percentChange.add(data.getFloat(3));
                tempList.add(new GridViewElement(data.getString(0), data.getFloat(3), data.getFloat(1)));
            } while (data.moveToNext());
        }
        data.close();

        for (int i = 0; i < percentChange.size(); i++) {

            if (percentChange.get(i) < secondThreshold * -1) {
                underSecondThreshold++;
            } else if (percentChange.get(i) >= secondThreshold * -1 && percentChange.get(i) < firstThreshold * -1) {
                underFirstThreshold++;
            } else if (percentChange.get(i) >= firstThreshold * -1 && percentChange.get(i) < 0) {
                underZero++;
            } else if (percentChange.get(i) >= 0 && percentChange.get(i) < firstThreshold) {
                overZero++;
            } else if (percentChange.get(i) >= firstThreshold && percentChange.get(i) <= secondThreshold) {
                overFirstThreshold++;
            } else if (percentChange.get(i) > secondThreshold) {
                overSecondThreshold++;
            }
        }

        percentUnderSecondThreshold = (float) underSecondThreshold / percentChange.size() * 100;
        percentUnderFirstThreshold = (float) underFirstThreshold / percentChange.size() * 100;
        percentUnderZero = (float) underZero / percentChange.size() * 100;
        percentOverZero = (float) overZero / percentChange.size() * 100;
        percentOverFirstThreshold = (float) overFirstThreshold / percentChange.size() * 100;
        percentOverSecondThreshold = (float) overSecondThreshold / percentChange.size() * 100;

        cryptoGridViewList.add(new GridViewElement("< -" + secondThreshold + "%", percentUnderSecondThreshold, underSecondThreshold));
        cryptoGridViewList.add(new GridViewElement("-" + secondThreshold + "% - -" + firstThreshold + "%", percentUnderFirstThreshold, underFirstThreshold));
        cryptoGridViewList.add(new GridViewElement("-" + firstThreshold + "% - 0%", percentUnderZero, underZero));
        cryptoGridViewList.add(new GridViewElement("0% - " + firstThreshold + "%", percentOverZero, overZero));
        cryptoGridViewList.add(new GridViewElement(firstThreshold + "% - " + secondThreshold + "%", percentOverFirstThreshold, overFirstThreshold));
        cryptoGridViewList.add(new GridViewElement("> " + secondThreshold + "%", percentOverSecondThreshold, overSecondThreshold));

        for (int i = 0; i < biggestSymbols.size(); i++) {
            for (int j = 0; j < tempList.size(); j++) {
                if (tempList.get(j).getSymbol().equals(biggestSymbols.get(i))) {
                    biggestSymbols.set(i, biggestSymbols.get(i).replace("USDT", ""));
                    cryptoGridViewList.add(new GridViewElement(biggestSymbols.get(i), tempList.get(j).getPercent(), tempList.get(j).getValue()));
                }
            }
        }

        //Verification of percentages global table
        data = databaseDB.retrieveParam(30);
        data.moveToFirst();
        long timeOfPercentagesUpdate = 0;
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 30");
            databaseDB.addParam(30, "Time when global percentages were last updated.", "", 0, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 30);
            databaseDB.addParam(30, "Time when global percentages were last updated.", "", 0, 0);
        } else {
            timeOfPercentagesUpdate = data.getInt(3);
        }
        data.close();

        //Verification of last value of param hours for global
        data = databaseDB.retrieveParam(31);
        data.moveToFirst();
        int valueHoursPrevious = 0;
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 31");
            databaseDB.addParam(31, "Global hours check.", "", howManyHoursGlobal, 0);
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 31);
            databaseDB.addParam(1, "Global hours check.", "", howManyHoursGlobal, 0);
        } else {
            valueHoursPrevious = data.getInt(3);
        }
        data.close();


        //Check if last entry in percentages was over 30 minutes ago - then complete calculation again
        if (valueHoursPrevious == howManyHoursGlobal) {

            if (System.currentTimeMillis() - 30 * 60 * 1000L > timeOfPercentagesUpdate) {
                Log.e(TAG, "GLOBAL A hours previous" + valueHoursPrevious + " how many hours " + howManyHoursGlobal);
                globalCriteriaCalculating(howManyHoursGlobal, firstThreshold, secondThreshold);
            } else if (System.currentTimeMillis() - 15 * 60 * 1000L > timeOfPercentagesUpdate) {
                Log.e(TAG, "GLOBAL B hours previous" + valueHoursPrevious + " how many hours " + howManyHoursGlobal);
                PercentagesOfChanges changes = new PercentagesOfChanges(percentUnderSecondThreshold, percentUnderFirstThreshold, percentUnderZero, percentOverZero, percentOverFirstThreshold, percentOverSecondThreshold, System.currentTimeMillis());
                databaseDB.addPercentages(changes);
            } else {
                Log.e(TAG, "GLOBAL C hours previous" + valueHoursPrevious + " how many hours " + howManyHoursGlobal);
                databaseDB.clearTable(TABLE_HISTORIC_PERCENTAGES);
            }
        } else {
            Log.e(TAG, "GLOBAL D hours previous" + valueHoursPrevious + " how many hours " + howManyHoursGlobal);
            globalCriteriaCalculating(howManyHoursGlobal, firstThreshold, secondThreshold);
            databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, howManyHoursGlobal, ID, "31");
        }

        //If it is on => go on
        if (isAutomaticForTestEnabled == 1 || isAutomaticForRealEnabled == 1) {

            ArrayList<PercentagesOfChanges> percentages = new ArrayList<>();
            data = databaseDB.retrievePercentages(System.currentTimeMillis() - howManyHoursGlobal * 60 * 60 * 1000L, System.currentTimeMillis());
            data.moveToFirst();
            if (data.getCount() == 0) {
                Log.e(TAG, "Table " + TABLE_HISTORIC_PERCENTAGES + " is empty");
            } else {
                do {
                    percentages.add(new PercentagesOfChanges(data.getFloat(1), data.getFloat(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getLong(7)));
                } while (data.moveToNext());
            }
            data.close();
            boolean isItGoodForShort;
            boolean isItGoodForLong;
            isItGoodForShort = isPercentageInFavorV2(percentages,1,2);
            isItGoodForLong = isPercentageInFavorV2(percentages,0,2);

//            isItGoodForLong = isPercentageInFavorV3(percentages, 0);
//            isItGoodForShort = isPercentageInFavorV3(percentages, 1);
            databaseDB.clearHistoricPercentages(System.currentTimeMillis() - 12 * 60 * 60 * 1000L);


            String infoOfOrder = "LEVEL 3 AutomaticTest: " + isAutomaticForTestEnabled + " AutomaticReal: " + isAutomaticForRealEnabled + " Percentage Favor(long, short, size): " + isItGoodForLong + " " + isItGoodForShort + " " + percentages.size() + " ListSize(Long, Short): " + cryptoForLONGOrders.size() + " " + cryptoForSHORTOrders.size() + " Under: " + percentUnderZero + "% " + percentUnderFirstThreshold + "% " + percentUnderSecondThreshold + " Over: " + percentOverZero + "% " + percentOverFirstThreshold + "% " + percentOverSecondThreshold + "%";
            Log.e(TAG, infoOfOrder);
            ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "result");

            if (doWeUseGlobalCriteria) {
                if (isItGoodForShort && cryptoForSHORTOrders.size() > 0) { //cryptoForSHORTOrders.size() > cryptoForLONGOrders.size()
                    serviceFinishedEverything++;
                    makeOrdersFunction(cryptoForSHORTOrders, isAutomaticForTestEnabled, isAutomaticForRealEnabled);

                } else if (isItGoodForLong && cryptoForLONGOrders.size() > 0) { //cryptoForLONGOrders.size() > cryptoForSHORTOrders.size() &&
                    serviceFinishedEverything++;
                    makeOrdersFunction(cryptoForLONGOrders, isAutomaticForTestEnabled, isAutomaticForRealEnabled);

                } else {
                    serviceFinishedEverything = 6;
                    sendMessageToActivity();
                }
            } else {
                ArrayList<ListViewElement> combinedList = new ArrayList<>(cryptoForSHORTOrders);
                combinedList.addAll(cryptoForLONGOrders);
                Collections.shuffle(combinedList);

                if (combinedList.size() > 0) {
                    serviceFinishedEverything++;
                    makeOrdersFunction(combinedList, isAutomaticForTestEnabled, isAutomaticForRealEnabled);
                } else  {
                    serviceFinishedEverything = 6;
                    sendMessageToActivity();
                }


//                if (cryptoForSHORTOrders.size() > cryptoForLONGOrders.size()) {
//                    serviceFinishedEverything++;
//                    makeOrdersFunction(cryptoForSHORTOrders, isAutomaticForTestEnabled, isAutomaticForRealEnabled);
//                } else if (cryptoForLONGOrders.size() > cryptoForSHORTOrders.size()) {
//                    serviceFinishedEverything++;
//                    makeOrdersFunction(cryptoForLONGOrders, isAutomaticForTestEnabled, isAutomaticForRealEnabled);
//                } else {
//                    serviceFinishedEverything = 6;
//                    sendMessageToActivity();
//                }
            }
        } else {
            serviceFinishedEverything = 6;
            sendMessageToActivity();
        }

    }

    private void globalCriteriaCalculating(int howManyHoursGlobal, int firstThreshold, int secondThreshold) {

        databaseDB.clearTable(TABLE_HISTORIC_PERCENTAGES);

        for (int i = 1; i <= 48; i++) {

            float percentUnderSecondThreshold = 0, percentUnderFirstThreshold = 0, percentUnderZero = 0, percentOverZero = 0, percentOverFirstThreshold = 0, percentOverSecondThreshold = 0;
            int underSecondThreshold = 0, underFirstThreshold = 0, underZero = 0, overZero = 0, overFirstThreshold = 0, overSecondThreshold = 0;
            List<Float> percentChange = new ArrayList<>();
            //Get percent change of crypto between certain time
            Cursor data = databaseDB.getCryptoPercentChangeAccordingToCertainTime(howManyHoursGlobal, false, i);
            data.moveToFirst();
            if (data.getCount() == 0) {
                Log.e(TAG, "Table " + TABLE_NAME_KLINES_DATA + " is empty");
            } else {
                do {
                    // Log.e(TAG, "Percentages: " +  data.getString(0) + " " + data.getFloat(1) + " " +  data.getFloat(2) + " " + data.getFloat(3));
                    percentChange.add(data.getFloat(3));
                } while (data.moveToNext());
            }
            data.close();

            for (int j = 0; j < percentChange.size(); j++) {

                if (percentChange.get(j) < secondThreshold * -1) {
                    underSecondThreshold++;
                } else if (percentChange.get(j) >= secondThreshold * -1 && percentChange.get(j) < firstThreshold * -1) {
                    underFirstThreshold++;
                } else if (percentChange.get(j) >= firstThreshold * -1 && percentChange.get(j) < 0) {
                    underZero++;
                } else if (percentChange.get(j) >= 0 && percentChange.get(j) < firstThreshold) {
                    overZero++;
                } else if (percentChange.get(j) >= firstThreshold && percentChange.get(j) <= secondThreshold) {
                    overFirstThreshold++;
                } else if (percentChange.get(j) > secondThreshold) {
                    overSecondThreshold++;
                }
            }

            percentUnderSecondThreshold = (float) underSecondThreshold / percentChange.size() * 100;
            percentUnderFirstThreshold = (float) underFirstThreshold / percentChange.size() * 100;
            percentUnderZero = (float) underZero / percentChange.size() * 100;
            percentOverZero = (float) overZero / percentChange.size() * 100;
            percentOverFirstThreshold = (float) overFirstThreshold / percentChange.size() * 100;
            percentOverSecondThreshold = (float) overSecondThreshold / percentChange.size() * 100;

            PercentagesOfChanges changes = new PercentagesOfChanges(percentUnderSecondThreshold, percentUnderFirstThreshold, percentUnderZero, percentOverZero, percentOverFirstThreshold, percentOverSecondThreshold, System.currentTimeMillis() - 15 * 60 * 1000L * (i - 1));
            databaseDB.addPercentages(changes);

            if (i == 1) {
                databaseDB.updateWithWhereClauseINT(TABLE_NAME_CONFIG, VALUE_INT, System.currentTimeMillis(), ID, "30");
            }

        }
    }

    private ArrayList<ListViewElement> getListOfSymbolsAccordingToProvidedTime(long timeFrom, long timeTo, String tableName, boolean groupSymbols) {

        long currentTime = System.currentTimeMillis();
        ArrayList<ListViewElement> returnList = new ArrayList<>();
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("HH:mm:ss - EEE, dd");
        @SuppressLint("SimpleDateFormat") DateFormat df2 = new SimpleDateFormat("HH:mm");
        @SuppressLint("SimpleDateFormat") DateFormat df3 = new SimpleDateFormat("dd.MM.yy HH:mm");

        Cursor data = databaseDB.firstAppearOfTokenInCertainTimeV3(currentTime - timeFrom, currentTime - timeTo, tableName, groupSymbols);
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "Nothing in [historic_approved_tokens 1]");
        } else {
            do {
                int isItLongInt = data.getInt(1);
                boolean isItLong;
                if (isItLongInt == 1) {
                    isItLong = true;
                } else {
                    isItLong = false;
                }
                String symbol = data.getString(0);
                long approveTime = data.getLong(3);
                float approvedPrice = data.getFloat(4);
                int strategyNr = data.getInt(2);

                //We are searching for highest/lowest price in certain time for that symbol and then look for opposite high/low between entry and opposite
                float maxMinPrice = 0;
                float maxMinPrice2 = 0;
                long closeTimeMaxMin = 0;

                Log.e(TAG, "1 " + symbol + " IsItLong: " + isItLong + " TimeApproved: " + df.format(approveTime) + " TimeFrom: " + df.format((currentTime - timeFrom)) + " TimeTo: "
                        + df.format((currentTime - timeTo)) + " ApprovedPrice: " + approvedPrice + " MaxMinPrice: " + maxMinPrice + " MaxMinPrice2: " + maxMinPrice2 + " CloseTimeMaxMin: " + df.format(closeTimeMaxMin) + " CurrentTime - ApprovedTime: " + (currentTime - approveTime));

                long twoHours = 3600000;

                // Because we are taking open_time ant interval 15 m, we need to take at least one Kline for start from approvetime
                Cursor data2;
                if (currentTime - approveTime < twoHours) { //900000
                    data2 = databaseDB.maxOrMinPriceForSymbolInCertainTimeAndInterval(symbol, approveTime, currentTime - timeTo, isItLong, "3m"); //approveTime - 300000
                } else {
                    data2 = databaseDB.maxOrMinPriceForSymbolInCertainTimeAndInterval(symbol, approveTime + 900000, currentTime - timeTo, isItLong, "15m"); //Without + 900000
                }

                data2.moveToFirst();
                if (data2.getCount() == 0) {
                    Log.e(TAG, "Nothing in [historic_approved_tokens 2]");
                } else {
                    maxMinPrice = data2.getFloat(1);
                    closeTimeMaxMin = data2.getLong(2);
                }
                data2.close();

                Log.e(TAG, "2 " + symbol + " IsItLong: " + isItLong + " TimeApproved: " + df.format(approveTime) + " TimeFrom: " + df.format((currentTime - timeFrom)) + " TimeTo: "
                        + df.format((currentTime - timeTo)) + " ApprovedPrice: " + approvedPrice + " MaxMinPrice: " + maxMinPrice + " MaxMinPrice2: " + maxMinPrice2 + " CloseTimeMaxMin: " + df.format(closeTimeMaxMin));

                if (currentTime - approveTime < twoHours) {
                    data2 = databaseDB.maxOrMinPriceForSymbolInCertainTimeAndInterval(symbol, approveTime, closeTimeMaxMin, !isItLong, "3m");
                } else {
                    data2 = databaseDB.maxOrMinPriceForSymbolInCertainTimeAndInterval(symbol, approveTime + 900000, closeTimeMaxMin, !isItLong, "15m"); //Without + 900000
                }

                data2.moveToFirst();
                if (data2.getCount() == 0) {
                    Log.e(TAG, "Nothing in [historic_approved_tokens 3]");
                } else {
                    maxMinPrice2 = data2.getFloat(1);
                }
                data2.close();

                if (approveTime > currentTime - timeFrom) {
                    Timestamp stamp = new Timestamp(approveTime);
                    String date = df3.format(new Date(stamp.getTime()));
                    float percentOfChange = ((maxMinPrice / approvedPrice) * 100) - 100;
                    float percentOfChange2 = ((maxMinPrice2 / approvedPrice) * 100) - 100;

                    //symbol = symbol.replace("USDT", "");

                    if (isItLong) {
                        returnList.add(new ListViewElement(symbol, percentOfChange, percentOfChange2, approvedPrice, date, isItLong, strategyNr));
                    } else {
                        returnList.add(new ListViewElement(symbol, percentOfChange, percentOfChange2, approvedPrice, date, isItLong, strategyNr));
                    }

                    Log.e(TAG, "3 " + symbol + " IsItLong: " + isItLong + " TimeApproved: " + df.format(approveTime) + " TimeFrom: " + df.format((currentTime - timeFrom)) + " TimeTo: "
                            + df.format((currentTime - timeTo)) + " ApprovedPrice: " + approvedPrice + " MaxMinPrice: " + maxMinPrice + " MaxMinPrice2: " + maxMinPrice2 + " CloseTimeMaxMin: " + df.format(closeTimeMaxMin));
                }
            } while (data.moveToNext());
        }
        data.close();

        if (returnList.size() == 0) {
            Log.e(TAG, "Nothing good in DB");
            //returnList.add(new ListViewElement("Nothing good", 0, 0, "", true));
        } else {
            returnList.sort(new Comparator<ListViewElement>() {
                public int compare(ListViewElement o1, ListViewElement o2) {
                    return o2.getTime().compareTo(o1.getTime());
                    //return Float.compare(o1.getPercentChange(), o2.getPercentChange());
                }
            });
        }
        Log.e(TAG, "Return list size: " + returnList.size());
        return returnList;
    }

    public boolean isPercentageInFavorV2(ArrayList<PercentagesOfChanges> percents, int isItForShort, int howManyToCheckFromStart) {

        boolean accepted = false;
        int temp = 0;
        float nextNumberShort = percents.get(0).getUnderFirstThreshold() + percents.get(0).getUnderSecondThreshold();
        float nextNumberLong = percents.get(0).getOverFirstThreshold() + percents.get(0).getOverSecondThreshold();

        if (isItForShort == 1 && nextNumberShort > 10) {
            temp++;
        } else if (isItForShort == 0 && nextNumberLong > 10){
            temp++;
        } else {
            return false;
        }

        if (howManyToCheckFromStart <= percents.size()) {
            for (int i = 1; i <= howManyToCheckFromStart; i++) {

                float prevNumberShort = percents.get(i).getUnderFirstThreshold() + percents.get(i).getUnderSecondThreshold();
                float prevNumberLong = percents.get(i).getOverFirstThreshold() + percents.get(i).getOverSecondThreshold();

                if (isItForShort == 1) {
                    if (prevNumberShort * 0.9 < nextNumberShort && nextNumberShort > nextNumberLong * 1.25) {
                        temp++;
                    }
                } else {
                    if (prevNumberLong * 0.9 < nextNumberLong && nextNumberLong > nextNumberShort * 1.25) {
                        temp++;
                    }
                }

                if (temp >= howManyToCheckFromStart) {
                    accepted = true;
                    break;
                }
                nextNumberShort = prevNumberShort;
                nextNumberLong = prevNumberLong;
            }
            Log.e(TAG, "HowMany to reach: " + howManyToCheckFromStart + " result: " + temp);
            return accepted;
        } else {
            return false;
        }
    }

    public boolean isPercentageInFavorV3(ArrayList<PercentagesOfChanges> percents, int isItForShort) {

        if (isItForShort == 1) {

            if ((percents.get(0).getOverSecondThreshold() + percents.get(0).getOverFirstThreshold() + percents.get(0).getOverZero()) < 10 && (percents.get(0).getUnderSecondThreshold() + percents.get(0).getUnderFirstThreshold()) > percents.get(0).getUnderZero()) {
                return true;
            } else {
                return false;
            }

        } else {

            if ((percents.get(0).getUnderSecondThreshold() + percents.get(0).getUnderFirstThreshold() + percents.get(0).getUnderZero()) < 10 && (percents.get(0).getOverSecondThreshold() + percents.get(0).getOverFirstThreshold()) > percents.get(0).getOverZero()) { // && (percents.get(1).getOverSecondThreshold() + percents.get(1).getOverFirstThreshold()) < percents.get(1).getOverZero()
                return true;
            } else {
                return false;
            }
        }

    }

    public boolean isPercentageInFavor(ArrayList<PercentagesOfChanges> percents, int isItShort) {

        int howManyTendencies = (int) (percents.size() * 0.8);
        boolean accepted = false;
        int temp = 0;

        float prevNumber = 0;
        if (isItShort == 1) {
            prevNumber = percents.get(0).getUnderZero() + percents.get(0).getUnderFirstThreshold() + percents.get(0).getUnderSecondThreshold();
        } else {
            prevNumber = percents.get(0).getOverZero() + percents.get(0).getOverFirstThreshold() + percents.get(0).getOverSecondThreshold();
        }

        for (int i = 1; i < percents.size(); i++) {
            float currentNumber = 0;
            if (isItShort == 1) {
                currentNumber = percents.get(i).getUnderZero() + percents.get(i).getUnderFirstThreshold() + percents.get(i).getUnderSecondThreshold();
            } else {
                currentNumber = percents.get(i).getOverZero() + percents.get(i).getOverFirstThreshold() + percents.get(i).getOverSecondThreshold();
            }

            if (currentNumber > prevNumber + 2) {
                temp++;
            } else if (currentNumber < prevNumber - 2) {
                temp--;
            } else {
                temp++;
            }

            Log.e(TAG, "Value: " + currentNumber + " previous number: " + prevNumber + " index: " + i + " howMany to reach: " + howManyTendencies + " result: " + temp);

            if (temp >= howManyTendencies) {
                accepted = true;
                break;
            }
            prevNumber = currentNumber;
        }

        return accepted;
    }

    public void makeOrdersFunction(ArrayList<ListViewElement> listOfCryptosToTry, int isAutomaticTestEnabled, int isAutomaticRealEnabled) {

        // Retrieve test accounts balances to prepare for making orders
        ArrayList<Float> testAccountBalances = new ArrayList<>();
        int margin;
        float stopLimit;
        float takeProfit;
        int realAccountNr = 3;
        float realAccountBalance;

        Cursor data;
        for (int i = 6; i < 11; i++) {
            data = databaseDB.retrieveParam(i);
            data.moveToFirst();
            if (data.getCount() == 0) {
                Log.e(TAG, "There is no param for test account " + (i - 5));
                databaseDB.addParam(i, "Test account nr " + (i - 5) + " balance", "", 0, 100);
                testAccountBalances.add(100f);
            } else if (data.getCount() >= 2) {
                databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, i);
                databaseDB.addParam(i, "Test account nr " + (i - 5) + " balance", "", 0, 100);
                testAccountBalances.add(100f);
            } else {
                testAccountBalances.add(data.getFloat(4));
            }
            data.close();
        }

        data = databaseDB.retrieveParam(3);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param nr 3");
            realAccountBalance = 0;
        } else {
            data.moveToFirst();
            realAccountBalance = data.getFloat(4);
        }
        data.close();

        data = databaseDB.retrieveAllFromTable(TABLE_NAME_ORDERS);
        ArrayList<OrderListViewElement> currentOrders = new ArrayList<>();
        data.moveToFirst();
        if (data.getCount() == 0) {
            Log.e(TAG, "No active orders");
        } else {
            do {
                OrderListViewElement tempToken = new OrderListViewElement(data.getString(1), data.getInt(2), data.getFloat(3), data.getFloat(4), data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(9), data.getInt(8), data.getInt(11), data.getInt(10), data.getInt(12), data.getInt(13), data.getString(14), data.getFloat(15));
                currentOrders.add(tempToken);
                Log.e(TAG, tempToken.toString());
            } while (data.moveToNext());
        }
        data.close();

        //Default params for my order
        data = databaseDB.retrieveParam(11);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param 11");
            databaseDB.addParam(11, "Margin automatic", "", 1, 0);
            margin = 1;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 11);
            databaseDB.addParam(11, "Margin automatic", "", 1, 0);
            margin = 1;
        } else {
            data.moveToFirst();
            margin = data.getInt(3);
        }
        data.close();

        data = databaseDB.retrieveParam(12);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param 12");
            databaseDB.addParam(12, "SL automatic", "", 0, 1);
            stopLimit = 1;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 12);
            databaseDB.addParam(12, "SL automatic", "", 0, 1);
            stopLimit = 1;
        } else {
            data.moveToFirst();
            stopLimit = data.getFloat(4);
        }
        data.close();

        data = databaseDB.retrieveParam(13);
        if (data.getCount() == 0) {
            Log.e(TAG, "There is no param 13");
            databaseDB.addParam(13, "TP automatic", "", 0, 2);
            takeProfit = 2;
        } else if (data.getCount() >= 2) {
            databaseDB.deleteWithWhereClause(TABLE_NAME_CONFIG, ID, 13);
            databaseDB.addParam(13, "TP automatic", "", 0, 2);
            takeProfit = 2;
        } else {
            data.moveToFirst();
            takeProfit = data.getFloat(4);
        }
        data.close();
        boolean isItCrossed = false;

        ArrayList<String> currentMadeOrders = new ArrayList<>();
        //For each test account try to make order if balance is good
        for (int i = 0; i < 5; i++) {
            Log.e(TAG, "AAAAA" + listOfCryptosToTry.toString());
            data = databaseDB.retrieveActiveOrdersOnAccount(i + 6, "MARKET", 0);

            if (data.getCount() == 0 && isAutomaticTestEnabled == 1) {

                if (testAccountBalances.get(i) > 30) {

                    Random random = new Random();
                    // Generate a random index that has not been used before
                    int index = random.nextInt(listOfCryptosToTry.size());
                    String indexText = "No order for account nr " + (i + 6) + " Random test index: " + index + " size: " + listOfCryptosToTry.size();
                    Log.e(TAG, indexText);
                    ServiceFunctionsOther.writeToFile(indexText, getApplicationContext(), "results");

                    // Get the element at the random index
                    ListViewElement randomElement = listOfCryptosToTry.get(index);

                    boolean thisSymbolIsAlreadyOrdered = false;

                    for (OrderListViewElement obj : currentOrders) {
                        // Check if the field value equals the search string
                        if (obj.getSymbol().equals(randomElement.getText())) {
                            thisSymbolIsAlreadyOrdered = true;
                        }
                    }

                    for (String obj2 : currentMadeOrders) {
                        // Check if the field value equals the search string
                        if (obj2.equals(randomElement.getText())) {
                            thisSymbolIsAlreadyOrdered = true;
                        }
                    }

                    if (!thisSymbolIsAlreadyOrdered) {

                        int entryAmount = (int) (testAccountBalances.get(i) * 0.98);
                        currentMadeOrders.add(randomElement.getText());

                        boolean isItShort;

                        if (randomElement.isItLONG()) {
                            isItShort = false;
                        } else {
                            isItShort = true;
                        }

                        int finalI = i;

                        getMarkPrice(randomElement.getText())
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
                                        String infoOfOrder = "LEVEL 4 [" + randomElement.getText() + "] " + " MarkPrice: " + markPrice.getMarkPrice() + " EntryAmount$: " + entryAmount + " AccountNr: " + (finalI + 1) + " Leverage: " + margin + " isItShort: " + isItShort;
                                        Log.e(TAG, infoOfOrder);
                                        ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "result");
                                        ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "orders");
                                        ServiceFunctionsOther.createNotificationWithText(infoOfOrder, TAG, getApplicationContext());

                                        ServiceFunctionsAPI.makeOrderFunction(false, randomElement.getText(), entryAmount, stopLimit, takeProfit, margin, markPrice.getMarkPrice(), isItCrossed, isItShort, System.currentTimeMillis(), testAccountBalances.get(finalI), (finalI + 6), getApplicationContext(), null);

                                    }

                                    @Override
                                    public void onError(@NonNull Throwable e) {
                                        // handle any errors that occur
                                        Log.e(TAG, "An error has occurred: " + e.getMessage());
                                    }

                                    @Override
                                    public void onComplete() {
                                        // do something when the observable completes
                                    }
                                });

                    }

                }
            } else {
                String indexText = "There are active orders on account " + (i + 6);
                Log.e(TAG, indexText);
                ServiceFunctionsOther.writeToFile(indexText, getApplicationContext(), "results");
            }
            serviceFinishedEverything++;
            data.close();
        }


        //Check how many orders can Ybe according to account balance
        int nrOfOrders = 3;
        float multiplierOfAccountBalance = 0.65f;
        if (realAccountBalance > 21 && realAccountBalance <= 100) {
            nrOfOrders = 4;
            multiplierOfAccountBalance = 0.5f;
        } else if (realAccountBalance > 100 && realAccountBalance <= 750) {
            nrOfOrders = 5;
            multiplierOfAccountBalance = 0.4f;
        } else if (realAccountBalance > 750) {
            nrOfOrders = 6;
            multiplierOfAccountBalance = 0.3f;
        }

        data = databaseDB.retrieveActiveOrdersOnAccount(1, "MARKET", 1);
        if (data.getCount() <= nrOfOrders && isAutomaticRealEnabled == 1) { ///!!!!!!!
            Log.e(TAG, "No order for REAL account.");
            Log.e(TAG, currentOrders.toString());
              if (realAccountBalance > 5) {

                //Generate a random index that has not been used before
                Random random = new Random();
                int index = random.nextInt(listOfCryptosToTry.size());
                String indexText = "No order for account nr REAL, Random test index: " + index + " size: " + listOfCryptosToTry.size();
                Log.e(TAG, indexText);
                ServiceFunctionsOther.writeToFile(indexText, getApplicationContext(), "results");

                // Get the element at the random index
                ListViewElement randomElement = listOfCryptosToTry.get(index);
                boolean isThereSuchOrderForRandomizedSymbolOnRealAccount = false;

                for (int i = 0; i < currentOrders.size(); i++) {

                    if (currentOrders.get(i).getIsItReal() == 1 && currentOrders.get(i).getSymbol().equals(randomElement.getText())) {
                        isThereSuchOrderForRandomizedSymbolOnRealAccount = true;
                        Log.e(TAG, "Random symbol: " + randomElement.getText() + " Current orders symbol: " + currentOrders.get(i).getSymbol() + " is it real: " + currentOrders.get(i).getIsItReal());

                    }

                }

                int entryAmount = (int) (realAccountBalance * (multiplierOfAccountBalance * 1 + (data.getCount() / 10)));
                boolean isItShort;
                isItShort = !randomElement.isItLONG();

                if (!isThereSuchOrderForRandomizedSymbolOnRealAccount) {

                    getMarkPrice(randomElement.getText())
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
                                    String infoOfOrder = "LEVEL 4 [" + randomElement.getText() + "] " + " MarkPrice: " + markPrice.getMarkPrice() + " EntryAmount$: " + entryAmount + " AccountNr: REAL Leverage: " + margin + " isItShort: " + isItShort;
                                    Log.e(TAG, infoOfOrder);
                                    ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "result");
                                    ServiceFunctionsOther.writeToFile(infoOfOrder, getApplicationContext(), "orders");
                                    ServiceFunctionsOther.createNotificationWithText(infoOfOrder, TAG, getApplicationContext());

                                    ServiceFunctionsAPI.makeOrderFunction(true, randomElement.getText(), entryAmount, stopLimit, takeProfit, margin, markPrice.getMarkPrice(), isItCrossed, isItShort, System.currentTimeMillis(), realAccountBalance, realAccountNr, getApplicationContext(), null);

                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    // handle any errors that occur
                                    Log.e(TAG, "An error has occurred: " + e.getMessage());
                                }

                                @Override
                                public void onComplete() {
                                    // do something when the observable completes
                                }
                            });
                }
            }
        } else {
            Log.e(TAG, "There is active order on REAL account ");
        }
        serviceFinishedEverything++;
        data.close();
        sendMessageToActivity();

    }

    public Observable<MarkPrice> getMarkPrice(String symbol) {
        return RetrofitClientFutures.getInstance()
                .getMyApi()
                .getMarkPrice(symbol)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
