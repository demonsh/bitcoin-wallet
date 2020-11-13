package de.schildbach.wallet.ui.inheritance;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import de.schildbach.wallet.R;
import de.schildbach.wallet.data.AppDatabase;
import de.schildbach.wallet.data.InheritanceTxDao;
import de.schildbach.wallet.data.TxEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.ui.inheritance.heir.NewHairTx;

/**
 * InheritanceHeirActivity.class
 *
 * Functions
 * - Scan QR code with tx
 * - Withdraw tx
 * - Check tx status(if it is not spend)
 */
public class InheritanceHeirActivity extends AbstractWalletActivity {

    private ArrayAdapter adapter;

    private InheritanceTxDao txDao;
    private List<TxEntity> txList = Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inheritance_heir);

        txDao = AppDatabase.getDatabase(this.getBaseContext()).txDao();

        txList = txDao.getAll();

        adapter = new TxViewAdapter(this,
                R.layout.layout_owner_address_view, txList);

        final ListView listview = findViewById(R.id.list_inheritance);

        listview.setAdapter(adapter);
        listview.setOnItemClickListener(onTxItemClick());

    }

    private AdapterView.OnItemClickListener onTxItemClick() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                final Intent intent = new Intent(InheritanceHeirActivity.this, InheritanceHeirDetailActivity.class);
                intent.putExtra("txEntity", txList.get(i));
                startActivity(intent);

            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.hair_list_options, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.add) {

            startActivity(new Intent(InheritanceHeirActivity.this,
                                                   NewHairTx.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        txList.clear();
        txList.addAll(txDao.getAll());

        adapter.notifyDataSetChanged();
    }

    private class TxViewAdapter extends ArrayAdapter<TxEntity> {

        private Context context;
        private int resource;

        public TxViewAdapter(Context context, int resource, List<TxEntity> list) {
            super(context, resource, list);
            this.context = context;
            this.resource = resource;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TxEntity item = getItem(position);

            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(resource, parent, false);

            TextView vDesc = convertView.findViewById(R.id.description);
            TextView vAddr = convertView.findViewById(R.id.address);

            vDesc.setText(item.getLabel());

            vAddr.setText(item.getTx());

            return convertView;
        }
    }
}
