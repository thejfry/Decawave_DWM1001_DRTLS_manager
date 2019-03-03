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
import android.widget.TextView;

import org.jetbrains.annotations.Nullable;

import static com.google.common.base.Preconditions.checkState;

/**
 * List preference
 */
public class ListPreference extends Preference {

    private static final int NOTHING_SELECTED_IDX = -1;

    private String[] mLabels;
    private String[] mValues;
    private String mSelectedValue = null;
    private int mSelectedValueIdx = NOTHING_SELECTED_IDX;

    public ListPreference(int preferenceId, View rootView, TextView titleView, TextView summaryView, String[] labels, String[] values) {
        super(preferenceId, rootView, titleView, summaryView);
        checkState(labels.length == values.length);
        this.mLabels = labels;
        this.mValues = values;
    }

    /** Select value from the list of values */
    public void selectValue(@Nullable String selectedValue) {

        boolean found = false;
        if (selectedValue != null) {
            for (int i = 0; i < mValues.length; i++) {
                if (selectedValue.equals(mValues[i])) {
                    // found
                    selectValueAt(i);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            // not found
            selectValueAt(NOTHING_SELECTED_IDX);
        }
    }

    /** Select value at position */
    public void selectValueAt(int index) {
        if (index >= 0 && index < mValues.length) {
            mSelectedValueIdx = index;
            mSelectedValue = mValues[index];
            setSummary(mLabels[index]);
        } else {
            mSelectedValueIdx = NOTHING_SELECTED_IDX;
            mSelectedValue = null;
            setSummary(null);
        }
    }

    public String[] getLabels() {
        return mLabels;
    }

    public String[] getValues() {
        return mValues;
    }


    public String getSelectedValue() {
        return mSelectedValue;
    }

    public int getSelectedValueIdx() {
        return mSelectedValueIdx;
    }
}
