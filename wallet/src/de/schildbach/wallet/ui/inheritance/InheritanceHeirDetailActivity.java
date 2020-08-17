package de.schildbach.wallet.ui.inheritance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.util.encoders.Hex;

import de.schildbach.wallet.R;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.util.Inheritance;

public class InheritanceHeirDetailActivity extends AbstractWalletActivity {

    private Wallet wallet;
    private String tx;


    private static final String TAG = "InheHeirDetActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heir_details);

        Intent intent = getIntent();

        WalletApplication application = getWalletApplication();
        wallet = application.getWallet();

        tx = intent.getStringExtra("tx");

        final TextView txView = findViewById(R.id.heir_tx);
        txView.setText(tx);

        final Button btn = findViewById(R.id.send_inheritance_tx);

        btn.setOnClickListener(e -> {

            try {
                NetworkParameters params = wallet.getParams();
                byte[] hex = Hex.decode(tx);
                Transaction transaction = new Transaction(params, hex);

                Inheritance.broadcastTx(transaction, this.wallet);
                finish();
            } catch (Exception exc) {

                Log.e(TAG, exc.toString());

                Toast.makeText(this, "Failed to send tx...", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(this, "Tx sent", Toast.LENGTH_LONG).show();

        });
    }


}
