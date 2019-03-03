/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.decawave.argomanager.R;
import com.decawave.argomanager.components.ErrorManager;
import com.decawave.argomanager.components.NetworkNodeManager;
import com.decawave.argomanager.error.ErrorDetail;
import com.decawave.argomanager.error.IhErrorManagerListener;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.ui.listadapter.DeviceErrorsListAdapter;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Shows recyclerView of networks and unassigned nodes.
 */
public class DeviceErrorFragment extends AbstractArgoFragment implements IhErrorManagerListener {
    // dependencies
    @Inject
    ErrorManager errorManager;

    @Inject
    NetworkNodeManager networkNodeManager;

    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    @BindView(R.id.elementList)
    RecyclerView elementList;

    @BindView(R.id.noErrors)
    TextView noErrors;

    ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            throw new IllegalStateException("drag not supported");
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            adapter.onSwiped(viewHolder);
        }
    };

        // members
    private DeviceErrorsListAdapter adapter;

    public DeviceErrorFragment() {
        super(FragmentType.DEVICE_ERRORS);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new DeviceErrorsListAdapter(getMainActivity(), errorManager, networkNodeManager);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_error_log, menu);
        menu.findItem(R.id.action_clear).setOnMenuItemClickListener((mi) -> { errorManager.clearErrors(); return true; });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.device_errors_list, container, false);
        ButterKnife.bind(this, v);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(elementList);
        // configure the recycler view overall layout
        elementList.setLayoutManager(new LinearLayoutManager(getActivity()));
        // configure the adapter
        elementList.setAdapter(adapter);
        // the adapter contents will be set later (onResume)
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // load the adapter with discovered nodes (only transient ones)
        adapter.setErrors(errorManager.getErrors());
        // update the list
        adapter.notifyDataSetChanged();
        // mark all errors as read
        errorManager.markErrorsAsRead();
        //
        adjustUi();
    }

    void adjustUi() {
        if (adapter.getItemCount() == 0) {
            elementList.setVisibility(View.GONE);
            noErrors.setVisibility(View.VISIBLE);
        } else {
            elementList.setVisibility(View.VISIBLE);
            noErrors.setVisibility(View.GONE);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // error manager listener (keep datasource owned by the adapter in sync with the real error manager)
    //

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    @Override
    public void onErrorDetailAdded(@NonNull String deviceBleAddress, @NonNull ErrorDetail errorDetail) {
        int count = adapter.getItemCount();
        adapter.newError(deviceBleAddress, errorDetail);
        if (count == 0) {
            // there must be change
            adjustUi();
        }
    }

    @Override
    public void onErrorRemoved(@NonNull String deviceBleAddress) {
        int count = adapter.getItemCount();
        adapter.removeErrors(deviceBleAddress);
        if (count > 0) {
            // there might have been change
            adjustUi();
        }
    }

    @Override
    public void onErrorsClear() {
        int count = adapter.getItemCount();
        adapter.removeAllErrors();
        if (count > 0) {
            adjustUi();
        }
    }
}
