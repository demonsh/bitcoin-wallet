package de.schildbach.wallet.ui.inheritance.heir;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.bitcoinj.wallet.Wallet;

import de.schildbach.wallet.R;
import de.schildbach.wallet.data.AppDatabase;
import de.schildbach.wallet.data.InheritanceTxDao;
import de.schildbach.wallet.data.TxEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.ui.inheritance.InheritanceHeirActivity;
import de.schildbach.wallet.util.Inheritance;

public class NewHairTx extends AbstractWalletActivity {

    private InheritanceTxDao txDao;

    private EditText ownerAddressView;
    private EditText txView;
    private EditText blocksView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_hair_tx);

        this.txDao = AppDatabase.getDatabase(this.getBaseContext()).txDao();

        ownerAddressView = findViewById(R.id.ownerAddress);
        blocksView = findViewById(R.id.blocks);

    }

    private void saveTx() {
        String ownerAddress = ownerAddressView.getText().toString();
        String tx = txView.getText().toString();
        String label = ((EditText)findViewById(R.id.label)).getText().toString();

        String blocks = blocksView.getText().toString();

        TxEntity txEntity = new TxEntity(tx,ownerAddress, label, blocks);
        txDao.insertOrUpdate(txEntity);

        Wallet wallet = getWalletApplication().getWallet();
        wallet.addWatchedAddress()

        Toast.makeText(this, "Transaction added", Toast.LENGTH_LONG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_hair_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.save) {

            try {

                saveTx();

                finish();

            } catch (Exception exc) {
                log.error(exc.toString());
                Toast.makeText(this, exc.getMessage(), Toast.LENGTH_LONG);

                return false;
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}