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

    @ColumnInfo(name = "pubKey")
    private String pubKey;

    public InheritanceEntity(@NonNull String address, String label, String pubKey) {
        this.address = address;
        this.label = label;
        this.pubKey = pubKey;
    }

    public String getAddress() {
        return address;
    }

    public String getPubKey() {
        return pubKey;
    }

    public String getLabel() {
        return label;
    }

}
