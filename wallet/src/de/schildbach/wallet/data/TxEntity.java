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

    @ColumnInfo(name = "blocks")
    private String blocks;

    public TxEntity(@NonNull String tx, String ownerAddress, String label, String blocks) {
        this.tx = tx;
        this.ownerAddress = ownerAddress;
        this.label = label;
        this.blocks = blocks;
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

    public String getBlocks() {
        return blocks;
    }
}
