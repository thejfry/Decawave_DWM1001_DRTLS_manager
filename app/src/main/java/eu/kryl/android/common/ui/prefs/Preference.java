/*
 * Copyright 2017, Pavel Kryl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kryl.android.common.ui.prefs;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Strings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Preference
 */
public class Preference {

    private int preferenceId;
    private String title;
    private View rootView;
    private TextView titleView;
    private TextView summaryView;

    private boolean enabled = true;

    public Preference(int preferenceId, View rootView, TextView titleView, TextView summaryView) {
        this.preferenceId = preferenceId;
        this.rootView = rootView;
        this.titleView = titleView;
        this.summaryView = summaryView;
    }

    public void setTitle(@NotNull String title) {
        this.title = title;
        titleView.setText(title);

    }

    public void setSummary(@Nullable String summary) {
        if (Strings.isNullOrEmpty(summary)) {
            summaryView.setVisibility(View.GONE);
        } else {
            summaryView.setVisibility(View.VISIBLE);
            summaryView.setText(summary);
        }
    }

    public int getPreferenceId() {
        return preferenceId;
    }

    public String getTitle() {
        return title;
    }

    public void setEnabled(boolean enabled) {
        setEnabledRecursive(this.rootView, enabled, true);
        this.rootView.setClickable(enabled);
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Interface definition for a callback to be invoked when the value of this
     * {@link Preference} has been changed by the user and is
     * about to be set and/or persisted.  This gives the client a chance
     * to prevent setting and/or persisting the value.
     */
    public interface OnPreferenceChangeListener {

        /**
         * Called when a Preference has been changed by the user. This is
         * called before the state of the Preference is about to be updated and
         * before the state is persisted.
         *
         * @param preference The changed Preference.
         * @param newValue   The new value of the Preference.
         * @return True to update the state of the Preference with the new value.
         */
        boolean onPreferenceChange(Preference preference, Object newValue);
    }

    /**
     * Interface definition for a callback to be invoked when a {@link Preference} is
     * clicked.
     */
    public interface OnPreferenceClickListener {
        /**
         * Called when a Preference has been clicked.
         *
         * @param preference The Preference that was clicked.
         * @return True if the click was handled.
         */
        boolean onPreferenceClick(Preference preference);
    }


    private static void setEnabledRecursive(View v, boolean enabled, boolean alsoThis) {
        if (alsoThis) v.setEnabled(enabled);
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int cnt = vg.getChildCount();
            for (int i = 0; i < cnt; i++) {
                setEnabledRecursive(vg.getChildAt(i), enabled, true);
            }
        }
    }

}
