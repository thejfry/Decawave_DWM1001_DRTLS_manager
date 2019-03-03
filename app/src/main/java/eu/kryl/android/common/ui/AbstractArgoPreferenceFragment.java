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

package eu.kryl.android.common.ui;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.decawave.argomanager.R;
import com.decawave.argomanager.ui.fragment.AbstractArgoFragment;
import com.decawave.argomanager.ui.fragment.FragmentType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import eu.kryl.android.common.hub.InterfaceHub;
import eu.kryl.android.common.ui.prefs.CheckBoxPreference;
import eu.kryl.android.common.ui.prefs.CustomPreference;
import eu.kryl.android.common.ui.prefs.EditTextPreference;
import eu.kryl.android.common.ui.prefs.EditTextPreferenceDialogFragment;
import eu.kryl.android.common.ui.prefs.IhPreferenceValueChangedListener;
import eu.kryl.android.common.ui.prefs.ListPreference;
import eu.kryl.android.common.ui.prefs.ListPreferenceDialogFragment;
import eu.kryl.android.common.ui.prefs.Preference;


/**
 * Base fragment class for preferences
 */
public abstract class AbstractArgoPreferenceFragment extends AbstractArgoFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    /** Container for preferences to be added */
    private ViewGroup viewContainer;

    /** Map of preference ID to Preference object */
    private Map<Integer, Preference> prefsMap = new HashMap<>();

    public AbstractArgoPreferenceFragment(FragmentType fragmentType) {
        super(fragmentType);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View content = inflater.inflate(R.layout.fragment_empty_prefs, container, false);
        viewContainer = (ViewGroup) content.findViewById(R.id.prefsContainer);

        onCreateView(inflater);

        return content;
    }

    @Override
    public void onResume() {
        super.onResume();
        InterfaceHub.registerHandler(internalPrefChangeListener);
    }

    @Override
    public void onPause() {
        InterfaceHub.unregisterHandler(internalPrefChangeListener);
        super.onPause();
    }

    /**
     * Method to be implemented by subclasses to add their own preferences
     */
    protected abstract void onCreateView(LayoutInflater inflater);

    /**
     * Add preference group
     */
    protected void addPreferenceGroup(LayoutInflater inflater, @NotNull String title) {
        removePreviousDividerIfFound();

        final TextView prefGroupView = (TextView) inflater.inflate(R.layout.pref_preference_group, viewContainer, false);
        prefGroupView.setId(R.id.pref_preference_group);
        prefGroupView.setText(title);

        viewContainer.addView(prefGroupView);
    }

    /**
     * Add preference
     */
    protected Preference addPreference(LayoutInflater inflater, int prefKey, @NotNull String title) {
        return addPreference(inflater, prefKey, title, null);
    }

    /**
     * Add preference
     */
    protected Preference addPreference(
            LayoutInflater inflater, int prefId, @NotNull String title, @Nullable String summary) {

        final View view = inflater.inflate(R.layout.pref_preference_item, viewContainer, false);
        view.setId(prefId);

        final TextView titleView = (TextView) view.findViewById(R.id.title);
        final TextView summaryView = (TextView) view.findViewById(R.id.summary);
        final Preference preference = new Preference(prefId, view, titleView, summaryView);
        preference.setTitle(title);
        preference.setSummary(summary);

        view.setTag(preference);
        view.setOnClickListener(internalPrefClickListener);

        viewContainer.addView(view);
        prefsMap.put(prefId, preference);


        // also add divider after the preference
        addDivider(inflater);

        return preference;
    }

    /**
     * Add list preference
     */
    protected ListPreference addListPreference(
            LayoutInflater inflater, int prefId, @NotNull String title,
            String[] entries, String[] entryValues,
            @Nullable String selectedValue) {

        final View view = inflater.inflate(R.layout.pref_preference_item, viewContainer, false);
        view.setId(prefId);

        final TextView titleView = (TextView) view.findViewById(R.id.title);
        final TextView summaryView = (TextView) view.findViewById(R.id.summary);
        final ListPreference preference = new ListPreference(prefId, view, titleView, summaryView, entries, entryValues);
        preference.setTitle(title);
        preference.selectValue(selectedValue);

        view.setTag(preference);
        view.setOnClickListener(internalPrefClickListener);

        viewContainer.addView(view);
        prefsMap.put(prefId, preference);

        // also add divider after the preference
        addDivider(inflater);

        return preference;
    }

    /**
     * Add checkbox preference
     */
    protected CheckBoxPreference addCheckBoxPreference(
            LayoutInflater inflater, int prefId, @NotNull String title, @Nullable String summary) {

        final View view = inflater.inflate(R.layout.pref_checkbox_preference_item, viewContainer, false);
        view.setId(prefId);

        final TextView titleView = (TextView) view.findViewById(R.id.title);
        final TextView summaryView = (TextView) view.findViewById(R.id.summary);
        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        final CheckBoxPreference preference = new CheckBoxPreference(prefId, view, titleView, summaryView, checkBox);
        preference.setTitle(title);
        preference.setSummary(summary);

        view.setTag(preference);
        view.setOnClickListener(internalPrefClickListener);

        viewContainer.addView(view);
        prefsMap.put(prefId, preference);


        // also add divider after the preference
        addDivider(inflater);

        return preference;
    }

    /**
     * Add custom preference
     */
    protected CustomPreference addCustomPreference(
            LayoutInflater inflater, int prefId, @NotNull String title, @Nullable String summary, int customViewLayoutResId) {

        final View view = inflater.inflate(R.layout.pref_custom_preference_item, viewContainer, false);
        view.setId(prefId);

        final ViewStub viewStub = (ViewStub) view.findViewById(R.id.customView);
        viewStub.setLayoutResource(customViewLayoutResId);
        final View customView = viewStub.inflate();

        final TextView titleView = (TextView) view.findViewById(R.id.title);
        final TextView summaryView = (TextView) view.findViewById(R.id.summary);
        final CustomPreference preference = new CustomPreference(prefId, view, titleView, summaryView, customView);
        preference.setTitle(title);
        preference.setSummary(summary);

        view.setTag(preference);
        view.setOnClickListener(internalPrefClickListener);


        viewContainer.addView(view);
        prefsMap.put(prefId, preference);


        // also add divider after the preference
        addDivider(inflater);

        return preference;
    }

    /**
     * Add edit text preference
     */
    protected EditTextPreference addEditTextPreference(
            LayoutInflater inflater, int prefId, @NotNull String title, @Nullable String summary, EditTextPreference.ValueType valueType, int maxLength) {

        final View view = inflater.inflate(R.layout.pref_preference_item, viewContainer, false);
        view.setId(prefId);

        final TextView titleView = (TextView) view.findViewById(R.id.title);
        final TextView summaryView = (TextView) view.findViewById(R.id.summary);
        final EditTextPreference preference = new EditTextPreference(prefId, view, titleView, summaryView, valueType, maxLength);
        preference.setTitle(title);
        preference.setSummary(summary);

        view.setTag(preference);
        view.setOnClickListener(internalPrefClickListener);

        viewContainer.addView(view);
        prefsMap.put(prefId, preference);

        // also add divider after the preference
        addDivider(inflater);

        return preference;
    }


    /**
     * Add divider
     */
    private void addDivider(LayoutInflater inflater) {

        final View divider = new View(inflater.getContext());
        divider.setId(R.id.pref_preference_divider);
        final int color = ContextCompat.getColor(inflater.getContext(), R.color.color_preference_separator);
        divider.setBackgroundColor(color);

        viewContainer.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private void removePreviousDividerIfFound() {
        // remove previous divider (if found)
        final int childCount = viewContainer.getChildCount();
        if (childCount > 0) {
            final View lastChild = viewContainer.getChildAt(childCount - 1);
            if (lastChild.getId() == R.id.pref_preference_divider) {
                viewContainer.removeView(lastChild);
            }
        }
    }

    /** Internal preference click listener */
    private View.OnClickListener internalPrefClickListener = v -> {
        final Preference preference = (Preference) v.getTag();
        if (!preference.isEnabled()) {
            // preference is disabled -> ignore clicks
            return;
        }

        final boolean handled = onPreferenceClick(preference);
        if (!handled) {
            // click not handled
            onPreferenceClickGeneric(preference);
        }
    };

    protected void onPreferenceClickGeneric(Preference preference) {
        if (preference instanceof ListPreference) {
            // show list preference dialog
            final ListPreference listPreference = (ListPreference) preference;
            ListPreferenceDialogFragment.showDialog(getFragmentManager(),
                    listPreference.getPreferenceId(), listPreference.getTitle(),
                    listPreference.getLabels(), listPreference.getSelectedValueIdx());

        } else if (preference instanceof EditTextPreference) {
            // show edittext preference dialog
            final EditTextPreference etPreference = (EditTextPreference) preference;
            EditTextPreferenceDialogFragment.showDialog(getFragmentManager(),
                    etPreference.getPreferenceId(), etPreference.getTitle(),
                    etPreference.getStringValue(), etPreference.getValueType(), etPreference.getMaxLength());

        } else if (preference instanceof CheckBoxPreference) {
            // switch checkbox preference
            final CheckBoxPreference cbPreference = (CheckBoxPreference) preference;
            final boolean newCheckedState = !cbPreference.isChecked();
            cbPreference.setChecked(newCheckedState);
            onPreferenceChange(preference, cbPreference.isChecked());
        }
    }

    /** Internal preference change listener */
    private IhPreferenceValueChangedListener internalPrefChangeListener = (prefId, newValue) -> {
        final Preference preference = prefsMap.get(prefId);
        if (preference != null) {
            // it is our preference

            if (preference instanceof ListPreference) {
                // on list preference value changed
                final ListPreference listPreference = (ListPreference) preference;
                listPreference.selectValueAt((Integer) newValue);
                onPreferenceChange(preference, listPreference.getSelectedValue());
            } else if (preference instanceof EditTextPreference) {
                // text value changed
                onPreferenceChange(preference, newValue);
            }
        }
    };

    protected abstract void updateUi();

}
