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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class PatternRepeatRouteEvent {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternRouteSingle());
        execs.add(new PatternRouteCascade());
        execs.add(new PatternRouteTimer());
        return execs;
    }

    /**
     * Test route of an event within a env.listener("s0").
     * The Listener when it receives an event will generate a single new event
     * that it routes back into the eventService, up to X number of times.
     */
    private static class PatternRouteSingle implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[every tag=SupportBean]";
            env.compileDeploy(epl);

            SingleRouteUpdateListener listener = new SingleRouteUpdateListener(env.runtime());
            env.statement("s0").addListener(listener);

            // Send first event that triggers the loop
            sendEvent(env.runtime(), 0);

            // Should have fired X times
            assertEquals(1000, listener.getCount());

            env.undeployAll();
        }
    }

    /**
     * Test route of multiple events within a env.listener("s0").
     * The Listener when it receives an event will generate multiple new events
     * that it routes back into the eventService, up to X number of times.
     */
    private static class PatternRouteCascade implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select * from pattern[every tag=SupportBean]";
            env.compileDeploy(epl);

            CascadeRouteUpdateListener listener = new CascadeRouteUpdateListener(env.runtime());
            env.statement("s0").addListener(listener);

            // Send first event that triggers the loop
            sendEvent(env.runtime(), 2);       // the 2 translates to number of new events routed

            // Should have fired X times
            assertEquals(9, listener.getCountReceived());
            assertEquals(8, listener.getCountRouted());

            //  Num    Received         Routes      Num
            //  2             1           2         3
            //  3             2           6         4
            //  4             6             -

            env.undeployAll();
        }
    }

    private static class PatternRouteTimer implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);

            String epl = "@name('s0') select * from pattern[every tag=SupportBean]";
            SingleRouteUpdateListener eventListener = new SingleRouteUpdateListener(env.runtime());
            env.compileDeploy(epl).statement("s0").addListener(eventListener);

            epl = "@name('s1') select * from pattern[every timer:at(*,*,*,*,*,*)]";
            SingleRouteUpdateListener timeListener = new SingleRouteUpdateListener(env.runtime());
            env.compileDeploy(epl).statement("s1").addListener(timeListener);

            assertEquals(0, timeListener.getCount());
            assertEquals(0, eventListener.getCount());

            env.advanceTime(10000);

            assertEquals(1, timeListener.getCount());
            assertEquals(1000, eventListener.getCount());

            env.undeployAll();
        }
    }

    private static void sendEvent(EPRuntime runtime, int intValue) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intValue);
        runtime.getEventService().sendEventBean(theEvent, theEvent.getClass().getSimpleName());
    }

    private static void routeEvent(EPRuntime runtime, int intValue) {
        SupportBean theEvent = new SupportBean();
        theEvent.setIntPrimitive(intValue);
        runtime.getEventService().routeEventBean(theEvent, theEvent.getClass().getSimpleName());
    }

    private static class SingleRouteUpdateListener implements UpdateListener {

        private final EPRuntime runtime;

        public SingleRouteUpdateListener(EPRuntime runtime) {
            this.runtime = runtime;
        }

        private int count = 0;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            count++;
            if (count < 1000) {
                routeEvent(this.runtime, 0);
            }
        }

        public int getCount() {
            return count;
        }
    }

    private static class CascadeRouteUpdateListener implements UpdateListener {

        private final EPRuntime runtime;

        public CascadeRouteUpdateListener(EPRuntime runtime) {
            this.runtime = runtime;
        }

        private int countReceived = 0;
        private int countRouted = 0;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            countReceived++;
            SupportBean theEvent = (SupportBean) (newEvents[0].get("tag"));
            int numNewEvents = theEvent.getIntPrimitive();

            for (int i = 0; i < numNewEvents; i++) {
                if (numNewEvents < 4) {
                    routeEvent(this.runtime, numNewEvents + 1);
                    countRouted++;
                }
            }
        }

        int getCountReceived() {
            return countReceived;
        }

        int getCountRouted() {
            return countRouted;
        }
    }
}
