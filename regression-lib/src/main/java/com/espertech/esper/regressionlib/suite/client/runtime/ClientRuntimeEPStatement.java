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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.context.ContextPartitionSelectorAll;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ClientRuntimeEPStatement {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimeEPStatementListenerWReplay());
        execs.add(new ClientRuntimeEPStatementAlreadyDestroyed());
        return execs;
    }

    private static class ClientRuntimeEPStatementListenerWReplay implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportBean#length(2)");
            SupportUpdateListener listener = new SupportUpdateListener();

            // test empty statement
            env.statement("s0").addListenerWithReplay(listener);
            assertTrue(listener.isInvoked());
            assertEquals(1, listener.getNewDataList().size());
            assertNull(listener.getNewDataList().get(0));
            listener.reset();

            env.sendEventBean(new SupportBean("E1", 1));
            assertEquals("E1", listener.assertOneGetNewAndReset().get("theString"));
            env.undeployAll();
            listener.reset();

            // test 1 event
            env.compileDeploy("@name('s0') select * from SupportBean#length(2)");
            env.sendEventBean(new SupportBean("E1", 1));
            env.statement("s0").addListenerWithReplay(listener);
            assertEquals("E1", listener.assertOneGetNewAndReset().get("theString"));
            env.undeployAll();
            listener.reset();

            // test 2 events
            env.compileDeploy("@name('s0') select * from SupportBean#length(2)");
            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(new SupportBean("E2", 1));
            env.statement("s0").addListenerWithReplay(listener);
            EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), new String[]{"theString"}, new Object[][]{{"E1"}, {"E2"}});
            EPStatement stmt = env.statement("s0");
            env.undeployAll();
            listener.reset();
        }
    }

    private static class ClientRuntimeEPStatementAlreadyDestroyed implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportBean");
            EPStatement statement = env.statement("s0");
            env.undeployAll();
            assertTrue(statement.isDestroyed());
            tryInvalid(statement, stmt -> stmt.iterator());
            tryInvalid(statement, stmt -> stmt.iterator(new ContextPartitionSelectorAll()));
            tryInvalid(statement, stmt -> stmt.safeIterator());
            tryInvalid(statement, stmt -> stmt.safeIterator(new ContextPartitionSelectorAll()));
            tryInvalid(statement, stmt -> stmt.addListenerWithReplay(new SupportUpdateListener()));
            tryInvalid(statement, stmt -> stmt.addListener(new SupportUpdateListener()));
            tryInvalid(statement, stmt -> stmt.setSubscriber(this));
            tryInvalid(statement, stmt -> stmt.setSubscriber(this, "somemethod"));
        }
    }

    private static void tryInvalid(EPStatement stmt, Consumer<EPStatement> consumer) {
        try {
            consumer.accept(stmt);
            fail();
        } catch (IllegalStateException ex) {
            assertEquals(ex.getMessage(), "Statement has already been undeployed");
        }
    }
}
