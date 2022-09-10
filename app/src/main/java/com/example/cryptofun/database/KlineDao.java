package com.example.cryptofun.database;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;

public interface KlineDao {

    @Query("SELECT * FROM kline_data")
    Flowable<List<Kline>> getAllKlines();

    @Query("DELETE FROM kline_data")
    void deleteAllKlines();

    @Delete
    void deleteSingleKline(Kline kline);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertKlines(Kline... klines);

    @Query("SELECT * FROM kline_data WHERE cryptoSymbol = '% :symbol %' and klineInterval = '% :interval %' ORDER BY openTime ASC")
    Flowable<List<Kline>> getSortedKlineInterval(String symbol, String interval);

}
