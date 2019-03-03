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

package eu.kryl.android.common.async;

/**
 *
 */
public interface SbHandler {

    void post(Runnable runnable);

    /**
     * Posts the given runnable to be executed in the future.
     */
    void postDelayed(Runnable runnable, long delayInMillis);

    /**
     * Posts the given runnable to be executed in the future, associating the runnable with the
     * given token.
     * @see #removeCallbacks(Object)
     */
    void postDelayed(Runnable runnable, long delayInMillis, Object token);

    boolean postAtFrontOfQueue(Runnable runnable);

    /**
     * @return true if the current thread is the same on which runs this handler
     */
    boolean currentThreadHandler();

    /**
     * Clears all posted runnables.
     */
    void clear();

    /**
     * Removes all pending instance of given runnable.
     */
    void removeCallbacks(Runnable runnable);

    /**
     * Removes a runnable previously scheduled, identified by the token.
     * @see #postDelayed(Runnable, long, Object)
     */
    void removeCallbacks(Object runnableToken);
}
