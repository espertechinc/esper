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
package com.espertech.esper.regressionlib.support.filter;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.scopetest.SupportListener;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class FilterTestMultiStmtExecution implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(FilterTestMultiStmtExecution.class);

    private final FilterTestMultiStmtCase theCase;
    private final String testCaseName;

    public FilterTestMultiStmtExecution(Class originator, FilterTestMultiStmtCase theCase) {
        this.theCase = theCase;
        this.testCaseName = originator.getSimpleName() + " permutation " + Arrays.toString(theCase.getFilters());
    }

    @Override
    public String name() {
        return testCaseName;
    }

    @Override
    public String[] milestoneStats() {
        return new String[]{theCase.getStats()};
    }

    public void run(RegressionEnvironment env) {

        AtomicInteger milestone = new AtomicInteger();
        boolean[] existingStatements = new boolean[theCase.getFilters().length];
        boolean[] startedStatements = new boolean[theCase.getFilters().length];
        SupportListener[] initialListeners = new SupportListener[theCase.getFilters().length];

        // create statements
        for (int i = 0; i < theCase.getFilters().length; i++) {
            String filter = theCase.getFilters()[i];
            String stmtName = "S" + i;
            String epl = "@Name('" + stmtName + "') select * from SupportBean(" + filter + ")";
            env.compileDeploy(epl).addListener(stmtName);
            existingStatements[i] = true;
            startedStatements[i] = true;
            initialListeners[i] = env.listener(stmtName);

            try {
                assertSendEvents(existingStatements, startedStatements, initialListeners, env, theCase.getItems());
            } catch (AssertionError ex) {
                String message = "Failed after create stmt " + i + " and before milestone P" + milestone.get();
                log.error(message, ex);
                throw new AssertionError(message, ex);
            }

            env.milestone(milestone.getAndIncrement());

            try {
                assertSendEvents(existingStatements, startedStatements, initialListeners, env, theCase.getItems());
            } catch (AssertionError ex) {
                throw new AssertionError("Failed after create stmt " + i + " and after milestone P" + milestone.get(), ex);
            }
        }

        // stop statements
        for (int i = 0; i < theCase.getFilters().length; i++) {
            String stmtName = "S" + i;
            env.undeployModuleContaining(stmtName);
            startedStatements[i] = false;

            try {
                assertSendEvents(existingStatements, startedStatements, initialListeners, env, theCase.getItems());
            } catch (AssertionError ex) {
                throw new AssertionError("Failed after stop stmt " + i + " and before milestone P" + milestone.get(), ex);
            }

            env.milestone(milestone.get());

            try {
                assertSendEvents(existingStatements, startedStatements, initialListeners, env, theCase.getItems());
            } catch (RuntimeException ex) {
                throw new RuntimeException("Failed after stop stmt " + i + " and after milestone P" + milestone.get(), ex);
            } catch (AssertionError ex) {
                throw new RuntimeException("Failed after stop stmt " + i + " and after milestone P" + milestone.get(), ex);
            }

            milestone.getAndIncrement();
        }

        // destroy statements
        env.undeployAll();
    }

    private static void assertSendEvents(boolean[] existingStatements, boolean[] startedStatements, SupportListener[] initialListeners, RegressionEnvironment env, List<FilterTestMultiStmtAssertItem> items) {
        int eventNum = -1;
        for (FilterTestMultiStmtAssertItem item : items) {
            eventNum++;
            env.sendEventBean(item.getBean());
            String message = "Failed at event " + eventNum;

            if (item.getExpectedPerStmt().length != startedStatements.length) {
                Assert.fail("Number of boolean expected-values not matching number of statements for item " + eventNum);
            }

            for (int i = 0; i < startedStatements.length; i++) {
                String stmtName = "S" + i;
                if (!existingStatements[i]) {
                    assertNull(message, env.statement(stmtName));
                } else {
                    if (!startedStatements[i]) {
                        assertNull(env.statement(stmtName));
                        assertFalse(initialListeners[i].getAndClearIsInvoked());
                    } else if (!item.getExpectedPerStmt()[i]) {
                        SupportListener listener = env.listener(stmtName);
                        assertFalse(message, listener.getAndClearIsInvoked());
                    } else {
                        SupportListener listener = env.listener(stmtName);
                        Assert.assertTrue(message, listener.isInvoked());
                        Assert.assertSame(message, item.getBean(), listener.assertOneGetNewAndReset().getUnderlying());
                    }
                }
            }
        }
    }
}