package de.schildbach.wallet.ui.inheritance;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.util.encoders.Hex;

import java.util.Collections;
import java.util.List;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.R;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.addressbook.AddressBookDatabase;;
import de.schildbach.wallet.data.InheritanceDao;
import de.schildbach.wallet.data.InheritanceEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.ui.inheritance.owner.InheritanceOwnerDetailsActivity;
import de.schildbach.wallet.ui.inheritance.owner.NewOwnerTx;
import de.schildbach.wallet.util.Inheritance;

public final class InheritanceOwnerActivity extends AbstractWalletActivity {

    private AnimatorSet enterAnimation;
    private View contentView;
    private View levitateView;

    private ArrayAdapter adapter;
    private String signedTx;

    private InheritanceDao inheritanceDao;
    private List<InheritanceEntity> list = Collections.emptyList();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inheritance);

        this.inheritanceDao = AddressBookDatabase.getDatabase(this.getBaseContext()).inheritanceDao();

        final ListView listview = (ListView) findViewById(R.id.list_heir);

        list = inheritanceDao.getAll();

        adapter = new AddressViewAdapter(this,
                R.layout.layout_owner_address_view, list);
        listview.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String address = list.get(i).getAddress();

                final Intent intent = new Intent(InheritanceOwnerActivity.this, InheritanceOwnerDetailsActivity.class);
                intent.putExtra("address", address);
                startActivity(intent);

            }
        });

    }
    @Override
    protected void onResume() {
        super.onResume();

        list.clear();
        list.addAll(inheritanceDao.getAll());

        adapter.notifyDataSetChanged();
    }




    /**
     * Sign tx for hair
     *
     * @param heirAddr
     * @return signed tx
     */
    private String signInheritanceTx(String heirAddr) {

        //TODO: addr validation
        if (heirAddr == null) {
            return "";
        }

        WalletApplication application = getWalletApplication();
        Wallet wallet = application.getWallet();

        Address ownerAddress = wallet.currentReceiveAddress();


        try {
            Address heirAddress = Address.fromString(Constants.NETWORK_PARAMETERS, heirAddr);
            Transaction tx = Inheritance.signInheritanceTx(ownerAddress, heirAddress, 6, wallet);
            signedTx = Hex.toHexString(tx.bitcoinSerialize());
            Toast.makeText(this, "Successfully signed inheritance transaction: " + signedTx.substring(0, 50) + "...", Toast.LENGTH_LONG).show();

            return signedTx;

        } catch (Exception exception) {
            Toast.makeText(this, "Failed to sign inheritance transaction: Some exception, see debug log... ", Toast.LENGTH_LONG).show();
        }

        Toast.makeText(this, "Failed to sign inheritance transaction: Heir address is missing", Toast.LENGTH_LONG).show();

        return "";

        //todo make this part of code not to crash application and show QR of inheritnace transaction
//        final BitmapDrawable bitmap = new BitmapDrawable(getResources(), Qr.bitmap(this.signedTx));
//        bitmap.setFilterBitmap(false);
//        final ImageView imageView = findViewById(R.id.bitcoin_address_qr);
//        imageView.setImageDrawable(bitmap);
    }

    private class AddressViewAdapter extends ArrayAdapter<InheritanceEntity> {

        private Context context;
        private int resource;

        public AddressViewAdapter(Context context, int resource, List<InheritanceEntity> list) {
            super(context, resource, list);
            this.context = context;
            this.resource = resource;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            InheritanceEntity item = getItem(position);

            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(resource, parent, false);

            TextView vDesc = convertView.findViewById(R.id.description);
            TextView vAddr = convertView.findViewById(R.id.address);

            vDesc.setText(item.getLabel());

            vAddr.setText(item.getAddress());

            return convertView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.hair_list_options, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.add) {

            startActivity(new Intent(InheritanceOwnerActivity.this,
                    NewOwnerTx.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
