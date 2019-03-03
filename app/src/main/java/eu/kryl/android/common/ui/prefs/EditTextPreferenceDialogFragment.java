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


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.decawave.argomanager.R;

import eu.kryl.android.appcompat.dialogs.AlertDialog;
import eu.kryl.android.common.hub.InterfaceHub;

/** List preference dialog fragment */
public class EditTextPreferenceDialogFragment extends DialogFragment {

    private static final String FRAGMENT_TAG = "etpreferencedlg";

    private static final String BK_PREF_ID = "prefId";
    private static final String BK_TITLE = "title";
    private static final String BK_VALUE = "value";
    private static final String BK_VALUE_TYPE = "valueType";
    private static final String BK_MAX_LENGTH = "maxlen";

    private int prefId;
    private String title;
    private String value;
    private EditTextPreference.ValueType valueType;
    private int maxLength;


    public EditTextPreferenceDialogFragment() {
    }

    @SuppressLint("ValidFragment")
    public EditTextPreferenceDialogFragment(int prefId, String title, String value, EditTextPreference.ValueType valueType, int maxLength) {
        this.prefId = prefId;
        this.title = title;
        this.value = value;
        this.valueType = valueType;
        this.maxLength = maxLength;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putInt(BK_PREF_ID, prefId);
        bundle.putString(BK_TITLE, title);
        bundle.putString(BK_VALUE, value);
        bundle.putString(BK_VALUE_TYPE, valueType.name());
        bundle.putInt(BK_MAX_LENGTH, maxLength);
        super.onSaveInstanceState(bundle);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        // restore dialog state
        if (bundle != null) {
            if (bundle.containsKey(BK_PREF_ID)) {
                prefId = bundle.getInt(BK_PREF_ID);
                title = bundle.getString(BK_TITLE);
                value = bundle.getString(BK_VALUE);
                valueType = EditTextPreference.ValueType.valueOf(bundle.getString(BK_VALUE_TYPE));
                maxLength = bundle.getInt(BK_MAX_LENGTH);
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        // custom view
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View content = inflater.inflate(R.layout.pref_dlg_edit_text, null);

        final EditText et = (EditText) content.findViewById(R.id.etPrefString);
        et.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(maxLength)
        });
        et.setText(value);
        builder.setView(content);

        switch (valueType) {
            case POSITIVE_INTEGER:
                et.setKeyListener(DigitsKeyListener.getInstance(false, false));
                break;
            default:
            case ANY_STRING:
                break;
        }

        // positive button
        builder.setPositiveButton(R.string.ok, (dialog, id) -> {
            // convert value
            final String stringValue = et.getText().toString();
            Object value1;
            switch (valueType) {
                case POSITIVE_INTEGER:
                    if (stringValue.isEmpty()) {
                        value1 = null;
                    } else {
                        try {
                            value1 = Integer.parseInt(stringValue);
                        } catch (NumberFormatException e) {
                            value1 = 0;
                        }
                    }
                    break;
                default:
                    value1 = stringValue;
                    break;
            }

            // notify callers
            InterfaceHub.getHandlerHub(IhPreferenceValueChangedListener.class).onPreferenceValueChanged(prefId, value1);
            dialog.dismiss();
        });

        // negative button
        builder.setNegativeButton(R.string.btn_cancel, null);

        return builder.create();
    }

    public static void showDialog(FragmentManager fm, int prefId, String title, String value, EditTextPreference.ValueType valueType, int maxLength) {
        final EditTextPreferenceDialogFragment f = new EditTextPreferenceDialogFragment(prefId, title, value, valueType, maxLength);
        f.show(fm, EditTextPreferenceDialogFragment.FRAGMENT_TAG);
    }

}