package de.schildbach.wallet.ui.inheritance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.util.encoders.Hex;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.R;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.data.TxEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.ui.inheritance.heir.NewHairTx;
import de.schildbach.wallet.util.Inheritance;
import de.schildbach.wallet.util.InterimAddressInfo;

public class InheritanceHeirDetailActivity extends AbstractWalletActivity {

    private Wallet wallet;
    private String tx;


    private static final String TAG = "InheHeirDetActivity";

    private TxEntity txEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heir_details);

        Intent intent = getIntent();

        WalletApplication application = getWalletApplication();
        wallet = application.getWallet();

        txEntity = (TxEntity) intent.getSerializableExtra("txEntity");

        tx = txEntity.getTx();

        final TextView txView = findViewById(R.id.heir_tx);
        txView.setText(tx);

        //Check tx status
        InterimAddressInfo interimAddressInfo = Inheritance.getInterimAddressInfo(
                Address.fromString(Constants.NETWORK_PARAMETERS, txEntity.getOwnerAddress()),
                wallet.currentReceiveAddress(),
                8,
                wallet);

        final TextView txStatusView = findViewById(R.id.tx_status);

        txStatusView.setText(String.valueOf(interimAddressInfo.blockTillDeadline));
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.hair_details_options, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        if (item.getItemId() == R.id.send) {

            send();
            return true;
        }

        if (item.getItemId() == R.id.withdraw) {

            withdraw();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void withdraw() {
        try {
            Inheritance.withdrawFromInterimAddress(
                    Address.fromString(Constants.NETWORK_PARAMETERS, txEntity.getOwnerAddress()),
                    wallet.currentReceiveAddress(),
                    6,
                    this.wallet);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to withdraw tx...", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Withdraw", Toast.LENGTH_LONG).show();
    }

    private void send(){

        try {
            NetworkParameters params = wallet.getParams();
            byte[] hex = Hex.decode(tx);
            Transaction transaction = new Transaction(params, hex);

            Inheritance.broadcastTx(transaction, wallet);
            finish();
        } catch (Exception exc) {

            Log.e(TAG, exc.toString());

            Toast.makeText(this, "Failed to send tx...", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Tx sent", Toast.LENGTH_LONG).show();
    }


}
