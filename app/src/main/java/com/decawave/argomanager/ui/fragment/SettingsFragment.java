/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.ui.fragment;

import android.view.LayoutInflater;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.BuildConfig;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.R;
import com.decawave.argomanager.firmware.FirmwareRepository;
import com.decawave.argomanager.ioc.ArgoComponent;
import com.decawave.argomanager.prefs.AppPreferenceAccessor;
import com.decawave.argomanager.prefs.ApplicationMode;
import com.decawave.argomanager.prefs.LengthUnit;
import com.decawave.argomanager.util.ToastUtil;

import javax.inject.Inject;

import eu.kryl.android.common.ui.AbstractArgoPreferenceFragment;
import eu.kryl.android.common.ui.prefs.ListPreference;
import eu.kryl.android.common.ui.prefs.Preference;
import io.fabric.sdk.android.Fabric;

/**
 * Argo project.
 */

public class SettingsFragment extends AbstractArgoPreferenceFragment {
    @Inject
    AppPreferenceAccessor appPreferenceAccessor;

    // declared preferences
    private ListPreference prefUnits;
    private ListPreference prefApplicationMode;
    private Preference prefVersion;

    private int mVersionClickCount;

    public SettingsFragment() {
        super(FragmentType.SETTINGS);
    }

    protected void onCreateView(LayoutInflater inflater) {

        prefUnits = addListPreference(inflater,
                R.id.pref_units,
                getString(R.string.pref_units_title),
                new String[] {
                        getString(LengthUnit.METRIC.labelResource),
                        getString(LengthUnit.IMPERIAL.labelResource),
                },
                new String[] { LengthUnit.METRIC.name(), LengthUnit.IMPERIAL.name() },
                appPreferenceAccessor.getLengthUnit().name());

        // we are allowing application mode only in DEBUG versions
        if (Constants.DEBUG_UI) {
            prefApplicationMode = addListPreference(inflater,
                    R.id.pref_application_mode,
                    getString(R.string.pref_application_mode_title),
                    new String[] {
                            getString(ApplicationMode.SIMPLE.labelResource),
                            getString(ApplicationMode.ADVANCED.labelResource),
                    },
                    new String[] { ApplicationMode.SIMPLE.name(), ApplicationMode.ADVANCED.name() },
                    appPreferenceAccessor.getApplicationMode().name());
        }


        prefVersion = addPreference(inflater,
                R.id.pref_version,
                getString(R.string.pref_version_title),
                getVersionText());

        addPreference(inflater,
                R.id.pref_device_id,
                getString(R.string.pref_device_id_title),
                ArgoApp.ANDROID_ID);

        addPreference(inflater,
                R.id.pref_about,
                getString(R.string.pref_about_title),
                daApp.getString(R.string.app_name) + "\n" +
                daApp.getString(R.string.settings_about)
                );

    }

    @Override
    public void onResume() {
        super.onResume();
        mVersionClickCount = 0;
        updateUi();
    }

    @Override
    protected void updateUi() {
        LengthUnit lengthUnit = appPreferenceAccessor.getLengthUnit();
        prefUnits.selectValue(lengthUnit.name());
        prefUnits.setEnabled(true);
    }

    private String getVersionText() {
        Crashlytics kit = Fabric.getKit(Crashlytics.class);
        try {
            StringBuilder version = new StringBuilder(BuildConfig.VERSION_NAME);
            version.append(" (" + BuildConfig.VERSION_CODE + ")");
            String tag = FirmwareRepository.DEFAULT_FIRMWARE[0].getMeta().tag;
            if (tag != null && tag.length() > 0) {
                version.append("\n").append(daApp.getString(R.string.pref_firmware)).append(": ").append(tag);
            }
            version.append("\n").append(daApp.getString(R.string.pref_build_time)).append(": ").append(BuildConfig.BUILD_TIME);

            if (BuildConfig.DEBUG) {
                version.append("\n{debug-build}");
            } else if (Constants.DEBUG) {
                version.append("\n{release-verbose}");
            }
            //
            if (kit == null) {
                version.append(" {!crashlytics}");
            }
            return version.toString();
        } catch (Exception e) {
            if (kit != null) {
                Crashlytics.logException(e);
            }
            return "";
        }
    }

    @Override
    protected void injectFrom(ArgoComponent injector) {
        injector.inject(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == prefUnits) {
            appPreferenceAccessor.setLengthUnit(LengthUnit.valueOf(prefUnits.getSelectedValue()));
            return true;
        } else if (preference == prefApplicationMode) {
            appPreferenceAccessor.setApplicationMode(ApplicationMode.valueOf(prefApplicationMode.getSelectedValue()));
            return true;
        }
        // else: do not allow change
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == prefVersion) {
            onVersionClicked();
        }
        // have default handling for all preferences
        return false;
    }

    private void onVersionClicked() {
        mVersionClickCount++;
        if (mVersionClickCount == 5) {
            Crashlytics crashlytics = Fabric.getKit(Crashlytics.class);
            if (crashlytics == null) {
                ToastUtil.showToast(R.string.crashlytics_not_configured, Toast.LENGTH_LONG);
            } else {
                Crashlytics.logException(new Exception());
                ToastUtil.showToast(R.string.crashreport_sent);
            }
        }
    }

}
