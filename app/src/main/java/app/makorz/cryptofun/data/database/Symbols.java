package app.makorz.cryptofun.data.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "crypto_symbols")
public class Symbols {

    @NonNull
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "cryptoId")
    private long tId;

    @ColumnInfo(name = "cryptoSymbol")
    private String tSymbol;

    public Symbols(@NonNull long tId, String tSymbol) {
        this.tId = tId;
        this.tSymbol = tSymbol;
    }

    @NonNull
    public long gettId() {
        return tId;
    }

    public String gettSymbol() {
        return tSymbol;
    }
}
