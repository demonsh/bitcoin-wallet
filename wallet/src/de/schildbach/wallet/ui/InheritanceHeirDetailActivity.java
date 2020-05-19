package de.schildbach.wallet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import de.schildbach.wallet.R;

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
            String toastText = tx;
            if (tx.length() > 49) {
                toastText = tx.substring(0, 50);
            }
            Toast.makeText(this, "Successfully send transaction: " + toastText + "...", Toast.LENGTH_LONG).show();

        });
    }
}
