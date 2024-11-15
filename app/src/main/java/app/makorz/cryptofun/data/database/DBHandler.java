package app.makorz.cryptofun.data.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import app.makorz.cryptofun.data.ApprovedToken;
import app.makorz.cryptofun.data.CryptoSymbolTickStep;
import app.makorz.cryptofun.data.PercentagesOfChanges;
import app.makorz.cryptofun.ui.orders.OrderListViewElement;
import app.makorz.cryptofun.ui.results.ResultsListElement;

import java.util.List;

public class DBHandler extends SQLiteOpenHelper {

    private static final String TAG = "DBHandler";

    private static final String DB_NAME = "cryptodb.db";
    private static final int DB_VERSION = 1;

    @SuppressLint("StaticFieldLeak")
    private static DBHandler sInstance;
    private final Context mContext;

    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private static final String ID_CRYPTO = "id_crypto";
    private static final String SYMBOL_CRYPTO = "symbol";
    private static final String AVG_PRICE = "avg_price";
    private static final String STEP_SIZE = "step_size";
    private static final String TICK_SIZE = "tick_size";

    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String INTERVAL = "interval";
    private static final String ID = "id";
    private static final String OPEN_TIME = "open_time";
    private static final String OPEN_PRICE = "open_price";
    private static final String HIGH_PRICE = "high_price";
    private static final String LOW_PRICE = "low_price";
    private static final String CLOSE_PRICE = "close_price";
    private static final String VOLUME = "volume";
    private static final String CLOSE_TIME = "close_time";
    private static final String NUMBER_OF_TRADES = "number_of_trades";

    private static final String TABLE_NAME_CONFIG = "config";
    private static final String DESCRIPTION = "description";
    private static final String VALUE_STRING = "value_string";
    private static final String VALUE_INT = "value_int";
    private static final String VALUE_REAL = "value_real";

    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String LONGSHORT = "longOrShort";
    private static final String TIME_APPROVED = "approve_time";
    private static final String PRICE_APPROVED = "price_when_approved";

    private static final String TABLE_NAME_APPROVED_HISTORIC = "historic_approved_tokens";

    private static final String TABLE_NAME_ORDERS = "current_orders";
    private static final String IS_IT_REAL = "isReal";
    private static final String ENTRY_AMOUNT = "entry_amount";
    private static final String ENTRY_PRICE = "entry_price";
    private static final String CURRENT_PRICE = "current_price";
    private static final String STOP_LIMIT = "stop_limit_price";
    private static final String TAKE_PROFIT = "take_profit_price";
    private static final String MARGIN = "margin";
    private static final String TIME_WHEN_PLACED = "time_when_placed";
    private static final String IS_IT_CROSSED = "isCrossed";
    private static final String IS_IT_SHORT = "isShort";
    private static final String WHAT_ACCOUNT = "account_nr";
    private static final String ORDER_ID = "order_id";
    private static final String ORDER_TYPE = "order_type";
    private static final String QUANTITY = "quantity";

    private static final String TABLE_NAME_ORDERS_HISTORIC = "historic_orders";
    private static final String ENTRY_AMOUNT_$ = "entry_amount_dollars";
    private static final String EXIT_AMOUNT_$ = "exit_amount_dollars";
    private static final String AMOUNT_PERCENT_CHANGE = "amount_percent_change";
    private static final String EXIT_PRICE = "exit_price";
    private static final String PRICE_PERCENT_CHANGE = "price_percent_change";
    private static final String MONEY_EARNED = "money_earned";
    private static final String ENTRY_TIME = "entry_time";
    private static final String EXIT_TIME = "exit_time";


    private static final String TABLE_HISTORIC_PERCENTAGES = "history_percentages";
    private static final String UNDER1 = "under1";
    private static final String UNDER2 = "under2";
    private static final String UNDER3 = "under3";
    private static final String OVER1 = "over1";
    private static final String OVER2 = "over2";
    private static final String OVER3 = "over3";


    private DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    public static synchronized DBHandler getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DBHandler(context.getApplicationContext());
        }
        return sInstance;
    }

    // below method is for creating a database by running a sqlite query
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        String query = "CREATE TABLE " + TABLE_SYMBOL_AVG + " (" + ID_CRYPTO + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + AVG_PRICE + " REAL, " + TICK_SIZE + " TEXT, " + STEP_SIZE + " TEXT)";
        sqLiteDatabase.execSQL(query);
        Log.i(TAG, query);

        query = "CREATE TABLE " + TABLE_NAME_KLINES_DATA + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + OPEN_TIME + " INTEGER, " + OPEN_PRICE + " REAL, " + HIGH_PRICE + " REAL, " + LOW_PRICE + " REAL, " + CLOSE_PRICE + " REAL, " + VOLUME + " REAL, " + CLOSE_TIME + " INTEGER, " + NUMBER_OF_TRADES + " INTEGER, " + INTERVAL + " TEXT, " + "FOREIGN KEY (" + SYMBOL_CRYPTO + ") REFERENCES " + TABLE_SYMBOL_AVG + " (" + SYMBOL_CRYPTO + "));";
        Log.i(TAG, query);
        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_CONFIG + " (" + ID + " INTEGER, " + DESCRIPTION + " TEXT, " + VALUE_STRING + " TEXT, " + VALUE_INT + " INT, " + VALUE_REAL + " REAL)";
        Log.i(TAG, query);
        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_APPROVED + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + VOLUME + " REAL, " + NUMBER_OF_TRADES + " INT, " + LONGSHORT + " INT, " + TIME_APPROVED + " INT, " + PRICE_APPROVED + " REAL )";
        Log.i(TAG, query);
        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_APPROVED_HISTORIC + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + VOLUME + " REAL, " + NUMBER_OF_TRADES + " INT, " + LONGSHORT + " INT, " + TIME_APPROVED + " INT, " + PRICE_APPROVED + " REAL )";
        Log.i(TAG, query);
        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_ORDERS + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + IS_IT_REAL + " INT, " + ENTRY_AMOUNT + " REAL, " + ENTRY_PRICE + " REAL, " + CURRENT_PRICE + " REAL, " + STOP_LIMIT + " REAL, " + TAKE_PROFIT + " REAL, " + MARGIN + " INT, " + TIME_WHEN_PLACED + " INT, " + IS_IT_CROSSED + " INT, " + IS_IT_SHORT + " INT, " + WHAT_ACCOUNT + " INT, " + ORDER_ID + " INT, " + ORDER_TYPE + " TEXT, " + QUANTITY + " REAL)";
        Log.i(TAG, query);
        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_ORDERS_HISTORIC + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + IS_IT_REAL + " INT, " + ENTRY_AMOUNT_$ + " REAL, " + EXIT_AMOUNT_$ + " REAL, " + ENTRY_PRICE + " REAL, " + EXIT_PRICE + " REAL, " + AMOUNT_PERCENT_CHANGE + " REAL, " + PRICE_PERCENT_CHANGE + " REAL, " + MONEY_EARNED + " REAL, " + ENTRY_TIME + " INT, " + EXIT_TIME + " INT, " + IS_IT_CROSSED + " INT, " + IS_IT_SHORT + " INT, " + WHAT_ACCOUNT + " INT, " + MARGIN + " INT, " + QUANTITY + " REAL)";
        Log.i(TAG, query);
        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_HISTORIC_PERCENTAGES + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + UNDER3 + " REAL, " + UNDER2 + " REAL, " + UNDER1 + " REAL, " + OVER1 + " REAL, " + OVER2 + " REAL, " + OVER3 + " REAL, " + TIME_WHEN_PLACED + " INT)";
        Log.i(TAG, query);
        sqLiteDatabase.execSQL(query);


    }


    public void addNewCrypto(List<CryptoSymbolTickStep> crypto) {
        Log.e(TAG, "addNewCrypto");
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Begin a database transaction
            db.beginTransaction();

            for (int i = 0; i < crypto.size(); i++) {
                values.put(SYMBOL_CRYPTO, crypto.get(i).getSymbol());
                values.put(TICK_SIZE, crypto.get(i).getTickSize());
                values.put(STEP_SIZE, crypto.get(i).getStepSize());
                db.insert(TABLE_SYMBOL_AVG, null, values);
                // Log.e(TAG, values.toString());
            }
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public void addApprovedNewCrypto(ApprovedToken symbol, String tableName) {
        Log.e(TAG, "addApprovedNewCrypto");
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Begin a database transaction
            db.beginTransaction();

            values.put(SYMBOL_CRYPTO, symbol.getSymbol());
            values.put(VOLUME, symbol.getVolumeOnKlines());
            values.put(NUMBER_OF_TRADES, symbol.getNrOfTradesOnKlines());
            values.put(LONGSHORT, symbol.getLongOrShort());
            values.put(TIME_APPROVED, symbol.getTime());
            values.put(PRICE_APPROVED, symbol.getPriceOnTimeOfApprove());
            db.insert(tableName, null, values);

            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }


    // id:1 -> Time of last update
    public void addParam(int id, String description, String value, long valueInt, float valueFloat) {
        Log.e(TAG, "addParam");
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Begin a database transaction
            db.beginTransaction();

            values.put(ID, id);
            values.put(DESCRIPTION, description);
            values.put(VALUE_STRING, value);
            values.put(VALUE_INT, valueInt);
            values.put(VALUE_REAL, valueFloat);
            db.insert(TABLE_NAME_CONFIG, null, values);

            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public void addPercentages(PercentagesOfChanges percents) {

        Log.e(TAG, "addPercentages");
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Begin a database transaction
            db.beginTransaction();

            values.put(UNDER3, percents.getUnderSecondThreshold());
            values.put(UNDER2, percents.getUnderFirstThreshold());
            values.put(UNDER1, percents.getUnderZero());
            values.put(OVER1, percents.getOverZero());
            values.put(OVER2, percents.getOverFirstThreshold());
            values.put(OVER3, percents.getOverSecondThreshold());
            values.put(TIME_WHEN_PLACED, percents.getTime());
            db.insert(TABLE_HISTORIC_PERCENTAGES, null, values);

            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }


    // this method is use to add new kline data to database, interval 1 - 3m, interval = 2 - 15m, interval = 3 - 24h
    public int addNewKlineData(List<rawTable_Kline> object) {

        Log.e(TAG, "addNewKlineData");
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();
        int i = 0;

        try {
            // Begin a database transaction
            db.beginTransaction();


            for (i = 0; i < object.size(); i++) {

                values.put(SYMBOL_CRYPTO, object.get(i).getTokenSymbol());
                values.put(OPEN_TIME, object.get(i).getOpenTime());
                values.put(OPEN_PRICE, object.get(i).getOpenPrice());
                values.put(HIGH_PRICE, object.get(i).getHighPrice());
                values.put(LOW_PRICE, object.get(i).getLowPrice());
                values.put(CLOSE_PRICE, object.get(i).getClosePrice());
                values.put(VOLUME, object.get(i).getVolume());
                values.put(CLOSE_TIME, object.get(i).getCloseTime());
                values.put(NUMBER_OF_TRADES, object.get(i).getNumberOfTrades());
                values.put(INTERVAL, object.get(i).getKlineInterval());
                db.insert(TABLE_NAME_KLINES_DATA, null, values);
            }

            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


        return i;
    }

    public void addNewOrder(OrderListViewElement element) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Begin a database transaction
            db.beginTransaction();

            values.put(SYMBOL_CRYPTO, element.getSymbol());
            values.put(ENTRY_AMOUNT, element.getEntryAmount());
            values.put(ENTRY_PRICE, element.getEntryPrice());
            values.put(CURRENT_PRICE, element.getCurrentPrice());
            values.put(STOP_LIMIT, element.getStopLimitPrice());
            values.put(TAKE_PROFIT, element.getTakeProfitPrice());
            values.put(MARGIN, element.getMargin());
            values.put(TIME_WHEN_PLACED, element.getTimeWhenPlaced());
            values.put(IS_IT_REAL, element.getIsItReal());
            values.put(IS_IT_CROSSED, element.getIsItCrossed());
            values.put(IS_IT_SHORT, element.getIsItShort());
            values.put(WHAT_ACCOUNT, element.getAccountNumber());
            values.put(ORDER_ID, element.getOrderID());
            values.put(ORDER_TYPE, element.getOrderType());
            values.put(QUANTITY, element.getQuantity());
            db.insert(TABLE_NAME_ORDERS, null, values);
            Log.e(TAG, values.toString());
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }
    }

    public void addNewHistoricOrder(ResultsListElement element) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Begin a database transaction
            db.beginTransaction();

            values.put(SYMBOL_CRYPTO, element.getSymbol());
            values.put(ENTRY_AMOUNT_$, element.getEntryAmount());
            values.put(EXIT_AMOUNT_$, element.getExitAmount());
            values.put(ENTRY_PRICE, element.getEntryPrice());
            values.put(EXIT_PRICE, element.getClosePrice());
            values.put(AMOUNT_PERCENT_CHANGE, element.getPercentOfAmountChange());
            values.put(PRICE_PERCENT_CHANGE, element.getPercentOfPriceChange());
            values.put(MONEY_EARNED, element.getMoneyEarned());
            values.put(IS_IT_REAL, element.getIsItReal());
            values.put(IS_IT_CROSSED, 0);
            values.put(IS_IT_SHORT, element.getIsItShort());
            values.put(WHAT_ACCOUNT, element.getAccountNr());
            values.put(ENTRY_TIME, element.getTimeEntry());
            values.put(EXIT_TIME, element.getTimeExit());
            values.put(QUANTITY, 0);
            values.put(MARGIN, element.getMargin());
            db.insert(TABLE_NAME_ORDERS_HISTORIC, null, values);
            Log.e(TAG, values.toString());
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }
    }


    public Cursor retrieveCryptoSymbolsToListView() {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT " + SYMBOL_CRYPTO + " FROM " + TABLE_SYMBOL_AVG;
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //    Log.i(TAG, query);
        return data;
    }

    public Cursor retrieveAllFromTable(String tableName) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + tableName;
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //    Log.i(TAG, query);
        return data;
    }

    public Cursor howManyRows(String tableName) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT count(*) FROM " + tableName;
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //    Log.i(TAG, query);
        return data;
    }
//
//    public Cursor retrieveOrder(String tableName) {
//        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
//        String query = "SELECT * FROM " + TABLE_NAME_ORDERS + " WHERE " + SYMBOL_CRYPTO + " = '" + symbol + "' and " + TIME_WHEN_PLACED + " = " + time + " and " + IS_IT_REAL + " = " + isItReal + " and " + IS_IT_SHORT + " = " + isItShort + " and " + MARGIN + " = " + margin + " and " + ORDER_ID + " = '" + orderId + "'";
//        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
//        Log.i(TAG, query);
//        return data;
//    }

    public Cursor retrieveAllFromTableApproved(String tableName) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + tableName + " ORDER BY " + TIME_APPROVED + " DESC";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //   Log.i(TAG, query);
        return data;
    }

    public Cursor retrieveDataToFindBestCrypto(String tableName, String tokenSymbol, String interval) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + tableName + " WHERE " + SYMBOL_CRYPTO + " = '" + tokenSymbol + "' and " + INTERVAL + " = '" + interval + "' ORDER BY " + OPEN_TIME + " DESC";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //   Log.i(TAG, query);
        return data;

    }

    public Cursor retrieveDataToFindBestCrypto2(String tableName, String tokenSymbol, String interval) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + tableName + " WHERE " + SYMBOL_CRYPTO + " = '" + tokenSymbol + "' and " + INTERVAL + " = '" + interval + "' ORDER BY " + OPEN_TIME + " ASC";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //   Log.i(TAG, query);
        return data;
    }

    public Cursor retrieveActiveOrdersOnAccount(int accountNumber, String orderType, int isReal) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME_ORDERS + " WHERE " + WHAT_ACCOUNT + " = " + accountNumber + " AND " + ORDER_TYPE + " = '" + orderType + "' AND " + IS_IT_REAL + " = " + isReal;
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //   Log.i(TAG, query);
        return data;

    }

    public Cursor retrieveParam(int id) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME_CONFIG + " WHERE " + ID + " = " + id;
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //   Log.i(TAG, query);
        return data;
    }

    public Cursor retrieveSymbolInfo(String symbol) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SYMBOL_AVG + " WHERE " + SYMBOL_CRYPTO + " = '" + symbol + "'";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //   Log.i(TAG, query);
        return data;
    }

    public Cursor nrOfKlinesForSymbolInInterval(String symbol, String interval) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT count(*) FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "'";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //Log.i(TAG, query);
        return data;
    }

    public Cursor retrieveLastCloseTime2(String symbol, String interval) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "' ORDER BY " + CLOSE_TIME + " DESC LIMIT 1";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //Log.i(TAG, query);
        return data;
    }

    public Cursor retrieveLastCloseTime(String interval) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' ORDER BY " + CLOSE_TIME + " DESC LIMIT 1";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //Log.i(TAG, query);
        return data;
    }

    public Cursor retrieveLastClosePrice(String symbol) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '3m' and " + SYMBOL_CRYPTO + " = '" + symbol + "' ORDER BY " + CLOSE_TIME + " DESC";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        // Log.i(TAG, query);
        return data;
    }

    public Cursor checkVolumeOfKlineInterval(String interval) {
        SQLiteDatabase db = getInstance(mContext).getReadableDatabase();
        Cursor data;
        String query = "SELECT " + SYMBOL_CRYPTO + "," + VOLUME + "," + NUMBER_OF_TRADES + ", max(" + CLOSE_TIME + "), " + CLOSE_PRICE + ", (volume*close_price) as Dollars FROM "
                + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' GROUP BY " + SYMBOL_CRYPTO + " ORDER BY Dollars DESC";
        //  Log.i(TAG,query);
        data = db.rawQuery(query, null);
        return data;
    }





//    public Cursor firstAppearOfTokenInCertainTime(long time1, long time2) {
//        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
//        Cursor data;
////        if (sqLiteDatabase.isOpen()) {
//        String query = "WITH timePeriodHistoric AS (SELECT * FROM historic_approved_tokens where approve_time BETWEEN " + time1 + " AND " + time2 + " ), " + "firstWriteOfToken AS (SELECT  *, MIN(approve_time) FROM timePeriodHistoric GROUP BY symbol, longOrShort), " + "klines AS (SELECT * FROM klines_data WHERE symbol in (SELECT symbol FROM firstWriteOfToken) AND interval LIKE '3m') " + "SELECT K.symbol, open_time, open_price, high_price, low_price, close_price, close_time, F.approve_time, F.longOrShort, F.price_when_approved FROM klines K " + "JOIN firstWriteOfToken F ON K.symbol = F.symbol " + "WHERE K.open_time = (SELECT MAX(open_time) FROM klines WHERE symbol = K.symbol) ORDER BY K.symbol asc";
//        Log.i(TAG,query);
//        data = sqLiteDatabase.rawQuery(query, null);
////
////        }
//        return data;
//    }

    public Cursor checkIfSymbolWasApprovedInCertainTime(String symbol, long time, int strategyNr) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        Cursor data;
        String query = "SELECT symbol FROM historic_approved_tokens where approve_time > " + time + " AND symbol ='" + symbol + "' AND number_of_trades = " + strategyNr;
        Log.i(TAG, query);
        data = sqLiteDatabase.rawQuery(query, null);
        return data;
    }

    public Cursor firstAppearOfTokenInCertainTimeV1(long time1, long time2, String tableName, boolean groupSymbols) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        Cursor data;
        String query;
        if (groupSymbols) {
            //Show only first appear of token that passed strategy
            query = "SELECT symbol, longOrShort, MIN(approve_time), price_when_approved FROM " + tableName + " where approve_time BETWEEN " + time1 + " AND " + time2 + " group by symbol, longOrShort order by symbol, approve_time asc";
        } else {
            //Show all appearances of token  that passed strategy
            query = "SELECT symbol, longOrShort, approve_time, price_when_approved FROM " + tableName + " where approve_time BETWEEN " + time1 + " AND " + time2 + " order by symbol, approve_time asc";
        }
        Log.i(TAG, query);
        data = sqLiteDatabase.rawQuery(query, null);
        return data;
    }

    //added for multiple strategies
    public Cursor firstAppearOfTokenInCertainTimeV3(long time1, long time2, String tableName, boolean groupSymbols) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        Cursor data;
        String query;
        if (groupSymbols) {
            //Show only first appear of token that passed strategy
            query = "SELECT symbol, longOrShort, number_of_trades, MIN(approve_time), price_when_approved FROM " + tableName + " where approve_time BETWEEN " + time1 + " AND " + time2 + " group by symbol, longOrShort, number_of_trades order by symbol, approve_time, number_of_trades asc";
        } else {
            //Show all appearances of token  that passed strategy
            query = "SELECT symbol, longOrShort, number_of_trades, approve_time, price_when_approved FROM " + tableName + " where approve_time BETWEEN " + time1 + " AND " + time2 + " order by symbol, approve_time, number_of_trades asc";
        }
        Log.i(TAG, query);
        data = sqLiteDatabase.rawQuery(query, null);
        return data;
    }


    public Cursor firstAppearOfTokenInCertainTimeV2(String name, long time1, long time2) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        Cursor data;
        String query = "SELECT symbol, MAX(high_price), MIN(low_price) FROM klines_data where open_time BETWEEN " + time1 + " AND " + time2 + " and interval = '15m' and symbol = '" + name + "'";
        Log.i(TAG, query);
        data = sqLiteDatabase.rawQuery(query, null);
        return data;
    }

    public Cursor maxOrMinPriceForSymbolInCertainTimeAndInterval(String name, long time1, long time2, boolean isItLong, String interval) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        Cursor data;
        String query;
        if (isItLong) {
            query = "SELECT symbol, MAX(high_price), close_time FROM klines_data where close_time BETWEEN " + time1 + " AND " + time2 + " and interval = '" + interval + "' and symbol = '" + name + "'";
        } else {
            query = "SELECT symbol, MIN(low_price), close_time FROM klines_data where close_time BETWEEN " + time1 + " AND " + time2 + " and interval = '" + interval + "' and symbol = '" + name + "'";
        }
        Log.i(TAG, query);
        data = sqLiteDatabase.rawQuery(query, null);
        return data;
    }

    public Cursor getCryptoPercentChangeAccordingToCertainTime(int hoursFromNow, boolean isItOneTimeCalculation, int multiplier) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        Cursor data;
        String query;
        if (isItOneTimeCalculation) {
            query = "SELECT a.symbol, a.recent_close_price AS close_price_recent, b.close_price AS close_price_4_hours_ago, ((a.recent_close_price - b.close_price) / b.close_price) * 100 AS percent_change FROM (SELECT symbol, close_price AS recent_close_price FROM klines_data WHERE close_time = (SELECT MAX(close_time) - 15 * 60 * 1000 FROM klines_data where interval = '15m') and interval = '15m' GROUP BY symbol) a JOIN (SELECT symbol,close_price FROM klines_data WHERE close_time = (SELECT MAX(close_time) - " + hoursFromNow + " * 60 * 60 * 1000 FROM klines_data where interval = '15m') and interval = '15m' GROUP BY symbol) b ON a.symbol = b.symbol";
        } else {
            query = "SELECT a.symbol, a.recent_close_price AS close_price_recent, b.close_price AS close_price_4_hours_ago, ((a.recent_close_price - b.close_price) / b.close_price) * 100 AS percent_change FROM (SELECT symbol, close_price AS recent_close_price FROM klines_data WHERE close_time = (SELECT MAX(close_time) - 15 * 60 * 1000 * " + multiplier + " FROM klines_data where interval = '15m') and interval = '15m' GROUP BY symbol) a JOIN (SELECT symbol,close_price FROM klines_data WHERE close_time = (SELECT MAX(close_time) - " + hoursFromNow + " * 60 * 60 * 1000 - 15 * 60 * 1000 * " + (multiplier - 1) + " FROM klines_data where interval = '15m') and interval = '15m' GROUP BY symbol) b ON a.symbol = b.symbol";
        }
        Log.i(TAG, query);
        data = sqLiteDatabase.rawQuery(query, null);
        return data;
    }

    public Cursor getBiggestNrOfTradesSymbolsToCertainTime(int hoursFromNow, int howMany) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        Cursor data;
        String query = "SELECT symbol, SUM(number_of_trades) FROM klines_data WHERE close_time >= (SELECT MAX(close_time) - " + hoursFromNow + " * 60 * 60 * 1000 FROM klines_data where interval = '15m') and interval = '15m' GROUP BY symbol ORDER BY SUM(number_of_trades) DESC LIMIT " + howMany;
        Log.i(TAG, query);
        data = sqLiteDatabase.rawQuery(query, null);
        return data;
    }

    public Cursor retrievePercentages(long time1, long time2) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_HISTORIC_PERCENTAGES + " WHERE " + TIME_WHEN_PLACED + " BETWEEN '" + time1 + "' AND '" + time2 + "' ORDER BY " + TIME_WHEN_PLACED + " DESC";
        //String query2 = "SELECT * FROM " + TABLE_HISTORIC_PERCENTAGES + " WHERE " + TIME_WHEN_PLACED + " BETWEEN '" + time1 + "' AND '" + time2 + "' GROUP BY " + UNDER1 + " ORDER BY " + TIME_WHEN_PLACED + " DESC";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        Log.i(TAG, query);
        return data;
    }

    public void deleteWithWhereClause(String tableName, String columnName, int idParam) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            @SuppressLint("Recycle") String query = "DELETE FROM " + tableName + " WHERE " + columnName + " = " + idParam;
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public void deleteOldApproved(String tableName, String columnName, long time) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            @SuppressLint("Recycle") String query = "DELETE FROM " + tableName + " WHERE " + columnName + " < " + time;
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }


    public void updateWithWhereClause(String tableName, String updatedColumnName, String value, String whereColumnName, String whereSymbol) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "UPDATE " + tableName + " SET " + updatedColumnName + " = '" + value + "' WHERE " + whereColumnName + " = '" + whereSymbol + "'";
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public void updateWithWhereClauseREAL(String tableName, String updatedColumnName, float value, String whereColumnName, String whereSymbol) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            String query = "UPDATE " + tableName + " SET " + updatedColumnName + " = '" + value + "' WHERE " + whereColumnName + " = '" + whereSymbol + "'";
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public void updateWithWhereClauseINT(String tableName, String updatedColumnName, long value, String whereColumnName, String whereSymbol) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "UPDATE " + tableName + " SET " + updatedColumnName + " = '" + value + "' WHERE " + whereColumnName + " = '" + whereSymbol + "'";
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public void updatePricesOfCryptoInOrder(String symbol, String column, float amount, long time) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            String query = "UPDATE " + TABLE_NAME_ORDERS + " SET " + column + " = " + amount + " WHERE " + SYMBOL_CRYPTO + " = '" + symbol + "' AND " + TIME_WHEN_PLACED + " = " + time;
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public Cursor checkIfThereAreDuplicates(String symbol, String interval) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT close_time, count(*) FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "' GROUP BY close_time ORDER BY 2 desc";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        //Log.i(TAG, query);
        return data;
    }


//    public void deleteKlinesAndInsertNewKlinesForSymbolInterval(String interval, String symbol, int limitNew, int limitOld, List<rawTable_Kline> object) {
//        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
//
//        try {
//            // Begin a database transaction
//            db.beginTransaction();
//            String query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + ID + " in (" + "SELECT " + ID + " FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "' ORDER BY " + CLOSE_TIME + " DESC LIMIT " + limitNew + ")";
//            db.execSQL(query);
//            query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + ID + " in (" + "SELECT " + ID + " FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "' ORDER BY " + CLOSE_TIME + " ASC LIMIT " + limitOld + ")";
//           // Log.i(TAG, query);
//            db.execSQL(query);
//            ContentValues values = new ContentValues();
//            int i = 0;
//            for (i = 0; i < object.size(); i++) {
//                values.put(SYMBOL_CRYPTO, object.get(i).getTokenSymbol());
//                values.put(OPEN_TIME, object.get(i).getOpenTime());
//                values.put(OPEN_PRICE, object.get(i).getOpenPrice());
//                values.put(HIGH_PRICE, object.get(i).getHighPrice());
//                values.put(LOW_PRICE, object.get(i).getLowPrice());
//                values.put(CLOSE_PRICE, object.get(i).getClosePrice());
//                values.put(VOLUME, object.get(i).getVolume());
//                values.put(CLOSE_TIME, object.get(i).getCloseTime());
//                values.put(NUMBER_OF_TRADES, object.get(i).getNumberOfTrades());
//                values.put(INTERVAL, object.get(i).getKlineInterval());
//                //Log.e(TAG, values.toString());
//                db.insert(TABLE_NAME_KLINES_DATA, null, values);
//            }
//
//            // Commit the transaction
//            db.setTransactionSuccessful();
//        } catch (Exception e) {
//            // Handle any exceptions that occur during the transaction
//            Log.e(TAG, "Error updating table " + e);
//        } finally {
//            // End the transaction
//            db.endTransaction();
//        }
//    }

    public void deleteKlinesOlderThan(String interval, long time) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            String query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + CLOSE_TIME + " < " + time;
            db.execSQL(query);
            // Log.i(TAG, query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }
    }

    public void deleteResultsOfClosedOrders(int isItReal, boolean isItAutomatic) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        try {
            // Begin a database transaction
            db.beginTransaction();
            String query;
            if (isItReal == 1) {
                query = "DELETE FROM " + TABLE_NAME_ORDERS_HISTORIC + " WHERE " + IS_IT_REAL + " = '" + isItReal + "'";
            } else if (isItAutomatic) {
                query = "DELETE FROM " + TABLE_NAME_ORDERS_HISTORIC + " WHERE " + IS_IT_REAL + " = '" + isItReal + "' and " + WHAT_ACCOUNT + " > " + 4;
            } else {
                query = "DELETE FROM " + TABLE_NAME_ORDERS_HISTORIC + " WHERE " + IS_IT_REAL + " = '" + isItReal + "' and " + WHAT_ACCOUNT + " <= " + 4;
            }

            db.execSQL(query);
            // Log.i(TAG, query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }
    }

    public void updateAndInsertNewKlineData(String interval, String symbol, List<rawTable_Kline> object) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            ContentValues values = new ContentValues();
            for (int i = 0; i < object.size(); i++) {
                values.put(SYMBOL_CRYPTO, object.get(i).getTokenSymbol());
                values.put(OPEN_TIME, object.get(i).getOpenTime());
                values.put(OPEN_PRICE, object.get(i).getOpenPrice());
                values.put(HIGH_PRICE, object.get(i).getHighPrice());
                values.put(LOW_PRICE, object.get(i).getLowPrice());
                values.put(CLOSE_PRICE, object.get(i).getClosePrice());
                values.put(VOLUME, object.get(i).getVolume());
                values.put(CLOSE_TIME, object.get(i).getCloseTime());
                values.put(NUMBER_OF_TRADES, object.get(i).getNumberOfTrades());
                values.put(INTERVAL, object.get(i).getKlineInterval());
                if (symbol.equals("ETHUSDT")) {
                    Log.e(TAG, "ResultDataDBWrite " + values.toString());
                }
                // Check if the row with the specified conditions exists
                Cursor cursor = db.query(TABLE_NAME_KLINES_DATA, null,
                        SYMBOL_CRYPTO + "=? AND " + INTERVAL + "=? AND " + OPEN_TIME + "=?",
                        new String[]{symbol, interval, String.valueOf(object.get(i).getOpenTime())},
                        null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    // Row exists, update it
                    db.update(TABLE_NAME_KLINES_DATA, values,
                            SYMBOL_CRYPTO + "=? AND " + INTERVAL + "=? AND " + OPEN_TIME + "=?",
                            new String[]{symbol, interval, String.valueOf(object.get(i).getOpenTime())});
                } else {
                    // Row does not exist, insert a new one
                    db.insert(TABLE_NAME_KLINES_DATA, null, values);
                }

                if (cursor != null) {
                    cursor.close();
                }
//                //Update one kline then add another
//                if (i == 0) {
//                    db.update(TABLE_NAME_KLINES_DATA, values,
//                            SYMBOL_CRYPTO + "=? AND " + INTERVAL + "=? AND " + OPEN_TIME + "=?",
//                            new String[]{symbol, interval, String.valueOf(object.get(i).getOpenTime())});
//                } else {
//                    db.insert(TABLE_NAME_KLINES_DATA, null, values);
//                }
            }
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public void updateLastKlineValues(String interval, String symbol, List<rawTable_Kline> object) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            ContentValues values = new ContentValues();
            for (int i = 0; i < object.size(); i++) {
                values.put(OPEN_TIME, object.get(i).getOpenTime());
                values.put(OPEN_PRICE, object.get(i).getOpenPrice());
                values.put(HIGH_PRICE, object.get(i).getHighPrice());
                values.put(LOW_PRICE, object.get(i).getLowPrice());
                values.put(CLOSE_PRICE, object.get(i).getClosePrice());
                values.put(VOLUME, object.get(i).getVolume());
                values.put(CLOSE_TIME, object.get(i).getCloseTime());
                values.put(NUMBER_OF_TRADES, object.get(i).getNumberOfTrades());
                if (symbol.equals("ETHUSDT")) {
                    Log.e(TAG, "ResultDataDBWrite " + values.toString());
                }
                db.update(TABLE_NAME_KLINES_DATA, values,
                        SYMBOL_CRYPTO + "=? AND " + INTERVAL + "=? AND " + OPEN_TIME + "=?",
                        new String[]{symbol, interval, String.valueOf(object.get(i).getOpenTime())});
            }
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }
    }

    public void deleteOrder(String symbol, long time, int isItReal, int isItShort, int margin) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "DELETE FROM " + TABLE_NAME_ORDERS + " WHERE " + SYMBOL_CRYPTO + " = '" + symbol + "' and " + TIME_WHEN_PLACED + " = " + time + " and " + IS_IT_REAL + " = " + isItReal + " and " + IS_IT_SHORT + " = " + isItShort + " and " + MARGIN + " = " + margin;
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }
    }

    public void deleteDuplicateOrderByID(long orderID) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "DELETE FROM " + TABLE_NAME_ORDERS + " WHERE " + ORDER_ID + " = '" + orderID + "' and " + MARGIN + " = 0";
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }
    }


    public void deleteAllKlinesForSymbolIntervalandAddNew(String interval, String symbol, List<rawTable_Kline> object) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            String query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "'";
            //Log.i(TAG, query);
            db.execSQL(query);
            ContentValues values = new ContentValues();
            int i = 0;
            for (i = 0; i < object.size(); i++) {
                values.put(SYMBOL_CRYPTO, object.get(i).getTokenSymbol());
                values.put(OPEN_TIME, object.get(i).getOpenTime());
                values.put(OPEN_PRICE, object.get(i).getOpenPrice());
                values.put(HIGH_PRICE, object.get(i).getHighPrice());
                values.put(LOW_PRICE, object.get(i).getLowPrice());
                values.put(CLOSE_PRICE, object.get(i).getClosePrice());
                values.put(VOLUME, object.get(i).getVolume());
                values.put(CLOSE_TIME, object.get(i).getCloseTime());
                values.put(NUMBER_OF_TRADES, object.get(i).getNumberOfTrades());
                values.put(INTERVAL, object.get(i).getKlineInterval());
                //Log.e(TAG, values.toString());
                db.insert(TABLE_NAME_KLINES_DATA, null, values);
            }
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }
    }


    public void clearHistoricApproved(long time) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            String query = "DELETE FROM " + TABLE_NAME_APPROVED_HISTORIC + " WHERE " + TIME_APPROVED + " <= " + time;
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public void clearHistoricPercentages(long time) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            String query = "DELETE FROM " + TABLE_HISTORIC_PERCENTAGES + " WHERE " + TIME_WHEN_PLACED + " <= " + time;
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    public void clearTable(String table_name) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "DELETE FROM " + table_name;
            Log.i(TAG, query);
            db.execSQL(query);
            // Commit the transaction
            db.setTransactionSuccessful();
        } catch (Exception e) {
            // Handle any exceptions that occur during the transaction
            Log.e(TAG, "Error updating table " + e);
        } finally {
            // End the transaction
            db.endTransaction();
        }


    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // this method is called to check if the table exists already.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SYMBOL_AVG);
        onCreate(sqLiteDatabase);
    }

}
