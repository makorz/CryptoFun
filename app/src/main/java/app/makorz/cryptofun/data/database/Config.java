package app.makorz.cryptofun.data.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "config_param")
public class Config {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "paramId")
    private long tId;

    @ColumnInfo(name = "paramDescription")
    private String tDescrtption;

    @ColumnInfo(name = "paramStringValue")
    private String tValueString;

    @ColumnInfo(name = "paramLongValue")
    private long tValueLong;

    public Config(long tId, String tDescrtption, String tValueString, long tValueLong) {
        this.tId = tId;
        this.tDescrtption = tDescrtption;
        this.tValueString = tValueString;
        this.tValueLong = tValueLong;
    }

    public long gettId() {
        return tId;
    }

    public String gettDescrtption() {
        return tDescrtption;
    }

    public String gettValueString() {
        return tValueString;
    }

    public long gettValueLong() {
        return tValueLong;
    }
}
