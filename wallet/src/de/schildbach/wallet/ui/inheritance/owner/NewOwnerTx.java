package de.schildbach.wallet.ui.inheritance.owner;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import de.schildbach.wallet.R;
import de.schildbach.wallet.data.AppDatabase;
import de.schildbach.wallet.data.InheritanceDao;
import de.schildbach.wallet.data.InheritanceEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;

public class NewOwnerTx extends AbstractWalletActivity {

    private InheritanceDao inheritanceDao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_owner_tx);

        this.inheritanceDao = AppDatabase.getDatabase(this.getBaseContext()).inheritanceDao();

    }

    private void save() {

        final EditText labelField = findViewById(R.id.heir_label);

        String  name=labelField.getText().toString();

        if("".equalsIgnoreCase(name)){
            labelField.setHint("please enter hair name");
            labelField.setError("please enter hair name");
            return;
        }

        final EditText addEditText = findViewById(R.id.heirAddress);
        String address = addEditText.getText().toString();

        if("".equalsIgnoreCase(address)){
            labelField.setHint("please enter hair address");
            labelField.setError("please enter hair address");
            return;
        }

        InheritanceEntity in = new InheritanceEntity(address, name);
        inheritanceDao.insertOrUpdate(in);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_hair_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.save) {

            try {

                save();

                finish();

            } catch (Exception exc) {
                log.error(exc.toString());
                Toast.makeText(this, exc.getMessage(), Toast.LENGTH_LONG);

                return false;
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
