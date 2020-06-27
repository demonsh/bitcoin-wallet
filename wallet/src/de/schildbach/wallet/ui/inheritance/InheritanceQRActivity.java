package de.schildbach.wallet.ui.inheritance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import de.schildbach.wallet.R;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.util.Qr;

public class InheritanceQRActivity extends AbstractWalletActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inheritance_q_r);

        Intent intent=getIntent();

        String tx = intent.getStringExtra("address");

        final ImageView address_qr = findViewById(R.id.address_qr);
        address_qr.setImageBitmap(Qr.bitmap(tx));
    }
}
