package com.example.cryptofun.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.example.cryptofun.data.ApprovedToken;
import com.example.cryptofun.data.PercentagesOfChanges;
import com.example.cryptofun.ui.view.OrderListViewElement;

import java.util.ArrayList;
import java.util.List;

public class DBHandler extends SQLiteOpenHelper {

    private static final String TAG = "DBHandler";

    private static final String DB_NAME = "cryptodb.db";
    private static final int DB_VERSION = 1;
    private static DBHandler sInstance;
    private Context mContext;

    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private static final String ID_CRYPTO = "id_crypto";
    private static final String SYMBOL_CRYPTO = "symbol";
    private static final String AVG_PRICE = "avg_price";

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

        String query = "CREATE TABLE " + TABLE_SYMBOL_AVG + " (" + ID_CRYPTO + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT," + AVG_PRICE + " REAL)";
        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_KLINES_DATA + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + OPEN_TIME + " INTEGER, " + OPEN_PRICE + " REAL, " + HIGH_PRICE + " REAL, " + LOW_PRICE + " REAL, " + CLOSE_PRICE + " REAL, " + VOLUME + " REAL, " + CLOSE_TIME + " INTEGER, " + NUMBER_OF_TRADES + " INTEGER, " + INTERVAL + " TEXT, " + "FOREIGN KEY (" + SYMBOL_CRYPTO + ") REFERENCES " + TABLE_SYMBOL_AVG + " (" + SYMBOL_CRYPTO + "));";

        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_CONFIG + " (" + ID + " INTEGER, " + DESCRIPTION + " TEXT, " + VALUE_STRING + " TEXT, " + VALUE_INT + " INT, " + VALUE_REAL + " REAL)";

        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_APPROVED + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + VOLUME + " REAL, " + NUMBER_OF_TRADES + " INT, " + LONGSHORT + " INT, " + TIME_APPROVED + " INT, " + PRICE_APPROVED + " REAL )";

        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_APPROVED_HISTORIC + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + VOLUME + " REAL, " + NUMBER_OF_TRADES + " INT, " + LONGSHORT + " INT, " + TIME_APPROVED + " INT, " + PRICE_APPROVED + " REAL )";

        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_ORDERS + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + IS_IT_REAL + " INT, " + ENTRY_AMOUNT + " REAL, " + ENTRY_PRICE + " REAL, " + CURRENT_PRICE + " REAL, " + STOP_LIMIT + " REAL, " + TAKE_PROFIT + " REAL, " + MARGIN + " INT, " + TIME_WHEN_PLACED + " INT, " + IS_IT_CROSSED + " INT, " + IS_IT_SHORT + " INT, " + WHAT_ACCOUNT + " INT)";

        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_HISTORIC_PERCENTAGES + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + UNDER3 + " REAL, " + UNDER2 + " REAL, " + UNDER1 + " REAL, " + OVER1 + " REAL, " + OVER2 + " REAL, " + OVER3 + " REAL, " + TIME_WHEN_PLACED + " INT)";

        sqLiteDatabase.execSQL(query);


    }


    public void addNewCrypto(List<String> symbolList) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Begin a database transaction
            db.beginTransaction();

            for (int i = 0; i < symbolList.size(); i++) {
                values.put(SYMBOL_CRYPTO, symbolList.get(i));
                db.insert(TABLE_SYMBOL_AVG, null, values);
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
    public void addParam(int id, String descr, String value, int valueInt, float valueFloat) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Begin a database transaction
            db.beginTransaction();

            values.put(ID, id);
            values.put(DESCRIPTION, descr);
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
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            // Begin a database transaction
            db.beginTransaction();

            values.put(UNDER3, percents.getUnder3());
            values.put(UNDER2, percents.getUnder2());
            values.put(UNDER1, percents.getUnder1());
            values.put(OVER1, percents.getOver1());
            values.put(OVER2, percents.getOver2());
            values.put(OVER3, percents.getOver3());
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

        Log.e(TAG, "klinesAreInsertedToDB");
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
            db.insert(TABLE_NAME_ORDERS, null, values);

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
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT " + SYMBOL_CRYPTO + " FROM " + TABLE_SYMBOL_AVG, null);

        return data;
    }

    public Cursor retrieveAllFromTable(String tableName) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + tableName, null);

        return data;
    }

    public Cursor retrieveAllFromTableApproved(String tableName) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + tableName + " ORDER BY " + TIME_APPROVED + " DESC", null);
        return data;
    }

    public Cursor retrieveDataToFindBestCrypto(String tableName, String tokenSymbol) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + tableName + " WHERE " + SYMBOL_CRYPTO + " = '" + tokenSymbol + "' ORDER BY " + OPEN_TIME + " DESC", null);
        return data;

    }

    public Cursor retrieveParam(int id) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + TABLE_NAME_CONFIG + " WHERE " + ID + " = " + id, null);
        return data;
    }

    public Cursor nrOfKlinesForSymbolInInterval(String symbol, String interval) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT count(*) FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "'", null);
        return data;
    }

    public Cursor retrieveLastCloseTime(String interval) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' ORDER BY " + CLOSE_TIME + " DESC", null);
        return data;
    }

    public Cursor retrieveLastClosePrice(String symbol) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '3m' and " + SYMBOL_CRYPTO + " = '" + symbol + "' ORDER BY " + CLOSE_TIME + " DESC", null);
        return data;
    }

    public Cursor checkVolumeOfKlineInterval(String interval) {
        SQLiteDatabase db = getInstance(mContext).getReadableDatabase();
        Cursor data = null;
        String selectFrom = "SELECT " + SYMBOL_CRYPTO + "," + VOLUME + "," + NUMBER_OF_TRADES + ", max(" + CLOSE_TIME + "), " + CLOSE_PRICE + ",        (volume*close_price) as Dollars FROM ";
        data = db.rawQuery(selectFrom + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' GROUP BY " + SYMBOL_CRYPTO + " ORDER BY Dollars DESC", null);
        return data;
    }


    public Cursor firstAppearOfTokenInCertainTime(long time1, long time2) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        Cursor data = null;
        if (sqLiteDatabase.isOpen()) {
            String query = "WITH timePeriodHistoric AS (SELECT * FROM historic_approved_tokens where approve_time BETWEEN " + time1 + " AND " + time2 + " ), " + "firstWriteOfToken AS (SELECT  *, MIN(approve_time) FROM timePeriodHistoric GROUP BY symbol, longOrShort), " + "klines AS (SELECT * FROM klines_data WHERE symbol in (SELECT symbol FROM firstWriteOfToken) AND interval LIKE '3m') " + "SELECT K.symbol, open_time, open_price, high_price, low_price, close_price, close_time, F.approve_time, F.longOrShort, F.price_when_approved FROM klines K " + "JOIN firstWriteOfToken F ON K.symbol = F.symbol " + "WHERE K.open_time = (SELECT MAX(open_time) FROM klines WHERE symbol = K.symbol) ORDER BY K.symbol asc";

            data = sqLiteDatabase.rawQuery(query, null);

        }
        return data;
    }

    public Cursor retrievePercentages(long time1, long time2) {
        SQLiteDatabase sqLiteDatabase = getInstance(mContext).getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_HISTORIC_PERCENTAGES + " WHERE " + TIME_WHEN_PLACED + " BETWEEN '" + time1 + "' AND '" + time2 + "' ORDER BY " + TIME_WHEN_PLACED + " DESC";
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery(query, null);
        Log.e(TAG, query);
        return data;
    }

    public void deleteWithWhereClause(String tableName, String columnName, int idParam) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            @SuppressLint("Recycle") String query = "DELETE FROM " + tableName + " WHERE " + columnName + " = " + idParam;
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

    public void updateWithWhereClauseINT(String tableName, String updatedColumnName, int value, String whereColumnName, String whereSymbol) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "UPDATE " + tableName + " SET " + updatedColumnName + " = '" + value + "' WHERE " + whereColumnName + " = '" + whereSymbol + "'";
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

    public void updateCurrentPriceOfCryptoInOrders(String symbol, String column, float amount, long time) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "UPDATE " + TABLE_NAME_ORDERS + " SET " + CURRENT_PRICE + " = " + amount + " WHERE " + SYMBOL_CRYPTO + " = '" + symbol + "' AND " + TIME_WHEN_PLACED + " = " + time;
            Log.e(TAG, query);
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


    public void deleteMostNewKlineForSymbolInterval(String interval, String symbol, int limit) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + ID + " in (" + "SELECT " + ID + " FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "' ORDER BY " + CLOSE_TIME + " DESC LIMIT " + limit + ")";
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

    public void deleteOldestKlinesForSymbolInterval(String interval, String symbol, int limit) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            String query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + ID + " in (" + "SELECT " + ID + " FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "' ORDER BY " + CLOSE_TIME + " ASC LIMIT " + limit + ")";
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

    public void deleteOrder(String symbol, long time, int isItReal, int isItShort, int margin) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "DELETE FROM " + TABLE_NAME_ORDERS + " WHERE " + SYMBOL_CRYPTO + " = '" + symbol + "' and " + TIME_WHEN_PLACED + " = " + time + " and " + IS_IT_REAL + " = " + isItReal + " and " + IS_IT_SHORT + " = " + isItShort + " and " + MARGIN + " = " + margin;
            Log.e(TAG, "DELETE Order: " + query);
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


    public void deleteAllKlinesForSymbolInterval(String interval, String symbol) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();

            String query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "'";
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


    public void clearHistoricApproved(long time) {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();

        try {
            // Begin a database transaction
            db.beginTransaction();
            Log.e(TAG, "CLEARED APPROVED HISTORY");
            String query = "DELETE FROM " + TABLE_NAME_APPROVED_HISTORIC + " WHERE " + TIME_APPROVED + " <= " + time;
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
            Log.e(TAG, "CLEARED PERCENTAGES HISTORY");
            String query = "DELETE FROM " + TABLE_HISTORIC_PERCENTAGES + " WHERE " + TIME_WHEN_PLACED + " <= " + time;
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

    public void close() {
        SQLiteDatabase db = getInstance(mContext).getWritableDatabase();
        if (db != null && db.isOpen()) {

        }
    }

}
