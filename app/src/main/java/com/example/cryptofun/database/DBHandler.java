package com.example.cryptofun.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.example.cryptofun.data.ApprovedToken;

import java.util.List;

public class DBHandler extends SQLiteOpenHelper {

    private static final String DB_NAME = "cryptodb.db";
    private static final int DB_VERSION = 1;

    private static DBHandler dbInstance;

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
    private static final String VALUE = "value";

    private static final String TABLE_NAME_APPROVED = "approved_tokens";
    private static final String LONGSHORT = "longOrShort";
    private static final String TIME_APPROVED = "approve_time";
    private static final String PRICE_APPROVED = "price_when_approved";

    private static final String TABLE_NAME_APPROVED_HISTORIC = "historic_approved_tokens";


    // creating a constructor for our database handler.
    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    //Singleton
    public static synchronized DBHandler getInstance(Context context) {
        if (dbInstance == null) {
            dbInstance = new DBHandler(context.getApplicationContext());
        }
        return dbInstance;
    }

    // below method is for creating a database by running a sqlite query
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        String query = "CREATE TABLE " + TABLE_SYMBOL_AVG + " (" + ID_CRYPTO + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT," + AVG_PRICE + " REAL)";
        sqLiteDatabase.execSQL(query);


        query = "CREATE TABLE " + TABLE_NAME_KLINES_DATA + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + OPEN_TIME + " INTEGER, " + OPEN_PRICE + " REAL, " + HIGH_PRICE + " REAL, " + LOW_PRICE + " REAL, " + CLOSE_PRICE + " REAL, " + VOLUME + " REAL, " + CLOSE_TIME + " INTEGER, " + NUMBER_OF_TRADES + " INTEGER, " + INTERVAL + " TEXT, " + "FOREIGN KEY (" + SYMBOL_CRYPTO + ") REFERENCES " + TABLE_SYMBOL_AVG + " (" + SYMBOL_CRYPTO + "));";

        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_CONFIG + " (" + ID + " INTEGER, " + DESCRIPTION + " TEXT, " + VALUE + " TEXT )";

        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_APPROVED + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + VOLUME + " REAL, " + NUMBER_OF_TRADES + " INT, " + LONGSHORT + " INT, " + TIME_APPROVED + " INT, " + PRICE_APPROVED + " REAL )";

        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_APPROVED_HISTORIC + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SYMBOL_CRYPTO + " TEXT, " + VOLUME + " REAL, " + NUMBER_OF_TRADES + " INT, " + LONGSHORT + " INT, " + TIME_APPROVED + " INT, " + PRICE_APPROVED + " REAL )";

        sqLiteDatabase.execSQL(query);

    }

    // this method is use to add new course to our sqlite database.
    public void addNewCrypto(List<String> symbolList) {

        // on below line we are creating a variable for
        // our sqlite database and calling writable method
        // as we are writing data in our database.
        SQLiteDatabase db = this.getWritableDatabase();

        // on below line we are creating a
        // variable for content values.
        ContentValues values = new ContentValues();
        db.execSQL("begin transaction");

        // on below line we are passing all values
        // along with its key and value pair.
        for (int i = 0; i < symbolList.size(); i++) {
            values.put(SYMBOL_CRYPTO, symbolList.get(i));
            // after adding all values we are passing
            // content values to our table.
            db.insert(TABLE_SYMBOL_AVG, null, values);

        }
        // at last we are closing our
        // database after adding database.
        db.execSQL("commit transaction");
        db.close();
    }

    public void addApprovedNewCrypto(ApprovedToken symbol, String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        db.execSQL("begin transaction");
        values.put(SYMBOL_CRYPTO, symbol.getSymbol());
        values.put(VOLUME, symbol.getVolumeOnKlines());
        values.put(NUMBER_OF_TRADES, symbol.getNrOfTradesOnKlines());
        values.put(LONGSHORT, symbol.getLongOrShort());
        values.put(TIME_APPROVED, symbol.getTime());
        values.put(PRICE_APPROVED, symbol.getPriceOnTimeOfApprove());
        db.insert(tableName, null, values);
        db.execSQL("commit transaction");
        db.close();
    }

    // id:1 -> Time of last update
    public void addParam(int id, String descr, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ID, id);
        values.put(DESCRIPTION, descr);
        values.put(VALUE, value);
        db.insert(TABLE_NAME_CONFIG, null, values);
        db.close();
    }


    // this method is use to add new kline data to database, interval 1 - 3m, interval = 2 - 15m, interval = 3 - 24h
    public int addNewKlineData(List<rawTable_Kline> object) {

        Log.e("DB", "klinesAreInsertedToDB");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        db.execSQL("begin transaction");

        int i;
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
        db.execSQL("commit transaction");
        db.close();
        return i;
    }

    public Cursor retrieveCryptoSymbolsToListView() {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT " + SYMBOL_CRYPTO + " FROM " + TABLE_SYMBOL_AVG, null);

        return data;
    }

    public Cursor retrieveAllFromTable(String tableName) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + tableName, null);

        return data;
    }

    public Cursor retrieveAllFromTableApproved(String tableName) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + tableName + " ORDER BY " + TIME_APPROVED + " DESC", null);

        return data;
    }

    public Cursor retrieveDataToFindBestCrypto(String tableName, String tokenSymbol) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + tableName + " WHERE " + SYMBOL_CRYPTO + " = '" + tokenSymbol + "' ORDER BY " + OPEN_TIME + " DESC", null);

        return data;

    }

    public Cursor retrieveParam(int id) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + TABLE_NAME_CONFIG + " WHERE " + ID + " = " + id, null);

        return data;
    }

    public void deleteWithWhereClause(String tableName, String columnName, int idParam) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        @SuppressLint("Recycle") String query = "DELETE FROM " + tableName + " WHERE " + columnName + " = " + idParam;
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();
    }

    public void deleteOldApproved(String tableName, String columnName, long time) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        @SuppressLint("Recycle") String query = "DELETE FROM " + tableName + " WHERE " + columnName + " < " + time;
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();
    }

    public Cursor nrOfKlinesForSymbolInInterval(String symbol, String interval) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT count(*) FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "'", null);
        return data;
    }

    public void updateWithWhereClause(String tableName, String updatedColumnName, String value, String whereColumnName, String whereSymbol) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.execSQL("begin transaction");
        String query = "UPDATE " + tableName + " SET " + updatedColumnName + " = '" + value + "' WHERE " + whereColumnName + " = '" + whereSymbol + "'";
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.execSQL("commit transaction");
        sqLiteDatabase.close();
    }

    public Cursor retrieveLastCloseTime(String interval) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' ORDER BY " + CLOSE_TIME + " DESC", null);
        return data;
    }

    public Cursor checkVolumeOfKlineInterval(String interval) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Cursor data = null;
        String selectFrom = "SELECT " + SYMBOL_CRYPTO + "," + VOLUME + "," + NUMBER_OF_TRADES + ", max(" + CLOSE_TIME + "), " + CLOSE_PRICE + ",        (volume*close_price) as Dollars FROM ";
        data = sqLiteDatabase.rawQuery(selectFrom + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' GROUP BY " + SYMBOL_CRYPTO + " ORDER BY Dollars DESC", null);
        return data;
    }

    public void deleteMostNewKlineForSymbolInterval(String interval, String symbol, int limit) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + ID + " in (" + "SELECT " + ID + " FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "' ORDER BY " + CLOSE_TIME + " DESC LIMIT " + limit + ")";
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();
    }

    public void deleteOldestKlinesForSymbolInterval(String interval, String symbol, int limit) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + ID + " in (" + "SELECT " + ID + " FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "' ORDER BY " + CLOSE_TIME + " ASC LIMIT " + limit + ")";
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();
    }

    public void deleteAllKlinesForSymbolInterval(String interval, String symbol) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME_KLINES_DATA + " WHERE " + INTERVAL + " = '" + interval + "' and " + SYMBOL_CRYPTO + " = '" + symbol + "'";
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();
    }

    public Cursor firstAppearOfTokenInCertainTime(long time1, long time2) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        Cursor data = null;
        if (sqLiteDatabase.isOpen()) {
            String query = "WITH timePeriodHistoric AS (SELECT * FROM historic_approved_tokens where approve_time BETWEEN " + time1 + " AND " + time2 + " ), " + "firstWriteOfToken AS (SELECT  *, MIN(approve_time) FROM timePeriodHistoric GROUP BY symbol, longOrShort), " + "klines AS (SELECT * FROM klines_data WHERE symbol in (SELECT symbol FROM firstWriteOfToken) AND interval LIKE '3m') " + "SELECT K.symbol, open_time, open_price, high_price, low_price, close_price, close_time, F.approve_time, F.longOrShort, F.price_when_approved FROM klines K " + "JOIN firstWriteOfToken F ON K.symbol = F.symbol " + "WHERE K.open_time = (SELECT MAX(open_time) FROM klines WHERE symbol = K.symbol) ORDER BY K.symbol asc";

            data = sqLiteDatabase.rawQuery(query, null);

        }
        return data;
    }

    public void clearHistoric(long time) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME_APPROVED_HISTORIC + " WHERE " + TIME_APPROVED + " <= " + time;
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();
        Log.e("DB_HANDLER", "CLEARED HISTORY");
    }

    public void clearTable(String table_name) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "DELETE FROM " + table_name;
        sqLiteDatabase.execSQL(query);
        sqLiteDatabase.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // this method is called to check if the table exists already.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SYMBOL_AVG);
        onCreate(sqLiteDatabase);
    }

    public void close() {
        SQLiteDatabase db = this.getWritableDatabase();
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

}
