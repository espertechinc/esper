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
package com.espertech.esperio;

import com.espertech.esper.adapter.AdapterState;
import com.espertech.esper.adapter.AdapterStateManager;
import com.espertech.esper.adapter.IllegalStateTransitionException;
import junit.framework.TestCase;

public class TestFeedStateManager extends TestCase {
    private AdapterStateManager stateManager;

    public void testValidTransitionsFromOpened() {
        open();
        start();

        open();
        destroy();
    }

    public void testValidTransitionsFromStarted() {
        open();
        start();
        stop();

        open();
        start();
        pause();

        open();
        start();
        destroy();
    }

    public void testValidTransitionsFromPaused() {
        open();
        start();
        pause();
        stop();

        open();
        start();
        pause();
        destroy();

        open();
        start();
        pause();
        resume();
    }

    public void testInvalidTransitionsFromOpened() {
        open();

        failOnStop();
        failOnPause();
        failOnResume();
    }

    public void testInvalidTransitionsFromStarted() {
        open();
        start();

        failOnStart();
        failOnResume();
    }

    public void testInvalidTransitionsFromPaused() {
        open();
        start();
        pause();

        failOnStart();
        failOnPause();
    }

    public void testInvalidTransitionsFromDestroyed() {
        open();
        destroy();

        failOnStart();
        failOnStop();
        failOnPause();
        failOnResume();
        failOnDestroy();
    }

    private void failOnDestroy() {
        try {
            stateManager.destroy();
            fail();
        } catch (IllegalStateTransitionException ex) {
            // Expected
        }
    }

    private void failOnStart() {
        try {
            stateManager.start();
            fail();
        } catch (IllegalStateTransitionException ex) {
            // Expected
        }
    }

    private void failOnResume() {
        try {
            stateManager.resume();
            fail();
        } catch (IllegalStateTransitionException ex) {
            // Expected
        }
    }

    private void failOnPause() {
        try {
            stateManager.pause();
            fail();
        } catch (IllegalStateTransitionException ex) {
            // Expected
        }
    }

    private void failOnStop() {
        try {
            stateManager.stop();
            fail();
        } catch (IllegalStateTransitionException ex) {
            // Expected
        }
    }

    private void start() {
        stateManager.start();
        assertEquals(AdapterState.STARTED, stateManager.getState());
    }

    private void open() {
        stateManager = new AdapterStateManager();
        assertEquals(AdapterState.OPENED, stateManager.getState());
    }

    private void destroy() {
        stateManager.destroy();
        assertEquals(AdapterState.DESTROYED, stateManager.getState());
    }

    private void stop() {
        stateManager.stop();
        assertEquals(AdapterState.OPENED, stateManager.getState());
    }

    private void pause() {
        stateManager.pause();
        assertEquals(AdapterState.PAUSED, stateManager.getState());
    }

    private void resume() {
        stateManager.resume();
        assertEquals(AdapterState.STARTED, stateManager.getState());
    }
}
