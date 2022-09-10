package com.example.cryptofun.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


@Database(entities = {Config.class, Kline.class, Symbols.class}, version = 1)
public abstract class CryptoDatabase extends RoomDatabase {

    private static volatile CryptoDatabase INSTANCE;

    public abstract KlineDao klineDao();
    public abstract SymbolDao symbolDao();
    public abstract ConfigDao configDao();

    public static CryptoDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CryptoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    CryptoDatabase.class, "CryptoFunDB.db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}
