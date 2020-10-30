package de.schildbach.wallet.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "inheritanceTx")
public class TxEntity implements Serializable {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "tx")
    private String tx;

    @ColumnInfo(name = "ownerAddress")
    private String ownerAddress;

    @ColumnInfo(name = "label")
    private String label;

    public TxEntity(@NonNull String tx, String ownerAddress, String label) {
        this.tx = tx;
        this.ownerAddress = ownerAddress;
        this.label = label;
    }

    public String getTx() {
        return tx;
    }

    public String getLabel() {
        return label;
    }

    public String getOwnerAddress() {
        return ownerAddress;
    }
}
