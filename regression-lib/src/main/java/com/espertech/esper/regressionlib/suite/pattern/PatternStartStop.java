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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.util.SafeIterator;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

public class PatternStartStop {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new PatternStartStopOne());
        execs.add(new PatternAddRemoveListener());
        execs.add(new PatternStartStopTwo());
        return execs;
    }

    private static class PatternStartStopTwo implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select * from pattern [every(a=SupportBean or b=SupportBeanComplexProps)]";
            EPCompiled compiled = env.compile(stmtText);
            env.deploy(compiled).addListener("s0");

            for (int i = 0; i < 100; i++) {
                sendAndAssert(env);

                SupportListener listener = env.listener("s0");
                listener.reset();
                env.undeployModuleContaining("s0");

                env.sendEventBean(new SupportBean());
                env.sendEventBean(SupportBeanComplexProps.makeDefaultBean());
                assertFalse(listener.isInvoked());

                env.deploy(compiled).addListener("s0");
            }

            env.undeployAll();
        }
    }

    private static class PatternStartStopOne implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') @IterableUnbound select * from pattern[every tag=SupportBean]";
            EPCompiled compiled = env.compile(epl);
            env.deploy(compiled).addListener("s0");
            EPStatement stmt = env.statement("s0");
            assertEquals(StatementType.SELECT, stmt.getProperty(StatementProperty.STATEMENTTYPE));
            assertNull(stmt.getProperty(StatementProperty.CONTEXTNAME));
            assertNull(stmt.getProperty(StatementProperty.CONTEXTDEPLOYMENTID));

            // Pattern started when created
            TestCase.assertFalse(env.statement("s0").iterator().hasNext());
            SafeIterator<EventBean> safe = env.statement("s0").safeIterator();
            TestCase.assertFalse(safe.hasNext());
            safe.close();

            // Stop pattern
            SupportListener listener = env.listener("s0");
            listener.reset();

            env.undeployModuleContaining("s0");
            sendEvent(env);
            assertFalse(listener.isInvoked());

            // Start pattern
            env.deploy(compiled).addListener("s0");
            assertFalse(env.iterator("s0").hasNext());

            // Send event
            SupportBean theEvent = sendEvent(env);
            assertSame(theEvent, env.iterator("s0").next().get("tag"));
            safe = env.statement("s0").safeIterator();
            TestCase.assertSame(theEvent, safe.next().get("tag"));
            safe.close();

            // Stop pattern
            listener = env.listener("s0");
            listener.reset();
            stmt = env.statement("s0");
            env.undeployModuleContaining("s0");
            sendEvent(env);
            try {
                stmt.iterator();
            } catch (IllegalStateException ex) {
                assertEquals("Statement has already been undeployed", ex.getMessage());
            }
            assertFalse(listener.isInvoked());

            // Start again, iterator is zero
            env.deploy(compiled);
            assertFalse(env.iterator("s0").hasNext());
            env.undeployAll();
        }
    }

    private static class PatternAddRemoveListener implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') @IterableUnbound select * from pattern[every tag=SupportBean]";
            env.compileDeploy(epl);

            // Pattern started when created

            // Add listener
            SupportUpdateListener listener = new SupportUpdateListener();
            env.statement("s0").addListener(listener);
            assertNull(env.listener("s0").getLastNewData());
            TestCase.assertFalse(env.iterator("s0").hasNext());

            // Send event
            SupportBean theEvent = sendEvent(env);
            assertEquals(theEvent, listener.getAndResetLastNewData()[0].get("tag"));
            TestCase.assertSame(theEvent, env.statement("s0").iterator().next().get("tag"));

            // Remove listener
            env.statement("s0").removeListener(listener);
            theEvent = sendEvent(env);
            TestCase.assertSame(theEvent, env.iterator("s0").next().get("tag"));
            assertNull(listener.getLastNewData());

            // Add listener back
            env.statement("s0").addListener(listener);
            theEvent = sendEvent(env);
            TestCase.assertSame(theEvent, env.iterator("s0").next().get("tag"));
            assertEquals(theEvent, listener.getAndResetLastNewData()[0].get("tag"));

            env.undeployAll();
        }
    }

    private static void sendAndAssert(RegressionEnvironment env) {
        for (int i = 0; i < 1000; i++) {
            Object theEvent = null;
            if (i % 3 == 0) {
                theEvent = new SupportBean();
            } else {
                theEvent = SupportBeanComplexProps.makeDefaultBean();
            }

            env.sendEventBean(theEvent);

            EventBean eventBean = env.listener("s0").assertOneGetNewAndReset();
            if (theEvent instanceof SupportBean) {
                TestCase.assertSame(theEvent, eventBean.get("a"));
                assertNull(eventBean.get("b"));
            } else {
                TestCase.assertSame(theEvent, eventBean.get("b"));
                assertNull(eventBean.get("a"));
            }
        }
    }

    private static SupportBean sendEvent(RegressionEnvironment env) {
        SupportBean theEvent = new SupportBean();
        env.sendEventBean(theEvent);
        return theEvent;
    }
}
