package de.schildbach.wallet.ui;

import android.os.Bundle;

import de.schildbach.wallet.R;

public class InheritanceHeirActivity extends AbstractWalletActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inheritance_heir);
    }
}
