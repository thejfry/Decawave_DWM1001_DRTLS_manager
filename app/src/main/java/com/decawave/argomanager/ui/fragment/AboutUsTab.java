package com.decawave.argomanager.ui.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.decawave.argomanager.R;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;

import javax.inject.Inject;


public class AboutUsTab extends AbstractArgoFragment {

    // dependencies
    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    public AboutUsTab() {
        super(FragmentType.ABOUT_US);
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_about_us_tab,container,false);


        return view;
    }
}
