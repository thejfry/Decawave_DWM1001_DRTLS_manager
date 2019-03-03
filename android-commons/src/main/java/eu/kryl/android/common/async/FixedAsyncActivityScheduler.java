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

import android.os.SystemClock;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import eu.kryl.android.common.Constants;

/**
 * Provides the ability to schedule execution of a potentially async activity.
 * The async activity to execute cannot be changed once schedule is constructed.
 *
 * One can configure:
 * - activity/runnable
 * - scheduling delay (time from the last call to {@link #schedule()} to execution of the activity)
 * - which handler is used for activity execution (default is global fallbackWorkerHandler)
 *
 * Once the scheduler instance is created/configured, one can call {@link #schedule()} to
 * have the activity executed in the configured way (immediately/with delay on a specific handler).
 *
 * Do not forget to configure the fallback handler via {@link #setFallbackWorkerHandler(SbHandler)}
 * once the application is started/booted.
 *
 */
public class FixedAsyncActivityScheduler {
    // reference to worker handler on which we are scheduling the async asyncActivity
    private static volatile SbHandler fallbackWorkerHandler;
    private static final long MIN_SCHEDULE_DELAY_MS = 10;

    // user configuration
    private Runnable asyncActivity;
    private final long scheduleDelayMs;
    private final boolean allowReschedule;
    private final boolean allowOverlap;

    // internal modifiable state
    private Runnable internalRunnable;
    private boolean reschedule;
    // this is valid only if allowOverlap is false
    private boolean executing;
    private boolean rerunAfterExecute;
    //
    private long lastRescheduleTime;
    private boolean scheduled;
    private final Object lock;
    private final SbHandler handler;

    /**
     * Use this constructor to have your activity scheduled (and rescheduled) to be
     * executed after the specified delay on the fallback worker handler.
     *
     * @param scheduleDelayMs minimal delay between the last {@link #schedule()} call
     *                        and activity execution
     * @param activity activity to be executed
     *
     * @see #setFallbackWorkerHandler(SbHandler)
     */
    public FixedAsyncActivityScheduler(long scheduleDelayMs, Runnable activity) {
        this(scheduleDelayMs, null, activity, true, false);
    }

    /**
     * Use this constructor to have your activity scheduled on the specified
     * handler to be executed as soon as possible (next dispatch round, no delay).
     *
     * @param h handler used for activity execution
     * @param activity activity to be executed
     *
     * @see #setFallbackWorkerHandler(SbHandler)
     */
    public FixedAsyncActivityScheduler(@NotNull SbHandler h, Runnable activity) {
        this(0, h, activity, false, false);
    }

    /**
     * Use this constructor to have your activity scheduled (and rescheduled) to be
     * executed after the specified delay on the specified handler.
     *
     * @param scheduleDelayMs minimal delay between the last {@link #schedule()} call
     *                        and activity execution
     * @param h handler used for activity scheduling + execution
     * @param activity activity to be executed
     *
     * @see #setFallbackWorkerHandler(SbHandler)
     */
    public FixedAsyncActivityScheduler(long scheduleDelayMs, @NotNull SbHandler h, Runnable activity) {
        this(scheduleDelayMs, h, activity, true, false);
    }

    private FixedAsyncActivityScheduler(long scheduleDelayMs, @Nullable SbHandler h, Runnable activity, boolean allowReschedule, boolean allowOverlap) {
        Preconditions.checkState(scheduleDelayMs == 0 || scheduleDelayMs >= MIN_SCHEDULE_DELAY_MS, "scheduleDelayMs must be greater than " + MIN_SCHEDULE_DELAY_MS);
        Preconditions.checkNotNull(fallbackWorkerHandler);
        this.asyncActivity = activity;
        this.scheduleDelayMs = scheduleDelayMs;
        this.reschedule = false;
        this.rerunAfterExecute = false;
        this.lastRescheduleTime = 0;
        this.lock = new Object();
        this.handler = h != null ? h : fallbackWorkerHandler;
        this.allowReschedule = allowReschedule;
        this.allowOverlap = allowOverlap;
    }

    public void setAsyncActivity(Runnable asyncActivity) {
        this.asyncActivity = asyncActivity;
    }

    public enum ScheduleResult {
        SCHEDULED,
        RESCHEDULED,
        KEPT_SCHEDULED
    }

    /**
     * Schedules the activity execution.
     * If this is called multiple times - there are multiple schedule calls before
     * the execution takes place - the activity is executed only once.
     * If there is configured a delay between schedule and execute,
     * the execution will not occur sooner than:
     *
     * 'time of last schedule() invocation' + the specified delay
     *
     * @return depends on actually performed scheduling action
     */
    public ScheduleResult schedule() {
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(handler);
        }
        final ScheduleResult r;
        synchronized (lock) {
            // we are touching the shared state
            if (!scheduled) {
                if (scheduleDelayMs == 0) {
                    handler.post(getInternalRunnable());
                } else {
                    handler.postDelayed(getInternalRunnable(), scheduleDelayMs);
                }
                scheduled = true;
                r = ScheduleResult.SCHEDULED;
            } else if (allowReschedule) {
                // we are already scheduled, initiate reschedule
                reschedule = true;
                lastRescheduleTime = SystemClock.uptimeMillis();
                r = ScheduleResult.RESCHEDULED;
            } else {
                r = ScheduleResult.KEPT_SCHEDULED;
            }
        }
        return r;
    }

    private Runnable getInternalRunnable() {
        if (internalRunnable == null) {
            internalRunnable = new Runnable() {

                @Override
                public void run() {
                    synchronized (lock) {
                        // we are touching the shared state
                        if (reschedule) {
                            // we need to reschedule
                            if (Constants.DEBUG) {
                                // some checks
                                Preconditions.checkState(scheduled);
                                Preconditions.checkState(lastRescheduleTime > 0);
                            }
                            // reset the reschedule flag, we are handling the reschedule now
                            reschedule = false;
                            lastRescheduleTime = 0;
                            // determine the reschedule delay
                            long delayMillis = (lastRescheduleTime + scheduleDelayMs) - SystemClock.uptimeMillis();
                            if (delayMillis >= MIN_SCHEDULE_DELAY_MS) {
                                // it makes sense to wait/reschedule
                                handler.postDelayed(getInternalRunnable(), delayMillis);
                                // keep scheduled = true
                                return;
                            }
                        } // multi-else: we do not need to reschedule
                        // we are about to execute, check if we are executing
                        if (!allowOverlap) {
                            if (executing) {
                                // we need to wait till the execution finishes
                                rerunAfterExecute = true;
                                // we keep scheduled = true, because the flag rerunAfterExecute assures
                                // that internal runnable executes as soon as the previous instance finishes
                                return;
                            }
                        }
                        // notify
                        onBeforeAsyncActivityRun();
                        // reset the scheduled flag
                        scheduled = false;
                        //
                        executing = true;
                    }
                    // now execute the asyncActivity - unsynchronized
                    asyncActivity.run();

                    // after the activity is done do state changes again
                    synchronized (lock) {
                        executing = false;
                        // notify
                        onAfterAsyncActivityRun();
                        // check potential overlap/reschedule
                        if (!allowOverlap) {
                            // we might have some pending scheduled invocations
                            if (rerunAfterExecute) {
                                // we need to schedule the internal runnable now
                                handler.post(getInternalRunnable());
                                rerunAfterExecute = false;
                            } // else: no need to rerun, nothing to do...
                        }
                    }
                }
            };
        }
        return internalRunnable;
    }

    public boolean isScheduled() {
        synchronized (lock) {
            return scheduled;
        }
    }

    public boolean isExecuting() {
        synchronized (lock) {
            return executing;
        }
    }

    /**
     * Notification method - executed inside synchronized block to notify
     * about just about to be executed async activity.
     * The async activity self is not executed in a synchronized block,
     * therefore this is the place to modify some 'shared state'.
     */
    protected void onBeforeAsyncActivityRun() {
        // empty by default
    }


    /**
     * Notification method - executed inside synchronized block to notify
     * about about just finished async activity.
     * The async activity self is not executed in a synchronized block,
     * therefore this is the place to modify some 'shared state'.
     */
    protected void onAfterAsyncActivityRun() {
        // empty by default
    }

    /**
     * Sets up a fallback worker handler to be used when one does not specify
     * explicit handler in the constructor.
     *
     * @param fallbackWorkerHandler handler to be used for activity scheduling/execution
     */
    public static void setFallbackWorkerHandler(SbHandler fallbackWorkerHandler) {
        FixedAsyncActivityScheduler.fallbackWorkerHandler = fallbackWorkerHandler;
    }

}
