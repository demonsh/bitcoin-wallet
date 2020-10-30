package de.schildbach.wallet.ui.inheritance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

        //Check tx status
        InterimAddressInfo interimAddressInfo = Inheritance.getInterimAddressInfo(
                Address.fromString(Constants.NETWORK_PARAMETERS, txEntity.getOwnerAddress()),
                wallet.currentReceiveAddress(),
                8,
                wallet);

        final TextView txStatusView = findViewById(R.id.tx_status);

        txStatusView.setText(String.valueOf(interimAddressInfo.blockTillDeadline));

        Button withdrawBtn = findViewById(R.id.withdraw);
        withdrawBtn.setOnClickListener(onWithdraw());
    }


    private View.OnClickListener onWithdraw() {
        return v -> {

            try {
                Inheritance.withdrawFromInterimAddress(
                        Address.fromString(Constants.NETWORK_PARAMETERS, txEntity.getOwnerAddress()),
                        wallet.currentReceiveAddress(),
                        6,
                        this.wallet);
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to withdraw tx...", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(this, "Withdraw", Toast.LENGTH_LONG).show();
        };
    }


}
