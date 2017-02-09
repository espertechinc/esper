/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.adapter;

/**
 * A utility to manage the state transitions for an InputAdapter.
 */
public class AdapterStateManager {
    private AdapterState state = AdapterState.OPENED;
    private boolean stateTransitionsAllowed = true;

    /**
     * @return the state
     */
    public AdapterState getState() {
        return state;
    }

    /**
     * Transition into the STARTED state (from the OPENED state).
     *
     * @throws IllegalStateTransitionException if the transition is not allowed
     */
    public void start() throws IllegalStateTransitionException {
        assertStateTransitionsAllowed();
        if (state != AdapterState.OPENED) {
            throw new IllegalStateTransitionException("Cannot start from the " + state + " state");
        }
        state = AdapterState.STARTED;
    }

    /**
     * Transition into the OPENED state.
     *
     * @throws IllegalStateTransitionException if the transition isn't allowed
     */
    public void stop() throws IllegalStateTransitionException {
        assertStateTransitionsAllowed();
        if (state != AdapterState.STARTED && state != AdapterState.PAUSED) {
            throw new IllegalStateTransitionException("Cannot stop from the " + state + " state");
        }
        state = AdapterState.OPENED;
    }

    /**
     * Transition into the PAUSED state.
     *
     * @throws IllegalStateTransitionException if the transition isn't allowed
     */
    public void pause() throws IllegalStateTransitionException {
        assertStateTransitionsAllowed();
        if (state != AdapterState.STARTED) {
            throw new IllegalStateTransitionException("Cannot pause from the " + state + " state");
        }
        state = AdapterState.PAUSED;
    }

    /**
     * Transition into the STARTED state (from the PAUSED state).
     *
     * @throws IllegalStateTransitionException if the state transition is not allowed
     */
    public void resume() throws IllegalStateTransitionException {
        assertStateTransitionsAllowed();
        if (state != AdapterState.PAUSED) {
            throw new IllegalStateTransitionException("Cannot resume from the " + state + " state");
        }
        state = AdapterState.STARTED;
    }

    /**
     * Transition into the DESTROYED state.
     *
     * @throws IllegalStateTransitionException if the transition isn't allowed
     */
    public void destroy() throws IllegalStateTransitionException {
        if (state == AdapterState.DESTROYED) {
            throw new IllegalStateTransitionException("Cannot destroy from the " + state + " state");
        }
        state = AdapterState.DESTROYED;
    }

    /**
     * Disallow future state changes, and throw an IllegalStateTransitionException if they
     * are attempted.
     */
    public void disallowStateTransitions() {
        stateTransitionsAllowed = false;
    }

    private void assertStateTransitionsAllowed() {
        if (!stateTransitionsAllowed) {
            throw new IllegalStateTransitionException("State transitions have been disallowed");
        }
    }
}
