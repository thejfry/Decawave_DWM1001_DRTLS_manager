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

import android.os.Handler;
import android.os.Looper;

import com.google.common.base.Preconditions;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.android.AndroidValidate;
import eu.kryl.android.common.log.ComponentLog;

/**
 * Dispatches &amp; Enqueues asynchronous tasks.
 *
 * @see Task
 */
public class TaskDispatcher {
    private static final ComponentLog log = new ComponentLog(TaskDispatcher.class);


    ////////////////////////////////////////////////////////////////////////////////////////////
    //
    // task dispatcher contract
    //

    /**
     * Submits task for execution.
     *
     * @param task task to be executed
     *
     * @see TaskDispatcher.TaskEventListener
     * @see Task
     */
    public void submitTask(Task task) {
        if (mErrorTask == null) {
            if (Constants.DEBUG) {
                log.d("task enqueue: " + task);
            }
            taskQueue.addFirst(new EnqueuedTask(task, null, false));
            if (!listenerNotificationInProgress) {
                // initiate consume now
                consumeTask();
            } else {
                // initiate consume in the next dispatch round
                // let us notify all listeners first
                handler.post(consumeTaskRunnable);
            }
        } else {
            // there is an error
            log.w("task enqueue request " + task + " ignored, the TaskDispatcher is in error state");
        }
    }

    public Task getErrorTask() {
        return mErrorTask == null ? null : mErrorTask.task;
    }

    public void resetErrorState() {
        if (Constants.DEBUG) log.d("resetErrorState(): tasks size = " + taskQueue.size());
        mErrorTask = null;
    }

    public Task getPendingTask() {
        return mPendingTask == null ? null : mPendingTask.task;
    }

    public void registerTaskEventListener(TaskEventListener listener) {
        mTaskEventListeners.add(listener);
    }

    public boolean isTaskQueueEmpty() {
        return taskQueue.isEmpty();
    }

    /**
     * Task event listener - listens for various event tasks.
     */
    public interface TaskEventListener {

        public void onTaskSkipped(Task task);

        public void beforeTaskExecute(Task task);

        public void afterTaskExecute(Task task, boolean success);

        public void onDispatcherReset(Task task);

    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    //
    // implementation
    //


    private class EnqueuedTask {
        // task reference
        public final Task task;
        /**
         * When task T1 is started as a prerequisite of some other task T0, the
         * task queue will have to be cleared of all tasks in the tree of tasks (the tree
         * here is formed by the {@link #prerequisiteOf} relationship), all the way up
         * to the root task (which may or may not be a direct 'parent' of this task),
         * whenever T1 finishes its execution in an error state.
         *
         * (i.e. if a task from a set of prerequisite tasks fails, the root task must
         * fail as well)
         *
         * Once the task queue is cleaned of the current tree, reset this field to null!
         *
         * @deprecated I believe this is not needed at all
         */
        public final EnqueuedTask prerequisiteOf;

        // just optimization to avoid repeated prerequisites checking
        public boolean prerequisitesChecked;


        private EnqueuedTask(Task t, EnqueuedTask prerequisiteOf, boolean prereqChecked) {
            this.task = t;
            this.prerequisiteOf = prerequisiteOf;
            this.prerequisitesChecked = prereqChecked;
        }

        @Override
        public String toString() {
            return "EnqueuedTask{" +
                    "task=" + task +
                    '}';
        }
    }

    // listeners
    private List<TaskEventListener> mTaskEventListeners;
    // whether the listener dispatch is in progress
    private boolean listenerNotificationInProgress;
    // handler
    private Handler handler;
    //////////////////////////////////////////////////////////////
    // state
    private EnqueuedTask mErrorTask;
    private EnqueuedTask mPendingTask;
    // current task queue (to be executed)
    private Deque<EnqueuedTask> taskQueue = new LinkedList<>();
    // token which identifies dispatch round (from reset to next reset)
    private Object dispatchToken;
    // consume task runnable
    private Runnable consumeTaskRunnable = new Runnable() {

        @Override
        public void run() {
            consumeTask();
        }
    };



    public TaskDispatcher() {
        mTaskEventListeners = new LinkedList<>();
        handler = new Handler(Looper.myLooper());
        resetTaskDispatcher(null);

    }

    /**
     * Consumes a Task from {@link #taskQueue}, if:
     *   1. there is a waiting task in the queue,
     *   2. another Task is not executing at the moment.
     *
     * NOTE 1: the consumed task will not execute if it has already been
     * executed successfully before.
     *
     * NOTE 2: execution of the consumed task will get deferred, if the
     * task has unfulfilled prerequisites.
     */
    private void consumeTask() {
        if (Constants.DEBUG) {
            AndroidValidate.runningOnUiThread();
            Preconditions.checkNotNull(dispatchToken);
        }
        if (mPendingTask != null) {
            // Another task is currently executing. Just return, since
            // we are going to get called again when that pending task completes..
            if (Constants.DEBUG) {
                log.d("there is a pending task, cannot consume() now");
            }
            return;
        }

        if (mErrorTask != null) {
            // A task has just completed in an error state. Just return.
            // if one wants to continue in Task dispatching, he should call resetErrorState() first
            if (Constants.DEBUG) {
                log.d("task dispatcher is in an error state, cannot consume() now");
            }
            return;
        }

        if (taskQueue.isEmpty()) {
            return;
        }

        EnqueuedTask t2Run;

        do {
            // skip tasks whose execute does not make sense
            boolean makesSense;
            do {
                if (taskQueue.isEmpty()) {
                    // nothing to consume
                    mErrorTask = null;
                    mPendingTask = null;
                    return;
                }
                t2Run = taskQueue.removeLast();
                makesSense = t2Run.task.executeMakesSense();
                if (!makesSense) {
                    notifyListeners(ListenerNotificationType.TASK_SKIPPED, t2Run.task);
                    if (Constants.DEBUG) {
                        log.d("skipping task " + t2Run + ", does not make sense");
                    }
                }
            } while (!makesSense);

            // we've got a task whose execute makes sense

            // check prerequisites
            boolean isPrerequisitesMet = true;
            if (!t2Run.prerequisitesChecked) {
                // check prerequisites
                for (Task tPrereq : t2Run.task.prerequisites) {
                    if (tPrereq.executeMakesSense()) {
                        if (Constants.DEBUG) {
                            log.d("adding prerequisite " + tPrereq);
                        }
                        taskQueue.addFirst(new EnqueuedTask(tPrereq, t2Run, false));
                        isPrerequisitesMet = false;
                    } else {
                        // we do not notify about skipped prerequisites during their makesSense() evaluation
                        if (Constants.DEBUG) {
                            log.d("skipping prerequisite " + tPrereq + ", does not make sense");
                        }
                    }
                }
                // prerequisites are now checked
                t2Run.prerequisitesChecked = true;
            }
            if (!isPrerequisitesMet) {
                // At least one dependent task needs to run before this one can.
                // The ones needed were already added to the queue above, but
                // we need to add the current task as well.
                taskQueue.addFirst(t2Run);
                // start working on the prerequisites..
            } else {
                // no prerequisites, continue with processing the t2Run
                break;
            }

        } while (true);

        // at this point we _know the task can/must be started.
        mErrorTask = null;
        mPendingTask = t2Run;

        if (Constants.DEBUG)
            log.d("executing task " + t2Run.task);

        notifyListeners(ListenerNotificationType.BEFORE_TASK_EXECUTE, t2Run.task, true);
        final EnqueuedTask _t2Run = t2Run;
        final Object _dispatchToken = dispatchToken;
        t2Run.task.execute(new Task.AsyncResultCallback() {

            @Override
            public void onAsyncJobDone(boolean success) {
                if (Constants.DEBUG) {
                    log.d("onAsyncJobDone(): " + _t2Run.task + ", success = " + success);
                }
                if (_dispatchToken == dispatchToken) {
                    // setup dispatcher state
                    mPendingTask = null;
                    if (success) {
                        mErrorTask = null;
                    } else {
                        mErrorTask = _t2Run;
                        handleTaskError();
                    }
                    notifyListeners(ListenerNotificationType.AFTER_TASK_EXECUTE, _t2Run.task, success);
                    // consume next task (if any and if not error - inside condition)
                    consumeTask();
                } else {
                    if (Constants.DEBUG) {
                        log.d("ignoring async notification from task " + _t2Run);
                    }
                }
            }

            @Override
            public void onFatalErrorResetTaskDispatcher() {
                if (Constants.DEBUG) {
                    log.d("onFatalErrorResetTaskDispatcher(): " + _t2Run.task);
                }
                // eliminate previous callbacks
                if (_dispatchToken == dispatchToken) {
                    // we need to completely reset our state and state of all tasks
                    resetTaskDispatcher(_t2Run.task);
                }
            }
        });
        // execute MUST be the last command performed here!,
        // because some tasks are synchronous - onAsyncJobDone is performed immediately
    }

    private void resetTaskDispatcher(Task cause) {
        // reset all tasks
        for (EnqueuedTask enqueuedTask : taskQueue) {
            enqueuedTask.task.reset();
        }
        taskQueue.clear();
        // create new token - so that notifications from earlier dispatch (if any)
        // are safely ignored
        dispatchToken = new Object();
        // then we notify our listeners
        notifyListeners(ListenerNotificationType.ON_RESET, cause, true);
    }


    private enum ListenerNotificationType {
        TASK_SKIPPED,
        BEFORE_TASK_EXECUTE,
        AFTER_TASK_EXECUTE,
        ON_RESET
    }

    // b1 - is just a helper parameter, so that one method signature can server for various listener invocations
    private void notifyListeners(ListenerNotificationType notificationType, Task task) {
        notifyListeners(notificationType, task, true);
    }

    private void notifyListeners(ListenerNotificationType notificationType, Task task, boolean b1) {
        if (Constants.DEBUG) {
            log.d("notifyListeners: " + notificationType + ", task = " + task
                    + (notificationType == ListenerNotificationType.AFTER_TASK_EXECUTE ? ", success = " + b1 : "")
            );
        }
        listenerNotificationInProgress = true;
        for (TaskEventListener listener : mTaskEventListeners) {
            switch (notificationType) {
                case TASK_SKIPPED:
                    listener.onTaskSkipped(task);
                    break;
                case BEFORE_TASK_EXECUTE:
                    listener.beforeTaskExecute(task);
                    break;
                case AFTER_TASK_EXECUTE:
                    listener.afterTaskExecute(task, b1);
                    break;
                case ON_RESET:
                    listener.onDispatcherReset(task);
                    break;
            }
        }
        listenerNotificationInProgress = false;
    }

    private void handleTaskError() {
        if (mErrorTask != null && taskQueue.size() > 0) {
            // clear entire task queue
            log.d("Dropping task(s) from queue, due to a failed (sub)task: " + mErrorTask);
            // we do not need to reset the tasks, just clear the queue
            taskQueue.clear();
        }
    }

    private void clearTaskQueueOfRelatedTasks(EnqueuedTask t) {
        if (Constants.DEBUG) AndroidValidate.runningOnUiThread();
        // find the root parent task
        while (t.prerequisiteOf != null) {
            t = t.prerequisiteOf;
        }

        // add all descendant tasks
        Set<EnqueuedTask> tasks2Delete = new HashSet<>();
        addChildren(t, tasks2Delete);

        // reset all tasks to be removed.. (mainly reference to a parent task)
        for (EnqueuedTask enqTask : tasks2Delete) {
            enqTask.task.reset();
        }

        if (Constants.DEBUG)
            log.d("Dropping task(s) from queue, due to a failed (sub)task: " + tasks2Delete);

        taskQueue.removeAll(tasks2Delete);
    }

    private void addChildren(EnqueuedTask parent, Set<EnqueuedTask> descendants) {
        for (EnqueuedTask runner : taskQueue) {
            if (runner.prerequisiteOf == parent) {
                addChildren(runner, descendants);
            }
        }
        descendants.add(parent);
    }


}
