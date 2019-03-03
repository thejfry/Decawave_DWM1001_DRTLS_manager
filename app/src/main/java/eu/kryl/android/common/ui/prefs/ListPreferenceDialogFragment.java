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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import eu.kryl.android.appcompat.dialogs.AlertDialog;
import eu.kryl.android.common.hub.InterfaceHub;

/** List preference dialog fragment */
public class ListPreferenceDialogFragment extends DialogFragment {

    private static final String FRAGMENT_TAG = "listpreferencedlg";

    private static final String BK_PREF_ID = "prefId";
    private static final String BK_TITLE = "title";
    private static final String BK_LABELS = "labels";
    private static final String BK_SELECTED_IDX = "selectedIdx";

    private int prefId;
    private String title;
    private String[] labels;
    private int selectedIdx;


    public ListPreferenceDialogFragment() {
    }

    @SuppressLint("ValidFragment")
    public ListPreferenceDialogFragment(int prefId, String title, String[] labels, int selectedIdx) {
        this.prefId = prefId;
        this.title = title;
        this.labels = labels;
        this.selectedIdx = selectedIdx;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putInt(BK_PREF_ID, prefId);
        bundle.putString(BK_TITLE, title);
        bundle.putStringArray(BK_LABELS, labels);
        bundle.putInt(BK_SELECTED_IDX, selectedIdx);
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
                labels = bundle.getStringArray(BK_LABELS);
                selectedIdx = bundle.getInt(BK_SELECTED_IDX);
            }
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        builder.setSingleChoiceItems(convert(labels), selectedIdx,
                (dialog, which) -> {
                    // item selected
                    InterfaceHub.getHandlerHub(IhPreferenceValueChangedListener.class).onPreferenceValueChanged(prefId, which);
                    dialog.dismiss();
                });

        return builder.create();
    }

    private static CharSequence[] convert(String[] strings) {
        final CharSequence[] result = new CharSequence[strings.length];
        int i = 0;
        for (String s : strings) {
            result[i] = strings[i];
            i++;
        }
        return result;
    }

    public static void showDialog(FragmentManager fm, int prefId, String title, String[] labels, int selectedIdx) {
        final ListPreferenceDialogFragment f = new ListPreferenceDialogFragment(prefId, title, labels, selectedIdx);
        f.show(fm, ListPreferenceDialogFragment.FRAGMENT_TAG);
    }

}