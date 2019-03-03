/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.decawave.argo.api.YesNoAsync;
import com.decawave.argomanager.ArgoApp;
import com.decawave.argomanager.Constants;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * @see ConcurrentOperationQueue
 */
public class ConcurrentOperationQueueImpl implements ConcurrentOperationQueue {
    private static final ComponentLog log = new ComponentLog(ConcurrentOperationQueueImpl.class);
    // members - state
    private final Map<String, Long> earliestResourceOperationSysTime;
    private OperationExecutionSupervisor supervisor;
    private PriorityQueue<OperationInfo> operationQueue;
    private OperationInfo checkExecuteScheduledFor;
    private int counter;

    private class OperationInfo implements Comparable {
        Action1<Token> executable;
        Priority priority;
        String resource;
        Token token;
        boolean executing;
        final int serialId;

        OperationInfo(Action1<Token> executable, Priority priority, String resource, int serialId) {
            this.executable = executable;
            this.priority = priority;
            this.resource = resource;
            this.serialId = serialId;
            this.executing = false;
            // double-binding
            this.token = new TokenImpl(this);
        }

        @Override
        public int compareTo(@NonNull Object o) {
            Preconditions.checkState(o instanceof OperationInfo, "second operand is not of type OperationInfo");
            // we want the HIGH priority go first
            OperationInfo that = (OperationInfo) o;
            int i = that.priority.ordinal() - this.priority.ordinal();
            if (i != 0) {
                return i;
            } // else: order by serialId (keep the insertion order)
            return this.serialId - that.serialId;
        }

        @Override
        public String toString() {
            return "OperationInfo{" +
                    "priority=" + priority +
                    ", resource='" + resource + '\'' +
                    ", token=" + System.identityHashCode(token) +
                    ", executing=" + executing +
                    '}';
        }
    }

    private class OperationExecutionSupervisor {
        // limits
        private int overallLimit;
        private EnumMap<Priority,AtomicInteger> limitsByPriority;
        // current state
        private int blockCounter;
        private List<Action0> onBlockedCallbacks;
        private int overallCounter;
        private EnumMap<Priority,AtomicInteger> operationByPriorityCounter;
        private Set<String> usingResources;

        OperationExecutionSupervisor(int overallLimit) {
            this.usingResources = new HashSet<>();
            this.onBlockedCallbacks = new LinkedList<>();
            // initialize limits
            this.overallLimit = overallLimit;
            this.limitsByPriority = new EnumMap<>(Priority.class);
            for (Priority priority : Priority.values()) {
                // do not limit particular priority at all
                limitsByPriority.put(priority, new AtomicInteger(overallLimit));
            }
            // initialize counters
            this.operationByPriorityCounter = new EnumMap<>(Priority.class);
            for (Priority priority : Priority.values()) {
                this.operationByPriorityCounter.put(priority, new AtomicInteger(0));
            }
            this.overallCounter = 0;
        }

        void block(Action0 onBlocked) {
            this.blockCounter++;
            if (Constants.DEBUG) {
                Preconditions.checkState(blockCounter == 1 || overallCounter == 0,
                        "FIXME: blockCounter = " + blockCounter + ", overallCounter = " + overallCounter);
            }
            if (overallCounter == 0) {
                // we have satisfied the block request immediately
                onBlocked.call();
            } else {
                // there are some pending requests, store the callback for future reference
                this.onBlockedCallbacks.add(onBlocked);
            }
        }

        boolean unblock() {
            if (Constants.DEBUG) {
                Preconditions.checkState(this.blockCounter > 0, "cannot call unblock");
            }
            this.blockCounter--;
            // is there any change/did we really unblock?
            return this.blockCounter == 0;
        }


        int setPriorityLimit(Priority priority, int limit) {
            if (Constants.DEBUG) {
                Preconditions.checkNotNull(priority);
                Preconditions.checkState(limit <= overallLimit, "limit " + limit + " cannot be greater than overall limit: " + overallLimit);
            }
            AtomicInteger i = this.limitsByPriority.get(priority);
            int oldValue = i.intValue();
            i.set(limit);
            return oldValue;
        }


        void onOperationStarting(OperationInfo operationInfo) {
            if (Constants.DEBUG) {
                Preconditions.checkNotNull(operationInfo);
                Preconditions.checkState(!usingResources.contains(operationInfo.resource));
                Preconditions.checkState(blockCounter == 0);
            }
            overallCounter++;
            if (operationInfo.resource != null) {
                usingResources.add(operationInfo.resource);
            }
            int opByPrioCounter = operationByPriorityCounter.get(operationInfo.priority).incrementAndGet();
            // validate
            if (Constants.DEBUG) {
                Preconditions.checkState(overallCounter <= overallLimit, "limit overflow: " + overallCounter + ", limit: " + overallLimit);
                Preconditions.checkState(opByPrioCounter <= limitsByPriority.get(operationInfo.priority).intValue(),
                        "limit by priority overflow: " + opByPrioCounter + ", limit: " + limitsByPriority.get(operationInfo.priority).intValue());
            }
        }

        void onOperationFinished(OperationInfo operationInfo) {
            if (Constants.DEBUG) {
                Preconditions.checkNotNull(operationInfo.priority);
                Preconditions.checkState(operationInfo.resource == null || usingResources.contains(operationInfo.resource));
            }
            if (operationInfo.resource != null) {
                usingResources.remove(operationInfo.resource);
            }
            int opByPrioCounter = operationByPriorityCounter.get(operationInfo.priority).decrementAndGet();
            overallCounter--;
            if (overallCounter == 0 && blockCounter > 0 && !onBlockedCallbacks.isEmpty()) {
                // the callbacks are awaiting to be called
                for (Action0 onBlockedCallback : onBlockedCallbacks) {
                    onBlockedCallback.call();
                }
                // do not call the callbacks again
                onBlockedCallbacks.clear();
            }
            if (Constants.DEBUG) {
                Preconditions.checkState(blockCounter >= 0, "block counter is negative!");
                Preconditions.checkState(overallCounter >= 0, "overall counter is negative!");
                Preconditions.checkState(opByPrioCounter >= 0, "operation by priority counter is negative!");
            }
        }

        YesNoAsync isBlocked() {
            if (blockCounter == 0) {
                return YesNoAsync.NO;
            } else if (overallCounter == 0) {
                return YesNoAsync.YES;
            } else {
                if (Constants.DEBUG) {
                    Preconditions.checkState(overallCounter > 0, "overall counter = " + overallCounter);
                }
                return YesNoAsync.TO_NO;
            }
        }

        boolean canExecuteOperation(OperationInfo operationInfo) {
            if (blockCounter == 0) {
                if (operationInfo.resource != null) {
                    // check resources
                    if (usingResources.contains(operationInfo.resource)) {
                        // the resource is currently used
                        return false;
                    }
                }
                // check limits
                if (overallCounter < overallLimit) {
                    // check limit by priority
                    int prioLimit = limitsByPriority.get(operationInfo.priority).intValue();
                    int prioCount = operationByPriorityCounter.get(operationInfo.priority).intValue();
                    if (prioCount < prioLimit) {
                        return true;
                    }
                }
            } // multi-else:
            return false;
        }

    }
    
    private class TokenImpl implements Token {
        // keep priority within the token
        private final OperationInfo oi;

        TokenImpl(OperationInfo oi) {
            this.oi = oi;
        }

        @Override
        public String toString() {
            return "TokenImpl{" +
                    "@=" + System.identityHashCode(this) +
                    ", oi=" + oi +
                    '}';
        }
    }

    public ConcurrentOperationQueueImpl(int limit) {
        this.supervisor = new OperationExecutionSupervisor(limit);
        this.operationQueue = new PriorityQueue<>();
        this.earliestResourceOperationSysTime = new HashMap<>();
        this.counter = 0;
    }

    @Override
    public Token operationEnqueue(@NotNull Action1<Token> operation,
                                  @NotNull Priority priority, @Nullable String operationResource) {
        OperationInfo oi = new OperationInfo(operation, priority, operationResource, counter++);
        if (Constants.DEBUG) {
            log.d("operationEnqueue: " + "oi = [" + oi + "]");
        }
        this.operationQueue.add(oi);
        // check if there is big enough read-ahead limit to execute the operation
        checkExecute();
        return oi.token;
    }

    private Runnable checkExecuteRunnable = this::checkExecuteScheduled;

    private void checkExecuteScheduled() {
        // reset the scheduled operation info reference first
        this.checkExecuteScheduledFor = null;
        // pretend as if we were called from nowhere
        checkExecute();
    }

    // check the queue and execute operation(s) at head till supervisor tells so
    private void checkExecute() {
        OperationInfo headOperation = getHeadCandidateForExecute();
        // we need to check the timing
        if (headOperation != null && checkExecuteScheduledFor != headOperation) {
            // we've got a fresh head operation
            // check that enough time has elapsed
            Long sysTime = earliestResourceOperationSysTime.get(headOperation.resource);
            long delay;
            if (sysTime != null && (delay = sysTime - SystemClock.uptimeMillis()) > 0) {
                // time for execution of operation at head has not elapsed yet
                if (checkExecuteScheduledFor != null) {
                    // there is already scheduled something
                    // check if we need to reschedule
                    long scheduledSysTime = earliestResourceOperationSysTime.get(checkExecuteScheduledFor.resource);
                    if (scheduledSysTime > sysTime) {
                        // we need to reschedule for an earlier time
                        ArgoApp.uiHandler.removeCallbacks(checkExecuteRunnable);
                        ArgoApp.uiHandler.postDelayed(checkExecuteRunnable, delay);
                    }
                } else {
                    // there is not scheduled anything
                    ArgoApp.uiHandler.postDelayed(checkExecuteRunnable, delay);
                }
                // we will change the info for which operation are we scheduled
                checkExecuteScheduledFor = headOperation;
                // terminate recursion
            } else {
                // either there is no restriction about the time or the time has elapsed
                // remove the operation from the queue
                operationQueue.remove();
                // execute the operation NOW
                doLaunchOperation(headOperation);
                // optimization
                if (getHeadCandidateForExecute() != null) {
                    // there is a candidate for execute, call recursively (infinite recursion will not occur)
                    checkExecute();
                }
            }
        }
        // else:
        // either there is no operation to execute or
        // there is scheduled next execute for the head operation, no need to do anything else now
        // terminate recursion
    }

    private OperationInfo getHeadCandidateForExecute() {
        if (!operationQueue.isEmpty()) {
            // there is at least one element in the queue
            OperationInfo head = this.operationQueue.peek();
            if (supervisor.canExecuteOperation(head)) {
                return head;
            }
        } // else:
        return null;
    }

    @Override
    public void blockProcessing(Action0 onBlockCompleted) {
        supervisor.block(onBlockCompleted);
    }

    @Override
    public void unblockProcessing() {
        if (supervisor.unblock()) {
            // there might be pending tasks to execute
            checkExecute();
        }
    }

    @Override
    public YesNoAsync isProcessingBlocked() {
        return supervisor.isBlocked();
    }

    @Override
    public int limitOperationExecutionByPriority(Priority priority, int limit) {
        if (Constants.DEBUG) {
            log.d("limitOperationExecutionByPriority: " + "priority = [" + priority + "], limit = [" + limit + "]");
        }
        int i = supervisor.setPriorityLimit(priority, limit);
        // check
        if (limit > i) {
            // we've raised the limit, maybe there is an operation to be executed
            checkExecute();
        }
        return i;
    }

    @Override
    public void onOperationFinished(Token operationToken) {
        // find the operation in the queue
        if (Constants.DEBUG) {
            log.d("onOperationFinished: " + "operationToken = [" + operationToken + "]");
        }
        _onOperationFinished((TokenImpl) operationToken, 0);
    }

    @Override
    public void onOperationFinished(Token operationToken,
                                    long delayBeforeSameResourceOperation) {
        if (Constants.DEBUG) {
            log.d("onOperationFinished: " + "operationToken = [" + operationToken + "], delayBeforeSameResourceOperation = [" + delayBeforeSameResourceOperation + "]");
        }
        _onOperationFinished((TokenImpl) operationToken, delayBeforeSameResourceOperation);
    }

    private void _onOperationFinished(TokenImpl operationToken,
                                      long delayBeforeSameResourceOperation) {
        // let the supervisor know
        supervisor.onOperationFinished(operationToken.oi);
        String resource = operationToken.oi.resource;
        if (delayBeforeSameResourceOperation == 0) {
            earliestResourceOperationSysTime.remove(resource);
        } else {
            // note down the next operation execution time
            earliestResourceOperationSysTime.put(resource, SystemClock.uptimeMillis() + delayBeforeSameResourceOperation);
        }
        // can we perform the next operation in the queue?
        checkExecute();
    }

    private void doLaunchOperation(OperationInfo oi) {
        if (Constants.DEBUG) {
            log.d("executing operation (token=" + oi.token + ")");
        }
        // let the supervisor know
        supervisor.onOperationStarting(oi);
        // set up oi execution flags
        oi.executing = true;
        oi.executable.call(oi.token);
    }

}
