package com.juzix.wallet.component.ui.presenter;


import android.os.Handler;
import android.os.Message;

import com.juzix.wallet.R;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.ImportIndividualMnemonicPhraseContract;
import com.juzix.wallet.component.ui.view.MainActivity;
import com.juzix.wallet.engine.IndividualWalletManager;
import com.juzix.wallet.entity.IndividualWalletEntity;

public class ImportIndividualMnemonicPhrasePresenter extends BasePresenter<ImportIndividualMnemonicPhraseContract.View> implements ImportIndividualMnemonicPhraseContract.Presenter {

    public ImportIndividualMnemonicPhrasePresenter(ImportIndividualMnemonicPhraseContract.View view) {
        super(view);
    }

    @Override
    public void init() {
        ImportIndividualMnemonicPhraseContract.View view = getView();
        if (view != null){
            view.showQRCode(view.getKeystoreFromIntent());
        }
    }

    @Override
    public void parseQRCode(String QRCode) {
        getView().showQRCode(QRCode);
    }

    @Override
    public void importMnemonic(String phrase, String name, String password, String repeatPassword) {
//        if (!AppUtil.validWalletPwd(password)) {
//            showShortToast(string(R.string.validTips, "6", "32"));
//            return;
//        }
        if (!password.equals(repeatPassword)) {
            showShortToast(string(R.string.passwordTips));
            return;
        }

        showLoadingDialog();
        new Thread(){
            @Override
            public void run() {
                IndividualWalletEntity walletEntity = new IndividualWalletEntity.Builder().build();
                int                    code         = IndividualWalletManager.getInstance().importMnemonic(walletEntity, phrase, name, password);
                switch (code) {
                    case IndividualWalletManager.CODE_OK:
                        mHandler.sendEmptyMessage(MSG_OK);
                        break;
                    case IndividualWalletManager.CODE_ERROR_MNEMONIC:
                        mHandler.sendEmptyMessage(MSG_MNEMONIC_ERROR);
                        break;
                    case IndividualWalletManager.CODE_ERROR_NAME:
                        break;
                    case IndividualWalletManager.CODE_ERROR_PASSWORD:
                        mHandler.sendEmptyMessage(MSG_PASSWORD_FAILED);
                        break;
                    case IndividualWalletManager.CODE_ERROR_WALLET_EXISTS:
                        mHandler.sendEmptyMessage(MSG_WALLET_EXISTS);
                        break;
                    case IndividualWalletManager.CODE_ERROR_UNKNOW:
                        mHandler.sendEmptyMessage(MSG_PASSWORD_FAILED);
                        break;
                }
            }
        }.start();
    }

    private static final int MSG_OK = 1;
    private static final int MSG_PASSWORD_FAILED = -1;
    private static final int MSG_MNEMONIC_ERROR = -2;
    private static final int MSG_WALLET_EXISTS = -3;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_OK:
                    dismissLoadingDialogImmediately();
                    MainActivity.actionStart(currentActivity());
                    currentActivity().finish();
                    break;
                case MSG_PASSWORD_FAILED:
                    dismissLoadingDialogImmediately();
                    showLongToast(string(R.string.validPasswordError));
                    break;
                case MSG_MNEMONIC_ERROR:
                    dismissLoadingDialogImmediately();
                    showLongToast(string(R.string.parsedError, string(R.string.mnemonicPhrase)));
                    break;
                case MSG_WALLET_EXISTS:
                    dismissLoadingDialogImmediately();
                    showLongToast(string(R.string.walletExists));
                    break;
            }
        }
    };
}
