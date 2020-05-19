package de.schildbach.wallet.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface InheritanceTxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(TxEntity txEntity);

    @Query("DELETE FROM inheritanceTx WHERE tx = :tx")
    void delete(String tx);
//
//    @Query("SELECT * FROM address_book WHERE address LIKE '%' || :constraint || '%' OR label LIKE '%' || :constraint || '%' ORDER BY label COLLATE LOCALIZED ASC")
//    List<AddressBookEntry> get(String constraint);
//
    @Query("SELECT * FROM inheritanceTx")
    List<TxEntity> getAll();
//
//    @Query("SELECT * FROM address_book WHERE address NOT IN (:except) ORDER BY label COLLATE LOCALIZED ASC")
//    LiveData<List<AddressBookEntry>> getAllExcept(Set<String> except);
}
