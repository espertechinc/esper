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
package com.espertech.esper.regressionlib.suite.epl.insertinto;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanWithoutProps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for populating an empty type:
 * - an empty insert-into property list is allowed, i.e. "insert into EmptySchema()"
 * - an empty select-clause is not allowed, i.e. "select from xxx" fails
 * - we require "select null from" (unnamed null column) for populating an empty type
 */
public class EPLInsertIntoEmptyPropType {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLInsertIntoNamedWindowModelAfter());
        execs.add(new EPLInsertIntoCreateSchemaInsertInto());
        return execs;
    }

    private static class EPLInsertIntoNamedWindowModelAfter implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema EmptyPropSchema()", path);
            env.compileDeploy("@name('window') create window EmptyPropWin#keepall as EmptyPropSchema", path);
            env.compileDeploy("insert into EmptyPropWin() select null from SupportBean", path);

            env.sendEventBean(new SupportBean());

            env.assertIterator("window", iterator -> {
                EventBean[] events = EPAssertionUtil.iteratorToArray(iterator);
                assertEquals(1, events.length);
                assertEquals("EmptyPropWin", events[0].getEventType().getName());
            });

            // try fire-and-forget query
            env.compileExecuteFAFNoResult("insert into EmptyPropWin select null", path);
            env.assertIterator("window", iterator -> assertEquals(2, EPAssertionUtil.iteratorToArray(iterator).length));
            env.compileExecuteFAFNoResult("delete from EmptyPropWin", path); // empty window

            // try on-merge
            env.compileDeploy("on SupportBean_S0 merge EmptyPropWin " +
                "when not matched then insert select null", path);
            env.sendEventBean(new SupportBean_S0(0));
            env.assertIterator("window", iterator -> assertEquals(1, EPAssertionUtil.iteratorToArray(iterator).length));

            // try on-insert
            env.compileDeploy("on SupportBean_S1 insert into EmptyPropWin select null", path);
            env.sendEventBean(new SupportBean_S1(0));
            env.assertIterator("window", iterator -> assertEquals(2, EPAssertionUtil.iteratorToArray(iterator).length));

            env.undeployAll();
        }
    }

    private static class EPLInsertIntoCreateSchemaInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryAssertionInsertMap(env, true);
            tryAssertionInsertMap(env, false);
            tryAssertionInsertOA(env);
            tryAssertionInsertBean(env);
        }
    }

    private static void tryAssertionInsertBean(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create schema MyBeanWithoutProps as " + SupportBeanWithoutProps.class.getName(), path);
        env.compileDeploy("insert into MyBeanWithoutProps select null from SupportBean", path);
        env.compileDeploy("@name('s0') select * from MyBeanWithoutProps", path).addListener("s0");

        env.sendEventBean(new SupportBean());
        env.assertEventNew("s0", event -> assertTrue(event.getUnderlying() instanceof SupportBeanWithoutProps));

        env.undeployAll();
    }

    private static void tryAssertionInsertMap(RegressionEnvironment env, boolean soda) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy(soda, "create map schema EmptyMapSchema as ()", path);
        env.compileDeploy("insert into EmptyMapSchema() select null from SupportBean", path);
        env.compileDeploy("@name('s0') select * from EmptyMapSchema", path).addListener("s0");

        env.sendEventBean(new SupportBean());
        env.assertEventNew("s0", event -> {
            assertTrue(((Map) event.getUnderlying()).isEmpty());
            assertEquals(0, event.getEventType().getPropertyDescriptors().length);
        });
        env.undeployAll();
    }

    private static void tryAssertionInsertOA(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create objectarray schema EmptyOASchema()", path);
        env.compileDeploy("insert into EmptyOASchema select null from SupportBean", path);
        env.compileDeploy("@name('s0') select * from EmptyOASchema", path).addListener("s0").setSubscriber("s0");

        env.sendEventBean(new SupportBean());
        env.assertEventNew("s0", event -> assertEquals(0, ((Object[]) event.getUnderlying()).length));

        env.assertSubscriber("s0", subscriber -> {
            Object[] lastNewSubscriberData = subscriber.getLastNewData();
            assertEquals(1, lastNewSubscriberData.length);
            assertEquals(0, ((Object[]) lastNewSubscriberData[0]).length);
        });

        env.undeployAll();
    }
}
