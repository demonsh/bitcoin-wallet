package de.schildbach.wallet.ui.inheritance.owner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.R;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.data.AbstractWalletLiveData;
import de.schildbach.wallet.data.AppDatabase;
import de.schildbach.wallet.data.InheritanceDao;
import de.schildbach.wallet.data.InheritanceEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.util.Inheritance;
import de.schildbach.wallet.util.InterimAddressInfo;
import de.schildbach.wallet.util.Qr;

public class InheritanceOwnerDetailsActivity extends AbstractWalletActivity {

    /**
     * Heir's address
     */
    private String address;
    private Transaction tx;
    private Wallet wallet;

    private String txString;
    private String ownerAddress;

    private InheritanceDao inheritanceDao;

    private ClipboardManager clipboardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inheritance_q_r);


        this.clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        final TextView addressView = findViewById(R.id.address);
        final TextView txView = findViewById(R.id.tx);

        Intent intent = getIntent();

        address = intent.getStringExtra("address");

        this.inheritanceDao = AppDatabase.getDatabase(this.getBaseContext()).inheritanceDao();

        InheritanceEntity current = inheritanceDao.get(address);

        if (current.getTx() == null) {
            try {
                WalletApplication application = getWalletApplication();
                wallet = application.getWallet();
                Address ownerAddress = wallet.currentReceiveAddress();
                this.ownerAddress = ownerAddress.toString();

                Address heirAddress = Address.fromString(Constants.NETWORK_PARAMETERS, address);
                tx = Inheritance.signInheritanceTx(ownerAddress, heirAddress, 6, wallet);

                String signedTx = Hex.toHexString(tx.bitcoinSerialize());
                this.txString = signedTx;

                InheritanceEntity inheritanceEntity = inheritanceDao.get(address);
                inheritanceEntity.setTx(signedTx);
                inheritanceEntity.setOwnerAddress(ownerAddress.toString());

                inheritanceDao.insertOrUpdate(inheritanceEntity);

            } catch (Exception exc) {
                Toast.makeText(this, "Failed to sign inheritance transaction: Some exception, see debug log... ", Toast.LENGTH_LONG).show();
            }
        }

        if (current.getTx() != null) {
            WalletApplication application = getWalletApplication();
            wallet = application.getWallet();

            String owner = current.getOwnerAddress();
            this.ownerAddress = owner;

            String tx = current.getTx();
            this.txString = tx;

            NetworkParameters params = wallet.getParams();
            byte[] hex = Hex.decode(tx);
            this.tx = new Transaction(params, hex);

            //Check tx status
            InterimAddressInfo interimAddressInfo = Inheritance.getInterimAddressInfo(
                    Address.fromString(Constants.NETWORK_PARAMETERS, ownerAddress),
                    Address.fromString(Constants.NETWORK_PARAMETERS, address),
                    8,
                    wallet);

            final TextView txStatusView = findViewById(R.id.tx_status);

            txStatusView.setText(String.valueOf(interimAddressInfo.blockTillDeadline));
        }

        addressView.setText(this.address);
        addressView.setOnClickListener(copyToClipBoardAddress());

        txView.setText(this.txString);
        txView.setOnClickListener(copyToClipBoardTx());

    }

    private TextView.OnClickListener copyToClipBoardAddress() {
        return v ->{
            // Creates a new text clip to put on the clipboard
            ClipData clip = ClipData.newPlainText("Address copied", this.ownerAddress);
            clipboardManager.setPrimaryClip(clip);
            Toast.makeText(this, "Address copied", Toast.LENGTH_SHORT).show();
        };
    }

    private TextView.OnClickListener copyToClipBoardTx() {
        return v ->{
            // Creates a new text clip to put on the clipboard
            ClipData clip = ClipData.newPlainText("Tx copied", this.txString);
            clipboardManager.setPrimaryClip(clip);
            Toast.makeText(this, "Tx copied", Toast.LENGTH_SHORT).show();
        };
    }

    private View.OnClickListener onBroadCastTx() {
        return v -> {

            broadcast();
        };
    }

    private void broadcast() {
        try {
            Inheritance.broadcastTx(this.tx, this.wallet);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send tx...", Toast.LENGTH_LONG).show();
        }
        Toast.makeText(this, "Tx sent", Toast.LENGTH_LONG).show();
    }


    private View.OnClickListener onWithdraw() {
        return v -> {

            withdraw();
        };
    }

    private void withdraw() {
        try {
            Inheritance.withdrawFromInterimAddress(
                    Address.fromString(Constants.NETWORK_PARAMETERS, ownerAddress),
                    Address.fromString(Constants.NETWORK_PARAMETERS, address),
                    6,
                    this.wallet);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to withdraw tx...", Toast.LENGTH_LONG).show();
        }
        Toast.makeText(this, "Withdraw", Toast.LENGTH_LONG).show();
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.hair_details_options, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        if (item.getItemId() == R.id.send) {

            broadcast();
            return true;
        }

        if (item.getItemId() == R.id.withdraw) {

            withdraw();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
