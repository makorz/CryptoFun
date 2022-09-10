package com.example.cryptofun.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


import java.util.List;

public class DBHandler extends SQLiteOpenHelper {

    private static final String DB_NAME = "cryptodb.db";
    private static final int DB_VERSION = 1;

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


    // creating a constructor for our database handler.
    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // below method is for creating a database by running a sqlite query
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        String query = "CREATE TABLE " + TABLE_SYMBOL_AVG + " ("
                + ID_CRYPTO + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SYMBOL_CRYPTO + " TEXT,"
                + AVG_PRICE + " REAL)";
        sqLiteDatabase.execSQL(query);


        query = "CREATE TABLE " + TABLE_NAME_KLINES_DATA + " ("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SYMBOL_CRYPTO + " TEXT, "
                + OPEN_TIME + " INTEGER, "
                + OPEN_PRICE + " REAL, "
                + HIGH_PRICE + " REAL, "
                + LOW_PRICE + " REAL, "
                + CLOSE_PRICE + " REAL, "
                + VOLUME + " REAL, "
                + CLOSE_TIME + " INTEGER, "
                + NUMBER_OF_TRADES + " INTEGER, "
                + INTERVAL + " TEXT, "
                + "FOREIGN KEY (" + SYMBOL_CRYPTO + ") REFERENCES "
                + TABLE_SYMBOL_AVG + " (" + SYMBOL_CRYPTO + "));";

        sqLiteDatabase.execSQL(query);

        query = "CREATE TABLE " + TABLE_NAME_CONFIG + " ("
                + ID + " INTEGER, "
                + DESCRIPTION + " TEXT, "
                + VALUE + " TEXT )";

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
        db.close();
    }

    // this method is use to add new kline data to database, interval 1 - 3m, interval = 2 - 15m, interval = 3 - 24h
//    public void addNewKlineData(String interval, List<rawTable_Kline> object) {
    public int addNewKlineData(List<rawTable_Kline> object) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

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

    public Cursor retrieveDataToFindBestCrypto(String tableName, String tokenSymbol) {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        @SuppressLint("Recycle") Cursor data = sqLiteDatabase.rawQuery("SELECT * FROM " + tableName + " WHERE " + SYMBOL_CRYPTO + " = '" +
                tokenSymbol + "' ORDER BY " + OPEN_TIME + " ASC", null);
        return data;
    }

    public void clearTable(String table_name) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "DELETE FROM " + table_name;
        sqLiteDatabase.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // this method is called to check if the table exists already.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SYMBOL_AVG);
        onCreate(sqLiteDatabase);
    }

}
