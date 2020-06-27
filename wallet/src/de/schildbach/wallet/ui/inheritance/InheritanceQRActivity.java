package de.schildbach.wallet.ui.inheritance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.util.encoders.Hex;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.R;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.data.InheritanceEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.util.Inheritance;
import de.schildbach.wallet.util.Qr;

public class InheritanceQRActivity extends AbstractWalletActivity {

    /**
     * Heir's address
     */
    private String address;
    private Transaction tx;
    private Wallet wallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inheritance_q_r);

        Intent intent=getIntent();

        address = intent.getStringExtra("address");

        try {
            WalletApplication application = getWalletApplication();
            wallet = application.getWallet();
            Address ownerAddress = wallet.currentReceiveAddress();

            Address heirAddress = Address.fromString(Constants.NETWORK_PARAMETERS, address);
            tx = Inheritance.signInheritanceTx(ownerAddress, heirAddress, 6, wallet);
            String signedTx = Hex.toHexString(tx.bitcoinSerialize());
           // Toast.makeText(this, "Successfully signed inheritance transaction: " + signedTx.substring(0, 50) + "...", Toast.LENGTH_LONG).show();

            final ImageView address_qr = findViewById(R.id.address_qr);
            address_qr.setImageBitmap(Qr.bitmap(signedTx));

            //TODO: save tx

        } catch (Exception exception) {
            Toast.makeText(this, "Failed to sign inheritance transaction: Some exception, see debug log... ", Toast.LENGTH_LONG).show();
        }



        Button broadcastBtn = findViewById(R.id.inheritance_broadcast);

        broadcastBtn.setOnClickListener(v -> {
            broadCast();
        });


    }

    private void broadCast() {
        //TODO:
        Inheritance.broadcastInheritanceTx(tx, wallet);
        //TODO: how to check/track the result
    }

//    private void signInheritanceTx(View v) {
//        WalletApplication application = getWalletApplication();
//        Wallet wallet = application.getWallet();
//
//        Address ownerAddress = wallet.currentReceiveAddress();
//
//        InheritanceEntity heirEntity = inheritanceDao
//                .getAll()
//                .stream()
//                .filter(entity -> "heirAddress".equals(entity.getLabel()))
//                .findAny()
//                .orElse(null);
//
//        if (heirEntity != null) {
//            try {
//                Address heirAddress = Address.fromString(Constants.NETWORK_PARAMETERS, heirEntity.getAddress());
//                Transaction tx = Inheritance.signInheritanceTx(ownerAddress, heirAddress, 6, wallet);
//                signedTx = Hex.toHexString(tx.bitcoinSerialize());
//                Toast.makeText(this, "Successfully signed inheritance transaction: " + signedTx.substring(0, 50) + "...", Toast.LENGTH_LONG).show();
//            } catch (Exception exception) {
//                Toast.makeText(this, "Failed to sign inheritance transaction: Some exception, see debug log... ", Toast.LENGTH_LONG).show();
//            }
//        } else {
//            Toast.makeText(this, "Failed to sign inheritance transaction: Heir address is missing", Toast.LENGTH_LONG).show();
//        }
//
//        //todo make this part of code not to crash application and show QR of inheritnace transaction
////        final BitmapDrawable bitmap = new BitmapDrawable(getResources(), Qr.bitmap(this.signedTx));
////        bitmap.setFilterBitmap(false);
////        final ImageView imageView = findViewById(R.id.bitcoin_address_qr);
////        imageView.setImageDrawable(bitmap);
//    }
}
