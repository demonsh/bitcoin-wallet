/*
 * Copyright the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.ui.inheritance;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.bitcoinj.core.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.schildbach.wallet.R;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.ui.Event;
import de.schildbach.wallet.ui.WalletActivityViewModel;
import de.schildbach.wallet.ui.WalletAddressDialogFragment;
import de.schildbach.wallet.ui.WalletAddressViewModel;

/**
 * @author Andreas Schildbach
 */
public final class InheritanceAddressFragment extends Fragment {
    private AbstractWalletActivity activity;
    @Nullable
    private NfcAdapter nfcAdapter;

    private ImageView currentAddressQrView;
    private CardView currentAddressQrCardView;

    private WalletActivityViewModel activityViewModel;
    private WalletAddressViewModel viewModel;

    private static final Logger log = LoggerFactory.getLogger(InheritanceAddressFragment.class);

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        this.activity = (AbstractWalletActivity) context;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityViewModel = new ViewModelProvider(activity).get(WalletActivityViewModel.class);
        viewModel = new ViewModelProvider(this).get(WalletAddressViewModel.class);

        viewModel.qrCode.observe(this, qrCode -> {
            final BitmapDrawable qrDrawable = new BitmapDrawable(getResources(), qrCode);
            qrDrawable.setFilterBitmap(false);
            currentAddressQrView.setImageDrawable(qrDrawable);
            currentAddressQrCardView.setOnClickListener(v -> viewModel.showWalletAddressDialog.setValue(Event.simple()));
        });
        viewModel.bitcoinUri.observe(this, bitcoinUri -> {
            final NfcAdapter nfcAdapter = InheritanceAddressFragment.this.nfcAdapter;
            if (nfcAdapter != null)
                nfcAdapter.setNdefPushMessage(createNdefMessage(bitcoinUri.toString()), activity);
            activityViewModel.addressLoadingFinished();
        });
        viewModel.showWalletAddressDialog.observe(this, new Event.Observer<Void>() {
            @Override
            public void onEvent(final Void v) {
                final Address address = viewModel.currentAddress.getValue();
                WalletAddressDialogFragment.show(getParentFragmentManager(), address, viewModel.ownName.getValue());
                log.info("Current address enlarged: {}", address);
            }
        });
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.wallet_address_fragment, container, false);
        currentAddressQrView = view.findViewById(R.id.bitcoin_address_qr);

        currentAddressQrCardView = view.findViewById(R.id.bitcoin_address_qr_card);
        currentAddressQrCardView.setPreventCornerOverlap(false);
        currentAddressQrCardView.setUseCompatPadding(false);
        currentAddressQrCardView.setMaxCardElevation(0); // we're using Lollipop elevation

        return view;
    }

    private static NdefMessage createNdefMessage(final String uri) {
        if (uri != null)
            return new NdefMessage(new NdefRecord[] { NdefRecord.createUri(uri) });
        else
            return null;
    }
}
