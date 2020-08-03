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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import de.schildbach.wallet.R;
import de.schildbach.wallet.data.AppDatabase;
import de.schildbach.wallet.data.InheritanceEntity;
import de.schildbach.wallet.data.InheritanceTxDao;
import de.schildbach.wallet.data.TxEntity;
import de.schildbach.wallet.ui.AbstractWalletActivity;
import de.schildbach.wallet.ui.scan.ScanActivity;
import de.schildbach.wallet.util.CheatSheet;

/**
 * InheritanceHeirActivity.class
 *
 * Functions
 * - Scan QR code with tx
 * - Withdraw tx
 * - Check tx status(if it is not spend)
 */
public class InheritanceHeirActivity extends AbstractWalletActivity {

    private AnimatorSet enterAnimation;
    private View contentView;
    private View levitateView;

    private ArrayAdapter adapter;

    private InheritanceTxDao txDao;
    private List<TxEntity> txList = Collections.emptyList();

    private EditText ownerAddressView;
    private EditText txView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inheritance_heir);

        this.txDao = AppDatabase.getDatabase(this.getBaseContext()).txDao();

        contentView = findViewById(android.R.id.content);
        enterAnimation = buildEnterAnimation(contentView);

        levitateView = contentView.findViewWithTag("levitate");

        //Scan QR button. Scan Tx
        final View sendQrButton = findViewById(R.id.wallet_actions_send_qr);
        sendQrButton.setOnClickListener(v -> handleScan(v));
        CheatSheet.setup(sendQrButton);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
            sendQrButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.fg_on_dark_bg_network_significant), PorterDuff.Mode.SRC_ATOP);


        //List of inherited tx
        final ListView listview = (ListView) findViewById(R.id.list_inheritance);

        txList = txDao.getAll();

        adapter = new TxViewAdapter(this,
                R.layout.layout_owner_address_view, txList);
        listview.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {


                final Intent intent = new Intent(InheritanceHeirActivity.this,InheritanceHeirDetailActivity.class);
                intent.putExtra("tx", txList.get(i).getTx());
                startActivity(intent);

            }
        });


        ownerAddressView = findViewById(R.id.ownerAddress);
        txView = findViewById(R.id.tx);

        final Button addButton = findViewById(R.id.addTx);
        addButton.setOnClickListener(onAddTxButtonListener());

    }

    private View.OnClickListener onAddTxButtonListener() {
        return v->{
            try {

                String ownerAddress = ownerAddressView.getText().toString();
                String tx = txView.getText().toString();

                TxEntity in = new TxEntity(tx,ownerAddress, "heirAddress");
                txDao.insertOrUpdate(in);

            } catch (Exception exc) {
                log.error(exc.toString());
                Toast.makeText(InheritanceHeirActivity.this, exc.getMessage(), Toast.LENGTH_LONG);
            }

        };
    }

    public void handleScan(final View clickView) {
        // The animation must be ended because of several graphical glitching that happens when the
        // Camera/SurfaceView is used while the animation is running.
        enterAnimation.end();
        ScanActivity.startForResult(this, clickView, 0);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {

                //TODO: btc address
                 String input = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);

                try {
                    input = decompress(input);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (DataFormatException e) {
                    e.printStackTrace();
                }

                adapter.add(input);
                adapter.notifyDataSetChanged();

                saveTx(input);

                log.debug(input);

            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private String decompress(String str) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();

        inflater.setInput(str.getBytes("ISO-8859-1"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(str.getBytes().length);

        byte[] buffer = new byte[1024];

        while (!inflater.finished()) {

            int count = inflater.inflate(buffer);

            outputStream.write(buffer, 0, count);

        }

        outputStream.close();

//        byte[] output = outputStream.toByteArray();

       return outputStream.toString();
    }

    private void saveTx(String input) {

//            try {
//
//                TxEntity in = new TxEntity(input, "heirAddress");
//                txDao.insertOrUpdate(in);
//
//            } catch (Exception exc) {
//                log.error(exc.toString());
//                Toast.makeText(InheritanceHeirActivity.this, exc.getMessage(), Toast.LENGTH_LONG);
//            }

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
