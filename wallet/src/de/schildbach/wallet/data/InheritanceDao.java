package de.schildbach.wallet.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface InheritanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(InheritanceEntity inheritanceEntry);

    @Query("DELETE FROM inheritance WHERE address = :address")
    void delete(String address);
//
    @Query("SELECT * FROM inheritance WHERE address =:address")
    InheritanceEntity get(String address);
//
    @Query("SELECT * FROM inheritance")
    List<InheritanceEntity> getAll();
//
//    @Query("SELECT * FROM address_book WHERE address NOT IN (:except) ORDER BY label COLLATE LOCALIZED ASC")
//    LiveData<List<AddressBookEntry>> getAllExcept(Set<String> except);
}
