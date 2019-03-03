/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.dialog;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.components.NetworkModel;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.util.TextWatcherAdapter;
import com.google.common.base.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.kryl.android.appcompat.dialogs.AlertDialog;
import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubHandler;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.daApp;
import static com.decawave.argomanager.ioc.IocContext.daCtx;

/** Dialog to pick a item */
public class NetworkPickerDialogFragment extends DialogFragment {

    public static final ComponentLog log = new ComponentLog(NetworkPickerDialogFragment.class).disable();

    private static final String FRAGMENT_TAG = "networkpicker";

    private static final String BK_SELECTED_NETWORK_ID = "selectedNetworkId";
    private static final String BK_NEW_NETWORK_NAME = "newNetworkName";

    // null-value pattern
    public static final int NEW_NETWORK_ID = Integer.MAX_VALUE;

    // do not take the zero for real
    public static final NetworkModel NEW_NETWORK = new NetworkModel((short) 0);

    @Inject
    NetworkNodeManager networkNodeManager;

    // ***************************
    // * INPUT
    // ***************************

    @SuppressWarnings("NullableProblems")
    @NotNull
    private NetworkModel[] networks;

    @Nullable
    // we have integer here because we want to represent new network with a special out-of-range value
    private Integer selectedNetworkId;
    private String newNetworkName;

    // ***************************
    // * OTHER
    // ***************************

    private Button okBtn;
    private Adapter adapter;

    // ***************************
    // * CONSTRUCTOR
    // ***************************

    public NetworkPickerDialogFragment() {
        daCtx.inject(this);
        Map<Short, NetworkModel> _n = networkNodeManager.getNetworks();
        int length = _n.size() + 1;
        networks = _n.values().toArray(new NetworkModel[length]);
        // initialize the last element in the array
        networks[length - 1] = NEW_NETWORK;
    }

    /**
     * use this if you want to preselect specific network item
     */
    private static Bundle getArgsForNetwork(short networkId) {
        Bundle b = new Bundle();
        b.putInt(BK_SELECTED_NETWORK_ID, networkId);
        return b;
    }

    /**
     * use this method if you want to preselect new network... item
     */
    private static Bundle getArgsForNewNetwork(String newNetworkName) {
        Bundle b = new Bundle();
        b.putString(BK_NEW_NETWORK_NAME, newNetworkName);
        return b;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (selectedNetworkId != null) {
            bundle.putInt(BK_SELECTED_NETWORK_ID, selectedNetworkId);
        }
        if (newNetworkName != null) {
            bundle.putString(BK_NEW_NETWORK_NAME, newNetworkName);
        }
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        Bundle b = bundle;
        if (b == null) {
            b = getArguments();
        }
        if (b != null) {
            if (b.containsKey(BK_SELECTED_NETWORK_ID)) {
                setSelectedNetworkId(b.getInt(BK_SELECTED_NETWORK_ID));
            }
            newNetworkName = b.getString(BK_NEW_NETWORK_NAME);
            if (newNetworkName != null && bundle != b) {
                // we have used arguments - new network name was injected, this means it is selected
                setSelectedNetworkId(NEW_NETWORK_ID);
            }
        }



        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        adapter = new Adapter();
        @SuppressLint("InflateParams")
        final View content = LayoutInflater.from(getActivity()).inflate(R.layout.dlg_item_picker, null);
        final RecyclerView recyclerView = (RecyclerView) content.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);

        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        builder.setTitle(R.string.title_choose_network);
        builder.setView(content);
        builder.setRemoveTopPadding(true);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            if (selectedNetworkId != null) {
                if (selectedNetworkId.equals(NEW_NETWORK_ID)) {
                    InterfaceHub.getHandlerHub(IhCallback.class).onNewNetworkPicked(newNetworkName);
                } else {
                    // it is safe to downcast
                    InterfaceHub.getHandlerHub(IhCallback.class).onNetworkPicked(selectedNetworkId.shortValue());
                }
                dismiss();
            } else {
                updateOkButtonState();
            }
        });

        AlertDialog dlg = builder.create();
        okBtn = dlg.getPositiveButton();

        updateOkButtonState();

        return dlg;
    }

    private void updateOkButtonState() {
        if (Constants.DEBUG) log.d("updateOkButtonState()");
        if (Objects.equal(selectedNetworkId, NEW_NETWORK_ID)) {
            // check that there is non-empty new network name
            if (networkNodeManager.hasNetworkByName(newNetworkName)) {
                // network name already used
                okBtn.setEnabled(false);
            } else {
                okBtn.setEnabled(newNetworkName != null && newNetworkName.length() > 0);
            }
        } else {
            okBtn.setEnabled(selectedNetworkId != null && networkNodeManager.getNetworks().get(selectedNetworkId.shortValue()) != null);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    public static void showDialog(FragmentManager fm, Short selectedNetworkId) {
        final NetworkPickerDialogFragment f = new NetworkPickerDialogFragment();
        if (selectedNetworkId != null) {
            f.setArguments(getArgsForNetwork(selectedNetworkId));
        }
        f.show(fm, NetworkPickerDialogFragment.FRAGMENT_TAG);
    }

    public static void showDialog(FragmentManager fm, @Nullable String newNetworkName) {
        final NetworkPickerDialogFragment f = new NetworkPickerDialogFragment();
        if (newNetworkName != null) {
            f.setArguments(getArgsForNewNetwork(newNetworkName));
        }
        f.show(fm, NetworkPickerDialogFragment.FRAGMENT_TAG);
    }


    // ***************************
    // * INNER CLASSES
    // ***************************

    /**
     * Adapter
     */
    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        static final int ITEM_TYPE_EXISTING_NETWORK = 0;
        static final int ITEM_TYPE_NEW_NETWORK = 1;


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View v;
            switch (viewType) {
                case ITEM_TYPE_EXISTING_NETWORK:
                    v = inflater.inflate(R.layout.li_dlg_network_picker, parent, false);
                    break;
                case ITEM_TYPE_NEW_NETWORK:
                    v = inflater.inflate(R.layout.li_dlg_new_network, parent, false);
                    break;
                default:
                    throw new IllegalStateException("unsupported viewType: " + viewType);
            }
            return new ViewHolder(v);
        }

        @Override
        public int getItemViewType(int position) {
            if (networks.length - 1 == position) {
                return ITEM_TYPE_NEW_NETWORK;
            } else {
                return ITEM_TYPE_EXISTING_NETWORK;
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            if (Constants.DEBUG) log.d("onBindViewHolder: position = " + position);
            holder.bind(networks[position]);
        }

        @Override
        public int getItemCount() {
            return networks.length;
        }

    }

    private void onNewSelectedNetwork() {
        if (Constants.DEBUG) log.d("onNewSelectedNetwork");
        adapter.notifyDataSetChanged();
        updateOkButtonState();
        View currentFocusView = getDialog().getCurrentFocus();
        if (currentFocusView != null) {
            InputMethodManager inputManager = (InputMethodManager)
                    daApp.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (!Objects.equal(NEW_NETWORK_ID, selectedNetworkId)) {
                inputManager.hideSoftInputFromWindow(
                        currentFocusView.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS
                );
            } else {
                inputManager.showSoftInput(
                        currentFocusView,
                        InputMethodManager.SHOW_IMPLICIT
                );
            }
        }}


    private void setSelectedNetworkId(@Nullable Integer selectedUuid) {
        this.selectedNetworkId = selectedUuid;
    }

    /**
     * View holder
     */
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.radio)
        RadioButton rb;
        @BindView(R.id.tvNetworkName) @Nullable
        TextView tvNetworkName;
        @BindView(R.id.etNewNetworkName) @Nullable
        EditText etNewNetworkName;
        @BindView(R.id.focusableView) @Nullable
        View focusableView;
        private View container;
        // data bean
        NetworkModel network;

        ViewHolder(View v) {
            super(v);
            // extract references to visual elements
            ButterKnife.bind(this, v);
            // set up the listeners
            if (etNewNetworkName != null) {
                etNewNetworkName.addTextChangedListener(new TextWatcherAdapter() {

                    @Override
                    public void afterTextChanged(Editable editable) {
                        newNetworkName = editable.toString();
                        updateOkButtonState();
                    }
                });
                etNewNetworkName.setOnTouchListener((v1, event) -> {
                    if (MotionEvent.ACTION_UP == event.getAction()) {
                        if (!Objects.equal(selectedNetworkId, NEW_NETWORK_ID)) {
                            onClick(container);
                        }
                    }
                    return false;
                });
            }
            //
            v.setOnClickListener(this);
            container = v;
        }

        @Override
        public void onClick(View view) {
            setSelectedNetworkId((Integer) view.getTag());
            onNewSelectedNetwork();
        }

        void bind(NetworkModel network) {
            // assign data bean
            this.network = network;
            int networkId;
            // set up visual elements content
            if (network == NEW_NETWORK) {
                // special handling - we do not retrieve network name of fake element
                assert etNewNetworkName != null;
                etNewNetworkName.setText(newNetworkName);
                networkId = NEW_NETWORK_ID;
            } else {
                // find the proper name of the network
                assert tvNetworkName != null;
                tvNetworkName.setText(network.getNetworkName());
                networkId = network.getNetworkId();
            }
            // toggle radio button
            if (selectedNetworkId != null && selectedNetworkId.equals(networkId)) {
                rb.setChecked(true);
                if (etNewNetworkName != null) {
                    if (Constants.DEBUG) log.d("ET requesting focus");
                    etNewNetworkName.requestFocus();
                } else {
                    if (Constants.DEBUG) log.d("FV requesting focus");
                    assert focusableView != null;
                    focusableView.requestFocus();
                }
            } else {
                rb.setChecked(false);
            }
            container.setTag(networkId);
        }
    }

    /**
     * Interface for UI callback
     */
    public interface IhCallback extends InterfaceHubHandler {

        void onNetworkPicked(short networkId);

        void onNewNetworkPicked(String networkName);

    }


}