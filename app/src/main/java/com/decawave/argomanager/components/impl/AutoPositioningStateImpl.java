/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import android.support.annotation.NonNull;

import com.annimon.stream.function.BiFunction;
import com.annimon.stream.function.Function;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.components.AutoPositioningState;
import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.kryl.android.common.log.ComponentLog;
import rx.functions.Action0;

/**
 * Argo project.
 */
class AutoPositioningStateImpl implements AutoPositioningState {
    private static final ComponentLog log = new ComponentLog(AutoPositioningStateImpl.class);
    // members
    private Map<Long,CompositeState> nodeStateMap;
    // caches
    private OverallState overallStateStateCache;
    private TaskState initiatorCheckStateCache;
    private TaskState nodeDistanceCollectionStateCache;
    private TaskState nodePositionSaveStateCache;
    //
    private final StateListener listener;

    private class CompositeState {
        TaskState initiatorCheckState;
        TaskState distanceCollectionState;
        TaskState positionSaveState;

        CompositeState() {
            reset();
        }

        TaskState setDistanceCollectionState(TaskState distanceCollectionState) {
            TaskState oldState = this.distanceCollectionState;
            this.distanceCollectionState = distanceCollectionState;
            return oldState;
        }

        TaskState setPositionSaveState(TaskState positionSaveState) {
            TaskState oldState = this.positionSaveState;
            this.positionSaveState = positionSaveState;
            return oldState;
        }

        TaskState setInitiatorCheckState(TaskState initiatorCheckState) {
            TaskState oldState = this.initiatorCheckState;
            this.initiatorCheckState = initiatorCheckState;
            return oldState;
        }

        TaskState getDistanceCollectionState() {
            return distanceCollectionState;
        }

        TaskState getInitiatorCheckState() {
            return initiatorCheckState;
        }

        TaskState getPositionSaveState() {
            return positionSaveState;
        }
        
        NodeState getRunningState() {
            if (initiatorCheckState == TaskState.RUNNING) {
                return NodeState.CHECKING_INITIATOR;
            } else if (distanceCollectionState == TaskState.RUNNING) {
                return NodeState.COLLECTING_DISTANCES;
            } else if (positionSaveState == TaskState.RUNNING) {
                return NodeState.SAVING_POSITION;
            } else {
                return NodeState.IDLE;
            }
        }

        public void reset() {
            this.distanceCollectionState = TaskState.NOT_STARTED;
            this.positionSaveState = TaskState.NOT_STARTED;
            this.initiatorCheckState = TaskState.NOT_STARTED;
        }

        @Override
        public String toString() {
            return "CompositeState{" +
                    "distanceCollectionState=" + distanceCollectionState +
                    ", positionSaveState=" + positionSaveState +
                    '}';
        }
    }


    interface StateListener {

        void onOverallStateChanged(OverallState overallStateStatus);

        void onNodeStatusChanged(long nodeId);

        void onReset();

    }

    AutoPositioningStateImpl(@NotNull StateListener listener) {
        this.listener = listener;
        doReset(null);
    }

    @Override
    public TaskState getInitiatorCheckState() {
        if (initiatorCheckStateCache != null) {
            return initiatorCheckStateCache;
        }
        return initiatorCheckStateCache = computeTaskState(CompositeState::getInitiatorCheckState);
    }

    @Override
    public TaskState getDistanceCollectionState() {
        if (nodeDistanceCollectionStateCache != null) {
            return nodeDistanceCollectionStateCache;
        }
        return nodeDistanceCollectionStateCache = computeTaskState(CompositeState::getDistanceCollectionState);
    }

    @Override
    public TaskState getPositionSaveState() {
        if (nodePositionSaveStateCache != null) {
            return nodePositionSaveStateCache;
        }
        return nodePositionSaveStateCache = computeTaskState(CompositeState::getPositionSaveState);
    }

    @NonNull
    private TaskState computeTaskState(Function<CompositeState,TaskState> stateGetFunction) {
        if (nodeStateMap.isEmpty()) {
            // when we have no nodes, we are pretending that we have not started yet
            return TaskState.NOT_STARTED;
        }
        boolean stateFlags[] = new boolean[TaskState.values().length];
        // initialize to false
        for (int i = 0; i < stateFlags.length; i++) {
            stateFlags[i] = false;
        }
        for (CompositeState compositeState : nodeStateMap.values()) {
            TaskState taskState = stateGetFunction.apply(compositeState);
            stateFlags[taskState.ordinal()] = true;
        }
        // RUNNING has the highest priority
        if (stateFlags[TaskState.RUNNING.ordinal()]) {
            // if there is at least one RUNNING we are considered RUNNING (TERMINATION is atomic from state POV, but launch not)
            return TaskState.RUNNING;
        }
        // else: there is not a single RUNNING task
        if (stateFlags[TaskState.TERMINATED.ordinal()]) {
            // if there is at least one terminated, we are considered TERMINATED
            return TaskState.TERMINATED;
        }
        // else: there is not a single TERMINATED or RUNNING task
        if (stateFlags[TaskState.FAILED.ordinal()]) {
            // if there is at least one failed, we are considered FAILED
            return TaskState.FAILED;
        }
        // else: there is not a single TERMINATED, RUNNING or FAILED task
        if (stateFlags[TaskState.SUCCESS.ordinal()]) {
            // there still might be some NOT_STARTED - if the given action is useless to perform on the node
            // for example if we are saving positions, and the freshly computed position is the same
            // as the already saved one
            // all are considered success
            return TaskState.SUCCESS;
        }
        // all tasks are NOT_STARTED
        if (stateFlags[TaskState.NOT_STARTED.ordinal()]) {
            return TaskState.NOT_STARTED;
        }
        // else:
        throw new IllegalStateException("strange, FIXME!");
    }

    @Override
    public OverallState getOverallState() {
        if (overallStateStateCache != null) {
            return overallStateStateCache;
        }
        if (getInitiatorCheckState() == TaskState.RUNNING) {
            return overallStateStateCache = OverallState.CHECKING_INITIATOR;
        } else if (getDistanceCollectionState() == TaskState.RUNNING) {
            return overallStateStateCache = OverallState.COLLECTING_DISTANCES;
        } else if (getPositionSaveState() == TaskState.RUNNING) {
            return overallStateStateCache = OverallState.SAVING_POSITION;
        } else {
            return overallStateStateCache = OverallState.IDLE;
        }
    }

    @Override
    public NodeState getRunningNodeState(long nodeId) {
        CompositeState nodeState = nodeStateMap.get(nodeId);
        return nodeState == null ? null : nodeState.getRunningState();
    }

    @Override
    public TaskState getInitiatorCheckState(long nodeId) {
        CompositeState state = nodeStateMap.get(nodeId);
        return state != null ? state.getInitiatorCheckState() : null;
    }

    @Override
    public TaskState getNodeDistanceCollectionState(long nodeId) {
        CompositeState state = nodeStateMap.get(nodeId);
        return state != null ? state.getDistanceCollectionState() : null;
    }

    @Override
    public TaskState getNodePositionSaveState(long nodeId) {
        CompositeState state = nodeStateMap.get(nodeId);
        return state != null ? state.getPositionSaveState() : null;
    }

    interface TaskStateSupplier {

        TaskState getState();

    }

    @Override
    public void setNodeDistanceCollectionState(long nodeId, TaskState state) {
        if (Constants.DEBUG) {
            log.d("setNodeDistanceCollectionState: " + "nodeId = [" + nodeId + "], state = [" + state + "]");
        }
        genericSetNodeState(nodeId, state, this::getDistanceCollectionState,
                () -> nodeDistanceCollectionStateCache = null, CompositeState::setDistanceCollectionState);
    }

    @Override
    public void setNodePositionSaveState(long nodeId, TaskState state) {
        if (Constants.DEBUG) {
            log.d("setNodePositionSaveState: " + "nodeId = [" + nodeId + "], state = [" + state + "]");
        }
        genericSetNodeState(nodeId, state, this::getPositionSaveState,
                () -> nodePositionSaveStateCache = null, CompositeState::setPositionSaveState);
    }

    @Override
    public void setInitiatorCheckState(long nodeId, TaskState state) {
        if (Constants.DEBUG) {
            log.d("setInitiatorCheckState: " + "nodeId = [" + nodeId + "], state = [" + state + "]");
        }
        genericSetNodeState(nodeId, state, this::getInitiatorCheckState,
                () -> initiatorCheckStateCache = null, CompositeState::setInitiatorCheckState);
    }

    private void genericSetNodeState(long nodeId, TaskState state, TaskStateSupplier taskStateSupplier,
                                     Action0 taskStateCacheInvalidation, BiFunction<CompositeState, TaskState, TaskState> stateSetMethod) {
        TaskState oldDcs = taskStateSupplier.getState();
        OverallState oldOverallStateState = getOverallState();
        boolean change = setNodeTaskState(nodeId, state, stateSetMethod);
        if (change) {
            if (oldDcs != state) {
                taskStateCacheInvalidation.call();
                // this might lead to change of state
                TaskState newDcs = taskStateSupplier.getState();
                if (oldDcs != newDcs) {
                    // this really led to change of state (but not necessarily overall state)
                    overallStateStateCache = null;
                    OverallState newOverallState = getOverallState();
                    if (newOverallState != oldOverallStateState) {
                        // it led to change of overall state
                        listener.onOverallStateChanged(newOverallState);
                    }
                }
            }
            listener.onNodeStatusChanged(nodeId);
        }
    }

    private boolean setNodeTaskState(long nodeId,
                                  TaskState state,
                                  BiFunction<CompositeState, TaskState, TaskState> stateSetMethod) {
        if (Constants.DEBUG) {
            Preconditions.checkState(state != TaskState.NOT_STARTED, "should not explicitly set NOT_STARTED state");
        }
        CompositeState compositeState = nodeStateMap.get(nodeId);
        TaskState oldState = stateSetMethod.apply(compositeState, state);
        if (Constants.DEBUG) {
            Preconditions.checkNotNull(oldState, "you should initialize node " + nodeId + " first (use reset() function)");
        }
        return state != oldState;
    }

    @Override
    public void reset(@NotNull Set<Long> newNodeIds) {
        if (Constants.DEBUG) {
            log.d("reset() called with: " + "newNodeIds = [" + newNodeIds + "]");
            Preconditions.checkNotNull(newNodeIds);
        }
        doReset(newNodeIds);
        this.listener.onReset();
    }

    @Override
    public void resetStates() {
        for (Map.Entry<Long, CompositeState> entry : nodeStateMap.entrySet()) {
            entry.getValue().reset();
            listener.onNodeStatusChanged(entry.getKey());
        }
    }

    private void doReset(Set<Long> newNodeIds) {
        this.nodeStateMap = new HashMap<>();
        // initialize the map
        if (newNodeIds != null) {
            for (Long nodeId : newNodeIds) {
                // initialize to NOT_STARTED
                nodeStateMap.put(nodeId, new CompositeState());
            }
        }
        // invalidate caches
        this.overallStateStateCache = null;
        this.nodeDistanceCollectionStateCache = null;
        this.nodePositionSaveStateCache = null;
    }

    @Override
    public void terminateOngoingOperation() {
        if (Constants.DEBUG) log.d("terminateOngoingOperation()");
        boolean changeInitiatorCheck = false;
        boolean changePositionSave = false;
        boolean changeDistanceCollection = false;
        OverallState oldOverallState = getOverallState();
        for (CompositeState compositeState : nodeStateMap.values()) {
            if (compositeState.initiatorCheckState == TaskState.RUNNING) {
                compositeState.initiatorCheckState = TaskState.TERMINATED;
                changeInitiatorCheck = true;
            }
            if (compositeState.positionSaveState == TaskState.RUNNING) {
                compositeState.positionSaveState = TaskState.TERMINATED;
                changePositionSave = true;
            }
            if (compositeState.distanceCollectionState == TaskState.RUNNING) {
                compositeState.distanceCollectionState = TaskState.TERMINATED;
                changeDistanceCollection = true;
            }
        }
        if (changeInitiatorCheck) {
            initiatorCheckStateCache = null;
        }
        if (changePositionSave) {
            nodePositionSaveStateCache = null;
        }
        if (changeDistanceCollection) {
            nodeDistanceCollectionStateCache = null;
        }
        overallStateStateCache = null;
        OverallState newOverallState = getOverallState();
        if (oldOverallState != newOverallState) {
            // call the listener
            listener.onOverallStateChanged(newOverallState);
        }
    }

}
