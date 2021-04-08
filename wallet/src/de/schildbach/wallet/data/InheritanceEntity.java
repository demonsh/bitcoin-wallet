package de.schildbach.wallet.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inheritance")
public class InheritanceEntity {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "address")
    private String address;

    @ColumnInfo(name = "label")
    private String label;

    @ColumnInfo(name = "tx")
    private String tx;

    @ColumnInfo(name = "ownerAddress")
    private String ownerAddress;

    @ColumnInfo(name = "blocks")
    private String blocks;



    public InheritanceEntity(@NonNull String address, String label, String blocks) {
        this.address = address;
        this.label = label;
        this.blocks = blocks;
    }

    public String getAddress() {
        return address;
    }

    public String getLabel() {
        return label;
    }

    public String getTx() {
        return tx;
    }

    public void setTx(String tx) {
        this.tx = tx;
    }

    public String getOwnerAddress() {
        return ownerAddress;
    }

    public void setOwnerAddress(String ownerAddress) {
        this.ownerAddress = ownerAddress;
    }

    public String getBlocks() {
        return blocks;
    }
}
