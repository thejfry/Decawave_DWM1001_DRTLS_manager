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

/**
 * EditText preference
 */
public class EditTextPreference extends Preference {

    public enum ValueType {
        ANY_STRING,
        POSITIVE_INTEGER
    }

    private ValueType valueType;
    private Object value;
    private int maxLength;

    public EditTextPreference(int preferenceId, View rootView, TextView titleView, TextView summaryView, ValueType valueType, int maxLength) {
        super(preferenceId, rootView, titleView, summaryView);
        this.valueType = valueType;
        this.maxLength = maxLength;
    }

    public void setValue(@Nullable Object value) {
        this.value = value;
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    @Nullable
    public String getStringValue() {
        return value == null ? null : value.toString();
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
}
