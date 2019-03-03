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

package eu.kryl.android.common.android;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * Cached preferences skeleton. Override this class to implement your own preferences.
 */
public abstract class AbstractCachedPreferences {

    /**
     * Preference file name
     */
    private String mPreferencesName;

    /**
     * Application context
     */
    protected Context mAppCtx;

    /**
     * Save all preferences here
     */
    protected abstract void onSave(SharedPreferences.Editor editor);

    /**
     * Load all preferences here
     */
    protected abstract void onLoad(SharedPreferences sharedPrefs);

    /**
     * Constructor should be called only once
     */
    public AbstractCachedPreferences(Context appCtx, String preferencesName) {
        this.mAppCtx = appCtx;
        this.mPreferencesName = preferencesName;
        loadPreferences();
    }

    /**
     * Load preferences (also assign default values if not set yet)
     */
    private void loadPreferences() {
        final SharedPreferences sharedPrefs = mAppCtx.getSharedPreferences(mPreferencesName, MODE_PRIVATE);
        onLoad(sharedPrefs);
    }

    /**
     * Save preferences. Call before application exits
     */
    protected void savePreferences() {
        final SharedPreferences.Editor editor = mAppCtx.getSharedPreferences(mPreferencesName, MODE_PRIVATE).edit();
        onSave(editor);
        editor.commit();
    }

}
