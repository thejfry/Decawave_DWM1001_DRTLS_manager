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

import com.google.common.base.Preconditions;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.kryl.android.common.Constants;
import eu.kryl.android.common.Pair;
import eu.kryl.android.common.fsm.FiniteStateMachine;
import eu.kryl.android.common.fsm.OnEnterHandler;
import eu.kryl.android.common.fsm.OnLeftHandler;
import eu.kryl.android.common.fsm.OnTransitionHandler;
import eu.kryl.android.common.log.ComponentLog;

public class FiniteStateMachineImpl<E extends Enum<E>> implements FiniteStateMachine<E> {
    // static variables
    private static boolean directTransitionAllowed = true;
    private static boolean executePostponedTransitions = true;
    private static Set<FiniteStateMachine<?>> postponedTransitionsFsms = new HashSet<>();
    private static List<Runnable> postponedTransitions = new LinkedList<>();
    // members
    private Map<E, List<OnEnterHandler<E>>> onEnterHandlers;
    private Map<E, List<OnLeftHandler<E>>> onLeftHandlers;
    private List<OnLeftHandler<E>> onLeftRuntimeHandlers;
    private Map<Pair<E,E>, List<OnTransitionHandler<E>>> onTransitionHandlers;
    private List<OnTransitionHandler<E>> onStateChangedHandlers;
    private E state;
    protected FsmScheduler<E> scheduler;
    private final boolean selfManaging;
    protected final ComponentLog log;
    protected final String name;
    // variables
    Map<String, Object> variables = new HashMap<String, Object>();


    public FiniteStateMachineImpl(String fsmName, Class<E> enumCls) {
        this(fsmName, enumCls, null);
    }

    public FiniteStateMachineImpl(String fsmName, Class<E> enumCls, ComponentLog log) {
        this(fsmName, enumCls, false, null, log);
    }

    public ComponentLog getLog() {
        return log;
    }

    @Override
    public void setVar(String name, Object value) {
        Preconditions.checkNotNull(value);
        Preconditions.checkNotNull(name);
        if (Constants.DEBUG) {
            log.d("set " + name + "=" + value);
        }
        variables.put(name, value);
    }
    
    @Override
    public void resetVar(String name) {
        if (Constants.DEBUG) {
            log.d("clear " + name);
        }
        variables.remove(name);
    }
    
    @Override
    public boolean getVarBool(String name) {
        final Object r = variables.get(name);
        return r != null ? (Boolean) r : false;
    }
    
    @Override
    public String getVarStr(String name) {
        return (String) variables.get(name);
    }
    
    @Override
    public Integer getVarInt(String name) {
        return (Integer) variables.get(name);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getVar(String name) {
        return (T) variables.get(name);
    }
    
    @Override
    public boolean isVarSet(String name) {
        return variables.containsKey(name);
    }
    
    public FiniteStateMachineImpl(String fsmName, Class<E> enumCls, boolean selfManagaginStates, E initialState, ComponentLog log) {
        this.name = fsmName;
        this.onEnterHandlers = new EnumMap<E, List<OnEnterHandler<E>>>(enumCls);
        this.onLeftHandlers = new EnumMap<E, List<OnLeftHandler<E>>>(enumCls);
        this.onTransitionHandlers = new HashMap<Pair<E,E>, List<OnTransitionHandler<E>>>();
        this.onStateChangedHandlers = new LinkedList<OnTransitionHandler<E>>();
        this.onLeftRuntimeHandlers = new LinkedList<OnLeftHandler<E>>();
        this.log = log != null ? log : new ComponentLog("fsm." + fsmName);
        this.scheduler = new FsmScheduler<>(this.log, enumCls);
        this.state = null;
        this.selfManaging = selfManagaginStates;
        if (initialState != null) {
            _setState(initialState);
        }
    }
    
    private List<OnEnterHandler<E>> _getOnEnterHandlersForState(E state) {
        List<OnEnterHandler<E>> list = onEnterHandlers.get(state);
        if (list == null) {
            list = new LinkedList<OnEnterHandler<E>>();
            onEnterHandlers.put(state, list);
        }
        return list;
    }
    
    private List<OnLeftHandler<E>> _getOnLeftHandlersForState(E state) {
        List<OnLeftHandler<E>> list = onLeftHandlers.get(state);
        if (list == null) {
            list = new LinkedList<OnLeftHandler<E>>();
            onLeftHandlers.put(state, list);
        }
        return list;
    }
    
    private List<OnTransitionHandler<E>> _getOnTransitionHandlers(E fromState, E toState) {
        Pair<E, E> p = new Pair<E,E>(fromState, toState);
        List<OnTransitionHandler<E>> list = onTransitionHandlers.get(p);
        if (list == null) {
            list = new LinkedList<OnTransitionHandler<E>>();
            onTransitionHandlers.put(p, list);
        }
        return list;
    }

    public void addOnStateEnteredHandler(E state, OnEnterHandler<E> handler) {
        _getOnEnterHandlersForState(state).add(handler);
    }
    
    @Override
    public void addOnTransitionHandler(E fromState, E toState, OnTransitionHandler<E> handler) {
        _getOnTransitionHandlers(fromState, toState).add(handler);
    }
    
    @Override
    public void addOnStateChangedHandler(OnTransitionHandler<E> handler) {
        onStateChangedHandlers.add(handler);
    }

    @Override
    public void addOnStateEnteredHandler(E state, final Runnable handler) {
        addOnStateEnteredHandler(state, new OnEnterHandler<E>() {

            @Override
            public void onEntered(E fromState) {
                handler.run();
            }
        });
    }
    
    @Override
    public void addOnStateLeftHandler(E state, final Runnable runnable) {
        addOnStateLeftHandler(state, new OnLeftHandler<E>() {

            @Override
            public void onLeft(E fromState) {
                runnable.run();
            }
        });
    }

    @Override
    public void addOnStateLeftHandler(E state, OnLeftHandler<E> handler) {
        _getOnLeftHandlersForState(state).add(handler);
    }
    
    
    @Override
    public void addOnStateEnteredHandler(OnEnterHandler<E> handler, E... states) {
        for (E state : states) {
            addOnStateEnteredHandler(state, handler);
        }
        
    }
    
    @Override
    public void addOnStateLeftHandler(OnLeftHandler<E> handler, E... states) {
        for (E state : states) {
            addOnStateLeftHandler(state, handler);
        }
    }
    
    @Override
    public void addOnTransitionHandler(E fromState, E toState, final Runnable handler) {
        _getOnTransitionHandlers(fromState, toState).add(new OnTransitionHandler<E>() {

            @Override
            public void onTransition(E fromState, E toState) {
                handler.run();
            }
        });
    }

    @Override
    public void addOnStateChangedHandler(final Runnable runnable) {
        onStateChangedHandlers.add(new OnTransitionHandler<E>() {

            @Override
            public void onTransition(E fromState, E toState) {
                runnable.run();
            }
        });
    }

    @Override
    public void scheduleRunnable(Runnable runnable) {
        scheduler.scheduleRunnableNow(runnable);
    }

    @Override
    public void scheduleRunnableForCurrentState(final Runnable runnable) {
        Runnable _runnable;
        // decorate runnable to be executed only if still in the same state
        final E stateWhenScheduled = getState();
        _runnable = new Runnable() {
            @Override
            public void run() {
                if (stateWhenScheduled == getState()) {
                    runnable.run();
                }
            }

        };
        scheduler.scheduleRunnableNow(_runnable);
    }

    @Override
    public void scheduleRunnable(Runnable runnable, long executeAfterMs) {
        if (executeAfterMs == 0) {
            scheduler.scheduleRunnableNow(runnable);
        } else {
            if (Constants.DEBUG)
                log.d("scheduling runnable after " + executeAfterMs);
            scheduler.scheduleRunnable(runnable, executeAfterMs);
        }
    }
    
    @Override
    public void unscheduleRunnable(Runnable runnable) {
        scheduler.unscheduleRunnable(runnable);
    }
    
    @Override
    public void scheduleOnCurrentStateKeptRunnable(Runnable runnable, long executeAfterMs, E resetState) {
        this.scheduler.scheduleStateKeptRunnable(runnable, executeAfterMs, resetState);
    }
    
    @Override
    public void scheduleOnCurrentStateLeftHandler(OnLeftHandler<E> onLeftHandler) {
        this.onLeftRuntimeHandlers.add(onLeftHandler);
    }
    
    private void onStateTransition(final E fromState, final E toState) {
        /////////////////////////////////////////////////////////////////////////////////////
        // first specific transition handlers
        if (onLeftHandlers.containsKey(fromState)) {
            for (final OnLeftHandler<E> h : _getOnLeftHandlersForState(fromState)) {
                h.onLeft(toState);
            }
        }
        if (onEnterHandlers.containsKey(toState)) {
            for (final OnEnterHandler<E> h : _getOnEnterHandlersForState(toState)) {
                h.onEntered(fromState);
            }
        }
        /////////////////////////////////////////////////////////////////////////////////////
        // now generic transition handlers
        if (onTransitionHandlers.containsKey(new Pair<E,E>(fromState, toState))) {
            for (OnTransitionHandler<E> h : _getOnTransitionHandlers(fromState, toState)) {
                h.onTransition(fromState, toState);
            }
        }
        for (OnTransitionHandler<E> h : onStateChangedHandlers) {
            h.onTransition(fromState, toState);
        }
        // now scheduled one-off/runtime handlers
        for (OnLeftHandler<E> h : onLeftRuntimeHandlers) {
            h.onLeft(toState);
        }
        onLeftRuntimeHandlers.clear();  // and clear the scheduled handlers
    }
    
    private boolean stateModificationAllowed = true;
    
    public final void setState(E newState, Object... params) {
        Preconditions.checkNotNull("FSM not active anymore", scheduler); 
        if (selfManaging) {
            throw new IllegalStateException("this FSM is configured as self-managing, it manages it's state internally");
        } // else:
        _setState(newState, params);
    }

    protected void _setState(final E newState, final Object... params) {
        Preconditions.checkState(stateModificationAllowed);
        if (directTransitionAllowed) {
            directTransitionAllowed = false;
            if (newState != state) {
                // validate transition
                if (!isTransitionValid(state, newState)) {
                    throw new IllegalStateException("illegal state transition required: " + state + "->" + newState);
                }
                // else:
                stateModificationAllowed = false;
                E oldState = state;
                state = newState;
                if (Constants.DEBUG)
                    log.d("@" + this.hashCode() + " " + oldState + " -> " + newState);
                // check scheduled states
                scheduler.onStateChanged(newState);
                // now when we have complete state injected, we can invoke internal transition/state 'handlers'
                onStateTransition(oldState, newState);
                stateModificationAllowed = true;
            }
            directTransitionAllowed = true;
            // do postponed stuff
            if (executePostponedTransitions) {
                executePostponedTransitions = false;
                // execute postponed state transitions
                while (!postponedTransitions.isEmpty()) {
                    postponedTransitions.remove(0).run();
                }
                executePostponedTransitions = true;
                // prepare for the next iteration
                postponedTransitionsFsms.clear();
            }
        } else {
            // put the transition to the queue
            // there must not be two different transitions of the same FSM within one initiating state transition
            Preconditions.checkState(!postponedTransitionsFsms.contains(this));

            final E oldState = this.state;
            final SetStateRunnable<E> runnable = new SetStateRunnable<E>(this.name, oldState, newState) {

                @Override
                public void run() {
                    if (Constants.DEBUG) {
                        log.d("@" + FiniteStateMachineImpl.this.hashCode() + " execute postponed: " + getName());
                    }

                    _setState(toState, params);
                }
            };

            if (Constants.DEBUG) {
                log.d("@" + this.hashCode() + " postpone " + runnable.getName());
            }

            postponedTransitions.add(runnable);
        }
    }
        
    // override if transition validation is required
    protected boolean isTransitionValid(E fromState, E toState) {
        return true;
    }
    
    @Override
    public final E getState() {
        return state;
    }
    
    public boolean isActive() {
        return scheduler != null;
    }
    
    @Override
    public void stop() {
        if (directTransitionAllowed) {
            _stop();
        } else {
            if (Constants.DEBUG)
                log.d("postponing stop()");
            Preconditions.checkState(!postponedTransitionsFsms.contains(this));
            postponedTransitions.add(new Runnable() {

                @Override
                public void run() {
                    _stop();
                }
                
            });
        }
    }

    public void _stop() {
        if (Constants.DEBUG)
            log.d("stop()");
        scheduler.unscheduleAll();
        scheduler = null;
        state = null;
    }

    abstract class SetStateRunnable<E> implements Runnable {
        String fsmName;
        E oldState;
        E toState;

        String getName() {
            return fsmName + ":" + oldState + "->" + toState;
        }

        public SetStateRunnable(String fsmName, E oldState, E toState) {
            this.fsmName = fsmName;
            this.oldState = oldState;
            this.toState = toState;
        }
    }
}
