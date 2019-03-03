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

package eu.kryl.android.common.fsm.impl;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.log.ComponentLog;

class FsmScheduler<E extends Enum<E>> extends Handler {
    @SuppressWarnings("PointlessBooleanExpression")
    private static final boolean LOG_SCHEDULER_DEBUGS = Constants.DEBUG && false;
    // message codes
    private static final int MSG_CODE_BASE = 100;
    private static final int MSG_CODE_RANGE = 200;
    private static Runnable VOID_RUNNABLE = new Runnable() {

        @Override
        public void run() {
            throw new IllegalStateException("should never run!");
        }
        
    };
    // scheduled runnable
    private int nextRunnableIdx = 0;
    private boolean debug = false;
    private ComponentLog log;
    private Runnable[] runnables = new Runnable[MSG_CODE_RANGE];
    
    private Set<Integer> stateKeptRunnables = new LinkedHashSet<>();
    private Map<E, Set<Integer>> stateChangeRunnables;
    private Set<Integer> unconditionalRunnables = new LinkedHashSet<>();
    
    
    FsmScheduler(@NotNull ComponentLog log, Class<E> enumClass) {
        Preconditions.checkNotNull(log);
        this.log = log;
        this.debug = false;
        this.stateChangeRunnables = new EnumMap<>(enumClass);
        // null the array
        for (int i = 0; i < runnables.length; i++) {
            runnables[i] = null;
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private void logd(String message) {
        if (debug) {
            log.d("scheduler: " + message);
        }
    }
    
    private int nextIdx() {
        while (runnables[nextRunnableIdx] != null) {
            nextRunnableIdx = (nextRunnableIdx + 1) % MSG_CODE_RANGE;
        }
        if (LOG_SCHEDULER_DEBUGS) {
            logd("nextIdx() nextRunnableIdx = " + nextRunnableIdx);
        }
        return nextRunnableIdx;
    }
    
    /**
     * If no state change occurs within timeoutInMillis, runnable is executed. 
     * @param timeoutInMillis timeout after which the runnable is executed
     */
    void scheduleStateKeptRunnable(Runnable runnable, long timeoutInMillis, E resetState) {
        final int idx = nextIdx();
        if (LOG_SCHEDULER_DEBUGS) {
            logd("scheduleStateKeptRunnable() idx = " + idx + ", timeoutInMillis = " + timeoutInMillis + ", resetState = " + resetState);
            Preconditions.checkState(runnables[idx] == null);
        }
        runnables[idx] = runnable;
        if (resetState == null) {
            stateKeptRunnables.add(idx);
        } else {
            Set<Integer> set = stateChangeRunnables.get(resetState);
            if (set == null) {
                set = new LinkedHashSet<>();
                stateChangeRunnables.put(resetState, set);
            }
            set.add(idx);
        }
        sendEmptyMessageDelayed(MSG_CODE_BASE + idx, timeoutInMillis);
    }
    
    /**
     * Might unschedule some runnables.
     */
    void onStateChanged(E newState) {
        if (LOG_SCHEDULER_DEBUGS) {
            if (!stateKeptRunnables.isEmpty()) {
                logd("unscheduling runnables " + TextUtils.join(",", stateKeptRunnables) + " associated with arbitrary state transition");
            }
        }
        unscheduleRunnables(stateKeptRunnables);
        stateKeptRunnables.clear();
        final Set<Integer> list = stateChangeRunnables.get(newState);
        if (list != null && !list.isEmpty()) {
            // 'unschedule'
            if (LOG_SCHEDULER_DEBUGS) {
                logd("unscheduling runnables " + TextUtils.join(",", list) + " associated with transition to state " + newState);
            }
            unscheduleRunnables(list);
            list.clear();
        }
    }

    private void unscheduleRunnables(Collection<Integer> list) {
        for (int msgCode : list) {
            // 'unschedule'
            removeMessages(MSG_CODE_BASE + msgCode);
            runnables[msgCode] = null;
        }
    }
    
    /**
     * Schedule arbitrary runnable to be executed ASAP.
     */
    void scheduleRunnableNow(Runnable runnable) {
        scheduleRunnable(runnable, 0);
    }
    
    /**
     * Schedule arbitrary runnable to be after delay seconds elapsed.
     */
    void scheduleRunnable(Runnable runnable, long delay) {
        final int idx = nextIdx();
        if (LOG_SCHEDULER_DEBUGS) {
            logd("scheduleRunnable() idx = " + idx + ", delay = " + delay);
        }
        unconditionalRunnables.add(idx);
        runnables[idx] = runnable;
        sendEmptyMessageDelayed(MSG_CODE_BASE + idx, delay);
    }
    
    void unscheduleRunnable(Runnable runnable) {
        for (int i = 0; i < runnables.length; i++) {
            if (runnables[i] == runnable && unconditionalRunnables.contains(i)) {
                // this is it
                runnables[i] = VOID_RUNNABLE;
                // continue - we might have more of these...
            }
        }
    }
    
    /**
     * Unschedules all scheduled runnables: both state changed and arbitrary runnables.
     */
    void unscheduleAll() {
        if (LOG_SCHEDULER_DEBUGS) {
            logd("unscheduling all runnables");
        }
        for (int i = 0; i < runnables.length; i++) {
            if (runnables[i] != null) {
                removeMessages(MSG_CODE_BASE + i);
                runnables[i] = null;
            }
        }
        stateChangeRunnables.clear();
        unconditionalRunnables.clear();
        stateKeptRunnables.clear();
    }
    
    public void handleMessage(Message msg) {
        final int idx = msg.what - MSG_CODE_BASE;
        if (idx >= 0 && idx < MSG_CODE_RANGE) {
            if (LOG_SCHEDULER_DEBUGS) logd("executing runnable on idx " + idx);
            Runnable r = null;
            if (runnables[idx] == null) {
                if (Constants.DEBUG) throw new IllegalStateException("runnable on idx " + idx + " is empty!");
            } else {
                // store & clear
                r = runnables[idx];
                runnables[idx] = null;
                if (r == VOID_RUNNABLE) logd("runnable on idx " + idx + " is VOID, skipping");
            }
            // clear from our datastructures
            boolean f = false;
            if (!stateKeptRunnables.remove(idx)) {
                if (!unconditionalRunnables.remove(idx)) {
                    for(Set<Integer> runnableIndex : stateChangeRunnables.values()) {
                        f = f | runnableIndex.remove(idx);
                    }
                } else {
                    f = true;
                }
            } else {
                f = true;
            }
            if (Constants.DEBUG) {
                Preconditions.checkState(f, "executed runnable at idx = " + idx + ", but it was not found in DS?!");
            }
            // finally execute the runnable - after the reference to scheduled runnable has been cleared
            if (r != null && r != VOID_RUNNABLE) r.run();
        } else {
            super.handleMessage(msg);
        }
    }

} 