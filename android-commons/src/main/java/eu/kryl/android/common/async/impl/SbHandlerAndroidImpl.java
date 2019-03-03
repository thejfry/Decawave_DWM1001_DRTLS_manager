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

package eu.kryl.android.common.async.impl;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.google.common.base.Preconditions;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.async.SbHandler;

/**
 * Handler implementation delegating the implementation
 * to {@link android.os.Handler}.
 */
public class SbHandlerAndroidImpl implements SbHandler {
    // delegate android handler
    private final Handler handler;

    public SbHandlerAndroidImpl(Handler handler) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(handler);
        }
        this.handler = handler;
    }

    @Override
    public void postDelayed(Runnable runnable, long delayInMillis, Object token) {
        long targetTime = SystemClock.uptimeMillis() + delayInMillis;
        this.handler.postAtTime(runnable, token, targetTime);
    }

    @Override
    public void postDelayed(Runnable runnable, long delayInMillis) {
        this.handler.postDelayed(runnable, delayInMillis);
    }

    @Override
    public void post(Runnable runnable) {
        this.handler.post(runnable);
    }

    @Override
    public boolean postAtFrontOfQueue(Runnable runnable) {
        return this.handler.postAtFrontOfQueue(runnable);
    }

    @Override
    public boolean currentThreadHandler() {
        return Looper.myLooper() == handler.getLooper();
    }

    @Override
    public void clear() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void removeCallbacks(Runnable runnable) {
        handler.removeCallbacks(runnable);
    }

    @Override
    public void removeCallbacks(Object runnableToken) {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(runnableToken, "this is too dangerous!");
        }
        this.handler.removeCallbacksAndMessages(runnableToken);
    }
}
