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

package eu.kryl.android.common.fsm;

import org.jetbrains.annotations.Nullable;

public interface FiniteStateMachine<E extends Enum<E>> {
    
    public void setVar(String name, Object value);
    
    public void resetVar(String name);
    
    public boolean getVarBool(String name);
    
    public String getVarStr(String name);
    
    public <T> T getVar(String name);
    
    public Integer getVarInt(String name);
    
    public boolean isVarSet(String name);
    
    public void addOnStateEnteredHandler(E state, OnEnterHandler<E> handler);
    
    public void addOnStateEnteredHandler(E state, Runnable handler);
    
    public void addOnStateEnteredHandler(OnEnterHandler<E> handler, E... state);

    public void addOnStateLeftHandler(E state, OnLeftHandler<E> handler);

    public void addOnStateLeftHandler(OnLeftHandler<E> handler, E... state);

    public void addOnStateLeftHandler(E state, Runnable runnable);

    public void addOnTransitionHandler(E fromState, E toState, OnTransitionHandler<E> handler);
    
    public void addOnTransitionHandler(E fromState, E toState, Runnable handler);
    
    public void addOnStateChangedHandler(OnTransitionHandler<E> handler);
    
    public void addOnStateChangedHandler(Runnable runnable);
    
    
    /**
     * Schedules given runnable to be executed after timeout or to be cancelled
     * upon state E is entered.
     * @param runnable scheduled runnable
     * @param executeAfterMs timeout after which the runnable is executed 
     * @param resetState determines the state upon whose execution the runnable is
     * unscheduled/reset, if null the runnable is unscheduled upon transition to any state
     */
    public void scheduleOnCurrentStateKeptRunnable(Runnable runnable, long executeAfterMs, @Nullable E resetState);
    
    /**
     * Schedules on state left handler executed once the current state is left.
     * @param onLeftHandler
     */
    public void scheduleOnCurrentStateLeftHandler(OnLeftHandler<E> onLeftHandler);

    /**
     * Schedules runnable to be executed unconditionally.
     * The runnable is not executed only if FSM is stopped (see {@link #stop()}
     * 
     * @param runnable scheduled runnable
     * @param executeAfterMs after which time to execute - 0 = now
     */
    public void scheduleRunnable(Runnable runnable, long executeAfterMs);

    public void scheduleRunnable(Runnable runnable);

    /**
     * Schedules the given runnable for execution but execute only if the state is
     * still the same when runnable is about to be executed.
     * @param runnable runnable to execute
     */
    void scheduleRunnableForCurrentState(Runnable runnable);

    /**
     * Unschedules all instances of runnable previously scheduled with scheduleRunnable().
     */
    public void unscheduleRunnable(Runnable runnable);
    
    public void setState(E newState, Object... params);
    
    public E getState();
    
    /**
     * Resets internal state to null:
     * 1. without invoking transition handlers
     * 2. unschedules all scheduled runnables
     * 
     * {@link #isActive()}
     */
    public void stop();
    
    /**
     * @return whether this FSM is active (stop() has not been called yet)
     * 
     * {@link #stop()}
     */
    public boolean isActive();
    
}
