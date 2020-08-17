package de.schildbach.wallet.ui.inheritance;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.util.encoders.Hex;

import java.util.Collections;
import java.util.List;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.R;
import de.schildbach.wallet.WalletApplication;
import de.schildbach.wallet.data.AppDatabase;
import de.schildbach.wallet.data.InheritanceDao;
import de.schildbach.wallet.data.InheritanceEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.ui.scan.ScanActivity;
import de.schildbach.wallet.util.CheatSheet;
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

        this.inheritanceDao = AppDatabase.getDatabase(this.getBaseContext()).inheritanceDao();


        contentView = findViewById(android.R.id.content);
        enterAnimation = buildEnterAnimation(contentView);
        levitateView = contentView.findViewWithTag("levitate");


        final View sendQrButton = findViewById(R.id.wallet_actions_send_qr);
        sendQrButton.setOnClickListener(v -> handleScan(v));
        CheatSheet.setup(sendQrButton);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
            sendQrButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.fg_on_dark_bg_network_significant), PorterDuff.Mode.SRC_ATOP);


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

                final Intent intent = new Intent(InheritanceOwnerActivity.this, InheritanceQRActivity.class);
                intent.putExtra("address", address);
                startActivity(intent);

            }
        });

        final Button saveBtn = findViewById(R.id.addHeirAddreBtn);
        saveBtn.setOnClickListener(onSaveClick());

    }

    private View.OnClickListener onSaveClick() {
        return v -> {

            final EditText labelField = findViewById(R.id.heir_label);

            String  name=labelField.getText().toString();

            if("".equalsIgnoreCase(name)){
                labelField.setHint("please enter hair name");
                labelField.setError("please enter hair name");
                return;
            }

            final EditText addEditText = findViewById(R.id.heir_address);
            String address = addEditText.getText().toString();

            if("".equalsIgnoreCase(name)){
                labelField.setHint("please enter hair address");
                labelField.setError("please enter hair address");
                return;
            }

            InheritanceEntity in = new InheritanceEntity(addEditText.getText().toString(), name);
            inheritanceDao.insertOrUpdate(in);

            Toast.makeText(this, "Hair address saved", Toast.LENGTH_LONG);


            list.clear();
            list.addAll(inheritanceDao.getAll());

            adapter.notifyDataSetChanged();
        };
    }


    public void handleScan(final View clickView) {
        // The animation must be ended because of several graphical glitching that happens when the
        // Camera/SurfaceView is used while the animation is running.
        enterAnimation.end();
        ScanActivity.startForResult(this, clickView, 0);
    }

    private AnimatorSet buildEnterAnimation(final View contentView) {
        final Drawable background = getWindow().getDecorView().getBackground();
        final int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        final Animator splashBackgroundFadeOut = AnimatorInflater.loadAnimator(this, R.animator.fade_out_drawable);
        final Animator splashForegroundFadeOut = AnimatorInflater.loadAnimator(this, R.animator.fade_out_drawable);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //TODO: class cust exc
//            splashBackgroundFadeOut.setTarget(((LayerDrawable) background).getDrawable(1));
//            splashForegroundFadeOut.setTarget(((LayerDrawable) background).getDrawable(2));
        } else {
            // skip this animation, as there is no splash icon
            splashBackgroundFadeOut.setDuration(0);
            splashForegroundFadeOut.setDuration(0);
        }
        final AnimatorSet fragmentEnterAnimation = new AnimatorSet();
        final AnimatorSet.Builder fragmentEnterAnimationBuilder =
                fragmentEnterAnimation.play(splashBackgroundFadeOut).with(splashForegroundFadeOut);

        final View slideInLeftView = contentView.findViewWithTag("slide_in_left");
        if (slideInLeftView != null) {
            final ValueAnimator slide = ValueAnimator.ofFloat(-1.0f, 0.0f);
            slide.addUpdateListener(animator -> {
                float animatedValue = (float) animator.getAnimatedValue();
                slideInLeftView.setTranslationX(
                        animatedValue * (slideInLeftView.getWidth() + slideInLeftView.getPaddingLeft()));
            });
            slide.setInterpolator(new DecelerateInterpolator());
            slide.setDuration(duration);
            slide.setTarget(slideInLeftView);
            final Animator fadeIn = AnimatorInflater.loadAnimator(this, R.animator.fade_in_view);
            fadeIn.setTarget(slideInLeftView);
            fragmentEnterAnimationBuilder.before(slide).before(fadeIn);
        }

        final View slideInRightView = contentView.findViewWithTag("slide_in_right");
        if (slideInRightView != null) {
            final ValueAnimator slide = ValueAnimator.ofFloat(1.0f, 0.0f);
            slide.addUpdateListener(animator -> {
                float animatedValue = (float) animator.getAnimatedValue();
                slideInRightView.setTranslationX(
                        animatedValue * (slideInRightView.getWidth() + slideInRightView.getPaddingRight()));
            });
            slide.setInterpolator(new DecelerateInterpolator());
            slide.setDuration(duration);
            slide.setTarget(slideInRightView);
            final Animator fadeIn = AnimatorInflater.loadAnimator(this, R.animator.fade_in_view);
            fadeIn.setTarget(slideInRightView);
            fragmentEnterAnimationBuilder.before(slide).before(fadeIn);
        }

        final View slideInTopView = contentView.findViewWithTag("slide_in_top");
        if (slideInTopView != null) {
            final ValueAnimator slide = ValueAnimator.ofFloat(-1.0f, 0.0f);
            slide.addUpdateListener(animator -> {
                float animatedValue = (float) animator.getAnimatedValue();
                slideInTopView.setTranslationY(
                        animatedValue * (slideInTopView.getHeight() + slideInTopView.getPaddingTop()));
            });
            slide.setInterpolator(new DecelerateInterpolator());
            slide.setDuration(duration);
            slide.setTarget(slideInTopView);
            final Animator fadeIn = AnimatorInflater.loadAnimator(this, R.animator.fade_in_view);
            fadeIn.setTarget(slideInTopView);
            fragmentEnterAnimationBuilder.before(slide).before(fadeIn);
        }

        final View slideInBottomView = contentView.findViewWithTag("slide_in_bottom");
        if (slideInBottomView != null) {
            final ValueAnimator slide = ValueAnimator.ofFloat(1.0f, 0.0f);
            slide.addUpdateListener(animator -> {
                float animatedValue = (float) animator.getAnimatedValue();
                slideInBottomView.setTranslationY(
                        animatedValue * (slideInBottomView.getHeight() + slideInBottomView.getPaddingBottom()));
            });
            slide.setInterpolator(new DecelerateInterpolator());
            slide.setDuration(duration);
            slide.setTarget(slideInBottomView);
            final Animator fadeIn = AnimatorInflater.loadAnimator(this, R.animator.fade_in_view);
            fadeIn.setTarget(slideInBottomView);
            fragmentEnterAnimationBuilder.before(slide).before(fadeIn);
        }

        if (levitateView != null) {
            final ObjectAnimator elevate = ObjectAnimator.ofFloat(levitateView, "elevation", 0.0f,
                    levitateView.getElevation());
            elevate.setDuration(duration);
            fragmentEnterAnimationBuilder.before(elevate);
            final Drawable levitateBackground = levitateView.getBackground();
            final Animator fadeIn = AnimatorInflater.loadAnimator(this, R.animator.fade_in_drawable);
            fadeIn.setTarget(levitateBackground);
            fragmentEnterAnimationBuilder.before(fadeIn);
        }

        return fragmentEnterAnimation;
    }


    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {

                //TODO: btc address
                final String input = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);

                final EditText addEditText = findViewById(R.id.heir_address);
                addEditText.setText(input);

//                final Intent newIntent = new Intent(InheritanceOwnerActivity.this, InheritanceOwnerNewHair.class);
//                newIntent.putExtra("address", input);
//                startActivityForResult(newIntent, 0);

            }

//            if(resultCode == 100){
//                list.clear();
//                list.addAll(inheritanceDao.getAll());
//
//                adapter.notifyDataSetChanged();
//            }
        }


        super.onActivityResult(requestCode, resultCode, intent);

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
}
