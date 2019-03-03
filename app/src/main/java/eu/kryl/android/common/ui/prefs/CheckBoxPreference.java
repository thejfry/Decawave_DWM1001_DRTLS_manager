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
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * CheckBox preference
 */
public class CheckBoxPreference extends Preference {

    public static final int NOTHING_SELECTED_IDX = -1;

    private CheckBox mCheckBox;
    private boolean mChecked;

    public CheckBoxPreference(int preferenceId, View rootView, TextView titleView, TextView summaryView, CheckBox checkBox) {
        super(preferenceId, rootView, titleView, summaryView);
        this.mCheckBox = checkBox;
    }

    /** Select value from the list of values */
    public void setChecked(boolean checked) {
        this.mChecked = checked;
        mCheckBox.setChecked(checked);
    }

    public boolean isChecked() {
        return mChecked;
    }
}
