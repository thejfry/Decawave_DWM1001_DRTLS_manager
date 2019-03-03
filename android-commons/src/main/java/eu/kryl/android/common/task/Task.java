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

package eu.kryl.android.common.task;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.kryl.android.common.log.ComponentLog;

/**
 * These represent asynchronous units of execution.
 *
 * Might be client-server interaction (SKU, promocodes)
 * or IAB interaction (purchases, SKU descriptions - prices,...)
 *
 * Extend with other tasks as necessary.
 *
 */
public abstract class Task {

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // final stuff
    //

    /** list of tasks that had to run successfully before this one can start */
    public final List<Task> prerequisites;

    /** R.string.id to show to the user, when this task is running */
    public final int resIdPending;

    /** R.string.id to show to the user, when this task has failed */
    public final int resIdError;

    public final ComponentLog log;


    public Task(int pendingMsg, int errMsg, Task... prereq) {
        // initialize final members
        this.resIdPending = pendingMsg;
        this.resIdError = errMsg;
        // prerequisities must be unmodifiable
        if (prereq != null && prereq.length > 0) {
            this.prerequisites = Collections.unmodifiableList(Arrays.asList(prereq));
        } else {
            this.prerequisites = Collections.emptyList();
        }
        this.log = new ComponentLog(this.getClass());
        // reset the task initially
        reset();
    }

    public interface AsyncResultCallback
    {

        public void onAsyncJobDone(boolean success);

        public void onFatalErrorResetTaskDispatcher();

    }


    /**
     * Synchronously respond, whether it makes sense to execute this task.
     * This is introduced to avoid cross-references between tasks, mutually
     * resetting others state (previously isDone flag).
     *
     * @return true if the task should be executed, false otherwise
     *
     * @see #execute(Task.AsyncResultCallback)
     */
    public abstract boolean executeMakesSense();


    /**
     * Either synchronously or asynchronously execute the task.
     *
     * @param callback use the callback to let the scheduler know
     *                 when is the execution finished (use the callback
     *                 either synchronously or asynchronously)
     */
    public abstract void execute(final AsyncResultCallback callback);


    public void reset() {
        // empty by default
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + System.identityHashCode(this);
    }
}
