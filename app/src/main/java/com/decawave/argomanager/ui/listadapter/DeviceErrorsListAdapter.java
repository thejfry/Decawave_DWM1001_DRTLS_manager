/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.listadapter;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.decawave.argomanager.R;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.error.DeviceErrors;
import com.decawave.argomanager.error.ErrorCodeInterpreter;
import com.decawave.argomanager.error.ErrorDetail;
import com.decawave.argomanager.ui.MainActivity;
import com.decawave.argomanager.ui.fragment.DeviceDebugConsoleFragment;
import com.decawave.argomanager.ui.fragment.FragmentType;
import com.decawave.argomanager.util.Util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.kryl.android.common.log.ComponentLog;

import static com.decawave.argomanager.ArgoApp.daApp;

/**
 *
 */
public class DeviceErrorsListAdapter extends RecyclerView.Adapter<DeviceErrorsListAdapter.ViewHolder> {
    public static final ComponentLog log = new ComponentLog(DeviceErrorsListAdapter.class);

    private final MainActivity mainActivity;
    private final ErrorManager errorManager;
    private final NetworkNodeManager networkNodeManager;
    // members
    private List<DeviceErrors> errors;
    // item types
    private static final int ITEM_TYPE_DEVICE_ERRORS = 0;


    public DeviceErrorsListAdapter(MainActivity mainActivity,
                                   ErrorManager errorManager,
                                   NetworkNodeManager networkNodeManager) {
        this.networkNodeManager = networkNodeManager;
        this.errors = new LinkedList<>();
        this.mainActivity = mainActivity;
        this.errorManager = errorManager;
    }

    @Override
    public int getItemViewType(int position) {
        // we have only one item type at the moment
        return ITEM_TYPE_DEVICE_ERRORS;
    }

    public void setErrors(List<DeviceErrors> errors) {
        this.errors.clear();
        // reorganize
        this.errors.addAll(errors);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ITEM_TYPE_DEVICE_ERRORS) {
            View view = inflater.inflate(R.layout.li_device_errors, parent, false);
            return new DeviceErrorsHolder(view);
        } else {
            throw new IllegalStateException("FIXME viewType = " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder _holder, int p) {
        if (_holder instanceof DeviceErrorsHolder) {
            // adjust the position to reflect the index in the 'listItems'
            DeviceErrorsHolder holder = (DeviceErrorsHolder) _holder;
            DeviceErrors errorDetails = errors.get(p);
            holder.bind(errorDetails);
            setSeparatorsAppearance(holder, p);
        } // else: do nothing
    }

    private void setSeparatorsAppearance(ViewHolder holder, int index) {
        holder.cardTop.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        //
        if (index == errors.size() - 1) {
            // last list item in its category
            holder.lastNodeSeparator.setVisibility(View.VISIBLE);
            holder.nodeSeparator.setVisibility(View.GONE);
        } else {
            holder.lastNodeSeparator.setVisibility(View.GONE);
            holder.nodeSeparator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return errors.size();
    }

    public void newError(String deviceBleAddress, ErrorDetail errorDetail) {
        // find the proper device
        int i = 0;
        for (DeviceErrors error : errors) {
            if (error.deviceBleAddress.equals(deviceBleAddress)) {
                error.addError(errorDetail);
                notifyItemChanged(i);
                return;
            }
            i++;
        }
        // else: device not found yet
        errors.add(new DeviceErrors(deviceBleAddress, errorDetail));
        if (i > 0) {
            // we need to adjust visibility of separators
            notifyItemChanged(i - 1);
        }
        notifyItemInserted(i);
    }

    public void removeErrors(String deviceBleAddress) {
        // find the proper device
        int i = 0;
        Iterator<DeviceErrors> it = errors.iterator();
        while (it.hasNext()) {
            DeviceErrors deviceErrors = it.next();
            if (deviceErrors.deviceBleAddress.equals(deviceBleAddress)) {
                it.remove();
                notifyItemRemoved(i);
                if (!errors.isEmpty()) {
                    if (i == 0) {
                        // first item removed
                        notifyItemChanged(0);
                    } else if (i == errors.size()) {
                        // last item removed
                        notifyItemChanged(errors.size() - 1);
                    }
                }
                break;
            }
            i++;
        }
    }

    public void removeAllErrors() {
        errors.clear();
        notifyDataSetChanged();
    }

    public void onSwiped(RecyclerView.ViewHolder viewHolder) {
        int type = viewHolder.getItemViewType();
        if (type == ITEM_TYPE_DEVICE_ERRORS) {
            errorManager.removeDeviceErrors(((DeviceErrorsHolder) viewHolder).bleAddress);
        } else {
            throw new IllegalStateException("swipe to dismiss on invalid card type - " + type);
        }

    }

    abstract class ViewHolder extends RecyclerView.ViewHolder {
        // common stuff here
        @BindView(R.id.bottomSeparator)
        View nodeSeparator;
        @BindView(R.id.cardTop)
        View cardTop;
        @BindView(R.id.lastNodeBottomSeparator)
        View lastNodeSeparator;


        ViewHolder(View itemView) {
            super(itemView);
            // extract references
            ButterKnife.bind(this, itemView);
        }

    }

    class DeviceErrorsHolder extends ViewHolder {
        // references to views
        @BindView(R.id.nodeBleAddress)
        TextView tvBleAddress;
        @BindView(R.id.cardContent)
        LinearLayout cardContent;
        // identification of the device
        String bleAddress;

        DeviceErrorsHolder(View itemView) {
            super(itemView);
            // extract references
            ButterKnife.bind(this, itemView);
        }

        void bind(DeviceErrors deviceErrors) {
            // set tag to match the BLE address
            this.bleAddress = deviceErrors.deviceBleAddress;
            Long nodeId = networkNodeManager.bleToId(deviceErrors.deviceBleAddress);
            if (nodeId != null) {
                this.tvBleAddress.setText(deviceErrors.deviceBleAddress + " / " + Util.formatAsHexa(nodeId));
            } else {
                // we do not know node ID
                this.tvBleAddress.setText(deviceErrors.deviceBleAddress);
            }
            // remove all err text views
            while (cardContent.getChildCount() > 1) {
                cardContent.removeViewAt(1);
            }
            // iterate and add corresponding textual section for each error message
            for (ErrorDetail errorDetail : deviceErrors.getErrors()) {
                LinearLayout ll = (LinearLayout) LayoutInflater.from(mainActivity).inflate(R.layout.error_log_entry, cardContent, false);
                TextView tvTitle = (TextView) ll.findViewById(R.id.messageTitle);
                tvTitle.setTextColor(ContextCompat.getColor(mainActivity.getApplicationContext(),
                        errorDetail.getProperties().warningOnly ? R.color.log_warning : R.color.log_error));
                tvTitle.setText(Util.formatMsgTime(errorDetail.getTime()) + " " + daApp.getString(errorDetail.getProperties().warningOnly ? R.string.warning: R.string.error) + " " + ErrorCodeInterpreter.getName(errorDetail.errorCode));
                ((TextView) ll.findViewById(R.id.message)).setText(errorDetail.message);
                cardContent.addView(ll);
            }
            cardContent.setOnClickListener((v) ->
                    mainActivity.showFragment(FragmentType.DEVICE_DEBUG_CONSOLE, DeviceDebugConsoleFragment.getArgsForDevice(bleAddress)));
        }

    }

}