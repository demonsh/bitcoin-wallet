package de.schildbach.wallet.ui;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.PrefixedChecksummedBytes;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.uri.BitcoinURI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.schildbach.wallet.Constants;
import de.schildbach.wallet.R;
import de.schildbach.wallet.data.PaymentIntent;
import de.schildbach.wallet.ui.scan.ScanActivity;
import de.schildbach.wallet.ui.send.SendCoinsActivity;
import de.schildbach.wallet.ui.send.SweepWalletActivity;
import de.schildbach.wallet.util.CheatSheet;
import de.schildbach.wallet.util.Qr;

public final class InheritanceActivity extends AbstractWalletActivity {

    private AnimatorSet enterAnimation;
    private View contentView;
    private View levitateView;

    private ArrayList<String> list = new ArrayList<String>();
    private StableArrayAdapter adapter;
//    private RecyclerView recyclerViewHeir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inheritance);

        final Address address = Address.fromString(Constants.NETWORK_PARAMETERS, "tb1qj6jh32uhuy6jn8muryl77pysqscy7cr86m5vxv");
        final String addressStr = address.toString();

        contentView = findViewById(android.R.id.content);
        enterAnimation = buildEnterAnimation(contentView);

        levitateView = contentView.findViewWithTag("levitate");

//        final Dialog dialog = new Dialog(activity);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        dialog.setContentView(R.layout.wallet_address_dialog);
//        dialog.setCanceledOnTouchOutside(true);

        final String addressUri;
        //if (address instanceof LegacyAddress || addressLabel != null)
        if (address instanceof LegacyAddress)
            addressUri = BitcoinURI.convertToBitcoinURI(address, null, addressStr, null);
        else
            addressUri = address.toString().toUpperCase(Locale.US);

        final BitmapDrawable bitmap = new BitmapDrawable(getResources(), Qr.bitmap(addressUri));
        bitmap.setFilterBitmap(false);
        final ImageView imageView = findViewById(R.id.bitcoin_address_qr);
        imageView.setImageDrawable(bitmap);

//        imageView.setOnClickListener(v ->
//                WalletAddressDialogFragment.show(getParentFragmentManager(), address, viewModel.ownName.getValue()););
//
//        setOnClickListener(v -> viewModel.showWalletAddressDialog.setValue(Event.simple()));

        final View sendQrButton = findViewById(R.id.wallet_actions_send_qr);
        sendQrButton.setOnClickListener(v -> handleScan(v));
        CheatSheet.setup(sendQrButton);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
            sendQrButton.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.fg_on_dark_bg_network_significant), PorterDuff.Mode.SRC_ATOP);


        //Recycle view

//        recyclerViewHeir = findViewById(R.id.block_list);
//        recyclerViewHeir.setLayoutManager(new StickToTopLinearLayoutManager(this));
//        recyclerViewHeir.setAdapter(adapter);
//        recyclerViewHeir.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        final ListView listview = (ListView) findViewById(R.id.list_heir);


        //TODO: this is tmp imp;
        adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, list);
        listview.setAdapter(adapter);
        adapter.notifyDataSetChanged();


    }

    //TODO: this is tmp impl
    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

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
                final String input = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);

                adapter.add(input);
                adapter.notifyDataSetChanged();

//                new InputParser.StringInputParser(input) {
//                    @Override
//                    protected void handlePaymentIntent(final PaymentIntent paymentIntent) {
//                        SendCoinsActivity.start(WalletActivity.this, paymentIntent);
//                    }
//
//                    @Override
//                    protected void handlePrivateKey(final PrefixedChecksummedBytes key) {
//                        if (Constants.ENABLE_SWEEP_WALLET)
//                            SweepWalletActivity.start(WalletActivity.this, key);
//                        else
//                            super.handlePrivateKey(key);
//                    }
//
//                    @Override
//                    protected void handleDirectTransaction(final Transaction tx) throws VerificationException {
//                        application.processDirectTransaction(tx);
//                    }
//
//                    @Override
//                    protected void error(final int messageResId, final Object... messageArgs) {
//                        dialog(WalletActivity.this, null, R.string.button_scan, messageResId, messageArgs);
//                    }
//                }.parse();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

}
