package com.example.cryptofun.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;

@Dao
public interface SymbolDao {
    @Query("SELECT cryptoSymbol FROM crypto_symbols")
    Flowable<List<Symbols>> getSymbols();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertCrypto(Symbols symbol);

    @Query("DELETE FROM crypto_symbols")
    void deleteAllSymbols();

}
