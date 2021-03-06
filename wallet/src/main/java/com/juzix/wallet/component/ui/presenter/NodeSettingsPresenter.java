package com.juzix.wallet.component.ui.presenter;

import android.text.TextUtils;
import android.util.Log;

import com.juzix.wallet.R;
import com.juzix.wallet.app.SchedulersTransformer;
import com.juzix.wallet.component.ui.base.BaseActivity;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.NodeSettingsContract;
import com.juzix.wallet.engine.NodeManager;
import com.juzix.wallet.entity.NodeEntity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * @author matrixelement
 */
public class NodeSettingsPresenter extends BasePresenter<NodeSettingsContract.View> implements NodeSettingsContract.Presenter {

    private final static String TAG = NodeSettingsPresenter.class.getSimpleName();
    private final static String IP_WITH_HTTP_PREFIX = "^(http(s?)://)?((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)):\\d{4})";
    private final static String IP_WITHOUT_HTTP_PREFIX = "((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)):\\d{4})";

    private boolean mEdited;

    public NodeSettingsPresenter(NodeSettingsContract.View view) {
        super(view);
    }

    @Override
    public void edit() {
        if (isViewAttached()) {
            mEdited = !mEdited;
            if (mEdited) {
                getView().showTitleView(mEdited);
            } else {
                save();
            }
        }
    }

    @Override
    public void cancel() {
        mEdited = false;
        if (isViewAttached()) {
            List<NodeEntity> nodeList = getView().getNodeList();
            List<NodeEntity> removeNodeList = new ArrayList<>();
            if (nodeList != null && !nodeList.isEmpty()) {
                for (int i = 0; i < nodeList.size(); i++) {
                    NodeEntity node = nodeList.get(i);
                    if (node.isDefaultNode() || !TextUtils.isEmpty(node.getNodeAddress())) {
                        continue;
                    }

                    String nodeAddress = getView().getNodeAddress(node.getId());
                    if (TextUtils.isEmpty(nodeAddress) || !nodeAddress.trim().matches(IP_WITH_HTTP_PREFIX)) {
                        removeNodeList.add(node);
                    }

                }
            }
            if (!removeNodeList.isEmpty()) {
                getView().removeNodeList(removeNodeList);
            }
            getView().showTitleView(mEdited);
        }
    }

    @Override
    public void fetchNodes() {

        NodeManager.getInstance()
                .getNodeList()
                .compose(((BaseActivity) getView()).bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<NodeEntity>>() {
                    @Override
                    public void accept(List<NodeEntity> nodeEntityList) throws Exception {
                        if (isViewAttached()) {
                            getView().notifyDataChanged(nodeEntityList);
                        }
                    }
                });

    }

    @Override
    public void save() {
        if (isViewAttached()) {

            ((BaseActivity) getView()).hideSoftInput();

            List<NodeEntity> nodeList = getView().getNodeList();
            List<NodeEntity> removeNodeList = new ArrayList<>();
            List<NodeEntity> errorNodeList = new ArrayList<>();
            List<NodeEntity> normalNodeList = new ArrayList<>();
            if (nodeList != null && !nodeList.isEmpty()) {
                for (int i = 0; i < nodeList.size(); i++) {
                    NodeEntity node = nodeList.get(i);
                    if (node.isDefaultNode()) {
                        continue;
                    }

                    String nodeAddress = getView().getNodeAddress(node.getId());

                    if (TextUtils.isEmpty(nodeAddress)) {
                        removeNodeList.add(node);
                    } else {
                        NodeEntity nodeEntity = node.clone();
                        String address = nodeAddress.trim();
                        if (address.matches(IP_WITH_HTTP_PREFIX) || address.matches(IP_WITHOUT_HTTP_PREFIX)) {
                            if (address.matches(IP_WITHOUT_HTTP_PREFIX)) {
                                nodeEntity.setNodeAddress("http://".concat(address));
                            } else {
                                nodeEntity.setNodeAddress(address);
                            }
                            normalNodeList.add(nodeEntity);
                        } else {
                            nodeEntity.setFormatCorrect(true);
                            errorNodeList.add(nodeEntity);
                        }
                    }
                }
            }

            if (errorNodeList.isEmpty()) {
                if (!removeNodeList.isEmpty()) {
                    getView().removeNodeList(removeNodeList);
                }
                if (!normalNodeList.isEmpty()) {
                    insertNodeList(normalNodeList);
                    getView().updateNodeList(normalNodeList);
                }
                getView().showTitleView(mEdited);
            } else {
                showLongToast(R.string.node_format_error);
                getView().updateNodeList(errorNodeList);
            }

        }
    }

    @Override
    public void delete(NodeEntity nodeEntity) {
        NodeManager.getInstance()
                .deleteNode(nodeEntity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (aBoolean && nodeEntity.isChecked() && isViewAttached()) {
                    getView().setChecked(0);
                }
            }
        });
    }

    @Override
    public void updateNode(NodeEntity nodeEntity, boolean isChecked) {
        NodeManager.getInstance().updateNode(nodeEntity, isChecked);
    }

    private void insertNodeList(List<NodeEntity> nodeEntityList) {

        NodeManager.getInstance()
                .insertNodeList(nodeEntityList)
                .compose(new SchedulersTransformer())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean.booleanValue()) {
                            showLongToast(string(R.string.save_node_succeed));
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getMessage());
                    }
                });
    }
}
