package de.schildbach.wallet.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inheritanceTx")
public class TxEntity {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "tx")
    private String tx;

    @ColumnInfo(name = "label")
    private String label;

    public TxEntity(@NonNull String tx, String label) {
        this.tx = tx;
        this.label = label;
    }

    public String getTx() {
        return tx;
    }

    public String getLabel() {
        return label;
    }

}
