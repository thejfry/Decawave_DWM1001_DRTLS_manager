package com.decawave.argomanager.ui.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.decawave.argomanager.R;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;

import javax.inject.Inject;

/**
 * Allows user to select anchors based on their relative locations on the floorplan.
 */
public class SelectAnchorsFragment extends AbstractArgoFragment {

    // dependencies
    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    public SelectAnchorsFragment(){
        super(FragmentType.SELECT_ANCHORS);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.select_anchor, container,false);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
