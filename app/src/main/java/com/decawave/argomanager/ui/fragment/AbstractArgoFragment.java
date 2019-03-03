/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.ioc.IocContext;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.ui.MainActivity;

import javax.inject.Inject;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.hub.InterfaceHubHandler;
import eu.kryl.android.common.log.ComponentLog;

/**
 * Common predecessor for all fragments.
 */

public abstract class AbstractArgoFragment extends Fragment {
    // component log
    protected final ComponentLog log = new ComponentLog(getClass());

    public static final ArgoApp daApp = ArgoApp.daApp;

    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    public AbstractArgoFragment(FragmentType fragmentType) {
        this.fragmentType = fragmentType;
    }

    public interface OnFragmentSwitchedListener {

        void onFragmentSwitched(AbstractArgoFragment fragmentInstance);

    }

    public final FragmentType fragmentType;

    public String getScreenTitle() {
        if (fragmentType.hasScreenTitle) {
            return daApp.getString(fragmentType.screenTitleId);
        } else {
            return null;
        }
    }

    /**
     * Implemented usually by the parent Activity
     */
    OnFragmentSwitchedListener mListener;

    /**
     * Make sure our Activity implements {@link OnFragmentSwitchedListener}
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // set up a listener
        try {
            mListener = (OnFragmentSwitchedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " +
                    OnFragmentSwitchedListener.class.getName());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // make sure we @Inject the necessary fields
        injectFrom(IocContext.daCtx);
    }

    protected abstract void injectFrom(ArgoComponent injector);

    @Override
    final public void onStart() {
        // call super
        super.onStart();

        // parent Activity callback
        log.d("updating UI for " + fragmentType.name());
        mListener.onFragmentSwitched(this);
    }

    @Override
    public void onResume() {
        if (Constants.DEBUG) {
            log.d("onResume");
        }
        super.onResume();
        if (this instanceof InterfaceHubHandler) {
            InterfaceHub.registerHandler((InterfaceHubHandler) this);
        }
    }

    @Override
    public void onPause() {
        if (Constants.DEBUG) {
            log.d("onPause");
        }
        if (this instanceof InterfaceHubHandler) {
            InterfaceHub.unregisterHandler((InterfaceHubHandler) this);
        }
        super.onPause();
    }

    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    protected void dismiss() {
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getSupportFragmentManager().popBackStackImmediate();
        }
    }

}
