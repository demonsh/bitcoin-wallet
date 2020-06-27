package de.schildbach.wallet.ui.inheritance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.schildbach.wallet.R;
import de.schildbach.wallet.data.AppDatabase;
import de.schildbach.wallet.data.InheritanceDao;
import de.schildbach.wallet.data.InheritanceEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;

public class InheritanceOwnerNewHair extends AbstractWalletActivity {

    private String address;
    private String label;

    private InheritanceDao inheritanceDao;

    @Override
    protected void onCreate(Bundle inheritanceOwnerNewHair) {
        super.onCreate(inheritanceOwnerNewHair);
        setContentView(R.layout.activity_inheritance_owner_new_hair);

        this.inheritanceDao = AppDatabase.getDatabase(this.getBaseContext()).inheritanceDao();

        Intent intent = getIntent();

        address = intent.getStringExtra("address");

        final TextView txView = findViewById(R.id.heir_address);
        txView.setText(address);

        final EditText labelField = findViewById(R.id.heir_label);

        final Button btn = findViewById(R.id.save_heir_addr_btn);

        btn.setOnClickListener(v -> { onSaveClick(v);});

    }

    //Save hair address to db
    private void onSaveClick(View v) {

        try {
            InheritanceEntity in = new InheritanceEntity(address, label);
            inheritanceDao.insertOrUpdate(in);

            Toast.makeText(InheritanceOwnerNewHair.this, "Hair address saved", Toast.LENGTH_LONG);

            finish();

        } catch (Exception exc) {
            log.error(exc.toString());
            Toast.makeText(InheritanceOwnerNewHair.this, exc.getMessage(), Toast.LENGTH_LONG);
        }
    }
}
