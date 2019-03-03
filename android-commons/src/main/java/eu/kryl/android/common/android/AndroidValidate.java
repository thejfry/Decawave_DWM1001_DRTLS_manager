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

import android.os.Looper;

public class AndroidValidate {

    /**
     * Android specific check that the current code is not running on UI thread
     */
    public final static void notRunningOnUiThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("can't run on UI thread");
        }
    }

    /**
     * Android specific check that the current code is running on UI thread
     */
    public final static void runningOnUiThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // check that we are not in a test environment
            if (!Thread.currentThread().getName().contains("android.test.InstrumentationTestRunner")) {
                throw new IllegalStateException("must run on UI thread");
            }
        }
    }

}
