//package de.schildbach.wallet.ui;
//
//import android.app.Application;
//import android.graphics.Bitmap;
//import android.net.Uri;
//import android.os.AsyncTask;
//
//import androidx.annotation.NonNull;
//import androidx.lifecycle.AndroidViewModel;
//import androidx.lifecycle.MediatorLiveData;
//import androidx.lifecycle.MutableLiveData;
//
//import org.bitcoinj.core.Address;
//import org.bitcoinj.core.Coin;
//
//import de.schildbach.wallet.WalletApplication;
//import de.schildbach.wallet.data.ConfigOwnNameLiveData;
//import de.schildbach.wallet.data.SelectedExchangeRateLiveData;
//import de.schildbach.wallet.util.Qr;
//
//public class InheritanceOwnerViewModel extends AndroidViewModel {
//    private final WalletApplication application;
//    public final RequestCoinsViewModel.FreshReceiveAddressLiveData freshReceiveAddress;
//    private final ConfigOwnNameLiveData ownName;
//    public final MutableLiveData<String> bluetoothMac = new MutableLiveData<>();
//    public final MediatorLiveData<Bitmap> qrCode = new MediatorLiveData<>();
//    public final MutableLiveData<Event<Bitmap>> showBitmapDialog = new MutableLiveData<>();
//
//
//    public InheritanceOwnerViewModel(@NonNull Application application) {
//        super(application);
//
//        this.application = (WalletApplication) application;
//        this.freshReceiveAddress = new RequestCoinsViewModel.FreshReceiveAddressLiveData(this.application);
//
//        //??? What is it??
//        this.ownName = new ConfigOwnNameLiveData(this.application);
//
//        this.qrCode.addSource(freshReceiveAddress, receiveAddress -> maybeGenerateQrCode());
//        this.qrCode.addSource(ownName, label -> maybeGenerateQrCode());
//        this.qrCode.addSource(amount, amount -> maybeGenerateQrCode());
//        this.qrCode.addSource(bluetoothMac, bluetoothMac -> maybeGenerateQrCode());
//
//    }
//
//    private void maybeGenerateQrCode() {
//        final Address address = freshReceiveAddress.getValue();
//        if (address != null) {
//            AsyncTask.execute(() -> qrCode.postValue(
//                    Qr.bitmap(uri(address, amount.getValue(), ownName.getValue(), bluetoothMac.getValue()))));
//        }
//    }
//}
