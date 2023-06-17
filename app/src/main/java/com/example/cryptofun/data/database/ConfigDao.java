package com.example.cryptofun.data.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import io.reactivex.Completable;
import io.reactivex.Flowable;

@Dao
public interface ConfigDao {
    @Query("SELECT * FROM config_param WHERE paramId = ' :id'")
    Flowable<Config> getParam(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertParam(Config param);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateParam(Config... params);

    @Query("DELETE FROM config_param")
    void deleteAllSymbols();

}
