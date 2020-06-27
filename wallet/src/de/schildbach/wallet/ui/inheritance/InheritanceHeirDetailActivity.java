package de.schildbach.wallet.ui.inheritance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;

import de.schildbach.wallet.R;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.util.Inheritance;

public class InheritanceHeirDetailActivity extends AbstractWalletActivity {

    private String tx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heir_details);

        Intent intent = getIntent();

        tx = intent.getStringExtra("tx");

        final TextView txView = findViewById(R.id.heir_tx);
        txView.setText(tx);

        final Button btn = findViewById(R.id.send_inheritance_tx);

        btn.setOnClickListener(e -> {

            broadCast();

            String toastText = tx;
            if (tx.length() > 49) {
                toastText = tx.substring(0, 50);
            }
            Toast.makeText(this, "Successfully send transaction: " + toastText + "...", Toast.LENGTH_LONG).show();

        });
    }

    private void broadCast() {

//        WalletApplication application = getWalletApplication();
//        Wallet wallet = application.getWallet();
//
//
//        //How to convert tx from string to Transaction
//        Transaction tx = new Transaction()
//
//        //TODO:
//        Inheritance.broadcastInheritanceTx(tx, wallet);
//        //TODO: how to check/track the result
    }
}
